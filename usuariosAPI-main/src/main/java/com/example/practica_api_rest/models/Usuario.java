package com.example.practica_api_rest.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

	@Id
	@Column(name = "clave", length = 11)
	private String clave;

	@Column(name = "nombre", length = 11)
	private String nombre;

	@Column(name = "codigo", length = 11)
	private String codigo;

}
