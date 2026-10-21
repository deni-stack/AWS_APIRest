package com.example.practica_api_rest.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facturas")
@Getter
@Setter
public class Factura {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_clave", nullable = false)
	private Usuario usuario;

	@Column(nullable = false)
	private LocalDateTime fecha;

	/** Suma de los productos antes de aplicar el IVA. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	/** Impuesto al valor agregado aplicado a esta factura. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal iva;

	/** Porcentaje de IVA aplicado (se guarda por factura para no alterar el historico). */
	@Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
	private BigDecimal ivaPorcentaje;

	/** Subtotal + IVA: importe final a cobrar. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DetalleFactura> detalles = new ArrayList<>();

}