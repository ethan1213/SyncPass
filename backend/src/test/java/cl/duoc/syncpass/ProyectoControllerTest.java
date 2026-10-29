package cl.duoc.syncpass;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:projects;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client"})
@AutoConfigureMockMvc
class ProyectoControllerTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @MockitoBean CognitoIdentityProviderClient cognito;
 private final String project="{\"title\":\"Huerto comunitario\",\"descripcion\":\"Talleres barriales\",\"area\":\"Medioambiente\",\"unidadVecinal\":\"UV 12\",\"fechaInicio\":\"2026-10-30\",\"fechaTermino\":\"2026-12-30\",\"estado\":\"planificado\"}";
 private org.springframework.test.web.servlet.request.RequestPostProcessor admin(){return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));}
 @Test void adminCanCreateReadUpdateAndDeleteProject()throws Exception{
  var created=mvc.perform(post("/api/proyectos").with(admin()).contentType(MediaType.APPLICATION_JSON).content(project))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber()).andReturn();
  long id=json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(get("/api/proyectos/"+id).with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Huerto comunitario"));
  mvc.perform(put("/api/proyectos/"+id).with(admin()).contentType(MediaType.APPLICATION_JSON)
    .content(project.replace("\"planificado\"","\"activo\"").replace("\"estado\":","\"version\":0,\"estado\":")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("activo"));
  mvc.perform(delete("/api/proyectos/"+id).with(admin())).andExpect(status().isNoContent());
  mvc.perform(get("/api/proyectos/"+id).with(admin())).andExpect(status().isNotFound());
 }
 @Test void monitorCannotUseProjectMaintainer()throws Exception{
  mvc.perform(get("/api/proyectos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_monitor"))))
   .andExpect(status().isForbidden());
 }
}
