package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Proveedor;
import com.example.DyD_Natures.Repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        return proveedorRepository.findByEstadoExcluding((byte) 2); // Usa el método con @Query y pasa un Byte
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
            proveedor.setFechaRegistro(LocalDate.now()); // Establecer fecha de registro para nuevos
            proveedor.setEstado((byte) 1); // Nuevo proveedor por defecto es Activo (Byte)
        } else { // Si es una edición, mantener la fecha de registro existente
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
            proveedor.setEstado((byte) 2); // Cambiar estado a 2 = eliminado lógicamente (Byte)
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
}
