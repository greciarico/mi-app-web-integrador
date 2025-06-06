package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.Categoria;
import com.example.DyD_Natures.Repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}

