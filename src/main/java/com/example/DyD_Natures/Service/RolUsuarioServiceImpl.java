package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.RolUsuario;
import com.example.DyD_Natures.Repository.RolUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RolUsuarioServiceImpl implements RolUsuarioService {

    private final RolUsuarioRepository rolRepo;

    @Autowired
    public RolUsuarioServiceImpl(RolUsuarioRepository rolRepo) {
        this.rolRepo = rolRepo;
    }

    @Override
    public List<RolUsuario> listarRoles() {
        return rolRepo.findAll();
    }

    @Override
    public Optional<RolUsuario> obtenerRolPorId(Integer id) {
        return rolRepo.findById(id);
    }

    @Override
    public RolUsuario guardarRol(RolUsuario rol) {
        return rolRepo.save(rol);
    }

    @Override
    public void eliminarRol(Integer id) {
        rolRepo.deleteById(id);
    }
}
