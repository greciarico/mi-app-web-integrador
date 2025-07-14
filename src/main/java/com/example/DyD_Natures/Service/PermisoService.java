package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Permiso;
import java.util.List;
import java.util.Optional;

public interface PermisoService {
    List<Permiso> listarPermisos();
    Optional<Permiso> obtenerPermisoPorId(Integer id);
    Permiso guardarPermiso(Permiso permiso);
    void eliminarPermiso(Integer id);
}
