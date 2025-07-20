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

    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdUsuarioIsNot(String dni, Integer idUsuario);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.fechaRegistro BETWEEN :startDate AND :endDate AND u.estado = 1")
    Long countByFechaRegistroBetweenAndEstadoIsTrue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = 1")
    Long countAllActiveUsers();

    boolean existsByDniAndEstadoNot(String dni, Byte estadoExcluido);

    boolean existsByDniAndIdUsuarioIsNotAndEstadoNot(
            String dni,
            Integer idUsuario,
            Byte estadoExcluido
    );

    Optional<Usuario> findByDniAndEstadoNot(String dni, Byte estadoExcluido);

    Optional<Usuario> findByNombre(String nombre);

    List<Usuario> findByRolUsuario_TipoRolAndEstadoNot(String tipoRol,Byte estado);
}
