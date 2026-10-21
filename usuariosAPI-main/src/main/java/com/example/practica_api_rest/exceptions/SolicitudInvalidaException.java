package com.example.practica_api_rest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * La solicitud es incorrecta o viola una regla de validacion.
 * Se responde con 400.
 */
public class SolicitudInvalidaException extends ApiException {

	public SolicitudInvalidaException(String mensaje) {
		super(HttpStatus.BAD_REQUEST, mensaje);
	}
}
