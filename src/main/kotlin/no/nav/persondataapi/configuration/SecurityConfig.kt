package no.nav.persondataapi.configuration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http {
        authorizeExchange {
            authorize("/actuator/**", permitAll)
            authorize("/internal/**", permitAll)
            authorize(anyExchange, authenticated)
        }
        oauth2ResourceServer {
            jwt {
                jwtAuthenticationConverter = ReactiveJwtAuthenticationConverterAdapter(jwtConverter())
            }
        }
    }

    fun jwtConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        // Optionally map custom authorities from roles etc.
        converter.setPrincipalClaimName("NAVident")
        return converter
    }
}
