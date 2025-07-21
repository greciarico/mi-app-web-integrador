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

 
    Optional<Cliente> findByDni(String dni);
 
    boolean existsByDniAndIdClienteIsNot(String dni, Integer idCliente);
 
    boolean existsByDni(String dni);

 
    Optional<Cliente> findByRuc(String ruc);
 
    boolean existsByRucAndIdClienteIsNot(String ruc, Integer idCliente);
 
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
            "c.estado <> 2")  
    List<Cliente> findFilteredClientes(
            @Param("idRolCliente") Integer idRolCliente,
            @Param("searchTerm") String searchTerm);

 
    List<Cliente> findByEstadoIsNot(Byte estado);


    /**
     * Cuenta el número total de clientes registrados en un rango de fechas, excluyendo el estado 2 (fijo).
     * @param startDate Fecha de inicio (inclusive).
     * @param endDate Fecha de fin (inclusive).
     * @return Número de clientes registrados para el período.
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.fechaRegistro BETWEEN :startDate AND :endDate AND c.estado <> 2")
    Long countByFechaRegistroBetweenAndEstadoIsNot(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate); 

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.estado <> 2")
    Long countAllActiveClients();

    List<Cliente> findByEstado(Byte estado);

    boolean existsByDniAndEstadoNot(String dni, Byte estadoExcluido);


    boolean existsByDniAndIdClienteIsNotAndEstadoNot(
            String dni,
            Integer idCliente,
            Byte estadoExcluido
    );

    boolean existsByRucAndEstadoNot(String ruc, Byte estadoExcluido);

    boolean existsByRucAndIdClienteIsNotAndEstadoNot(
            String ruc,
            Integer idCliente,
            Byte estadoExcluido
    );

    Optional<Cliente> findByDniAndEstadoNot(String dni, Byte estadoExcluido);
    Optional<Cliente> findByRucAndEstadoNot(String ruc, Byte estadoExcluido);

}
