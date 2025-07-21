package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Model.DocumentoSequence;
import com.example.DyD_Natures.Repository.DocumentoSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentoSequenceService {

    @Autowired
    private DocumentoSequenceRepository documentoSequenceRepository;

    @Transactional
    public String getNextDocumentNumber(String tipoDocumento) {

        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseGet(() -> {

                    String prefix = "";
                    if ("FACTURA_VENTA".equals(tipoDocumento)) {
                        prefix = "FV01-";
                    } else if ("BOLETA_VENTA".equals(tipoDocumento)) {
                        prefix = "BV01-";
                    }

                    DocumentoSequence newSequence = new DocumentoSequence(tipoDocumento, 0, prefix);
                    return documentoSequenceRepository.save(newSequence);
                });

        sequence.setLastNumber(sequence.getLastNumber() + 1);
        documentoSequenceRepository.save(sequence); 

        return String.format("%s%06d", sequence.getPrefix(), sequence.getLastNumber());
    }

    @Transactional
    public void initializeSequence(String tipoDocumento, String prefix, Integer initialNumber) {
        documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .ifPresentOrElse(
                        seq -> System.out.println("Sequence for " + tipoDocumento + " already exists."),
                        () -> {
                            documentoSequenceRepository.save(new DocumentoSequence(tipoDocumento, initialNumber, prefix));
                            System.out.println("Initialized sequence for " + tipoDocumento);
                        }
                );
    }
}
