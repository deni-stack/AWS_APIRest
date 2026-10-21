package com.example.practica_api_rest.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class FacturaDTO {

	private Long id;
	private String usuarioClave;
	/** Datos del cliente al que se emitio la factura. */
	private UsuarioDTO usuario;
	private LocalDateTime fecha;
	/** Suma de los productos antes de IVA. */
	private BigDecimal subtotal;
	/** Importe del IVA aplicado. */
	private BigDecimal iva;
	/** Porcentaje de IVA que se aplico a esta factura. */
	private BigDecimal ivaPorcentaje;
	/** Subtotal + IVA. */
	private BigDecimal total;
	private List<DetalleFacturaDTO> productos;

}
