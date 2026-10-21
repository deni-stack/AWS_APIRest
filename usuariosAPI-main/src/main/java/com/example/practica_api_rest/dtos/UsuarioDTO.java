package com.example.practica_api_rest.dtos;

import lombok.Data;

import com.example.practica_api_rest.models.Usuario;

@Data
public class UsuarioDTO {

	private String clave;
	private String nombre;
	private String codigo;

	/** Construye el DTO a partir de la entidad. */
	public static UsuarioDTO de(Usuario usuario) {
		UsuarioDTO dto = new UsuarioDTO();
		dto.setClave(usuario.getClave());
		dto.setNombre(usuario.getNombre());
		dto.setCodigo(usuario.getCodigo());
		return dto;
	}

}
