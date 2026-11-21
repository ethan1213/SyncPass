package cl.duoc.syncpass;

import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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

/** Reglas del mantenedor que protegen el historial y los filtros del catalogo. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:crud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test", "syncpass.client-id=client"})
@AutoConfigureMockMvc
class TallerCrudTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    private final org.springframework.test.web.servlet.request.RequestPostProcessor admin =
        jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));

    @BeforeEach
    void reset() {
        db.update("DELETE FROM registros");
        db.update("DELETE FROM talleres");
        db.update("DELETE FROM proyectos_comunitarios");
    }

    private String taller(String titulo, String comuna, String fecha) {
        return """
               {"title":"%s","descripcion":"Demo","area":"Salud","unidadVecinal":"UV 1","comuna":"%s",
                "direccion":"Sala","fechaHora":"%s","docenteLider":"Docente","cuposMaximos":5}
               """.formatted(titulo, comuna, fecha);
    }

    private long crear(String cuerpo) throws Exception {
        var creado = mvc.perform(post("/api/eventos").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void catalogoFiltraPorComunaYTextoYLlegaOrdenado() throws Exception {
        crear(taller("Huerto urbano", "La Florida", "2026-11-20T10:00"));
        crear(taller("Taller de reciclaje", "Puente Alto", "2026-11-10T10:00"));

        mvc.perform(get("/api/eventos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Taller de reciclaje"));

        mvc.perform(get("/api/eventos").param("comuna", "la florida"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Huerto urbano"));

        mvc.perform(get("/api/eventos").param("q", "reciclaje"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void noSeBorraUnTallerConAsistenciaNiSeDejanMenosCuposQueInscritos() throws Exception {
        long id = crear(taller("Primeros auxilios", "Santiago", "2026-11-15T10:00"));
        var invitado = json.writeValueAsString(Map.of("dispositivoId", UUID.randomUUID(), "nombre", "Vecina"));
        mvc.perform(post("/api/eventos/" + id + "/ingreso/invitado")
                .contentType(MediaType.APPLICATION_JSON).content(invitado))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/eventos/" + id).with(admin)).andExpect(status().isConflict());

        var actual = json.readTree(mvc.perform(get("/api/eventos/" + id))
            .andReturn().getResponse().getContentAsString());
        var sinCupos = (com.fasterxml.jackson.databind.node.ObjectNode) actual;
        sinCupos.put("cuposMaximos", 0);
        mvc.perform(put("/api/eventos/" + id).with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(sinCupos.toString()))
            .andExpect(status().isBadRequest()); // 0 cupos no pasa la validación de entrada

        sinCupos.put("cuposMaximos", 1);
        mvc.perform(put("/api/eventos/" + id).with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(sinCupos.toString()))
            .andExpect(status().isOk());
    }

    @Test
    void noSeBorraUnProyectoConTalleresAsociados() throws Exception {
        var proyecto = """
                       {"title":"Barrio activo","descripcion":"Intervención","area":"Salud",
                        "unidadVecinal":"UV 1","fechaInicio":"2026-10-01","fechaTermino":"2026-12-01",
                        "estado":"activo"}
                       """;
        var creado = mvc.perform(post("/api/proyectos").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(proyecto))
            .andExpect(status().isCreated()).andReturn();
        long proyectoId = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        var conProyecto = (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(taller("Taller del proyecto", "Santiago", "2026-11-18T10:00"));
        conProyecto.put("proyectoId", proyectoId);
        crear(conProyecto.toString());

        mvc.perform(delete("/api/proyectos/" + proyectoId).with(admin)).andExpect(status().isConflict());

        mvc.perform(get("/api/eventos").param("proyectoId", String.valueOf(proyectoId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
