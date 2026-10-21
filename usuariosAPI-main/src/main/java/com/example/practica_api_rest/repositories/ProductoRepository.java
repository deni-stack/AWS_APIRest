package com.example.practica_api_rest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.practica_api_rest.models.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

}