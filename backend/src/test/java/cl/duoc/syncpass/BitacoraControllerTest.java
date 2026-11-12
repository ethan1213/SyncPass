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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:bitacora;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client"})
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
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
 }
}
