package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {
    // Consultas personalizadas para ventas si se necesitan
}

