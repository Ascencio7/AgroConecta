package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.adapter.ProductAdapter;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.utils.SessionManager;
import sv.edu.agroconecta.utils.CarritoManager;

public class BuyerModuleActivity extends AppCompatActivity {

    interface ProductoApi {
        @GET("productos")
        Call<List<Product>> getProductos();
    }

    private SearchView searchView;
    private RecyclerView recyclerView;
    private Button btnHistorial, btnMisPedidos;
    private ImageButton btnLogout;
    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private ProgressBar progressBar;
    private android.widget.TextView tvEmpty;
    private int usuarioId;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_module);

        sessionManager = new SessionManager(this);
        usuarioId = getIntent().getIntExtra("usuario_id", -1);

        initViews();
        setupRecyclerView();
        loadProducts();
        setupSearch();
        setupButtons();
    }

    private void initViews() {
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerViewProducts);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnMisPedidos = findViewById(R.id.btnMisPedidos);
        btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBarProductos);
        tvEmpty = findViewById(R.id.tvEmptyProductos);
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(this, allProducts,
                product -> agregarAlCarrito(product),
                (product, rating) -> rateProduct(product, rating)
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadProducts() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        ProductoApi productoApi = ApiClient.getClient().create(ProductoApi.class);

        productoApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    allProducts.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (allProducts.isEmpty()) {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvEmpty != null) { tvEmpty.setText("Error cargando productos"); tvEmpty.setVisibility(View.VISIBLE); }
                    Toast.makeText(BuyerModuleActivity.this,
                            "Error cargando productos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (tvEmpty != null) { tvEmpty.setText("Sin conexión. Intenta de nuevo."); tvEmpty.setVisibility(View.VISIBLE); }
                Toast.makeText(BuyerModuleActivity.this,
                        "Error de conexión", Toast.LENGTH_SHORT).show();
                Log.e("PRODUCTOS", "ERROR:", t);
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });
    }

    private void filterProducts(String query) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(p);
            }
        }
        adapter.updateList(filtered);
    }

    private void setupButtons() {
        btnHistorial.setOnClickListener(v ->
                startActivity(new Intent(this, MisPedidosActivity.class))
        );

        btnMisPedidos.setOnClickListener(v ->
                startActivity(new Intent(this, CarritoActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de que deseas salir?")
                    .setPositiveButton("Sí, salir", (dialog, which) -> {
                        sessionManager.logout();
                        Intent intent = new Intent(BuyerModuleActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void agregarAlCarrito(Product product) {
        Intent intent = new Intent(this, ProductoDetalleActivity.class);
        intent.putExtra("producto_id", product.getProductoId());
        intent.putExtra("nombre", product.getNombre());
        intent.putExtra("descripcion", product.getDescripcion());
        intent.putExtra("precio", product.getPrecio());
        intent.putExtra("imagen", product.getImagen());
        intent.putExtra("categoria", product.getCategoria());
        intent.putExtra("existencia", product.getExistencia());
        startActivity(intent);
    }

    private void rateProduct(Product product, float rating) {
        Toast.makeText(this, "⭐ Calificaste " + product.getName() + ": " + rating + "/5", Toast.LENGTH_SHORT).show();
    }
}