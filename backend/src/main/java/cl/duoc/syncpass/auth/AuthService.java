package cl.duoc.syncpass.auth;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

/**
 * Unico punto que habla con AWS Cognito.
 *
 * El navegador y el telefono nunca llaman a Cognito directamente: piden a esta
 * API, que valida y devuelve los tokens. Asi el identificador del cliente y la
 * politica de errores viven en un solo lugar.
 */
@Service
public class AuthService {

    private final CognitoIdentityProviderClient cognito;
    private final String clientId;

    public AuthService(CognitoIdentityProviderClient cognito, @Value("${syncpass.client-id}") String clientId) {
        this.cognito = cognito;
        this.clientId = clientId;
    }

    /** Cognito pidio un paso extra (MFA, cambio de clave) que esta API no resuelve. */
    public static class DesafioPendiente extends RuntimeException {
        public final HttpStatus status;
        public final String code;

        DesafioPendiente(HttpStatus status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    public Map<String, Object> registrar(String username, String email, String nickname, String password) {
        var respuesta = cognito.signUp(b -> b.clientId(clientId).username(username).password(password)
            .userAttributes(
                AttributeType.builder().name("email").value(email).build(),
                AttributeType.builder().name("nickname").value(nickname).build()));
        var envio = respuesta.codeDeliveryDetails();
        return Map.of("userConfirmed", respuesta.userConfirmed(),
            "destination", envio == null ? "" : envio.destination());
    }

    public void confirmar(String username, String codigo) {
        cognito.confirmSignUp(b -> b.clientId(clientId).username(username).confirmationCode(codigo));
    }

    public void reenviarCodigo(String username) {
        cognito.resendConfirmationCode(b -> b.clientId(clientId).username(username));
    }

    public Map<String, Object> iniciarSesion(String username, String password) {
        return tokens(cognito.initiateAuth(b -> b.clientId(clientId)
            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .authParameters(Map.of("USERNAME", username, "PASSWORD", password))), null);
    }

    public Map<String, Object> renovar(String refreshToken) {
        return tokens(cognito.initiateAuth(b -> b.clientId(clientId)
            .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
            .authParameters(Map.of("REFRESH_TOKEN", refreshToken))), refreshToken);
    }

    public void cerrarSesion(String refreshToken) {
        cognito.revokeToken(b -> b.clientId(clientId).token(refreshToken));
    }

    private static Map<String, Object> tokens(InitiateAuthResponse respuesta, String refreshActual) {
        var resultado = respuesta.authenticationResult();
        if (resultado == null)
            throw new DesafioPendiente(HttpStatus.UNAUTHORIZED,
                "CHALLENGE_" + respuesta.challengeNameAsString(),
                "La cuenta requiere un paso adicional en Cognito (" + respuesta.challengeNameAsString() + ").");
        // REFRESH_TOKEN_AUTH no devuelve un refresh token nuevo: se conserva el actual.
        return Map.of("accessToken", resultado.accessToken(),
            "idToken", resultado.idToken(),
            "refreshToken", resultado.refreshToken() != null ? resultado.refreshToken() : refreshActual,
            "expiresIn", resultado.expiresIn());
    }
}
