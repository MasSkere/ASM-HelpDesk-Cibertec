package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.response.UsuarioResponse;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Convertir una entidad Usuario a una respuesta segura
    private UsuarioResponse convertirRespuesta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getEstado()
        );
    }

    // Consultar todos los usuarios
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // Consultar un usuario por su ID
    public UsuarioResponse buscarPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "No se encontro el usuario con ID: " + id
                        )
                );

        return convertirRespuesta(usuario);
    }
}
