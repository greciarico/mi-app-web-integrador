package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Usuario;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDetailsService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String dni) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository
                .findByDniAndEstadoIsTrue(dni)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con DNI: " + dni));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1) El rol, con prefijo ROLE_
        String tipoRol = usuario.getRolUsuario().getTipoRol().toUpperCase();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + tipoRol));

        // 2) Cada permiso como autoridad plana
        usuario.getRolUsuario().getPermisos().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getNombre()))
        );

        // **3) Loguea las authorities para verificar en consola**
        log.info("Cargando usuario {} con authorities: {}", usuario.getDni(), authorities);

        // 4) Devuelve el UserDetails con todos los authorities
        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getDni())
                .password(usuario.getContrasena())
                .authorities(authorities)
                .build();
    }
}
