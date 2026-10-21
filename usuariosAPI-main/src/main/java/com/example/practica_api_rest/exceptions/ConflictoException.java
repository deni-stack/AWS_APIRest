package com.example.practica_api_rest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * La operacion choca con el estado actual del recurso (duplicado o recurso en uso).
 * Se responde con 409.
 */
public class ConflictoException extends ApiException {

	public ConflictoException(String mensaje) {
		super(HttpStatus.CONFLICT, mensaje);
	}
}
