package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    /**
     * Busca una Empresa por su número de RUC.
     * @param ruc El número de RUC a buscar.
     * @return Un Optional que contiene la Empresa si se encuentra, o vacío si no.
     */
    Optional<Empresa> findByRuc(String ruc);

    /**
     * Verifica si existe una empresa con un RUC dado.
     * @param ruc El número de RUC a verificar.
     * @return true si existe una empresa con ese RUC, false en caso contrario.
     */
    boolean existsByRuc(String ruc);

    /**
     * Verifica si existe una empresa con un RUC dado, excluyendo un ID de empresa específico.
     * Esto es útil para la validación al editar una empresa.
     * @param ruc El número de RUC a verificar.
     * @param idEmpresa El ID de la empresa a excluir de la búsqueda.
     * @return true si existe otra empresa con ese RUC, false en caso contrario.
     */
    boolean existsByRucAndIdEmpresaIsNot(String ruc, Integer idEmpresa);
}

