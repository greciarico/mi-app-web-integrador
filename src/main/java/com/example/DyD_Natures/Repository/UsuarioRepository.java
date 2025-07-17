package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByDniAndEstadoIsTrue(String dni);
    List<Usuario> findByEstadoNot(Byte estado);

    /**
     * Busca un Usuario por su número de DNI.
     * @param dni El número de DNI a buscar.
     * @return Un Optional que contiene el Usuario si se encuentra, o vacío si no.
     */
    Optional<Usuario> findByDni(String dni);

    /**
     * Verifica si existe un usuario con un DNI dado.
     * Este método es útil para la creación de nuevos usuarios.
     * @param dni El número de DNI a verificar.
     * @return true si existe un usuario con ese DNI, false en caso contrario.
     */
    boolean existsByDni(String dni);

    /**
     * Verifica si existe un usuario con un DNI dado, excluyendo un ID de usuario específico.
     * Este método es útil para la edición de usuarios, para permitir que un usuario mantenga su DNI
     * sin que se considere una duplicación consigo mismo.
     * @param dni El número de DNI a verificar.
     * @param idUsuario El ID del usuario a excluir de la búsqueda.
     * @return true si existe otro usuario con ese DNI (diferente al idUsuario proporcionado), false en caso contrario.
     */
    boolean existsByDniAndIdUsuarioIsNot(String dni, Integer idUsuario);

    /**
     * Cuenta el número total de usuarios registrados en un rango de fechas.
     * @param startDate Fecha de inicio (inclusive).
     * @param endDate Fecha de fin (inclusive).
     * @return Número de usuarios registrados para el período.
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.fechaRegistro BETWEEN :startDate AND :endDate AND u.estado = 1") // Asumo estado = 1 para activos
    Long countByFechaRegistroBetweenAndEstadoIsTrue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Para contar el total de usuarios activos en toda la historia (sin filtro de fecha de registro)
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = 1") // Asumo estado = 1 para activos
    Long countAllActiveUsers();

    /** Existe un usuario (estado ≠ 2) con este DNI? */
    boolean existsByDniAndEstadoNot(String dni, Byte estadoExcluido);

    /** Existe otro usuario distinto a idUsuario con este DNI y estado ≠ 2? */
    boolean existsByDniAndIdUsuarioIsNotAndEstadoNot(
            String dni,
            Integer idUsuario,
            Byte estadoExcluido
    );

    /** Si lo usas también para lectura: */
    Optional<Usuario> findByDniAndEstadoNot(String dni, Byte estadoExcluido);

    // --- MÉTODO CORREGIDO ---
    /**
     * Busca un Usuario por su nombre.
     * @param nombre El nombre a buscar.
     * @return Un Optional que contiene el Usuario si se encuentra, o vacío si no.
     */
    Optional<Usuario> findByNombre(String nombre);
}
