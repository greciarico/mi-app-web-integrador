package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DocumentoSequence; // Necesitarás crear este Modelo
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock; // Para bloqueo pesimista
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface DocumentoSequenceRepository extends JpaRepository<DocumentoSequence, Integer> {

    // Usa bloqueo pesimista para asegurar el incremento atómico de la secuencia
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentoSequence> findByTipoDocumento(String tipoDocumento);
}
