package com.example.practica_api_rest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Fallo al comunicarse con un servicio externo de almacenamiento (AWS S3).
 * Se responde con 502 porque el error no es del cliente.
 */
public class AlmacenamientoException extends ApiException {

	public AlmacenamientoException(String mensaje) {
		super(HttpStatus.BAD_GATEWAY, mensaje);
	}

	public AlmacenamientoException(String mensaje, Throwable causa) {
		super(HttpStatus.BAD_GATEWAY, mensaje, causa);
	}
}
