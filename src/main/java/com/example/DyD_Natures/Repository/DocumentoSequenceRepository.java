package com.example.DyD_Natures.Repository;

import com.example.DyD_Natures.Model.DocumentoSequence; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock; 
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface DocumentoSequenceRepository extends JpaRepository<DocumentoSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentoSequence> findByTipoDocumento(String tipoDocumento);
}
