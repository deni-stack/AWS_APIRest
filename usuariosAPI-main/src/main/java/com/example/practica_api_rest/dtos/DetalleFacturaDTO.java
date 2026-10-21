package com.example.practica_api_rest.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DetalleFacturaDTO {

	private Long productoId;
	private String nombreProducto;
	private Integer cantidad;
	private BigDecimal precioUnitario;
	private BigDecimal subtotal;

}