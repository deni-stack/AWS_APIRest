package com.example.practica_api_rest.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.practica_api_rest.dtos.CrearFacturaDTO;
import com.example.practica_api_rest.dtos.FacturaDTO;
import com.example.practica_api_rest.services.FacturaService;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

	private final FacturaService service;

	public FacturaController(FacturaService service) {
		this.service = service;
	}

	@GetMapping
	public List<FacturaDTO> listar(@RequestParam(required = false) String usuarioClave) {
		return service.listar(usuarioClave);
	}

	@GetMapping("/{id}")
	public FacturaDTO buscar(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	public ResponseEntity<FacturaDTO> crear(@RequestBody CrearFacturaDTO dto) {
		FacturaDTO creada = service.crear(dto);
		return ResponseEntity.created(URI.create("/facturas/" + creada.getId())).body(creada);
	}

}