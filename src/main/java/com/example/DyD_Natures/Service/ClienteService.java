package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.ClienteFilterDTO;
import com.example.DyD_Natures.Dto.api.ReniecDataDTO;
import com.example.DyD_Natures.Dto.api.SunatDataDTO;
import com.example.DyD_Natures.Integration.PeruDataApiClient;
import com.example.DyD_Natures.Model.Cliente;
import com.example.DyD_Natures.Model.TipoCliente;
import com.example.DyD_Natures.Repository.ClienteRepository;
import com.example.DyD_Natures.Repository.TipoClienteRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoClienteRepository tipoClienteRepository;

    @Autowired
    private PeruDataApiClient peruDataApiClient;

    private static final Integer TIPO_NATURAL_ID = 1;
    private static final Integer TIPO_JURIDICA_ID = 2;

    public List<Cliente> listarClientes(Integer idTipoCliente, String searchTerm) {
        return clienteRepository.findFilteredClientes(idTipoCliente, searchTerm);
    }

    public List<Cliente> listarTodosLosClientesActivos() {
        return clienteRepository.findByEstado((byte) 1);
    }

    public Optional<Cliente> obtenerClientePorId(Integer id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> obtenerClientePorDni(String dni) {
        return clienteRepository.findByDni(dni);
    }

    public Optional<Cliente> obtenerClientePorRuc(String ruc) {
        return clienteRepository.findByRuc(ruc);
    }

    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        if (cliente.getIdCliente() == null) {
            cliente.setFechaRegistro(LocalDate.now());
            cliente.setEstado((byte) 1);
        }

        Optional<TipoCliente> tipoClienteOpt = tipoClienteRepository.findById(cliente.getTipoCliente().getIdRolCliente());
        if (tipoClienteOpt.isEmpty()) {
            throw new RuntimeException("Tipo de Cliente no encontrado con ID: " + cliente.getTipoCliente().getIdRolCliente());
        }
        cliente.setTipoCliente(tipoClienteOpt.get());

        if (cliente.getTipoCliente().getIdRolCliente().equals(TIPO_NATURAL_ID)) {
            cliente.setRuc(null);
            cliente.setRazonSocial(null);
            cliente.setNombreComercial(null);

            if (cliente.getDni() == null || cliente.getDni().isEmpty() ||
                    cliente.getNombre() == null || cliente.getNombre().isEmpty() ||
                    cliente.getApPaterno() == null || cliente.getApPaterno().isEmpty() ||
                    cliente.getApMaterno() == null || cliente.getApMaterno().isEmpty()) {
                throw new IllegalArgumentException("Para persona natural, DNI, nombre y apellidos son obligatorios.");
            }
            if (cliente.getIdCliente() == null) {
                if (clienteRepository.existsByDni(cliente.getDni())) {
                    throw new IllegalArgumentException("El DNI ya está registrado para otra persona natural.");
                }
            } else {
                if (clienteRepository.existsByDniAndIdClienteIsNot(cliente.getDni(), cliente.getIdCliente())) {
                    throw new IllegalArgumentException("El DNI ya está registrado para otra persona natural.");
                }
            }
        } else if (cliente.getTipoCliente().getIdRolCliente().equals(TIPO_JURIDICA_ID)) {
            cliente.setDni(null);
            cliente.setNombre(null);
            cliente.setApPaterno(null);
            cliente.setApMaterno(null);

            if (cliente.getRuc() == null || cliente.getRuc().isEmpty() ||
                    cliente.getRazonSocial() == null || cliente.getRazonSocial().isEmpty() ||
                    cliente.getNombreComercial() == null || cliente.getNombreComercial().isEmpty()) {
                throw new IllegalArgumentException("Para persona jurídica, RUC, razón social y nombre comercial son obligatorios.");
            }
            if (cliente.getIdCliente() == null) {
                if (clienteRepository.existsByRuc(cliente.getRuc())) {
                    throw new IllegalArgumentException("El RUC ya está registrado para otra persona jurídica.");
                }
            } else {
                if (clienteRepository.existsByRucAndIdClienteIsNot(cliente.getRuc(), cliente.getIdCliente())) {
                    throw new IllegalArgumentException("El RUC ya está registrado para otra persona jurídica.");
                }
            }
        } else {
            throw new IllegalArgumentException("Tipo de cliente inválido.");
        }

        if (cliente.getDireccion() == null || cliente.getDireccion().isEmpty()) {
            throw new IllegalArgumentException("La dirección es obligatoria.");
        }
        if (cliente.getTelefono() != null && !cliente.getTelefono().isEmpty() && !cliente.getTelefono().matches("^\\d{9}$")) {
            throw new IllegalArgumentException("El teléfono debe tener 9 dígitos numéricos.");
        }

        return clienteRepository.save(cliente);
    }

    public void eliminarCliente(Integer id) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            cliente.setEstado((byte) 2);
            clienteRepository.save(cliente);
        } else {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
    }

   
    public boolean existsByDniExcludingCurrent(String dni, Integer idCliente) {
        return clienteRepository.existsByDniAndIdClienteIsNot(dni, idCliente);
    }

    public boolean existsByRucExcludingCurrent(String ruc, Integer idCliente) {
        return clienteRepository.existsByRucAndIdClienteIsNot(ruc, idCliente);
    }

   
    public boolean existsByDni(String dni) {
        return clienteRepository.existsByDni(dni);
    }

    public boolean existsByRuc(String ruc) {
        return clienteRepository.existsByRuc(ruc);
    }

    // ===============================================
    // NUEVOS MÉTODOS PARA CONSULTAR APIS EXTERNAS
    // ===============================================

    /**
     * Consulta datos de RENIEC para un DNI dado.
     * @param dni El DNI a consultar.
     * @return Un Optional que contiene ReniecDataDTO si se encuentran datos, o vacío si no.
     */
    public Optional<ReniecDataDTO> buscarReniecPorDni(String dni) {
        if (dni == null || dni.trim().length() != 8 || !dni.matches("\\d+")) {
            return Optional.empty(); // Validar formato básico antes de llamar a la API
        }
        return peruDataApiClient.getReniecData(dni);
    }

    /**
     * Consulta datos de SUNAT para un RUC dado.
     * @param ruc El RUC a consultar.
     * @return Un Optional que contiene SunatDataDTO si se encuentran datos, o vacío si no.
     */
    public Optional<SunatDataDTO> buscarSunatPorRuc(String ruc) {
        if (ruc == null || ruc.trim().length() != 11 || !ruc.matches("\\d+")) {
            return Optional.empty(); // Validar formato básico antes de llamar a la API
        }
        return peruDataApiClient.getSunatData(ruc);
    }

    /**
     * Busca registros de Cliente aplicando filtros dinámicamente para la generación de reportes.
     * EXCLUYE CLIENTES CON ESTADO 2 (ELIMINADO) POR DEFECTO.
     * @param filterDTO DTO con los criterios de búsqueda.
     * @return Lista de registros de Cliente que coinciden con los filtros.
     */
    public List<Cliente> buscarClientesPorFiltros(ClienteFilterDTO filterDTO) {
        return clienteRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterDTO.getEstado() != null) {

                if (filterDTO.getEstado() == 1 || filterDTO.getEstado() == 0) {
                    predicates.add(criteriaBuilder.equal(root.get("estado"), filterDTO.getEstado()));
                }
            } else {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("estado"), (byte) 1),
                        criteriaBuilder.equal(root.get("estado"), (byte) 0)
                ));
            }

            if (filterDTO.getNombreCompletoODoc() != null && !filterDTO.getNombreCompletoODoc().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreCompletoODoc().toLowerCase() + "%";
                Predicate nombrePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), searchTerm);
                Predicate apPaternoPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("apPaterno")), searchTerm);
                Predicate apMaternoPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("apMaterno")), searchTerm);
                Predicate dniPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("dni")), searchTerm);
                Predicate rucPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("ruc")), searchTerm);
                Predicate razonSocialPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("razonSocial")), searchTerm);
                Predicate nombreComercialPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("nombreComercial")), searchTerm);

                predicates.add(criteriaBuilder.or(
                        nombrePredicate,
                        apPaternoPredicate,
                        apMaternoPredicate,
                        dniPredicate,
                        rucPredicate,
                        razonSocialPredicate,
                        nombreComercialPredicate
                ));
            }

            if (filterDTO.getIdTipoCliente() != null) {
                Join<Cliente, TipoCliente> tipoClienteJoin = root.join("tipoCliente");
                predicates.add(criteriaBuilder.equal(tipoClienteJoin.get("idRolCliente"), filterDTO.getIdTipoCliente()));
            }

            if (filterDTO.getFechaRegistroStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroStart()));
            }
            if (filterDTO.getFechaRegistroEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroEnd()));
            }

            if (filterDTO.getDireccion() != null && !filterDTO.getDireccion().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getDireccion().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("direccion")), searchTerm));
            }

            if (filterDTO.getTelefono() != null && !filterDTO.getTelefono().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getTelefono().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("telefono")), searchTerm));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
    /**
     * Obtiene un TipoCliente por su ID.
     * Es útil para obtener los detalles del tipo de cliente (como su nombre)
     * en otras partes de la aplicación, incluyendo la generación de reportes.
     * @param idTipoCliente El ID del tipo de cliente a buscar.
     * @return Un Optional que contiene el TipoCliente si se encuentra, o Optional.empty() si no.
     */
    public Optional<TipoCliente> obtenerTipoClientePorId(Integer idTipoCliente) {
        return tipoClienteRepository.findById(idTipoCliente);
    }
}
