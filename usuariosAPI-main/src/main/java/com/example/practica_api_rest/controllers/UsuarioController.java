package com.example.practica_api_rest.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.practica_api_rest.dtos.UsuarioDTO;
import com.example.practica_api_rest.services.UsuarioService;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@GetMapping
	public List<UsuarioDTO> listar() {
		return service.listar();
	}

	@GetMapping("/{clave}")
	public UsuarioDTO buscar(@PathVariable String clave) {
		return service.buscarPorClave(clave);
	}

	@PostMapping
	public ResponseEntity<UsuarioDTO> crear(@RequestBody UsuarioDTO dto) {
		UsuarioDTO creado = service.crear(dto);
		return ResponseEntity.created(URI.create("/usuarios/" + creado.getClave())).body(creado);
	}

	@PutMapping("/{clave}")
	public UsuarioDTO actualizar(@PathVariable String clave, @RequestBody UsuarioDTO dto) {
		return service.actualizar(clave, dto);
	}

	@DeleteMapping("/{clave}")
	public ResponseEntity<Void> eliminar(@PathVariable String clave) {
		service.eliminar(clave);
		return ResponseEntity.noContent().build();
	}

}
