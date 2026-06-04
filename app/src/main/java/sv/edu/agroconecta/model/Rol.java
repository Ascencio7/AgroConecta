package sv.edu.agroconecta.model;

public class Rol {

    private int rol_id;
    private String nombre;

    public Rol() {}

    public int getRolId() {
        return rol_id;
    }

    public void setRolId(int rol_id) {
        this.rol_id = rol_id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}