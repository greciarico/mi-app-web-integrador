package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

    // Buscar cliente por DNI
    Optional<Cliente> findByDni(String dni);
    // Verificar si DNI existe excluyendo un ID (para edición)
    boolean existsByDniAndIdClienteIsNot(String dni, Integer idCliente);
    // Verificar si DNI existe (para creación)
    boolean existsByDni(String dni);

    // Buscar cliente por RUC
    Optional<Cliente> findByRuc(String ruc);
    // Verificar si RUC existe excluyendo un ID (para edición)
    boolean existsByRucAndIdClienteIsNot(String ruc, Integer idCliente);
    // Verificar si RUC existe (para creación)
    boolean existsByRuc(String ruc);

    /**
     * Busca clientes aplicando filtros opcionales por tipo de cliente y término de búsqueda.
     * @param idRolCliente ID del TipoCliente (1 para Natural, 2 para Jurídica) usando idRolCliente. Si es null, no filtra por tipo.
     * @param searchTerm Término de búsqueda que puede coincidir con nombre, apellidos, DNI, RUC, razón social, nombre comercial.
     * @return Lista de clientes filtrados.
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "( :idRolCliente IS NULL OR c.tipoCliente.idRolCliente = :idRolCliente ) AND " +
            "( :searchTerm IS NULL OR :searchTerm = '' OR " +
            "  LOWER(c.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.apPaterno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.apMaterno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.dni) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.ruc) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "  LOWER(c.nombreComercial) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ) AND " +
            "c.estado <> 2") // Excluir clientes con estado 2 (eliminado lógicamente)
    List<Cliente> findFilteredClientes(
            @Param("idRolCliente") Integer idRolCliente,
            @Param("searchTerm") String searchTerm);

    // Método para obtener clientes que no tienen estado 2 (eliminado lógicamente)
    List<Cliente> findByEstadoIsNot(Byte estado);


    /**
     * Cuenta el número total de clientes registrados en un rango de fechas, excluyendo el estado 2 (fijo).
     * @param startDate Fecha de inicio (inclusive).
     * @param endDate Fecha de fin (inclusive).
     * @return Número de clientes registrados para el período.
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.fechaRegistro BETWEEN :startDate AND :endDate AND c.estado <> 2")
    Long countByFechaRegistroBetweenAndEstadoIsNot(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate); // <-- ¡Aquí se ha quitado el tercer parámetro!

    // Para contar el total de clientes activos en toda la historia (sin filtro de fecha de registro)
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.estado <> 2")
    Long countAllActiveClients();

    // Devuelve únicamente los clientes con estado = 1
    List<Cliente> findByEstado(Byte estado);

    // --- Nuevos métodos que ignoran estado = 2 ---
    /** Para creación: ¿existe un cliente (estado ≠ 2) con este DNI? */
    boolean existsByDniAndEstadoNot(String dni, Byte estadoExcluido);

    /** Para edición: ¿existe otro cliente distinto a idCliente con este DNI y estado ≠ 2? */
    boolean existsByDniAndIdClienteIsNotAndEstadoNot(
            String dni,
            Integer idCliente,
            Byte estadoExcluido
    );

    /** Igual para RUC: creación */
    boolean existsByRucAndEstadoNot(String ruc, Byte estadoExcluido);

    /** Igual para RUC: edición */
    boolean existsByRucAndIdClienteIsNotAndEstadoNot(
            String ruc,
            Integer idCliente,
            Byte estadoExcluido
    );

    // Si necesitas buscar un Cliente por DNI/RUC ignorando eliminados:
    Optional<Cliente> findByDniAndEstadoNot(String dni, Byte estadoExcluido);
    Optional<Cliente> findByRucAndEstadoNot(String ruc, Byte estadoExcluido);

}
