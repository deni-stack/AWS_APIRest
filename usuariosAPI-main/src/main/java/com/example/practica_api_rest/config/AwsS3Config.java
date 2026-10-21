package com.example.practica_api_rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Construye el cliente de S3.
 *
 * Si se definen aws.s3.access-key y aws.s3.secret-key se usan credenciales
 * estaticas; si no, se delega en la cadena estandar de AWS (variables de
 * entorno, ~/.aws/credentials, perfil activo o el rol de la instancia EC2).
 * Asi la aplicacion arranca aunque falten credenciales y el error aparece
 * unicamente en el momento de subir un archivo.
 */
@Configuration
public class AwsS3Config {

	@Value("${aws.s3.access-key:}")
	private String accessKey;

	@Value("${aws.s3.secret-key:}")
	private String secretKey;

	@Value("${aws.s3.session-token:}")
	private String sessionToken;

	@Value("${aws.s3.region:us-east-2}")
	private String region;

	@Bean
	public S3Client s3Client() {
		S3ClientBuilder builder = S3Client.builder().region(Region.of(region));

		if (!accessKey.isBlank() && !secretKey.isBlank()) {
			AwsCredentials credenciales = sessionToken.isBlank()
					? AwsBasicCredentials.create(accessKey, secretKey)
					: AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
			builder.credentialsProvider(StaticCredentialsProvider.create(credenciales));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.create());
		}

		return builder.build();
	}
}
