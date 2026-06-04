package sv.edu.agroconecta.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import sv.edu.agroconecta.modelo.Categoria;

public interface CategoriaApi {
    @GET("categorias")
    Call<List<Categoria>> getCategorias();
}
