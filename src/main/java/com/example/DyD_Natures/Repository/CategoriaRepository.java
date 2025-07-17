package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar
import org.springframework.data.repository.query.Param; // Importar
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Importar


import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer>, JpaSpecificationExecutor<Categoria> { // Añadir JpaSpecificationExecutor

    List<Categoria> findByEstado(Byte estado);

    // CAMBIO CLAVE AQUÍ: Usamos @Query explícita para evitar problemas con Byte y "NOT"
    @Query("SELECT c FROM Categoria c WHERE c.estado <> :estadoExcluido")
    List<Categoria> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);

    // Método para verificar si una categoría con un nombre dado ya existe
    Optional<Categoria> findByNombreCategoria(String nombreCategoria);

    // Método para verificar si existe una categoría con un nombre dado
    boolean existsByNombreCategoria(String nombreCategoria);

    // Método para verificar si existe una categoría con un nombre dado, excluyendo un ID específico
    boolean existsByNombreCategoriaAndIdCategoriaIsNot(String nombreCategoria, Integer idCategoria);
    /**
     * Comprueba si existe una categoría con este nombre y estado distinto de 2 (no eliminada).
     */
    boolean existsByNombreCategoriaAndEstadoNot(String nombreCategoria, Byte estadoExcluido);

    /**
     * Igual que el anterior, pero excluye además la propia categoría (para editar).
     */
    boolean existsByNombreCategoriaAndIdCategoriaIsNotAndEstadoNot(
            String nombreCategoria,
            Integer idCategoria,
            Byte estadoExcluido
    );
}
