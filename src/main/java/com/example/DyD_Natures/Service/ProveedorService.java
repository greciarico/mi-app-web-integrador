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

    @Autowired
    private ProveedorRepository proveedorRepository;

    /**
     * Lista todos los proveedores que no tienen el estado '2' (eliminado lógicamente).
     * @return Lista de proveedores activos/inactivos (no eliminados).
     */
    public List<Proveedor> listarProveedoresActivos() {
        return proveedorRepository.findByEstadoExcluding((byte) 2); 
    }

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

    /**
     * Guarda un proveedor nuevo o actualiza uno existente.
     * @param proveedor El objeto Proveedor a guardar.
     * @return El Proveedor guardado.
     */
    public Proveedor guardarProveedor(Proveedor proveedor) {
        if (proveedor.getIdProveedor() == null) {
            proveedor.setFechaRegistro(LocalDate.now()); 
            proveedor.setEstado((byte) 1); 
        } else { 
            Optional<Proveedor> existingProveedorOpt = proveedorRepository.findById(proveedor.getIdProveedor());
            existingProveedorOpt.ifPresent(existingProveedor -> proveedor.setFechaRegistro(existingProveedor.getFechaRegistro()));
        }
        return proveedorRepository.save(proveedor);
    }

    /**
     * Realiza una eliminación lógica de un proveedor, cambiando su estado a '2'.
     * @param id El ID del proveedor a eliminar lógicamente.
     */
    public void eliminarProveedor(Integer id) {
        Optional<Proveedor> proveedorOpt = proveedorRepository.findById(id);
        if (proveedorOpt.isPresent()) {
            Proveedor proveedor = proveedorOpt.get();
            proveedor.setEstado((byte) 2); 
            proveedorRepository.save(proveedor);
        }
    }

    /**
     * Verifica si un RUC ya existe en la base de datos, excluyendo un ID de proveedor específico.
     * @param ruc El número de RUC a verificar.
     * @param idProveedor El ID del proveedor a excluir de la búsqueda (null para nuevas creaciones).
     * @return true si existe otro proveedor con ese RUC, false en caso contrario.
     */
    public boolean existsByRucExcludingId(String ruc, Integer idProveedor) {
        if (idProveedor != null) {
            return proveedorRepository.existsByRucAndIdProveedorIsNot(ruc, idProveedor);
        }
        return proveedorRepository.existsByRuc(ruc);
    }

    /**
     * Busca proveedores aplicando filtros dinámicamente para la generación de reportes.
     * @param filterDTO DTO con los criterios de búsqueda (nombre, ruc, razon social, correo, estados).
     * @return Lista de proveedores que coinciden con los filtros.
     */
    public List<Proveedor> buscarProveedoresPorFiltros(ProveedorFilterDTO filterDTO) {
        return proveedorRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

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

            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                if (!(filterDTO.getEstados().contains((byte) 0) && filterDTO.getEstados().contains((byte) 1))) {
                    predicates.add(root.get("estado").in(filterDTO.getEstados()));
                }
            }
            if (filterDTO.getFechaRegistroDesde() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroDesde()));
            }


            if (filterDTO.getFechaRegistroHasta() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaRegistro"), filterDTO.getFechaRegistroHasta()));
            }


            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
