package com.example.practica_api_rest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.practica_api_rest.models.DetalleFactura;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, Long> {

	boolean existsByProducto_Id(Long productoId);

}