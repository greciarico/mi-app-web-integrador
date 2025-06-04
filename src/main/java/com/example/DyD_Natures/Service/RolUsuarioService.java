package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Repository.RolUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolUsuarioService {

    @Autowired
    private RolUsuarioRepository rolUsuarioRepository;

    public List<RolUsuario> listarRoles() {
        return rolUsuarioRepository.findAll();
    }
}


