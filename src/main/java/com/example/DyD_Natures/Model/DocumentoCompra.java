package com.example.DyD_Natures.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

@Data
@Entity
@Table(name = "documento_compra")
public class DocumentoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra", nullable = false)
    private Integer idCompra;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documento_compra_proveedor1"))
    private Proveedor proveedor;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    private String tipoDocumento;

    @Column(name = "num_documento", nullable = false, length = 45)
    private String numDocumento;


    @JsonManagedReference
    @OneToMany(mappedBy = "documentoCompra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetalleCompra> detalleCompras = new ArrayList<>();

    @Column(name = "estado", nullable = true)
    private Byte estado = 1;

    public Byte getEstado() {
        return estado;
    }

    public void setEstado(Byte estado) {
        this.estado = estado;
    }

    public DocumentoCompra() {
    }

    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumDocumento() {
        return numDocumento;
    }

    public void setNumDocumento(String numDocumento) {
        this.numDocumento = numDocumento;
    }

    public List<DetalleCompra> getDetalleCompras() {
        return detalleCompras;
    }

    public void setDetalleCompras(List<DetalleCompra> detalleCompras) {
        this.detalleCompras.clear();
        if (detalleCompras != null) {
            for (DetalleCompra detalle : detalleCompras) {
                this.addDetalleCompra(detalle);
            }
        }
    }

    public void addDetalleCompra(DetalleCompra detalle) {
        this.detalleCompras.add(detalle);
        detalle.setDocumentoCompra(this);
    }

    public void removeDetalleCompra(DetalleCompra detalle) {
        this.detalleCompras.remove(detalle);
        detalle.setDocumentoCompra(null);
    }

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion") 
    private Usuario usuarioAnulacion; 


}
