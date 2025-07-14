package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Importar
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer>, JpaSpecificationExecutor<Venta> {
    // Consultas personalizadas para ventas si se necesitan

    /**
     * Calcula el total de los montos de venta en un rango de fechas.
     * @param startDate Fecha de inicio (inclusive).
     * @param endDate Fecha de fin (inclusive).
     * @return Suma de los totales de venta para el período, o 0.00 si no hay ventas.
     */
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fechaRegistro BETWEEN :startDate AND :endDate AND v.estado = 1")
    BigDecimal sumTotalByFechaRegistroBetweenAndEstado(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaRegistro BETWEEN :startDate AND :endDate AND v.estado = 1")
    Long countByFechaRegistroBetweenAndEstado(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Calcula la suma total de ventas por mes para un año específico.
     * Los resultados se ordenan por el número de mes.
     * @param year El año para el cual se calculan las ventas.
     * @return Una lista de Object[] donde cada Object[] contiene [mes (Integer), totalVentas (BigDecimal)].
     */
    @Query("SELECT MONTH(v.fechaRegistro) as month, SUM(v.total) as total " +
            "FROM Venta v " +
            "WHERE YEAR(v.fechaRegistro) = :year " +
            "GROUP BY MONTH(v.fechaRegistro) " +
            "ORDER BY month")
    List<Object[]> findTotalSalesByMonthForYear(@Param("year") int year);

}

