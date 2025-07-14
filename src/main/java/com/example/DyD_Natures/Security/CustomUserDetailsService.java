package com.example.DyD_Natures.Security;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioService usuarioService;

    @Autowired
    public CustomUserDetailsService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String dni) throws UsernameNotFoundException {
        // 1) Busca el usuario por DNI (username)
        Usuario usuario = usuarioService
                .obtenerUsuarioPorDni(dni)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con DNI: " + dni));

        // 2) Extrae los permisos del rol y los convierte en GrantedAuthority
        Set<GrantedAuthority> authorities = usuario.getRolUsuario()
                .getPermisos()
                .stream()
                .map(permiso -> new SimpleGrantedAuthority(permiso.getNombre()))
                .collect(Collectors.toSet());

        // 3) Crea y devuelve el UserDetails de Spring Security
        return User.builder()
                .username(usuario.getDni())
                .password(usuario.getContrasena())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(usuario.getEstado() != 1)  // estado=1 → habilitado
                .build();
    }
}
