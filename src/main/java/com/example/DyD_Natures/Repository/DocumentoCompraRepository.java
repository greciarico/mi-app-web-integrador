package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DocumentoCompra; // Asegúrate de que esta entidad exista y tenga 'fechaRegistro' y 'total'
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DocumentoCompraRepository extends JpaRepository<DocumentoCompra, Integer> {

    /**
     * Calcula la suma total de compras por mes para un año específico.
     * Los resultados se ordenan por el número de mes.
     * @param year El año para el cual se calculan las compras.
     * @return Una lista de Object[] donde cada Object[] contiene [mes (Integer), totalCompras (BigDecimal)].
     */
    @Query("SELECT MONTH(dc.fechaRegistro) as month, SUM(dc.total) as total " +
            "FROM DocumentoCompra dc " + // Usamos 'dc' para tu entidad DocumentoCompra
            "WHERE YEAR(dc.fechaRegistro) = :year " +
            "GROUP BY MONTH(dc.fechaRegistro) " +
            "ORDER BY month")
    List<Object[]> findTotalPurchasesByMonthForYear(@Param("year") int year);
}
