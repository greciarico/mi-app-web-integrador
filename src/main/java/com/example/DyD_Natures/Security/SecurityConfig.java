package com.example.DyD_Natures.Security;

import com.example.DyD_Natures.Service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioDetailsService uds;

    public SecurityConfig(UsuarioDetailsService uds) {
        this.uds = uds;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable())) // h2-console
                .authorizeHttpRequests(auth -> auth
                        // públicos
                        .requestMatchers("/login","/logout","/css/**","/js/**","/img/**","/h2-console/**").permitAll()



                        // 1) rutas AJAX que deben ser públicas
                        .requestMatchers("/productos/all").permitAll()
                        .requestMatchers("/clientes/all").permitAll()
                        .requestMatchers("/tasa/all").permitAll()

                        // Endponts activos
                        .requestMatchers("/productos/activos").permitAll()
                        .requestMatchers("/tasa/activos").permitAll()
                        .requestMatchers("/clientes/activos").permitAll()

                        // permisos
                        .requestMatchers("/tasa/**").hasAuthority("VER_IGV")
                        .requestMatchers("/usuarios/**").hasAuthority("VER_USUARIOS")
                        .requestMatchers("/roles/**").hasAuthority("GESTION_ROLES")
                        .requestMatchers("/clientes/**").hasAuthority("VER_CLIENTES")
                        .requestMatchers("/proveedores/**").hasAuthority("VER_PROVEEDORES")
                        .requestMatchers("/productos/**").hasAuthority("VER_PRODUCTOS")
                        .requestMatchers("/categorias/**").hasAuthority("VER_CATEGORIAS")
                        .requestMatchers("/merma/**").hasAuthority("VER_MERMA")
                        .requestMatchers("/documento-compra/**").hasAuthority("VER_COMPRAS")
                        .requestMatchers("/ventas/**").hasAuthority("VER_VENTAS")
                        .requestMatchers("/informacion-empresa/**").hasAuthority("VER_INFO_EMPRESA")
                        // dashboard raíz
                        .requestMatchers("/").authenticated()

                        // todo lo demás, autenticado
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("dni")
                        .passwordParameter("contrasena")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }
}
