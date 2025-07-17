package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar Query
import org.springframework.stereotype.Repository;

import java.util.List; // Necesitamos List para findAllWithVendedor
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Integer> {

    // Método para buscar el turno de caja abierto de un vendedor específico
    Optional<TurnoCaja> findByVendedorAndEstadoCuadre(Usuario vendedor, String estadoCuadre);

    // Nuevo método para obtener todos los turnos de caja y cargar explícitamente el vendedor (Usuario)
    // Esto previene LazyInitializationException cuando Thymeleaf intenta acceder a los datos del vendedor.
    @Query("SELECT t FROM TurnoCaja t JOIN FETCH t.vendedor ORDER BY t.fechaApertura DESC")
    List<TurnoCaja> findAllWithVendedor();
}
