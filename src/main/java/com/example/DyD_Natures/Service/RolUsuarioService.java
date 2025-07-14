package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.RolUsuario;
import java.util.List;
import java.util.Optional;

public interface RolUsuarioService {
    List<RolUsuario> listarRoles();
    Optional<RolUsuario> obtenerRolPorId(Integer id);
    RolUsuario guardarRol(RolUsuario rol);
    void eliminarRol(Integer id);
}
