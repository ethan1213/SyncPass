package cl.duoc.syncpass;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client"})
@AutoConfigureMockMvc
class ApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate db;
 @BeforeEach void reset(){db.update("DELETE FROM registros");db.update("DELETE FROM talleres");}
 String taller(){return "{\"title\":\"Taller ficticio\",\"descripcion\":\"Prueba académica\",\"area\":\"Tecnología\",\"unidadVecinal\":\"UV Demo\",\"comuna\":\"Comuna Demo\",\"direccion\":\"Recinto ficticio\",\"fechaHora\":\"2026-10-20T10:00\",\"docenteLider\":\"Coordinador Demo\",\"cuposMaximos\":20}";}
 long create()throws Exception{var body=mvc.perform(post("/api/eventos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_coordinador"))).contentType(MediaType.APPLICATION_JSON).content(taller())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return json.readTree(body).get("id").asLong();}
 @Test void publicReadButMutationsNeedRole()throws Exception{
  mvc.perform(get("/api/eventos")).andExpect(status().isOk());
  mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(taller())).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/eventos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_participante"))).contentType(MediaType.APPLICATION_JSON).content(taller())).andExpect(status().isForbidden());
 }
 @Test void crudAndStaleVersion()throws Exception{
  long id=create();var data=json.readTree(taller());((com.fasterxml.jackson.databind.node.ObjectNode)data).put("version",0).put("title","Taller actualizado");
  mvc.perform(put("/api/eventos/"+id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))).contentType(MediaType.APPLICATION_JSON).content(data.toString())).andExpect(status().isOk()).andExpect(jsonPath("version").value(1));
  mvc.perform(put("/api/eventos/"+id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))).contentType(MediaType.APPLICATION_JSON).content(data.toString())).andExpect(status().isConflict());
  mvc.perform(delete("/api/eventos/"+id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))).andExpect(status().isNoContent());
  mvc.perform(get("/api/eventos/"+id)).andExpect(status().isNotFound());
 }
 String batch(long id,String recordId,String type,int rating)throws Exception{return json.writeValueAsString(Map.of("registros",List.of(Map.of("id",recordId,"tipo",type,"tallerId",id,"participante","PART-DEMO","rating",rating,"timestamp",Instant.now().minusSeconds(1).toString()))));}
 @Test void retryDoesNotDuplicateAndParticipantCannotMarkAttendance()throws Exception{
  long id=create();String recordId=UUID.randomUUID().toString();String body=batch(id,recordId,"encuesta",5);
  var auth=jwt().jwt(j->j.subject("owner").claim("cognito:groups",List.of("participante"))).authorities(new SimpleGrantedAuthority("ROLE_participante"));
  for(int i=0;i<2;i++)mvc.perform(post("/api/sync/batch").with(auth).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("acceptedIds[0]").value(recordId));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM registros",Integer.class));
  mvc.perform(post("/api/sync/batch").with(auth).contentType(MediaType.APPLICATION_JSON).content(batch(id,UUID.randomUUID().toString(),"asistencia",5))).andExpect(status().isForbidden());
  mvc.perform(post("/api/sync/batch").with(auth).contentType(MediaType.APPLICATION_JSON).content(batch(id,UUID.randomUUID().toString(),"encuesta",6))).andExpect(status().isBadRequest());
  mvc.perform(delete("/api/eventos/"+id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))).andExpect(status().isConflict());
 }
 @Test void tokenMustBeAccessAndForOurClient(){
  var validator=SecurityConfig.accessValidator("our-client");
  var token=Jwt.withTokenValue("test").header("alg","RS256").subject("user").claim("client_id","our-client").claim("token_use","access").build();
  assertFalse(validator.validate(token).hasErrors());
  var wrong=Jwt.withTokenValue("test").header("alg","RS256").subject("user").claim("client_id","other").claim("token_use","access").build();
  assertTrue(validator.validate(wrong).hasErrors());
  var id=Jwt.withTokenValue("test").header("alg","RS256").subject("user").claim("client_id","our-client").claim("token_use","id").build();
  assertTrue(validator.validate(id).hasErrors());
 }
}
