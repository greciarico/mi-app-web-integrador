package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.core.authority.SimpleGrantedAuthority; // Asegúrate de esta importación

@Service
public class UsuarioDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String dni) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByDniAndEstadoIsTrue(dni)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con DNI: " + dni));

        String tipoRol = usuario.getRolUsuario().getTipoRol().toUpperCase(); // Obtiene "VENDEDOR" o "ADMINISTRADOR"

        // Spring Security User.builder().roles() automáticamente añade "ROLE_"
        return User.builder()
                .username(usuario.getDni())
                .password(usuario.getContrasena())
                .roles(tipoRol) // Esto se convierte en ROLE_VENDEDOR o ROLE_ADMINISTRADOR
                .build();
    }
}
