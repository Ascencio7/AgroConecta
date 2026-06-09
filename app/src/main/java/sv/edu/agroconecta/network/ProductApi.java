package sv.edu.agroconecta.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import okhttp3.ResponseBody;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import sv.edu.agroconecta.modelo.Product;

public interface ProductApi {
    @GET("productos")
    Call<List<Product>> getProductos();

    @GET("productos/{id}")
    Call<Product> getProductoPorId(@Path("id") int id);

    @POST("productos")
    Call<Product> crearProducto(@Body Product product);

    @PUT("productos/{id}")
    Call<ResponseBody> actualizarProducto(@Path("id") int id, @Body Product product);

    @DELETE("productos/{id}")
    Call<Void> eliminarProducto(@Path("id") int id);
}
