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
     * Trae TODOS los registros de IGV menos los eliminados (estado = 2).
     * Incluye tanto activos (1) como inactivos (0).
     */
    public List<Igv> listarIgvExcludingDeleted() {
        return igvRepository.findByEstadoExcluding((byte) 2);
    }

    /** Sólo IGV activos (estado = 1) */
    public List<Igv> listarSoloIgvActivos() {
        return igvRepository.findByEstado((byte) 1);
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
            igv.setFechaRegistro(LocalDate.now()); 
            igv.setEstado((byte) 1); 
        } else { 
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
            igv.setEstado((byte) 2); 
            igvRepository.save(igv);
        }
    }

}
