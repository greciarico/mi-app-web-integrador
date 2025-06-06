package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Empresa;
import com.example.DyD_Natures.Repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    // Asumimos un ID fijo para la información de la empresa
    private static final Integer EMPRESA_ID = 1;

    /**
     * Obtiene la información de la empresa (se asume un único registro con ID 1).
     * @return Un Optional que contiene la Empresa si existe, o vacío si no.
     */
    public Optional<Empresa> obtenerInformacionEmpresa() {
        return empresaRepository.findById(EMPRESA_ID);
    }

    /**
     * Guarda o actualiza la información de la empresa.
     * Si no existe un registro (es el primer guardado), se crea con ID 1 y fecha de registro.
     * @param empresa El objeto Empresa a guardar.
     * @return La Empresa guardada.
     */
    public Empresa guardarEmpresa(Empresa empresa) {
        // Siempre usamos el ID fijo para la empresa
        empresa.setIdEmpresa(EMPRESA_ID);

        // Si es una creación (no existe previamente), establece la fecha de registro
        if (!empresaRepository.existsById(EMPRESA_ID)) {
            empresa.setFechaRegistro(LocalDate.now());
        } else {
            // Si es una actualización, recupera la fecha de registro existente para no modificarla
            Optional<Empresa> existingEmpresaOpt = empresaRepository.findById(EMPRESA_ID);
            existingEmpresaOpt.ifPresent(existingEmpresa -> empresa.setFechaRegistro(existingEmpresa.getFechaRegistro()));
        }
        return empresaRepository.save(empresa);
    }

    /**
     * Verifica si un RUC ya existe en la base de datos, excluyendo el ID de la empresa actual.
     * @param ruc El número de RUC a verificar.
     * @return true si existe otra empresa con ese RUC, false en caso contrario.
     */
    public boolean existsByRucExcludingCurrent(String ruc) {
        // Al validar unicidad, siempre se excluye el ID fijo de la empresa (EMPRESA_ID)
        return empresaRepository.existsByRucAndIdEmpresaIsNot(ruc, EMPRESA_ID);
    }
}
