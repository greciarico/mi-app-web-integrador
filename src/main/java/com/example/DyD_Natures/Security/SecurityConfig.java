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
                        //    Esto es crucial para que el JavaScript pueda cargar la tabla inicial
                        //    incluso antes de que el usuario haga login o si hay filtros activos en páginas de inicio.
                        .requestMatchers("/usuarios/all", "/usuarios/checkDni").permitAll()
                        .requestMatchers("/productos/all").permitAll()
                        .requestMatchers("/proveedores/all", "/proveedores/checkRuc").permitAll()
                        .requestMatchers("/categorias/all", "/categorias/checkNombreCategoria").permitAll()
                        .requestMatchers("/igv/all").permitAll()
                        // =================================================================
                        // NUEVO: Rutas de Empresa para AJAX y vistas generales (ADMINISTRADOR)
                        // =================================================================
                        .requestMatchers("/informacion-empresa/data").permitAll() // Para que el JS cargue los datos iniciales
                        .requestMatchers("/informacion-empresa/checkRuc").permitAll() // Para la validación de RUC
                        .requestMatchers("/informacion-empresa/**").hasRole("ADMINISTRADOR") // Todas las demás operaciones CRUD/vista
                        // =================================================================

                        // 3. Rutas con roles específicos (más restrictivas van después de las public permitAll)
                        // Página de inicio (root)
                        .requestMatchers("/").hasRole("ADMINISTRADOR") // Solo ADMINISTRADOR para la página de inicio

                        // Módulos de Mantenimiento (generalmente para ADMINISTRADOR)
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR") // CRUD completo de usuarios
                        .requestMatchers("/productos/**").hasRole("ADMINISTRADOR") // CRUD completo de productos
                        .requestMatchers("/proveedores/**").hasRole("ADMINISTRADOR") // CRUD completo de proveedores
                        .requestMatchers("/categorias/**").hasRole("ADMINISTRADOR") // CRUD completo de categorías
                        .requestMatchers("/igv/**").hasRole("ADMINISTRADOR") // CRUD completo de IGV
                        .requestMatchers("/mantenimiento/clientes/**").hasRole("ADMINISTRADOR") // Mantenimiento de clientes
                        .requestMatchers("/mantenimiento/merma/**").hasRole("ADMINISTRADOR") // Mantenimiento de Merma

                        // Módulos de Registro de Compra y Venta (Ventas puede ser para VENDEDOR también)
                        .requestMatchers("/registro-compra/**").hasRole("ADMINISTRADOR") // Registro de compra
                        .requestMatchers("/registro-venta/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR") // Registro de venta
                        .requestMatchers("/ventas/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                        // Información de la Empresa (ya manejado arriba con hasRole("ADMINISTRADOR"))
                        // .requestMatchers("/informacion-empresa").authenticated() // Se cambia a hasRole("ADMINISTRADOR") arriba

                        // 4. Cualquier otra solicitud DEBE estar autenticada (regla general al final)
                        .anyRequest().authenticated()
                )
                // Configuración del formulario de login
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                // Configuración del logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                // Deshabilita CSRF (considera habilitarlo en producción con las configuraciones adecuadas)
                .csrf(csrf -> csrf.disable());

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