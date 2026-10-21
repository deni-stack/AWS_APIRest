package com.example.practica_api_rest.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoDTO {

	private Long id;
	private String nombre;
	private String descripcion;
	private BigDecimal precio;
	private Integer stock;
	/** URL publica del objeto en AWS S3. */
	private String urlImagen;

}
