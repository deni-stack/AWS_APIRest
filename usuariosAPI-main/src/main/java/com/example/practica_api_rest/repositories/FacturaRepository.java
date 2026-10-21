package com.example.practica_api_rest.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.practica_api_rest.models.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

	/** El usuario se trae en la misma consulta para evitar el N+1 al mapear el DTO. */
	@EntityGraph(attributePaths = "usuario")
	List<Factura> findAllByOrderByFechaDesc();

	@EntityGraph(attributePaths = "usuario")
	List<Factura> findByUsuario_ClaveOrderByFechaDesc(String clave);

	boolean existsByUsuario_Clave(String clave);

}
