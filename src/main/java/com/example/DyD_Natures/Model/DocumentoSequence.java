package com.example.DyD_Natures.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "documento_sequence")
public class DocumentoSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tipo_documento", unique = true, nullable = false)
    private String tipoDocumento; // ej., "BOLETA", "FACTURA"

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;

    @Column(name = "prefix")
    private String prefix; // ej., "B001-", "F001-"

    // Constructores
    public DocumentoSequence() {
    }

    public DocumentoSequence(String tipoDocumento, Integer lastNumber, String prefix) {
        this.tipoDocumento = tipoDocumento;
        this.lastNumber = lastNumber;
        this.prefix = prefix;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Integer getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(Integer lastNumber) {
        this.lastNumber = lastNumber;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
