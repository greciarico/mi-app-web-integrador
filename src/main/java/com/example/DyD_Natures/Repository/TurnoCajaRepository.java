package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.TurnoCaja;
import com.example.DyD_Natures.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List; 
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Integer> {


    Optional<TurnoCaja> findByVendedorAndEstadoCuadre(Usuario vendedor, String estadoCuadre);

    @Query("SELECT t FROM TurnoCaja t JOIN FETCH t.vendedor ORDER BY t.fechaApertura DESC")
    List<TurnoCaja> findAllWithVendedor();


    @Query("SELECT tc FROM TurnoCaja tc WHERE tc.vendedor = :vendedor AND FUNCTION('DATE', tc.fechaApertura) = :fechaApertura")
    Optional<TurnoCaja> findByVendedorAndFechaAperturaDia(Usuario vendedor, LocalDate fechaApertura);

    List<TurnoCaja> findByEstadoCuadreAndFechaAperturaBefore(String estadoCuadre, LocalDateTime dateTime);

    List<TurnoCaja> findByVendedorOrderByFechaAperturaDesc(Usuario vendedor);
}
