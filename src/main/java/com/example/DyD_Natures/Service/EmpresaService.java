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

    private static final Integer EMPRESA_ID = 1;

    public Optional<Empresa> obtenerInformacionEmpresa() {
        return empresaRepository.findById(EMPRESA_ID);
    }

    public Empresa guardarEmpresa(Empresa empresa) {

        empresa.setIdEmpresa(EMPRESA_ID);

        if (!empresaRepository.existsById(EMPRESA_ID)) {
            empresa.setFechaRegistro(LocalDate.now());
        } else {
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
        return empresaRepository.existsByRucAndIdEmpresaIsNot(ruc, EMPRESA_ID);
    }
}
