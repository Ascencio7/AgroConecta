package sv.edu.agroconecta.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;
import sv.edu.agroconecta.model.Rol;
import sv.edu.agroconecta.model.Usuario;

public interface UsuarioApi {

    @GET("usuarios")
    Call<List<Usuario>> getUsuarios();

    @POST("usuarios")
    Call<Usuario> createUsuario(@Body Usuario usuario);

    @PUT("usuarios/{id}")
    Call<Usuario> updateUsuario(@Path("id") int id, @Body Usuario usuario);

    // DELETE no retorna Usuario
    @DELETE("usuarios/{id}")
    Call<Void> eliminarUsuario(@Path("id") int id);

    @GET("roles")
    Call<List<Rol>> getRoles();
}