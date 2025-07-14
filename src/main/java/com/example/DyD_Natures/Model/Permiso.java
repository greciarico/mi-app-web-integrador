package com.example.DyD_Natures.Model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "permiso")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;        // Ej. "VER_USUARIOS", "EDT_VENTAS"

    @Column(name = "url_pattern", nullable = false, length = 100)
    private String urlPattern;    // Ej. "/usuarios/**", "/ventas/**"

    // Constructores
    public Permiso() {}
    public Permiso(String nombre, String urlPattern) {
        this.nombre = nombre;
        this.urlPattern = urlPattern;
    }

    // Getters / setters
    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUrlPattern() { return urlPattern; }
    public void setUrlPattern(String urlPattern) { this.urlPattern = urlPattern; }

    // equals/hashCode (por id)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permiso)) return false;
        Permiso p = (Permiso) o;
        return Objects.equals(idPermiso, p.idPermiso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPermiso);
    }
}
