package cl.duoc.syncpass.auth;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

/**
 * Registro e inicio de sesion mediante la API: el navegador solo habla con
 * API Gateway -> EC2, y este JAR es quien llama a Cognito. Las rutas protegidas
 * siguen validando el access token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[\\w.@+-]+") String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(max = 40) String nickname,
                                  @NotBlank @Size(min = 8, max = 128) String password) {}

    public record ConfirmRequest(@NotBlank String username, @NotBlank @Pattern(regexp = "\\d{4,8}") String code) {}

    public record UsernameRequest(@NotBlank String username) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest r) {
        return service.registrar(r.username(), r.email(), r.nickname(), r.password());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody ConfirmRequest r) {
        service.confirmar(r.username(), r.code());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@Valid @RequestBody UsernameRequest r) {
        service.reenviarCodigo(r.username());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest r) {
        return service.iniciarSesion(r.username(), r.password());
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@Valid @RequestBody RefreshRequest r) {
        return service.renovar(r.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest r) {
        service.cerrarSesion(r.refreshToken());
    }

    /**
     * Traduce los errores de Cognito a respuestas claras, sin revelar al iniciar
     * sesion si un usuario existe o no.
     */
    @ExceptionHandler(CognitoIdentityProviderException.class)
    ResponseEntity<?> cognitoError(CognitoIdentityProviderException ex) {
        return switch (ex) {
            case UsernameExistsException e ->
                error(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Ese nombre de usuario ya está registrado.");
            case InvalidPasswordException e ->
                error(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "La contraseña no cumple la política: mínimo 8 caracteres con mayúscula, minúscula, número y símbolo.");
            case CodeMismatchException e ->
                error(HttpStatus.BAD_REQUEST, "CODE_MISMATCH", "El código de verificación no es correcto.");
            case ExpiredCodeException e ->
                error(HttpStatus.BAD_REQUEST, "CODE_EXPIRED", "El código expiró. Solicita uno nuevo.");
            case UserNotConfirmedException e ->
                error(HttpStatus.FORBIDDEN, "USER_NOT_CONFIRMED",
                    "Confirma tu correo con el código antes de iniciar sesión.");
            case NotAuthorizedException e ->
                error(HttpStatus.UNAUTHORIZED, "NOT_AUTHORIZED", "Usuario o contraseña incorrectos.");
            case UserNotFoundException e ->
                error(HttpStatus.UNAUTHORIZED, "NOT_AUTHORIZED", "Usuario o contraseña incorrectos.");
            case TooManyRequestsException e ->
                error(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Demasiados intentos. Espera un momento.");
            case LimitExceededException e ->
                error(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Demasiados intentos. Espera un momento.");
            case InvalidParameterException e
                when String.valueOf(ex.awsErrorDetails().errorMessage()).contains("flow not enabled") ->
                error(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_FLOW_DISABLED",
                    "El cliente de Cognito no tiene habilitado ALLOW_USER_PASSWORD_AUTH.");
            case InvalidParameterException e ->
                error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                    "Cognito rechazó los datos: " + ex.awsErrorDetails().errorMessage());
            default -> error(HttpStatus.BAD_GATEWAY, "COGNITO_ERROR", "Cognito no pudo completar la operación.");
        };
    }

    @ExceptionHandler(AuthService.DesafioPendiente.class)
    ResponseEntity<?> challenge(AuthService.DesafioPendiente ex) {
        return error(ex.status, ex.code, ex.getMessage());
    }

    private static ResponseEntity<?> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
