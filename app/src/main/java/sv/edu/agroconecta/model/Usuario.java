package sv.edu.agroconecta.model;

import com.google.gson.annotations.SerializedName;

public class Usuario {

    @SerializedName("usuario_id")
    private int usuarioId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("correo")
    private String correo;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("fecha_registro")
    private String fechaRegistro;

    @SerializedName("estado")
    private Boolean estado;

    @SerializedName("rol_id")
    private int rolId;

    @SerializedName("nombre_rol")
    private String rol;

    // En el backend recibe "password"
    @SerializedName("password")
    private String password;

    public int getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getFechaRegistro() { return fechaRegistro; }
    public Boolean getEstado() { return estado; }
    public int getRolId() { return rolId; }
    public String getRol() { return rol; }
    public String getPassword() { return password; }

    public boolean isActivo() {
        return Boolean.TRUE.equals(estado);
    }

    public boolean isInactivo() {
        return Boolean.FALSE.equals(estado);
    }

    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public void setEstado(Boolean estado) { this.estado = estado; }
    public void setRolId(int rolId) { this.rolId = rolId; }
    public void setRol(String rol) { this.rol = rol; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return nombre + " (" + correo + ")";
    }
}