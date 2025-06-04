package com.example.DyD_Natures.Security;

import com.example.DyD_Natures.Service.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/plugins/**", "/dist/**").permitAll()

                        .requestMatchers("/").hasRole("ADMINISTRADOR")
                        .requestMatchers("/ventas/").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .anyRequest().permitAll()
                )
                .formLogin(login -> login
                        .loginPage("/login") // vista de login si la tienes personalizada
                        .defaultSuccessUrl("/", true) // redirige siempre a /layout al iniciar sesión
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // ... tus otras reglas (plugins, dist, login, etc.)
                        .requestMatchers("/usuarios/all").permitAll() // ¡Esta línea es CRÍTICA!
                        .anyRequest().authenticated()
                )
        // ... formLogin, logout, csrf
        ;
        return http.build();
    }

}
