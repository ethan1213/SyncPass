package cl.duoc.syncpass;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean JwtDecoder decoder(@Value("${syncpass.issuer}") String issuer,@Value("${syncpass.client-id}") String clientId) {
   var decoder = NimbusJwtDecoder.withJwkSetUri(issuer+"/.well-known/jwks.json").build();
   decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), accessValidator(clientId)));
   return decoder;
 }
 static OAuth2TokenValidator<Jwt> accessValidator(String clientId) {
   return jwt -> "access".equals(jwt.getClaimAsString("token_use")) && clientId.equals(jwt.getClaimAsString("client_id"))
     ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
 }
 @Bean SecurityFilterChain filter(HttpSecurity http) throws Exception {
   var groups = new JwtGrantedAuthoritiesConverter();
   groups.setAuthoritiesClaimName("cognito:groups"); groups.setAuthorityPrefix("ROLE_");
   var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(groups);
   return http.csrf(c -> c.disable()).cors(c -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health").permitAll()
      .requestMatchers(HttpMethod.GET,"/api/eventos","/api/eventos/*").permitAll()
      .requestMatchers(HttpMethod.POST,"/api/auth/*").permitAll()
      .anyRequest().authenticated())
    .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))).build();
 }
 @Bean CorsConfigurationSource cors(@Value("${syncpass.origins}") String origins) {
   var config = new CorsConfiguration(); config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
   config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
   config.setAllowedHeaders(List.of("Authorization","Content-Type"));
   var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config); return source;
 }
}
