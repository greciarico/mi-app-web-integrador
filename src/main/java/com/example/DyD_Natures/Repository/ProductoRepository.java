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

    // Esta consulta es equivalente a "where estado != 2"
    @Query("SELECT p FROM Producto p WHERE p.estado <> :estadoExcluido")
    List<Producto> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);

    // Método para buscar un producto por nombre
    Optional<Producto> findByNombre(String nombre);

    // Método para verificar si un producto existe por nombre, excluyendo un ID
    boolean existsByNombreAndIdProductoIsNot(String nombre, Integer idProducto);
}


