package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    // REMOVIDO: List<Producto> findByEstadoNot(Integer estado); // Ya no se usará este método derivado

    // NUEVO: Método con @Query explícita para evitar el problema de traducción de Hibernate con Byte y NOT
    // Esta consulta es equivalente a "where estado != 2"
    @Query("SELECT p FROM Producto p WHERE p.estado <> :estadoExcluido")
    List<Producto> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido); // El parámetro es Byte

    // Método para buscar un producto por nombre (ej. para validación de unicidad)
    Optional<Producto> findByNombre(String nombre);

    // Método para verificar si un producto existe por nombre, excluyendo un ID
    boolean existsByNombreAndIdProductoIsNot(String nombre, Integer idProducto);
}


