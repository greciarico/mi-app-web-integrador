package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    @Query("SELECT c FROM Categoria c WHERE c.estado <> :estadoExcluido")
    List<Categoria> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);

    // Método para verificar si una categoría con un nombre dado ya existe
    Optional<Categoria> findByNombreCategoria(String nombreCategoria);

    // Método para verificar si existe una categoría con un nombre dado
    boolean existsByNombreCategoria(String nombreCategoria);

    // Método para verificar si existe una categoría con un nombre dado, excluyendo un ID específico
    boolean existsByNombreCategoriaAndIdCategoriaIsNot(String nombreCategoria, Integer idCategoria);
}

