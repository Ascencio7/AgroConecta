package sv.edu.agroconecta.repository;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.ProductApi;

public class ProductoRepository {
    private ProductApi api;

    public ProductoRepository() {
        api = ApiClient.getClient().create(ProductApi.class);
    }

    public Call<List<Product>> getProductos() {
        return api.getProductos();
    }

    public Call<Product> getProductoPorId(int id) {
        return api.getProductoPorId(id);
    }

    public Call<Product> crearProducto(Product product) {
        return api.crearProducto(product);
    }

    public Call<ResponseBody> actualizarProducto(int id, Product product) {
        return api.actualizarProducto(id, product);
    }

    public Call<Void> eliminarProducto(int id) {
        return api.eliminarProducto(id);
    }
}