package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Igv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IgvRepository extends JpaRepository<Igv, Integer> {

    /**
     * Obtiene una lista de registros de IGV cuyo estado NO es el especificado.
     * Se usa una @Query explícita para evitar posibles problemas de traducción
     * con métodos derivados que incluyen 'Not' en campos de tipo Byte.
     * @param estadoExcluido El valor del estado a excluir (ej. 2 para eliminados lógicamente).
     * @return Lista de registros de IGV cuyo estado no coincide con el estadoExcluido.
     */
    @Query("SELECT i FROM Igv i WHERE i.estado <> :estadoExcluido")
    List<Igv> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);

    // Si tuvieras un campo único para IGV (ej. version o fecha única), lo añadirías aquí.
    // Por ahora, no hay validación de unicidad para el valor del IGV en sí, solo por ID.
}
