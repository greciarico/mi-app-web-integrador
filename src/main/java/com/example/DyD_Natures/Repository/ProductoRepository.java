package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {
    


    @Query("SELECT p FROM Producto p WHERE p.estado <> :estadoExcluido")
    List<Producto> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido); 


    Optional<Producto> findByNombre(String nombre);


    boolean existsByNombreAndIdProductoIsNot(String nombre, Integer idProducto);

    List<Producto> findByEstado(Byte estado);

    boolean existsByNombreAndEstadoNot(String nombre, Byte estadoExcluido);

    boolean existsByNombreAndIdProductoIsNotAndEstadoNot(
            String nombre, Integer idProducto, Byte estadoExcluido);

    Optional<Producto> findByNombreAndDescripcionAndCategoria_IdCategoriaAndEstadoNot(
            String nombre,
            String descripcion,
            Integer categoriaId,
            Byte estadoExcluido
    );
}
