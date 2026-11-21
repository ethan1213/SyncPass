package cl.duoc.syncpass;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:ingreso;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa","spring.datasource.password=",
 "syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client"})
@AutoConfigureMockMvc
class IngresoControllerTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;

 @Test void guestAndAccountAreIdempotentAndRespectCapacity() throws Exception {
  var body="{\"title\":\"Taller de prueba\",\"descripcion\":\"Demo\",\"area\":\"Salud\",\"unidadVecinal\":\"UV 1\",\"comuna\":\"Santiago\",\"direccion\":\"Sala\",\"fechaHora\":\"2026-11-12T10:00\",\"docenteLider\":\"Docente\",\"cuposMaximos\":2}";
  var created=mvc.perform(post("/api/eventos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
   .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
  long id=json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  String path="/api/eventos/"+id+"/ingreso";
  String guest=json.writeValueAsString(java.util.Map.of("dispositivoId",UUID.randomUUID(),"nombre","Invitado Demo"));
  mvc.perform(post(path+"/invitado").contentType(MediaType.APPLICATION_JSON).content(guest))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("presente"));
  mvc.perform(post(path+"/invitado").contentType(MediaType.APPLICATION_JSON).content(guest))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ya_presente"));
  mvc.perform(post(path+"/cuenta").contentType(MediaType.APPLICATION_JSON).content("{}"))
   .andExpect(status().isUnauthorized());
  mvc.perform(post(path+"/cuenta").with(jwt().jwt(j->j.subject("user-1")))
   .contentType(MediaType.APPLICATION_JSON).content("{}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.invitado").value(false));
  mvc.perform(post(path+"/invitado").contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(java.util.Map.of("dispositivoId",UUID.randomUUID(),"nombre","Otro"))))
   .andExpect(status().isConflict());
  mvc.perform(get("/api/eventos/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.inscritos").value(2));
 }
}
