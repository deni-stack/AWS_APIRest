package com.example.practica_api_rest.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.practica_api_rest.dtos.CrearFacturaDTO;
import com.example.practica_api_rest.dtos.DetalleFacturaDTO;
import com.example.practica_api_rest.dtos.FacturaDTO;
import com.example.practica_api_rest.dtos.ItemFacturaDTO;
import com.example.practica_api_rest.dtos.UsuarioDTO;
import com.example.practica_api_rest.exceptions.ConflictoException;
import com.example.practica_api_rest.exceptions.RecursoNoEncontradoException;
import com.example.practica_api_rest.exceptions.SolicitudInvalidaException;
import com.example.practica_api_rest.models.DetalleFactura;
import com.example.practica_api_rest.models.Factura;
import com.example.practica_api_rest.models.Producto;
import com.example.practica_api_rest.models.Usuario;
import com.example.practica_api_rest.repositories.FacturaRepository;
import com.example.practica_api_rest.repositories.ProductoRepository;
import com.example.practica_api_rest.repositories.UsuarioRepository;

@Service
public class FacturaService {

	private static final BigDecimal CIEN = new BigDecimal("100");

	private final FacturaRepository repository;
	private final UsuarioRepository usuarioRepository;
	private final ProductoRepository productoRepository;

	/** Porcentaje de IVA configurado en app.iva.porcentaje (12 por defecto). */
	@Value("${app.iva.porcentaje:12}")
	private BigDecimal porcentajeIva;

	public FacturaService(FacturaRepository repository, UsuarioRepository usuarioRepository,
			ProductoRepository productoRepository) {
		this.repository = repository;
		this.usuarioRepository = usuarioRepository;
		this.productoRepository = productoRepository;
	}

	@Transactional(readOnly = true)
	public List<FacturaDTO> listar(String usuarioClave) {
		List<Factura> facturas = usuarioClave == null || usuarioClave.isBlank()
				? repository.findAllByOrderByFechaDesc()
				: repository.findByUsuario_ClaveOrderByFechaDesc(usuarioClave);
		return facturas.stream().map(this::aDTO).toList();
	}

	@Transactional(readOnly = true)
	public FacturaDTO buscarPorId(Long id) {
		return repository.findById(id).map(this::aDTO)
				.orElseThrow(() -> new RecursoNoEncontradoException("Factura con id " + id + " no existe"));
	}

	@Transactional
	public FacturaDTO crear(CrearFacturaDTO dto) {
		if (dto == null || dto.getUsuarioClave() == null || dto.getUsuarioClave().isBlank()) {
			throw new SolicitudInvalidaException("usuarioClave es obligatorio");
		}
		if (dto.getProductos() == null || dto.getProductos().isEmpty()) {
			throw new SolicitudInvalidaException("la factura debe incluir al menos un producto");
		}

		Usuario usuario = usuarioRepository.findById(dto.getUsuarioClave())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Usuario con clave '" + dto.getUsuarioClave() + "' no existe"));
		Factura factura = new Factura();
		factura.setUsuario(usuario);
		factura.setFecha(LocalDateTime.now());

		Set<Long> productosAgregados = new HashSet<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		for (ItemFacturaDTO item : dto.getProductos()) {
			if (item == null || item.getProductoId() == null) {
				throw new SolicitudInvalidaException("productoId es obligatorio para cada producto");
			}
			if (item.getCantidad() == null || item.getCantidad() <= 0) {
				throw new SolicitudInvalidaException("la cantidad debe ser mayor que cero");
			}
			if (!productosAgregados.add(item.getProductoId())) {
				throw new SolicitudInvalidaException("no repita productos; indique la cantidad total en una sola linea");
			}

			Producto producto = productoRepository.findById(item.getProductoId())
					.orElseThrow(() -> new RecursoNoEncontradoException(
							"Producto con id " + item.getProductoId() + " no existe"));
			if (producto.getStock() == null || producto.getStock() < item.getCantidad()) {
				throw new ConflictoException("No hay stock suficiente para el producto con id " + producto.getId());
			}

			producto.setStock(producto.getStock() - item.getCantidad());
			DetalleFactura detalle = new DetalleFactura();
			detalle.setFactura(factura);
			detalle.setProducto(producto);
			detalle.setCantidad(item.getCantidad());
			detalle.setPrecioUnitario(producto.getPrecio());
			factura.getDetalles().add(detalle);
			subtotal = subtotal.add(producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
		}

		BigDecimal iva = calcularIva(subtotal);
		factura.setSubtotal(subtotal);
		factura.setIva(iva);
		factura.setIvaPorcentaje(porcentajeIva);
		factura.setTotal(subtotal.add(iva));
		return aDTO(repository.save(factura));
	}

	/** IVA del subtotal, redondeado a dos decimales. */
	private BigDecimal calcularIva(BigDecimal subtotal) {
		return subtotal.multiply(porcentajeIva).divide(CIEN, 2, RoundingMode.HALF_UP);
	}

	private FacturaDTO aDTO(Factura factura) {
		FacturaDTO dto = new FacturaDTO();
		dto.setId(factura.getId());
		dto.setUsuarioClave(factura.getUsuario().getClave());
		dto.setUsuario(UsuarioDTO.de(factura.getUsuario()));
		dto.setFecha(factura.getFecha());
		dto.setSubtotal(factura.getSubtotal());
		dto.setIva(factura.getIva());
		dto.setIvaPorcentaje(factura.getIvaPorcentaje());
		dto.setTotal(factura.getTotal());
		dto.setProductos(factura.getDetalles().stream().map(this::aDetalleDTO).toList());
		return dto;
	}

	private DetalleFacturaDTO aDetalleDTO(DetalleFactura detalle) {
		DetalleFacturaDTO dto = new DetalleFacturaDTO();
		dto.setProductoId(detalle.getProducto().getId());
		dto.setNombreProducto(detalle.getProducto().getNombre());
		dto.setCantidad(detalle.getCantidad());
		dto.setPrecioUnitario(detalle.getPrecioUnitario());
		dto.setSubtotal(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
		return dto;
	}
}
