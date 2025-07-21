
package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Venta;
import com.example.DyD_Natures.Model.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer>, JpaSpecificationExecutor<Venta> {
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fechaRegistro BETWEEN :startDate AND :endDate AND v.estado = 1")
    BigDecimal sumTotalByFechaRegistroBetweenAndEstado(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaRegistro BETWEEN :startDate AND :endDate AND v.estado = 1")
    Long countByFechaRegistroBetweenAndEstado(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    
    @Query("SELECT MONTH(v.fechaRegistro) as month, SUM(v.total) as total " +
            "FROM Venta v " +
            "WHERE YEAR(v.fechaRegistro) = :year " +
            "GROUP BY MONTH(v.fechaRegistro) " +
            "ORDER BY month")
    List<Object[]> findTotalSalesByMonthForYear(@Param("year") int year);

    List<Venta> findAllByUsuario_IdUsuario(Integer idUsuario);

    
    List<Venta> findByTurnoCaja(TurnoCaja turnoCaja);

}
