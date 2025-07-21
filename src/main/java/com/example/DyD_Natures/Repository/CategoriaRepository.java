package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;  
import org.springframework.data.repository.query.Param;  
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;  


import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer>, JpaSpecificationExecutor<Categoria> {  

    List<Categoria> findByEstado(Byte estado);

 
    @Query("SELECT c FROM Categoria c WHERE c.estado <> :estadoExcluido")
    List<Categoria> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);
 
    Optional<Categoria> findByNombreCategoria(String nombreCategoria);

 
    boolean existsByNombreCategoria(String nombreCategoria);

 
    boolean existsByNombreCategoriaAndIdCategoriaIsNot(String nombreCategoria, Integer idCategoria);
 
    boolean existsByNombreCategoriaAndEstadoNot(String nombreCategoria, Byte estadoExcluido);
 
    boolean existsByNombreCategoriaAndIdCategoriaIsNotAndEstadoNot(
            String nombreCategoria,
            Integer idCategoria,
            Byte estadoExcluido
    );
}
