package com.example.practica_api_rest.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.practica_api_rest.exceptions.AlmacenamientoException;
import com.example.practica_api_rest.exceptions.SolicitudInvalidaException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Sube y elimina las imagenes de los productos en el bucket de AWS S3.
 *
 * El bucket debe permitir lectura publica de los objetos (politica
 * PublicReadGetObject) para que las URLs devueltas se puedan mostrar
 * directamente en el navegador.
 */
@Service
public class S3Service {

	private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

	private static final String CARPETA = "productos/";

	/** Firmas binarias de los formatos admitidos, indexadas por content type. */
	private static final Map<String, String> TIPOS_ADMITIDOS = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp",
			"image/gif", "gif");

	private final S3Client s3Client;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.s3.region}")
	private String region;

	public S3Service(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	/**
	 * Sube la imagen y devuelve la URL publica del objeto.
	 *
	 * @throws SolicitudInvalidaException si el archivo no es una imagen valida
	 * @throws AlmacenamientoException     si S3 no acepta la subida
	 */
	public String subirImagen(MultipartFile archivo) {
		String tipo = validarYDetectarTipo(archivo);
		String clave = CARPETA + UUID.randomUUID() + "." + TIPOS_ADMITIDOS.get(tipo);

		try (InputStream contenido = archivo.getInputStream()) {
			s3Client.putObject(
					PutObjectRequest.builder()
							.bucket(bucketName)
							.key(clave)
							.contentType(tipo)
							.build(),
					RequestBody.fromInputStream(contenido, archivo.getSize()));
		} catch (IOException | S3Exception excepcion) {
			throw new AlmacenamientoException("No se pudo subir la imagen a S3: " + excepcion.getMessage(), excepcion);
		}

		return urlPublica(clave);
	}

	/**
	 * Elimina el objeto de S3 al que apunta una URL publica.
	 * Las URLs que no pertenecen a este bucket se ignoran.
	 */
	public void eliminarImagen(String url) {
		if (url == null || url.isBlank()) {
			return;
		}

		String clave = claveDesdeUrl(url);
		if (clave == null) {
			return;
		}

		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(clave).build());
		} catch (S3Exception excepcion) {
			throw new AlmacenamientoException("No se pudo eliminar la imagen anterior de S3: " + excepcion.getMessage(),
					excepcion);
		}
	}

	private String urlPublica(String clave) {
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, clave);
	}

	/**
	 * Valida el archivo y devuelve el content type real deducido de su
	 * contenido binario, no del cabecera enviado por el cliente.
	 */
	private String validarYDetectarTipo(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new SolicitudInvalidaException("La imagen del producto es obligatoria");
		}
		if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
			throw new SolicitudInvalidaException("La imagen supera el tamano maximo de 5 MB");
		}

		String tipo = detectarTipo(archivo);
		if (tipo == null) {
			throw new SolicitudInvalidaException(
					"El archivo debe ser una imagen JPG, PNG, WEBP o GIF valida");
		}
		return tipo;
	}

	private String detectarTipo(MultipartFile archivo) {
		byte[] cabecera = new byte[12];
		int leidos;
		try (InputStream contenido = archivo.getInputStream()) {
			leidos = contenido.read(cabecera);
		} catch (IOException excepcion) {
			throw new SolicitudInvalidaException("No se pudo leer el archivo de la imagen");
		}
		if (leidos < 4) {
			return null;
		}

		if (leidos >= 3 && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8
				&& (cabecera[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (leidos >= 8 && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P' && cabecera[2] == 'N'
				&& cabecera[3] == 'G' && (cabecera[4] & 0xFF) == 0x0D && (cabecera[5] & 0xFF) == 0x0A
				&& (cabecera[6] & 0xFF) == 0x1A && (cabecera[7] & 0xFF) == 0x0A) {
			return "image/png";
		}
		if (leidos >= 6 && cabecera[0] == 'G' && cabecera[1] == 'I' && cabecera[2] == 'F' && cabecera[3] == '8'
				&& (cabecera[4] == '7' || cabecera[4] == '9') && cabecera[5] == 'a') {
			return "image/gif";
		}
		if (leidos >= 12 && cabecera[0] == 'R' && cabecera[1] == 'I' && cabecera[2] == 'F' && cabecera[3] == 'F'
				&& cabecera[8] == 'W' && cabecera[9] == 'E' && cabecera[10] == 'B' && cabecera[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	/**
	 * Extrae la clave del objeto a partir de una URL publica.
	 * Devuelve null si la URL no apunta a este bucket.
	 */
	private String claveDesdeUrl(String url) {
		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException excepcion) {
			return null;
		}

		String host = uri.getHost();
		boolean esNuestroBucket = host != null
				&& (host.equals(bucketName + ".s3." + region + ".amazonaws.com")
						|| host.equals(bucketName + ".s3.amazonaws.com")
						|| host.equals("s3." + region + ".amazonaws.com"));
		if (!esNuestroBucket) {
			return null;
		}

		String ruta = uri.getPath();
		if (ruta == null || ruta.length() <= 1) {
			return null;
		}
		return ruta.substring(1);
	}
}
