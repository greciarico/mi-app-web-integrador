package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

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
            "( :idRolCliente IS NULL OR c.tipoCliente.idRolCliente = :idRolCliente ) AND " + // CAMBIADO: c.tipoCliente.idRolCliente
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
            @Param("idRolCliente") Integer idRolCliente, // CAMBIADO: idRolCliente
            @Param("searchTerm") String searchTerm);

    // Método para obtener clientes que no tienen estado 2 (eliminado lógicamente)
    List<Cliente> findByEstadoIsNot(Byte estado);
}