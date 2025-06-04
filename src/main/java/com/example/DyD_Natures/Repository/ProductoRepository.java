package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    // Métodos adicionales si requieres filtros por categoria, estado, etc.
    // Método para listar productos cuyo estado NO sea 2 (no eliminados)
    List<Producto> findByEstadoNot(Byte estado);
}


