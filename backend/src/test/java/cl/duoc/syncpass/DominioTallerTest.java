package cl.duoc.syncpass;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RF12: un taller puede restringirse por rol y/o zona; la preinscripcion queda en Inscripcion. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dominio;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test", "syncpass.client-id=client"})
@AutoConfigureMockMvc
class DominioTallerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    private final org.springframework.test.web.servlet.request.RequestPostProcessor admin =
        jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));

    @BeforeEach
    void reset() {
        db.update("DELETE FROM inscripcion");
        db.update("DELETE FROM usuario_zona");
        db.update("DELETE FROM registros");
        db.update("DELETE FROM talleres");
    }

    private long crear(String rolesPermitidos, String zonaRestringida) throws Exception {
        var cuerpo = new java.util.HashMap<String, Object>();
        cuerpo.put("title", "Taller restringido");
        cuerpo.put("descripcion", "Demo");
        cuerpo.put("area", "Salud");
        cuerpo.put("unidadVecinal", "UV 1");
        cuerpo.put("comuna", "Santiago");
        cuerpo.put("direccion", "Sala");
        cuerpo.put("fechaHora", "2026-11-20T10:00");
        cuerpo.put("docenteLider", "Docente");
        cuerpo.put("cuposMaximos", 5);
        cuerpo.put("rolesPermitidos", rolesPermitidos);
        cuerpo.put("zonaRestringida", zonaRestringida);
        var creado = mvc.perform(post("/api/eventos").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor cuenta(String sub, String... roles) {
        List<org.springframework.security.core.GrantedAuthority> authorities = java.util.Arrays.stream(roles)
            .<org.springframework.security.core.GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        return jwt().jwt(j -> j.subject(sub).claim("cognito:groups", List.of(roles))).authorities(authorities);
    }

    @Test
    void tallerRestringidoPorRolRechazaAQuienNoLoTieneYAceptaAQuienSi() throws Exception {
        long id = crear("monitor,coordinador", null);

        mvc.perform(post("/api/eventos/" + id + "/ingreso/cuenta").with(cuenta("participante-1", "participante"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/eventos/" + id + "/ingreso/cuenta").with(cuenta("monitor-1", "monitor"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("presente"));
    }

    @Test
    void unInvitadoNuncaPasaUnTallerRestringidoPorRol() throws Exception {
        long id = crear("monitor", null);
        var invitado = json.writeValueAsString(Map.of("dispositivoId", UUID.randomUUID(), "nombre", "Vecino"));

        mvc.perform(post("/api/eventos/" + id + "/ingreso/invitado")
                .contentType(MediaType.APPLICATION_JSON).content(invitado))
            .andExpect(status().isForbidden());
    }

    @Test
    void tallerRestringidoPorZonaExigeLaZonaDeclaradaDelUsuario() throws Exception {
        long id = crear(null, "UV 12 Los Castaños");

        mvc.perform(post("/api/eventos/" + id + "/ingreso/cuenta").with(cuenta("sinzona-1", "participante"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());

        mvc.perform(put("/api/usuarios/conzona-1/zona").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"zona\":\"UV 12 Los Castaños\"}"))
            .andExpect(status().isOk());

        mvc.perform(post("/api/eventos/" + id + "/ingreso/cuenta").with(cuenta("conzona-1", "participante"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("presente"));
    }

    @Test
    void preinscripcionQuedaPendienteYLuegoAsisteAlAcreditarse() throws Exception {
        long id = crear(null, null);

        mvc.perform(post("/api/eventos/" + id + "/inscripcion").with(cuenta("part-2", "participante")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("pendiente"));

        mvc.perform(get("/api/eventos/" + id + "/inscripciones").with(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].estado").value("pendiente"));

        mvc.perform(post("/api/eventos/" + id + "/ingreso/cuenta").with(cuenta("part-2", "participante"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/eventos/" + id + "/inscripciones").with(admin))
            .andExpect(jsonPath("$[0].estado").value("asistio"));
    }

    @Test
    void misInscripcionesMuestraPendienteYAsistioSoloDelUsuarioActual() throws Exception {
        long id = crear(null, null);
        long otroTaller = crear(null, null);
        mvc.perform(post("/api/eventos/" + id + "/inscripcion").with(cuenta("part-3", "participante")));
        mvc.perform(post("/api/eventos/" + otroTaller + "/ingreso/cuenta").with(cuenta("part-3", "participante"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"));

        mvc.perform(get("/api/mis-inscripciones").with(cuenta("part-3", "participante")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.tallerId == " + id + ")].estado").value("pendiente"))
            .andExpect(jsonPath("$[?(@.tallerId == " + otroTaller + ")].estado").value("asistio"));

        mvc.perform(get("/api/mis-inscripciones").with(cuenta("otro-usuario", "participante")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
