package cl.duoc.syncpass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
/**
 * Registro e inicio de sesion mediante la API: el navegador solo habla con API Gateway -> EC2,
 * y este JAR es quien llama a Cognito. Las rutas protegidas siguen validando el access token.
 */
@RestController @RequestMapping("/api/auth")
public class AuthController {
 public record RegisterRequest(@NotBlank @Size(min=3,max=64) @Pattern(regexp="[\\w.@+-]+") String username,
   @NotBlank @Email String email,@NotBlank @Size(max=40) String nickname,@NotBlank @Size(min=8,max=128) String password) {}
 public record ConfirmRequest(@NotBlank String username,@NotBlank @Pattern(regexp="\\d{4,8}") String code) {}
 public record UsernameRequest(@NotBlank String username) {}
 public record LoginRequest(@NotBlank String username,@NotBlank String password) {}
 public record RefreshRequest(@NotBlank String refreshToken) {}
 private final CognitoIdentityProviderClient cognito; private final String clientId;
 AuthController(CognitoIdentityProviderClient cognito,@Value("${syncpass.client-id}") String clientId){this.cognito=cognito;this.clientId=clientId;}

 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> register(@Valid @RequestBody RegisterRequest r) {
   var res=cognito.signUp(b->b.clientId(clientId).username(r.username()).password(r.password())
     .userAttributes(AttributeType.builder().name("email").value(r.email()).build(),AttributeType.builder().name("nickname").value(r.nickname()).build()));
   var delivery=res.codeDeliveryDetails();
   return Map.of("userConfirmed",res.userConfirmed(),"destination",delivery==null?"":delivery.destination());
 }
 @PostMapping("/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void confirm(@Valid @RequestBody ConfirmRequest r) { cognito.confirmSignUp(b->b.clientId(clientId).username(r.username()).confirmationCode(r.code())); }
 @PostMapping("/resend") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void resend(@Valid @RequestBody UsernameRequest r) { cognito.resendConfirmationCode(b->b.clientId(clientId).username(r.username())); }
 @PostMapping("/login")
 public Map<String,Object> login(@Valid @RequestBody LoginRequest r) {
   return tokens(cognito.initiateAuth(b->b.clientId(clientId).authFlow(AuthFlowType.USER_PASSWORD_AUTH).authParameters(Map.of("USERNAME",r.username(),"PASSWORD",r.password()))),null);
 }
 @PostMapping("/refresh")
 public Map<String,Object> refresh(@Valid @RequestBody RefreshRequest r) {
   return tokens(cognito.initiateAuth(b->b.clientId(clientId).authFlow(AuthFlowType.REFRESH_TOKEN_AUTH).authParameters(Map.of("REFRESH_TOKEN",r.refreshToken()))),r.refreshToken());
 }
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void logout(@Valid @RequestBody RefreshRequest r) { cognito.revokeToken(b->b.clientId(clientId).token(r.refreshToken())); }

 private static Map<String,Object> tokens(InitiateAuthResponse res,String currentRefresh) {
   var t=res.authenticationResult();
   if(t==null)throw new AuthChallengeException(HttpStatus.UNAUTHORIZED,"CHALLENGE_"+res.challengeNameAsString(),"La cuenta requiere un paso adicional en Cognito ("+res.challengeNameAsString()+").");
   // REFRESH_TOKEN_AUTH no devuelve un refresh token nuevo: se conserva el actual.
   return Map.of("accessToken",t.accessToken(),"idToken",t.idToken(),"refreshToken",t.refreshToken()!=null?t.refreshToken():currentRefresh,"expiresIn",t.expiresIn());
 }
 static class AuthChallengeException extends RuntimeException {
   final HttpStatus status; final String code;
   AuthChallengeException(HttpStatus status,String code,String message){super(message);this.status=status;this.code=code;}
 }
 /** Traduce los errores de Cognito a respuestas claras sin filtrar si un usuario existe o no al iniciar sesion. */
 @ExceptionHandler(CognitoIdentityProviderException.class)
 ResponseEntity<?> cognitoError(CognitoIdentityProviderException ex) {
   return switch(ex) {
     case UsernameExistsException e -> error(HttpStatus.CONFLICT,"USERNAME_EXISTS","Ese nombre de usuario ya está registrado.");
     case InvalidPasswordException e -> error(HttpStatus.BAD_REQUEST,"INVALID_PASSWORD","La contraseña no cumple la política: mínimo 8 caracteres con mayúscula, minúscula, número y símbolo.");
     case CodeMismatchException e -> error(HttpStatus.BAD_REQUEST,"CODE_MISMATCH","El código de verificación no es correcto.");
     case ExpiredCodeException e -> error(HttpStatus.BAD_REQUEST,"CODE_EXPIRED","El código expiró. Solicita uno nuevo.");
     case UserNotConfirmedException e -> error(HttpStatus.FORBIDDEN,"USER_NOT_CONFIRMED","Confirma tu correo con el código antes de iniciar sesión.");
     case NotAuthorizedException e -> error(HttpStatus.UNAUTHORIZED,"NOT_AUTHORIZED","Usuario o contraseña incorrectos.");
     case UserNotFoundException e -> error(HttpStatus.UNAUTHORIZED,"NOT_AUTHORIZED","Usuario o contraseña incorrectos.");
     case TooManyRequestsException e -> error(HttpStatus.TOO_MANY_REQUESTS,"TOO_MANY_REQUESTS","Demasiados intentos. Espera un momento.");
     case LimitExceededException e -> error(HttpStatus.TOO_MANY_REQUESTS,"TOO_MANY_REQUESTS","Demasiados intentos. Espera un momento.");
     case InvalidParameterException e when String.valueOf(ex.awsErrorDetails().errorMessage()).contains("flow not enabled") ->
       error(HttpStatus.SERVICE_UNAVAILABLE,"AUTH_FLOW_DISABLED","El cliente de Cognito no tiene habilitado ALLOW_USER_PASSWORD_AUTH.");
     case InvalidParameterException e -> error(HttpStatus.BAD_REQUEST,"INVALID_PARAMETER","Cognito rechazó los datos: "+ex.awsErrorDetails().errorMessage());
     default -> error(HttpStatus.BAD_GATEWAY,"COGNITO_ERROR","Cognito no pudo completar la operación.");
   };
 }
 @ExceptionHandler(AuthChallengeException.class)
 ResponseEntity<?> challenge(AuthChallengeException ex) { return error(ex.status,ex.code,ex.getMessage()); }
 private static ResponseEntity<?> error(HttpStatus status,String code,String message) { return ResponseEntity.status(status).body(Map.of("code",code,"message",message)); }
}
