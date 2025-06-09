package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Cliente;
import com.example.DyD_Natures.Model.TipoCliente;
import com.example.DyD_Natures.Repository.ClienteRepository;
import com.example.DyD_Natures.Repository.TipoClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoClienteRepository tipoClienteRepository;

    private static final Integer TIPO_NATURAL_ID = 1;
    private static final Integer TIPO_JURIDICA_ID = 2;

    public List<Cliente> listarClientes(Integer idTipoCliente, String searchTerm) {
        return clienteRepository.findFilteredClientes(idTipoCliente, searchTerm);
    }

    public List<Cliente> listarTodosLosClientesActivos() {
        return clienteRepository.findByEstadoIsNot((byte) 2);
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
}
