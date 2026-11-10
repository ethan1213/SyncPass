package cl.duoc.syncpass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
@Configuration
public class CognitoConfig {
 // AdminAddUserToGroup requiere el rol IAM de la instancia. El proveedor por defecto
 // obtiene credenciales temporales del perfil de EC2 sin guardarlas en el proyecto.
 @Bean CognitoIdentityProviderClient cognitoClient(@Value("${syncpass.region}") String region) {
   return CognitoIdentityProviderClient.builder().region(Region.of(region))
     .credentialsProvider(DefaultCredentialsProvider.create()).httpClient(UrlConnectionHttpClient.create()).build();
 }
}
