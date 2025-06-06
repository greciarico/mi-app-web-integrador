package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Igv;
import com.example.DyD_Natures.Repository.IgvRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class IgvService {

    @Autowired
    private IgvRepository igvRepository;

    /**
     * Lista todos los registros de IGV que no tienen el estado '2' (eliminado lógicamente).
     * @return Lista de registros de IGV activos/inactivos (no eliminados).
     */
    public List<Igv> listarIgvActivos() {
        return igvRepository.findByEstadoExcluding((byte) 2); // Usa el método con @Query y pasa un Byte
    }

    /**
     * Obtiene un registro de IGV por su ID.
     * @param id El ID del registro de IGV.
     * @return Un Optional que contiene el Igv si se encuentra, o vacío si no.
     */
    public Optional<Igv> obtenerIgvPorId(Integer id) {
        return igvRepository.findById(id);
    }

    /**
     * Guarda un registro de IGV nuevo o actualiza uno existente.
     * @param igv El objeto Igv a guardar.
     * @return El Igv guardado.
     */
    public Igv guardarIgv(Igv igv) {
        if (igv.getIdIgv() == null) {
            igv.setFechaRegistro(LocalDate.now()); // Establecer fecha de registro para nuevos
            igv.setEstado((byte) 1); // Nuevo IGV por defecto es Activo (Byte)
        } else { // Si es una edición, mantener la fecha de registro existente
            Optional<Igv> existingIgvOpt = igvRepository.findById(igv.getIdIgv());
            existingIgvOpt.ifPresent(existingIgv -> igv.setFechaRegistro(existingIgv.getFechaRegistro()));
        }
        return igvRepository.save(igv);
    }

    /**
     * Realiza una eliminación lógica de un registro de IGV, cambiando su estado a '2'.
     * @param id El ID del registro de IGV a eliminar lógicamente.
     */
    public void eliminarIgv(Integer id) {
        Optional<Igv> igvOpt = igvRepository.findById(id);
        if (igvOpt.isPresent()) {
            Igv igv = igvOpt.get();
            igv.setEstado((byte) 2); // Cambiar estado a 2 = eliminado lógicamente (Byte)
            igvRepository.save(igv);
        }
    }

    // No hay métodos de unicidad como DNI/RUC para IGV, a menos que tu lógica de negocio lo requiera (ej. un IGV activo por fecha)
    // Si necesitas validar que solo haya un IGV "activo" o por fecha, añadirías lógica aquí.
}
