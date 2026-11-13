package cl.duoc.syncpass.config;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores tecnicos a un cuerpo JSON uniforme para los tres clientes. */
@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<?> conflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("code", "CONFLICT",
            "message", "El registro está relacionado con otros datos o fue modificado. Recarga y revisa la operación."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldError();
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION",
            "message", field == null ? "Datos inválidos."
                : "Campo '" + field.getField() + "' inválido: " + field.getDefaultMessage()));
    }
}
