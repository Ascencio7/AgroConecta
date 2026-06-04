package sv.edu.agroconecta.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import sv.edu.agroconecta.modelo.Pedido;

public interface PedidoApi {
    @GET("pedidos")
    Call<List<Pedido>> getPedidos();

    @GET("pedidos/{id}")
    Call<Pedido> getPedidoPorId(@Path("id") int id);

    @GET("pedidos/usuario/{usuario_id}")
    Call<List<Pedido>> getPedidosPorUsuario(@Path("usuario_id") int usuarioId);

    @POST("pedidos")
    Call<Pedido> crearPedido(@Body Pedido pedido);

    @PUT("pedidos/{id}")
    Call<Pedido> actualizarPedido(@Path("id") int id, @Body Pedido pedido);

    @POST("calificaciones")
    Call<Void> calificar(@Body java.util.Map<String, Object> calificacion);
}