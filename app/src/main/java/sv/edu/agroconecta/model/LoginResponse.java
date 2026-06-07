package sv.edu.agroconecta.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private int usuario_id;
    private String nombre;
    private String correo;
    private String rol;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getUsuarioId() {
        return usuario_id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    // NO BORRAR SE USA PARA RENDER
    @Override
    public String toString() {
        return "LoginResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", usuario_id=" + usuario_id +
                ", nombre='" + nombre + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}