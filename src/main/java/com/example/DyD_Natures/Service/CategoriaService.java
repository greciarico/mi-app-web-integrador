package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Dto.CategoriaFilterDTO;
import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Repository.CategoriaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    /**
     * Lista todas las categorías que no tienen el estado '2' (eliminado lógicamente).
     * @return Lista de categorías activas/inactivas (no eliminadas).
     */
    public List<Categoria> listarCategoriasActivas() {
        // CAMBIO CLAVE AQUÍ: Llama al nuevo método y pasa un Byte
        return categoriaRepository.findByEstadoExcluding((byte) 2);
    }

    /**
     * Obtiene una categoría por su ID.
     * @param id El ID de la categoría.
     * @return Un Optional que contiene la Categoría si se encuentra, o vacío si no.
     */
    public Optional<Categoria> obtenerCategoriaPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    /**
     * Guarda una categoría nueva o actualiza una existente.
     * Si es una categoría nueva, establece el estado inicial a '1' (Activo).
     * @param categoria El objeto Categoria a guardar.
     * @return La Categoria guardada.
     */
    public Categoria guardarCategoria(Categoria categoria) {
        if (categoria.getIdCategoria() == null) {
            categoria.setEstado((byte) 1); // Nuevo por defecto es Activo (Byte)
        }
        return categoriaRepository.save(categoria);
    }

    /**
     * Realiza una eliminación lógica de una categoría, cambiando su estado a '2'.
     * @param id El ID de la categoría a eliminar lógicamente.
     */
    public void eliminarCategoria(Integer id) {
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (categoriaOpt.isPresent()) {
            Categoria categoria = categoriaOpt.get();
            categoria.setEstado((byte) 2); // CAMBIO CLAVE: Establece el estado a 2 (eliminado lógicamente)
            categoriaRepository.save(categoria);
        }
    }

    /**
     * Verifica si un nombre de categoría ya existe en la base de datos, excluyendo un ID de categoría específico.
     * @param nombreCategoria El nombre de categoría a verificar.
     * @param idCategoria El ID de la categoría a excluir de la búsqueda (null para nuevas creaciones).
     * @return true si existe otra categoría con ese nombre, false en caso contrario.
     */
    public boolean existsByNombreCategoriaExcludingId(String nombreCategoria, Integer idCategoria) {
        if (idCategoria != null) {
            return categoriaRepository.existsByNombreCategoriaAndIdCategoriaIsNot(nombreCategoria, idCategoria);
        }
        return categoriaRepository.existsByNombreCategoria(nombreCategoria);
    }
    /**
     * Busca categorías aplicando filtros dinámicamente para la generación de reportes.
     * @param filterDTO DTO con los criterios de búsqueda (nombre, estados).
     * @return Lista de categorías que coinciden con los filtros.
     */
    public List<Categoria> buscarCategoriasPorFiltros(CategoriaFilterDTO filterDTO) {
        return categoriaRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por nombre de categoría
            if (filterDTO.getNombreCategoria() != null && !filterDTO.getNombreCategoria().trim().isEmpty()) {
                String searchTerm = "%" + filterDTO.getNombreCategoria().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nombreCategoria")), searchTerm));
            }

            // Filtro por Estado - Ahora maneja múltiples selecciones
            if (filterDTO.getEstados() != null && !filterDTO.getEstados().isEmpty()) {
                // Si eligen ambos (activo y inactivo), no se añade este filtro (ya que se listan todos excepto eliminados)
                if (!(filterDTO.getEstados().contains((byte) 0) && filterDTO.getEstados().contains((byte) 1))) {
                    predicates.add(root.get("estado").in(filterDTO.getEstados()));
                }
            }

            // Excluir categorías con estado = 2 (eliminado) por defecto en los reportes
            predicates.add(criteriaBuilder.notEqual(root.get("estado"), (byte) 2));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
