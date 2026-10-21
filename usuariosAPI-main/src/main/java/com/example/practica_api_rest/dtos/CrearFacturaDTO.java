package com.example.practica_api_rest.dtos;

import java.util.List;

import lombok.Data;

@Data
public class CrearFacturaDTO {

	private String usuarioClave;
	private List<ItemFacturaDTO> productos;

}