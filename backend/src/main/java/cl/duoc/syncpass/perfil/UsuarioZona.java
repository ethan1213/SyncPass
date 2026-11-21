package cl.duoc.syncpass.perfil;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Zona/unidad vecinal declarada para una cuenta. Cognito no tiene este atributo. */
@Entity
@Table(name = "usuario_zona")
public class UsuarioZona {
    @Id public String sub;
    public String zona;
}
