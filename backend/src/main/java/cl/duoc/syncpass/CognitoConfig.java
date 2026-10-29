package cl.duoc.syncpass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
@Configuration
public class CognitoConfig {
 // SignUp, ConfirmSignUp, InitiateAuth y RevokeToken son operaciones publicas del cliente de la app:
 // no requieren credenciales IAM en EC2 (las del laboratorio expiran).
 @Bean CognitoIdentityProviderClient cognitoClient(@Value("${syncpass.region}") String region) {
   return CognitoIdentityProviderClient.builder().region(Region.of(region))
     .credentialsProvider(AnonymousCredentialsProvider.create()).httpClient(UrlConnectionHttpClient.create()).build();
 }
}
