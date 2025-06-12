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

    // Inyecta tu manejador de éxito personalizado
    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Permite acceso público a recursos estáticos y páginas de autenticación
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/plugins/**", "/dist/**", "/img/**").permitAll()

                        // 2. Permite acceso público a los endpoints AJAX para la carga inicial de datos y validaciones
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
                        // La raíz / SOLO puede ser accedida por ADMINISTRADOR
                        .requestMatchers("/").hasRole("ADMINISTRADOR")

                        // La página /vendedor solo para VENDEDOR
                        .requestMatchers("/vendedor").hasRole("VENDEDOR")

                        // Módulos de Mantenimiento (ADMINISTRADOR)
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/productos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/proveedores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/categorias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/igv/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/merma/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/clientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/informacion-empresa/**").hasRole("ADMINISTRADOR")

                        // Módulos de Venta (accesibles por ambos roles)
                        .requestMatchers("/registro-venta/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers("/ventas/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                        // 4. Cualquier otra solicitud DEBE estar autenticada (regla general al final)
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        // Aquí usamos el manejador de éxito personalizado
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll() // Permite a todos acceder a la página de login
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL para cerrar sesión
                        .logoutSuccessUrl("/login?logout") // Redirige a la página de login con un parámetro después del logout
                        .permitAll() // Permite a todos cerrar sesión
                )
                .csrf(csrf -> csrf.disable()); // Deshabilita CSRF por simplicidad; en producción, configúralo adecuadamente.

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
