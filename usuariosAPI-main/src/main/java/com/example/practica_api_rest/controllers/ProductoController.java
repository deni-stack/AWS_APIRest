package com.example.practica_api_rest.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.practica_api_rest.dtos.ActualizarStockDTO;
import com.example.practica_api_rest.dtos.ProductoDTO;
import com.example.practica_api_rest.exceptions.SolicitudInvalidaException;
import com.example.practica_api_rest.services.ProductoService;
import com.example.practica_api_rest.services.S3Service;

@RestController
@RequestMapping("/productos")
public class ProductoController {

	private final ProductoService service;
	private final S3Service s3Service;

	public ProductoController(ProductoService service, S3Service s3Service) {
		this.service = service;
		this.s3Service = s3Service;
	}

	@GetMapping
	public List<ProductoDTO> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	public ProductoDTO buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	/**
	 * Registra un producto con su imagen. La imagen es obligatoria: se envia
	 * como multipart/form-data con las partes 'producto' (JSON) e 'imagen'.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductoDTO> crear(@RequestPart("producto") ProductoDTO dto,
			@RequestPart("imagen") MultipartFile imagen) {
		// Se valida antes de subir la imagen para no dejar archivos huerfanos.
		service.validar(dto);

		String urlImagen = s3Service.subirImagen(imagen);
		dto.setUrlImagen(urlImagen);
		try {
			ProductoDTO creado = service.crear(dto);
			return ResponseEntity.created(URI.create("/productos/" + creado.getId())).body(creado);
		} catch (RuntimeException excepcion) {
			limpiarImagen(urlImagen);
			throw excepcion;
		}
	}

	/**
	 * La imagen no puede enviarse en JSON, por lo que se responde con un
	 * mensaje explicativo en lugar de un 415 generico.
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProductoDTO> crearSinImagen(@RequestBody ProductoDTO dto) {
		throw new SolicitudInvalidaException(
				"La imagen del producto es obligatoria. Envia la solicitud como multipart/form-data "
						+ "con las partes 'producto' (JSON) e 'imagen' (archivo).");
	}

	/**
	 * Actualiza un producto. Acepta JSON (se conserva la imagen actual) o
	 * multipart con una imagen nueva, que reemplaza a la anterior.
	 */
	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductoDTO actualizarConImagen(@PathVariable Long id,
			@RequestPart("producto") ProductoDTO dto,
			@RequestPart(value = "imagen", required = false) MultipartFile imagen) {
		String urlAnterior = service.buscarPorId(id).getUrlImagen();
		boolean hayImagenNueva = imagen != null && !imagen.isEmpty();

		// Se valida antes de subir la imagen para no dejar archivos huerfanos.
		service.validar(dto);

		String urlNueva = null;
		if (hayImagenNueva) {
			urlNueva = s3Service.subirImagen(imagen);
			dto.setUrlImagen(urlNueva);
		}

		ProductoDTO actualizado;
		try {
			actualizado = service.actualizar(id, dto);
		} catch (RuntimeException excepcion) {
			// El producto no se guardo: la imagen nueva queda huerfana y se limpia.
			if (urlNueva != null) {
				limpiarImagen(urlNueva);
			}
			throw excepcion;
		}

		// El producto ya apunta a la imagen nueva, asi que la anterior se puede
		// eliminar. Es best-effort: si falla, el producto conserva su imagen valida.
		if (urlNueva != null && urlAnterior != null && !urlAnterior.equals(urlNueva)) {
			limpiarImagen(urlAnterior);
		}
		return actualizado;
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ProductoDTO actualizar(@PathVariable Long id, @RequestBody ProductoDTO dto) {
		return service.actualizar(id, dto);
	}

	@PutMapping("/{id}/stock")
	public ProductoDTO aumentarStock(@PathVariable Long id, @RequestBody ActualizarStockDTO dto) {
		return service.aumentarStock(id, dto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		service.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Best-effort: la limpieza de un archivo huerfano no debe enmascarar el
	 * error que provoco la operacion fallida.
	 */
	private void limpiarImagen(String url) {
		try {
			s3Service.eliminarImagen(url);
		} catch (RuntimeException excepcion) {
			// Se ignora: el error original es el relevante.
		}
	}
}
