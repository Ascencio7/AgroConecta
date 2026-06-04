package sv.edu.agroconecta.repository;

import java.util.List;
import retrofit2.Call;
import sv.edu.agroconecta.model.Rol;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;

public class UsuarioRepository {
    private UsuarioApi api;

    public UsuarioRepository() {
        api = ApiClient.getClient().create(UsuarioApi.class);
    }

    // Usuarios

    public Call<List<Usuario>> getUsuarios() {
        return api.getUsuarios();
    }

    public Call<Usuario> crear(Usuario usuario) {
        return api.createUsuario(usuario);
    }

    public Call<Usuario> actualizar(int id, Usuario usuario) {
        return api.updateUsuario(id, usuario);
    }

    public Call<Void> eliminarUsuario(int id) {
        return api.eliminarUsuario(id);
    }

    // Roles
    public Call<List<Rol>> getRoles() {
        return api.getRoles();
    }
}