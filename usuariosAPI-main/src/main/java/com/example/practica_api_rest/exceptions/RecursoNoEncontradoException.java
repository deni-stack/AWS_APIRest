package com.example.practica_api_rest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * El recurso solicitado no existe. Se responde con 404.
 */
public class RecursoNoEncontradoException extends ApiException {

	public RecursoNoEncontradoException(String mensaje) {
		super(HttpStatus.NOT_FOUND, mensaje);
	}
}
