package com.example.practica_api_rest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base de los errores de negocio de la API.
 * Cada excepcion specialized define su propio codigo HTTP.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String mensaje) {
		super(mensaje);
		this.status = status;
	}

	public ApiException(HttpStatus status, String mensaje, Throwable causa) {
		super(mensaje, causa);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
