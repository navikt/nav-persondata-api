package no.nav.persondataapi.konfigurasjon

import jakarta.servlet.ServletContext
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestContextListener

@Configuration
class CustomServletInitializer {
	@Bean
	fun servletContextInitializer() =
		ServletContextInitializer { servletContext: ServletContext ->
			servletContext.addListener(RequestContextListener())
		}
}
