package com.example.practica_api_rest.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.practica_api_rest.dtos.UsuarioDTO;
import com.example.practica_api_rest.exceptions.ConflictoException;
import com.example.practica_api_rest.exceptions.RecursoNoEncontradoException;
import com.example.practica_api_rest.exceptions.SolicitudInvalidaException;
import com.example.practica_api_rest.models.Usuario;
import com.example.practica_api_rest.repositories.FacturaRepository;
import com.example.practica_api_rest.repositories.UsuarioRepository;

@Service
public class UsuarioService {

	private static final int LIMITE_LONGITUD = 11;

	private final UsuarioRepository repository;
	private final FacturaRepository facturaRepository;

	public UsuarioService(UsuarioRepository repository, FacturaRepository facturaRepository) {
		this.repository = repository;
		this.facturaRepository = facturaRepository;
	}

	@Transactional(readOnly = true)
	public List<UsuarioDTO> listar() {
		return repository.findAll().stream().map(this::aDTO).toList();
	}

	@Transactional(readOnly = true)
	public UsuarioDTO buscarPorClave(String clave) {
		return repository.findById(clave).map(this::aDTO)
				.orElseThrow(() -> new RecursoNoEncontradoException("Usuario con clave '" + clave + "' no existe"));
	}

	@Transactional
	public UsuarioDTO crear(UsuarioDTO dto) {
		validar(dto);
		if (repository.existsById(dto.getClave())) {
			throw new ConflictoException("Ya existe un usuario con clave '" + dto.getClave() + "'");
		}
		Usuario guardado = repository.save(aEntidad(dto));
		return aDTO(guardado);
	}

	@Transactional
	public UsuarioDTO actualizar(String clave, UsuarioDTO dto) {
		if (!repository.existsById(clave)) {
			throw new RecursoNoEncontradoException("Usuario con clave '" + clave + "' no existe");
		}
		validar(dto);
		if (!dto.getClave().equals(clave) && repository.existsById(dto.getClave())) {
			throw new ConflictoException("Ya existe un usuario con clave '" + dto.getClave() + "'");
		}
		Usuario entidad = aEntidad(dto);
		entidad.setClave(clave);
		return aDTO(repository.save(entidad));
	}

	@Transactional
	public void eliminar(String clave) {
		if (!repository.existsById(clave)) {
			throw new RecursoNoEncontradoException("Usuario con clave '" + clave + "' no existe");
		}
		if (facturaRepository.existsByUsuario_Clave(clave)) {
			throw new ConflictoException(
					"No se puede eliminar el usuario con clave '" + clave + "' porque tiene facturas");
		}
		repository.deleteById(clave);
	}

	private void validar(UsuarioDTO dto) {
		if (dto == null || dto.getClave() == null || dto.getClave().isBlank()) {
			throw new SolicitudInvalidaException("clave es obligatoria");
		}
		if (dto.getNombre() == null || dto.getNombre().isBlank()) {
			throw new SolicitudInvalidaException("nombre es obligatorio");
		}
		if (dto.getCodigo() == null || dto.getCodigo().isBlank()) {
			throw new SolicitudInvalidaException("codigo es obligatorio");
		}
		if (dto.getClave().length() > LIMITE_LONGITUD) {
			throw new SolicitudInvalidaException("clave supera " + LIMITE_LONGITUD + " caracteres");
		}
		if (dto.getNombre().length() > LIMITE_LONGITUD) {
			throw new SolicitudInvalidaException("nombre supera " + LIMITE_LONGITUD + " caracteres");
		}
		if (dto.getCodigo().length() > LIMITE_LONGITUD) {
			throw new SolicitudInvalidaException("codigo supera " + LIMITE_LONGITUD + " caracteres");
		}
	}

	private Usuario aEntidad(UsuarioDTO dto) {
		Usuario usuario = new Usuario();
		usuario.setClave(dto.getClave());
		usuario.setNombre(dto.getNombre());
		usuario.setCodigo(dto.getCodigo());
		return usuario;
	}

	private UsuarioDTO aDTO(Usuario usuario) {
		return UsuarioDTO.de(usuario);
	}
}
