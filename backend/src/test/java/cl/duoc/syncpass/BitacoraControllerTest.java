package cl.duoc.syncpass;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:bitacora;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client","GEMINI_API_KEY="})
@AutoConfigureMockMvc
class BitacoraControllerTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 private org.springframework.test.web.servlet.request.RequestPostProcessor role(String name) {
  return jwt().authorities(new SimpleGrantedAuthority("ROLE_"+name));
 }
 private long workshop() throws Exception {
  var body="{\"title\":\"Taller de reciclaje\",\"descripcion\":\"Prueba\",\"area\":\"Medioambiente\",\"unidadVecinal\":\"UV Demo\",\"comuna\":\"Demo\",\"direccion\":\"Recinto\",\"fechaHora\":\"2026-11-12T10:00\",\"docenteLider\":\"Docente\",\"cuposMaximos\":20}";
  var response=mvc.perform(post("/api/eventos").with(role("admin")).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isCreated()).andReturn();
  return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
 }
 @Test void sharedBitacoraHasSummaryAndGroundedAnswer() throws Exception {
  long id=workshop();
  var path="/api/eventos/"+id+"/bitacora";
  var notes=json.writeValueAsString(java.util.Map.of("contenido","Separamos plástico y papel. Lavamos los envases antes de reciclar. El cartón se deposita seco."));
  mvc.perform(put(path).with(role("participante")).contentType(MediaType.APPLICATION_JSON).content(notes)).andExpect(status().isForbidden());
  mvc.perform(put(path).with(role("coordinador")).contentType(MediaType.APPLICATION_JSON).content(notes))
   .andExpect(status().isOk()).andExpect(jsonPath("$.fragmentos.length()").value(3));
  mvc.perform(get(path).with(role("participante"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.resumen").value(org.hamcrest.Matchers.containsString("plástico")));
  mvc.perform(post(path+"/preguntas").with(role("participante")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"pregunta\":\"¿Qué hacemos con los envases?\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.fragmento").value(2))
   .andExpect(jsonPath("$.cita").value("Lavamos los envases antes de reciclar."));
  mvc.perform(post(path+"/preguntas").with(role("participante")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"pregunta\":\"¿Qué aprendimos de astronomía?\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.fragmento").value(0));
  mvc.perform(post(path+"/preguntas").with(role("participante")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"pregunta\":\"¡Hola! ¿Qué tal?\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.origen").value("conversacion"))
   .andExpect(jsonPath("$.respuesta").value(org.hamcrest.Matchers.startsWith("¡Hola!")));
  mvc.perform(post(path+"/preguntas").with(role("participante")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"pregunta\":\"¿Puedes resumir la sesión?\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.origen").value("resumen_publicado"))
   .andExpect(jsonPath("$.respuesta").value(org.hamcrest.Matchers.containsString("plástico")));
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
 }

 @Test void draftRequiresStaffAndPublicationIsSeparate() throws Exception {
  var path="/api/eventos/"+workshop()+"/bitacora";
  var body=json.writeValueAsString(java.util.Map.of(
   "contenido","El cartón se deposita seco.", "autorizaProveedor",true));
  mvc.perform(post(path+"/borrador").with(role("participante")).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isForbidden());
  mvc.perform(post(path+"/borrador").with(role("coordinador")).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isOk()).andExpect(jsonPath("$.origen").value("extractivo"));
  mvc.perform(get(path).with(role("participante"))).andExpect(status().isNotFound());
  var aprobado=json.writeValueAsString(java.util.Map.of(
   "contenido","El cartón se deposita seco.", "resumenRevisado","Depositar el cartón seco."));
  mvc.perform(put(path).with(role("coordinador")).contentType(MediaType.APPLICATION_JSON).content(aprobado))
   .andExpect(status().isOk());
  mvc.perform(get(path).with(role("participante")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.resumen").value("Depositar el cartón seco."));
 }

 @Test void audioRequiresAuthorizationAndConfiguredProvider() throws Exception {
  var path="/api/eventos/"+workshop()+"/bitacora/transcripcion";
  var audio=new org.springframework.mock.web.MockMultipartFile("audio","demo.wav","audio/wav",new byte[]{1});
  mvc.perform(multipart(path).file(audio).with(role("participante")).param("autorizaProveedor","true"))
   .andExpect(status().isForbidden());
  mvc.perform(multipart(path).file(audio).with(role("admin"))).andExpect(status().isBadRequest());
  mvc.perform(multipart(path).file(audio).with(role("admin")).param("autorizaProveedor","true"))
   .andExpect(status().isServiceUnavailable());
 }

 @Test void secondAccountSeesPublishedWorkshopAndUpdatedSummary() throws Exception {
  long id=workshop();
  var path="/api/eventos/"+id+"/bitacora";
  var first=jwt().jwt(j->j.subject("usuario-web")).authorities(new SimpleGrantedAuthority("ROLE_coordinador"));
  var second=jwt().jwt(j->j.subject("usuario-android")).authorities(new SimpleGrantedAuthority("ROLE_coordinador"));
  mvc.perform(get("/api/eventos").with(second)).andExpect(status().isOk())
   .andExpect(jsonPath("$[?(@.id == "+id+")].title").value(org.hamcrest.Matchers.hasItem("Taller de reciclaje")));
  mvc.perform(get(path).with(second)).andExpect(status().isNotFound());
  for (String material : java.util.List.of("papel", "cartón")) {
   String fuente="El material aceptado es "+material+".";
   var body=json.writeValueAsString(java.util.Map.of("contenido",fuente,"resumenRevisado","Material: "+material));
   mvc.perform(put(path).with(first).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
   mvc.perform(get(path).with(second)).andExpect(status().isOk())
    .andExpect(jsonPath("$.resumen").value("Material: "+material));
   mvc.perform(post(path+"/preguntas").with(second).contentType(MediaType.APPLICATION_JSON)
    .content("{\"pregunta\":\"¿Qué material se acepta?\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.cita").value(fuente));
  }
 }
}
