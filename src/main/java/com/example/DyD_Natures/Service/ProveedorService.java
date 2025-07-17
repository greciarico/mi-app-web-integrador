package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.ProveedorFilterDTO;
import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Repository.ProveedorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class ProveedorService {


    private static final byte ESTADO_ELIMINADO = 2;
    private static final byte ESTADO_ACTIVO    = 1;


    @Autowired
    private ProveedorRepository proveedorRepository;

    /**
     * Lista todos los proveedores que no tienen el estado '2' (eliminado lógicamente).
     * @return Lista de proveedores activos/inactivos (no eliminados).
     */
    public List<Proveedor> listarProveedoresActivos() {
        return proveedorRepository.findByEstadoExcluding((byte) 2); // Usa el método con @Query y pasa un Byte
    }

    // --- NUEVO MÉTODO AQUÍ ---
    /**
     * Lista solo los proveedores que están en estado '1' (Activo).
     * @return Lista de proveedores activos.
     */
    public List<Proveedor> listarSoloProveedoresActivos() {
        return proveedorRepository.findByEstado((byte) 1);
    }

    /**
     * Obtiene un proveedor por su ID.
     * @param id El ID del proveedor.
     * @return Un Optional que contiene el Proveedor si se encuentra, o vacío si no.
     */
    public Optional<Proveedor> obtenerProveedorPorId(Integer id) {
        return proveedorRepository.findById(id);
    }

    /**
     * Obtiene un proveedor por su número de RUC.
     * @param ruc El número de RUC a buscar.
     * @return Un Optional que contiene el Proveedor si se encuentra, o vacío si no.
     */
    public Optional<Proveedor> obtenerProveedorPorRuc(String ruc) {
        return proveedorRepository.findByRuc(ruc);
    }


    public Proveedor guardarProveedor(Proveedor dto) {
        String ruc = dto.getRuc();

        if (dto.getIdProveedor() == null) {
            // 1) ¿Existe un proveedor eliminado con ese RUC?
            Optional<Proveedor> eliminado = proveedorRepository.findByRucAndEstado(ruc, ESTADO_ELIMINADO);
            if (eliminado.isPresent()) {
                // “Resucitamos” ese registro
                Proveedor existente = eliminado.get();
                existente.setNombreComercial(dto.getNombreComercial());
                existente.setRazonSocial(dto.getRazonSocial());
                existente.setDireccion(dto.getDireccion());
                existente.setCorreo(dto.getCorreo());
                existente.setTelefono(dto.getTelefono());
                existente.setEstado(ESTADO_ACTIVO);
                // (opcional) no cambies la fechaRegistro, o la actualizas si así lo decides
                return proveedorRepository.save(existente);
            }

            // 2) Validar que no exista un activo con ese RUC
            if (proveedorRepository.existsByRucAndEstadoNot(ruc, ESTADO_ELIMINADO)) {
                throw new IllegalArgumentException("El RUC ya está registrado para otro proveedor.");
            }
            dto.setFechaRegistro(LocalDate.now());
            dto.setEstado(ESTADO_ACTIVO);
            return proveedorRepository.save(dto);

        } else {
            // EDICIÓN
            if (proveedorRepository.existsByRucAndIdProveedorIsNotAndEstadoNot(
                    ruc, dto.getIdProveedor(), ESTADO_ELIMINADO)) {
                throw new IllegalArgumentException("El RUC ya está registrado para otro proveedor.");
            }
            // conservar fechaRegistro original
            proveedorRepository.findById(dto.getIdProveedor())
                    .ifPresent(orig -> dto.setFechaRegistro(orig.getFechaRegistro()));
            return proveedorRepository.save(dto);
        }
    }

    public boolean existsByRucExcludingId(String ruc, Integer idProveedor) {
        if (idProveedor == null) {
            return proveedorRepository.existsByRucAndEstadoNot(ruc, ESTADO_ELIMINADO);
        }
        return proveedorRepository.existsByRucAndIdProveedorIsNotAndEstadoNot(ruc, idProveedor, ESTADO_ELIMINADO);
    }


    /**
     * Realiza una eliminación lógica de un proveedor, cambiando su estado a '2'.
     * @param id El ID del proveedor a eliminar lógicamente.
     */
    public void eliminarProveedor(Integer id) {
        Optional<Proveedor> proveedorOpt = proveedorRepository.findById(id);
        if (proveedorOpt.isPresent()) {
            Proveedor proveedor = proveedorOpt.get();
            proveedor.setEstado((byte) 2); // Cambiar estado a 2 = eliminado lógicamente (Byte)
            proveedorRepository.save(proveedor);
        }
    }


    // --- NUEVO MÉTODO PARA FILTRADO DE REPORTES ---
    /**
     * Busca proveedores aplicando filtros dinámicamente para la generación de reportes.
     * @param filterDTO DTO con los criterios de búsqueda (nombre, ruc, razon social, correo, estados).
     * @return Lista de proveedores que coinciden con los filtros.
     */
    public List<Proveedor> buscarProveedoresPorFiltros(ProveedorFilterDTO filterDTO) {
        return proveedorRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por nombre, ruc, razon social, correo (búsqueda general)
            if (filterDTO.getNombreRucRazonSocialCorreo() != null && !filterDTO.getNombreRucRazonSocialCorreo().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreRucRazonSocialCorreo().toLowerCase() + "%";
                Predicate generalSearchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombreComercial")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ruc")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("razonSocial")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("correo")), searchTerm)
                );
                predicates.add(generalSearchPredicate);
            }

            // Filtro por Estado - Ahora maneja múltiples selecciones
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                // Si eligen ambos (activo y inactivo), no se añade este filtro
                if (!(filterDTO.getEstados().contains((byte) 0) && filterDTO.getEstados().contains((byte) 1))) {
                    predicates.add(root.get("estado").in(filterDTO.getEstados()));
                }
            }

            // NUEVO: Filtro por Fecha de Registro "Desde"
            if (filterDTO.getFechaRegistroDesde() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroDesde()));
            }

            // NUEVO: Filtro por Fecha de Registro "Hasta"
            if (filterDTO.getFechaRegistroHasta() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroHasta()));
            }

            // Excluir proveedores con estado = 2 (eliminado) por defecto en los reportes
            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
