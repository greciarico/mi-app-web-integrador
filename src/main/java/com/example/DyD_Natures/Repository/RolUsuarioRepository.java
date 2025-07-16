package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolUsuarioRepository extends JpaRepository<RolUsuario, Integer> {
    Optional<RolUsuario> findByTipoRolIgnoreCase(String tipoRol);

}
