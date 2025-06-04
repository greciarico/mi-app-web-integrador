package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
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


}
