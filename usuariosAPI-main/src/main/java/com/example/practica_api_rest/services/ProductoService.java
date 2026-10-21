package com.example.practica_api_rest.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.practica_api_rest.dtos.ActualizarStockDTO;
import com.example.practica_api_rest.dtos.ProductoDTO;
import com.example.practica_api_rest.exceptions.ConflictoException;
import com.example.practica_api_rest.exceptions.RecursoNoEncontradoException;
import com.example.practica_api_rest.exceptions.SolicitudInvalidaException;
import com.example.practica_api_rest.models.Producto;
import com.example.practica_api_rest.repositories.DetalleFacturaRepository;
import com.example.practica_api_rest.repositories.ProductoRepository;

@Service
public class ProductoService {

	private final ProductoRepository repository;
	private final DetalleFacturaRepository detalleRepository;

	public ProductoService(ProductoRepository repository, DetalleFacturaRepository detalleRepository) {
		this.repository = repository;
		this.detalleRepository = detalleRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductoDTO> listar() {
		return repository.findAll().stream().map(this::aDTO).toList();
	}

	@Transactional(readOnly = true)
	public ProductoDTO buscarPorId(Long id) {
		return repository.findById(id).map(this::aDTO)
				.orElseThrow(() -> new RecursoNoEncontradoException("Producto con id " + id + " no existe"));
	}

	@Transactional
	public ProductoDTO crear(ProductoDTO dto) {
		validar(dto);
		return aDTO(repository.save(aEntidad(dto)));
	}

	/**
	 * Actualiza el producto. Si el DTO no trae urlImagen se conserva la imagen
	 * ya almacenada, de modo que un PUT en JSON no borre la foto existente.
	 */
	@Transactional
	public ProductoDTO actualizar(Long id, ProductoDTO dto) {
		Producto producto = repository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Producto con id " + id + " no existe"));
		validar(dto);

		producto.setNombre(dto.getNombre().trim());
		producto.setDescripcion(dto.getDescripcion());
		producto.setPrecio(dto.getPrecio());
		producto.setStock(dto.getStock());
		if (dto.getUrlImagen() != null && !dto.getUrlImagen().isBlank()) {
			producto.setUrlImagen(dto.getUrlImagen());
		}

		return aDTO(repository.save(producto));
	}

	@Transactional
	public ProductoDTO aumentarStock(Long id, ActualizarStockDTO dto) {
		if (dto == null || dto.getUnidades() == null || dto.getUnidades() <= 0) {
			throw new SolicitudInvalidaException("unidades debe ser mayor que cero");
		}
		Producto producto = repository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Producto con id " + id + " no existe"));
		if (producto.getStock() > Integer.MAX_VALUE - dto.getUnidades()) {
			throw new SolicitudInvalidaException("el stock resultante supera el limite permitido");
		}
		producto.setStock(producto.getStock() + dto.getUnidades());
		return aDTO(repository.save(producto));
	}

	@Transactional
	public void eliminar(Long id) {
		if (!repository.existsById(id)) {
			throw new RecursoNoEncontradoException("Producto con id " + id + " no existe");
		}
		if (detalleRepository.existsByProducto_Id(id)) {
			throw new ConflictoException(
					"No se puede eliminar el producto con id " + id + " porque aparece en facturas");
		}
		repository.deleteById(id);
	}

	/**
	 * Valida los datos del producto. Se expone para poder validar antes de
	 * subir la imagen a S3 y no dejar archivos huerfanos en el bucket.
	 */
	public void validar(ProductoDTO dto) {
		if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()) {
			throw new SolicitudInvalidaException("nombre es obligatorio");
		}
		if (dto.getNombre().length() > 120) {
			throw new SolicitudInvalidaException("nombre supera 120 caracteres");
		}
		if (dto.getDescripcion() != null && dto.getDescripcion().length() > 500) {
			throw new SolicitudInvalidaException("descripcion supera 500 caracteres");
		}
		if (dto.getPrecio() == null || dto.getPrecio().signum() <= 0) {
			throw new SolicitudInvalidaException("precio debe ser mayor que cero");
		}
		if (dto.getPrecio().scale() > 2 || dto.getPrecio().precision() > 10) {
			throw new SolicitudInvalidaException("precio debe tener maximo 10 digitos y 2 decimales");
		}
		if (dto.getStock() == null || dto.getStock() < 0) {
			throw new SolicitudInvalidaException("stock debe ser cero o mayor");
		}
	}

	private Producto aEntidad(ProductoDTO dto) {
		Producto producto = new Producto();
		producto.setNombre(dto.getNombre().trim());
		producto.setDescripcion(dto.getDescripcion());
		producto.setPrecio(dto.getPrecio());
		producto.setStock(dto.getStock());
		producto.setUrlImagen(dto.getUrlImagen());
		return producto;
	}

	private ProductoDTO aDTO(Producto producto) {
		ProductoDTO dto = new ProductoDTO();
		dto.setId(producto.getId());
		dto.setNombre(producto.getNombre());
		dto.setDescripcion(producto.getDescripcion());
		dto.setPrecio(producto.getPrecio());
		dto.setStock(producto.getStock());
		dto.setUrlImagen(producto.getUrlImagen());
		return dto;
	}
}
