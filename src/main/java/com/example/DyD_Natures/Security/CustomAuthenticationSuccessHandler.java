package com.example.DyD_Natures.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();



        boolean isVendedor = authorities.stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_VENDEDOR")); // Debe coincidir con ROLE_VENDEDOR

        String redirectUrl;
        if (isVendedor) {
            redirectUrl = "/vendedor";
        } else {
            redirectUrl = "/";
        }

        if (!response.isCommitted()) {
            response.sendRedirect(redirectUrl);
        }
    }
}
