package com.example.practica_api_rest.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.example.practica_api_rest.exceptions.ApiException;

/**
 * Traduce cualquier excepcion a una respuesta JSON homogenea:
 * {"detail": "...", "message": "..."}
 *
 * El frontend Angular lee {@code detail} y {@code message}, por lo que ambos
 * campos se devuelven siempre con el mismo texto.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> manejarApi(ApiException excepcion) {
		return responder(excepcion.getStatus(), excepcion.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> manejarArchivoDemasiadoGrande(MaxUploadSizeExceededException excepcion) {
		return responder(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamano maximo permitido de 5 MB");
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<Map<String, String>> manejarParteFaltante(MissingServletRequestPartException excepcion) {
		return responder(HttpStatus.BAD_REQUEST,
				"Falta la parte '" + excepcion.getRequestPartName() + "' en la solicitud multipart");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException excepcion) {
		String detalle = excepcion.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("La solicitud no es valida");
		return responder(HttpStatus.BAD_REQUEST, detalle);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> manejarCuerpoInvalido(HttpMessageNotReadableException excepcion) {
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido");
	}

	/**
	 * Cualquier error inesperado. Las excepciones que Spring ya sabe mapear
	 * (ErrorResponseException) conservan su propio codigo HTTP.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> manejarInesperado(Exception excepcion) {
		if (excepcion instanceof ErrorResponseException errorResponse) {
			HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
			String detalle = errorResponse.getBody() != null && errorResponse.getBody().getDetail() != null
					? errorResponse.getBody().getDetail()
					: excepcion.getMessage();
			return responder(status, detalle);
		}
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
	}

	private ResponseEntity<Map<String, String>> responder(HttpStatus status, String detalle) {
		Map<String, String> cuerpo = new LinkedHashMap<>();
		cuerpo.put("detail", detalle);
		cuerpo.put("message", detalle);
		return ResponseEntity.status(status).body(cuerpo);
	}
}
