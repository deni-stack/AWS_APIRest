package com.example.practica_api_rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion web transversal. Centraliza el CORS para todos los controladores,
 * de modo que no se repita la anotacion @CrossOrigin en cada uno.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.origins:http://localhost:4200}")
	private String[] origins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(origins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
