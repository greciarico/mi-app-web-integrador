package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar
import org.springframework.data.repository.query.Param; // Importar
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer>, JpaSpecificationExecutor<Proveedor> { // ¡Añadir JpaSpecificationExecutor!

    /**
     * Obtiene una lista de proveedores cuyo estado NO es el especificado.
     * Se usa una @Query explícita para evitar posibles problemas de traducción
     * con métodos derivados que incluyen 'Not' en campos de tipo Byte.
     * @param estadoExcluido El valor del estado a excluir (ej. 2 para proveedores eliminados lógicamente).
     * @return Lista de proveedores cuyo estado no coincide con el estadoExcluido.
     */
    @Query("SELECT p FROM Proveedor p WHERE p.estado <> :estadoExcluido")
    List<Proveedor> findByEstadoExcluding(@Param("estadoExcluido") Byte estadoExcluido);// El parámetro es Byte

    List<Proveedor> findByEstado(Byte estado);

    /**
     * Busca un Proveedor por su número de RUC.
     * @param ruc El número de RUC a buscar.
     * @return Un Optional que contiene el Proveedor si se encuentra, o vacío si no.
     */
    Optional<Proveedor> findByRuc(String ruc);

    /**
     * Verifica si existe un proveedor con un RUC dado.
     * @param ruc El número de RUC a verificar.
     * @return true si existe un proveedor con ese RUC, false en caso contrario.
     */
    boolean existsByRuc(String ruc);

    /**
     * Verifica si existe un proveedor con un RUC dado, excluyendo un ID de proveedor específico.
     * @param ruc El número de RUC a verificar.
     * @param idProveedor El ID del proveedor a excluir de la búsqueda.
     * @return true si existe otro proveedor con ese RUC (diferente al idProveedor proporcionado), false en caso contrario.
     */
    boolean existsByRucAndIdProveedorIsNot(String ruc, Integer idProveedor);

    // Nuevo: comprueba existencia de RUC **ignorando** estado eliminado (2)
    boolean existsByRucAndEstadoNot(String ruc, Byte estadoExcluido);

    // Nuevo: comprueba existencia de RUC en otros registros (excluyendo este ID) e ignorando eliminados
    boolean existsByRucAndIdProveedorIsNotAndEstadoNot(String ruc, Integer idProveedor, Byte estadoExcluido);
    /** Para detectar si existe un registro con RUC y estado = eliminado */
    Optional<Proveedor> findByRucAndEstado(String ruc, Byte estado);

}

