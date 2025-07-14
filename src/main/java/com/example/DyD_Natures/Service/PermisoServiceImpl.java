package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Permiso;
import com.example.DyD_Natures.Repository.PermisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermisoServiceImpl implements PermisoService {

    private final PermisoRepository permisoRepo;

    @Autowired
    public PermisoServiceImpl(PermisoRepository permisoRepo) {
        this.permisoRepo = permisoRepo;
    }

    @Override
    public List<Permiso> listarPermisos() {
        return permisoRepo.findAll();
    }

    @Override
    public Optional<Permiso> obtenerPermisoPorId(Integer id) {
        return permisoRepo.findById(id);
    }

    @Override
    public Permiso guardarPermiso(Permiso permiso) {
        return permisoRepo.save(permiso);
    }

    @Override
    public void eliminarPermiso(Integer id) {
        permisoRepo.deleteById(id);
    }
}
