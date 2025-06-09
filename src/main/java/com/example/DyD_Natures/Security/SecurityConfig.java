package com.example.DyD_Natures.Security; // Asegúrate de que el paquete sea correcto

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Permite acceso público a recursos estáticos y páginas de autenticación
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/plugins/**", "/dist/**", "/img/**").permitAll()

                        // 2. Permite acceso público a los endpoints AJAX para la carga inicial de datos y validaciones
                        // Estos endpoints usualmente son llamados por JS en las páginas, y la página en sí puede requerir autenticación.
                        // PermitAll es útil para carga inicial de datos. Si necesitas más seguridad, se puede restringir.
                        .requestMatchers("/usuarios/all", "/usuarios/checkDni").permitAll()
                        .requestMatchers("/productos/all").permitAll()
                        .requestMatchers("/proveedores/all", "/proveedores/checkRuc").permitAll()
                        .requestMatchers("/categorias/all", "/categorias/checkNombreCategoria").permitAll()
                        .requestMatchers("/igv/all").permitAll()
                        .requestMatchers("/informacion-empresa/data").permitAll()
                        .requestMatchers("/informacion-empresa/checkRuc").permitAll()
                        .requestMatchers("/merma/all").permitAll()
                        .requestMatchers("/merma/productos").permitAll()
                        .requestMatchers("/clientes/all").permitAll()
                        .requestMatchers("/clientes/tipos").permitAll()
                        .requestMatchers("/clientes/checkDni").permitAll()
                        .requestMatchers("/clientes/checkRuc").permitAll()
                        // Rutas de Documento de Compra
                        .requestMatchers("/documento-compra/all").permitAll() // Para cargar la tabla principal
                        .requestMatchers("/documento-compra/**").hasRole("ADMINISTRADOR") // Todas las demás operaciones CRUD de compra

                        // 3. Rutas con roles específicos (más restrictivas van después de las public permitAll)
                        .requestMatchers("/").hasRole("ADMINISTRADOR") // Página de inicio o dashboard

                        // Módulos de Mantenimiento (ADMINISTRADOR)
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/productos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/proveedores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/categorias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/igv/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/merma/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/clientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/informacion-empresa/**").hasRole("ADMINISTRADOR")

                        // Módulos de Venta
                        // La ruta principal /ventas y sus sub-rutas (all, nuevo, editar, visualizar, guardar)
                        // requieren roles de ADMINISTRADOR o VENDEDOR.
                        // Si '/ventas/all' necesitara ser totalmente público (sin autenticación),
                        // entonces se añadiría una línea específica para ella antes de la general de /ventas/**.
                        // Pero con el diseño actual, el usuario ya estará loggeado para acceder a /ventas.
                        .requestMatchers("/registro-venta/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR") // Si tienes una página de "registro-venta" separada
                        .requestMatchers("/ventas/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR") // Incluye /ventas/all, /ventas/nuevo, /ventas/editar, /ventas/visualizar, /ventas/guardar

                        // 4. Cualquier otra solicitud DEBE estar autenticada (regla general al final)
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // Redirige al dashboard después del login
                        .permitAll() // Permite a todos acceder a la página de login
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL para cerrar sesión
                        .logoutSuccessUrl("/login?logout") // Redirige a la página de login con un parámetro después del logout
                        .permitAll() // Permite a todos cerrar sesión
                )
                .csrf(csrf -> csrf.disable()); // Deshabilita CSRF por simplicidad en desarrollo; en producción, configúralo adecuadamente.

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
