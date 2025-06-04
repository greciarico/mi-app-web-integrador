package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Listar todos
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findByEstadoNot((byte) 2);
    }

    // Buscar por ID
    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    // Guardar o actualizar usuario
    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    // Eliminar usuario
    public void eliminarUsuario(Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setEstado((byte) 2);  // Cambiar estado a 2 = eliminado
            usuarioRepository.save(usuario);
        }
    }
    public Optional<Usuario> obtenerUsuarioPorDni(String dni) {
        return usuarioRepository.findByDni(dni);
    }

    /**
     * Verifica si un DNI ya existe en la base de datos, excluyendo un ID de usuario específico.
     * Este método es usado principalmente por el endpoint AJAX /usuarios/checkDni.
     * @param dni El número de DNI a verificar.
     * @param idUsuario El ID del usuario a excluir de la búsqueda (null para nuevas creaciones).
     * @return true si existe otro usuario con ese DNI, false en caso contrario.
     */
    public boolean existsByDniExcludingId(String dni, Integer idUsuario) {
        if (idUsuario != null) {
            return usuarioRepository.existsByDniAndIdUsuarioIsNot(dni, idUsuario);
        }
        return usuarioRepository.existsByDni(dni);
    }
}

