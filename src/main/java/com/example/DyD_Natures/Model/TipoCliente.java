package com.example.DyD_Natures.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_cliente")
public class TipoCliente {

    @Id
    @Column(name = "id_rol_cliente", nullable = false)
    private Integer idRolCliente;

    @Column(name = "rol_cliente", nullable = false, length = 100)
    private String rolCliente;

    public TipoCliente() {
    }

    public Integer getIdRolCliente() {
        return idRolCliente;
    }

    public void setIdRolCliente(Integer idRolCliente) {
        this.idRolCliente = idRolCliente;
    }

    public String getRolCliente() {
        return rolCliente;
    }

    public void setRolCliente(String rolCliente) {
        this.rolCliente = rolCliente;
    }
}


