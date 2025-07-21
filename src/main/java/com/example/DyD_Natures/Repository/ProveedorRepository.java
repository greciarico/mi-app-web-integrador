package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; 
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer>, JpaSpecificationExecutor<Proveedor> { 

    @Query("SELECT p FROM Proveedor p WHERE p.estado <> :estadoExcluido")
    List<Proveedor> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);

    List<Proveedor> findByEstado(Byte estado);

    Optional<Proveedor> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdProveedorIsNot(String ruc, Integer idProveedor);


    boolean existsByRucAndEstadoNot(String ruc, Byte estadoExcluido);


    boolean existsByRucAndIdProveedorIsNotAndEstadoNot(String ruc, Integer idProveedor, Byte estadoExcluido);

    Optional<Proveedor> findByRucAndEstado(String ruc, Byte estado);

}

