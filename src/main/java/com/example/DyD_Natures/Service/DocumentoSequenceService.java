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
        // Bloqueo pesimista ya manejado por el repositorio
        DocumentoSequence sequence = documentoSequenceRepository.findByTipoDocumento(tipoDocumento)
                .orElseGet(() -> {
                    // Si no existe, crea una nueva secuencia con un número inicial
                    // y un prefijo por defecto.
                    // DEBES AJUSTAR ESTOS VALORES INICIALES SEGÚN TUS NECESIDADES
                    String prefix = "";
                    if ("FACTURA_VENTA".equals(tipoDocumento)) {
                        prefix = "FV01-";
                    } else if ("BOLETA_VENTA".equals(tipoDocumento)) {
                        prefix = "BV01-";
                    }
                    // Puedes añadir más tipos según sea necesario

                    DocumentoSequence newSequence = new DocumentoSequence(tipoDocumento, 0, prefix);
                    return documentoSequenceRepository.save(newSequence);
                });

        sequence.setLastNumber(sequence.getLastNumber() + 1);
        documentoSequenceRepository.save(sequence); // Guarda el número incrementado

        // Formatear el número (ej. FV01-000001)
        return String.format("%s%06d", sequence.getPrefix(), sequence.getLastNumber());
    }

    // Opcional: Método para inicializar secuencias si no existen
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
