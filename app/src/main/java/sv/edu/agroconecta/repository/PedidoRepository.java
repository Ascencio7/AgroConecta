package sv.edu.agroconecta.repository;

import java.util.List;
import retrofit2.Call;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.PedidoApi;

public class PedidoRepository {
    private PedidoApi api;

    public PedidoRepository() {
        api = ApiClient.getClient().create(PedidoApi.class);
    }

    public Call<List<Pedido>> getPedidos() {
        return api.getPedidos();
    }

    public Call<Pedido> getPedidoPorId(int id) {
        return api.getPedidoPorId(id);
    }

    public Call<Pedido> crearPedido(Pedido pedido) {
        return api.crearPedido(pedido);
    }

    public Call<Pedido> actualizarPedido(int id, Pedido pedido) {
        return api.actualizarPedido(id, pedido);
    }
}