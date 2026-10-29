package cl.duoc.syncpass;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","syncpass.issuer=https://cognito-idp.us-east-1.amazonaws.com/test","syncpass.client-id=client"})
@AutoConfigureMockMvc
class AuthTest {
 @Autowired MockMvc mvc; @MockitoBean CognitoIdentityProviderClient cognito;
 String login="{\"username\":\"demo\",\"password\":\"Clave-Segura1\"}";
 @SuppressWarnings("unchecked") @Test void registerGoesThroughApiWithoutToken()throws Exception{
  when(cognito.signUp(any(Consumer.class))).thenReturn(SignUpResponse.builder().userConfirmed(false).codeDeliveryDetails(CodeDeliveryDetailsType.builder().destination("d***@e***").build()).build());
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\",\"email\":\"demo@example.com\",\"nickname\":\"Demo\",\"password\":\"Clave-Segura1\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("userConfirmed").value(false)).andExpect(jsonPath("destination").value("d***@e***"));
 }
 @Test void invalidRegisterIsRejectedBeforeCognito()throws Exception{
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\",\"email\":\"no-es-correo\",\"nickname\":\"Demo\",\"password\":\"Clave-Segura1\"}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("VALIDATION"));
  verifyNoInteractions(cognito);
 }
 @SuppressWarnings("unchecked") @Test void loginReturnsCognitoTokens()throws Exception{
  when(cognito.initiateAuth(any(Consumer.class))).thenReturn(InitiateAuthResponse.builder().authenticationResult(AuthenticationResultType.builder().accessToken("a").idToken("i").refreshToken("r").expiresIn(3600).build()).build());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
   .andExpect(status().isOk()).andExpect(jsonPath("accessToken").value("a")).andExpect(jsonPath("refreshToken").value("r")).andExpect(jsonPath("expiresIn").value(3600));
 }
 @SuppressWarnings("unchecked") @Test void wrongPasswordAndUnknownUserLookTheSame()throws Exception{
  when(cognito.initiateAuth(any(Consumer.class))).thenThrow(NotAuthorizedException.builder().message("Incorrect").build()).thenThrow(UserNotFoundException.builder().message("nope").build());
  for(int i=0;i<2;i++)mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("NOT_AUTHORIZED"));
 }
 @SuppressWarnings("unchecked") @Test void unconfirmedUserIsToldToConfirm()throws Exception{
  when(cognito.initiateAuth(any(Consumer.class))).thenThrow(UserNotConfirmedException.builder().message("x").build());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isForbidden()).andExpect(jsonPath("code").value("USER_NOT_CONFIRMED"));
 }
}
