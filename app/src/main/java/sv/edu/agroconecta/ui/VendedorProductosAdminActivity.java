package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.ProductAdminAdapter;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.ProductApi;
import com.google.android.material.button.MaterialButton;

public class VendedorProductosAdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdminAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private ProductApi productApi;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvNombreHeader;
    private SearchView searchView;
    private int vendedorId;
    private String vendedorNombre;
    
    private MaterialButton btnDisponibles, btnNoDisponibles;
    private boolean showingAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendedor_productos_admin);

        productApi = ApiClient.getClient().create(ProductApi.class);
        vendedorId = getIntent().getIntExtra("vendedor_id", -1);
        vendedorNombre = getIntent().getStringExtra("vendedor_nombre");

        initViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        loadProducts();

        tvNombreHeader.setText(vendedorNombre);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.fabAgregarProductoVendedor).setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarEditarProductoActivity.class);
            intent.putExtra("vendedor_id_forced", vendedorId);
            startActivity(intent);
        });
        
        View ivHeaderLogo = findViewById(R.id.ivHeaderLogoVendedorProductos);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        setupBottomNav();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerProductos);
        progressBar = findViewById(R.id.progressProductos);
        tvEmpty = findViewById(R.id.tvEmptyMessage);
        tvNombreHeader = findViewById(R.id.tvVendedorNombreHeader);
        searchView = findViewById(R.id.searchProducts);
        btnDisponibles = findViewById(R.id.btnProductosDisponibles);
        btnNoDisponibles = findViewById(R.id.btnProductosNoDisponibles);
    }

    private void setupFilters() {
        btnDisponibles.setOnClickListener(v -> {
            showingAvailable = true;
            updateFilterButtons();
            filter(searchView.getQuery().toString());
        });

        btnNoDisponibles.setOnClickListener(v -> {
            showingAvailable = false;
            updateFilterButtons();
            filter(searchView.getQuery().toString());
        });

        updateFilterButtons();
    }

    private void updateFilterButtons() {
        if (showingAvailable) {
            btnDisponibles.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnDisponibles.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnDisponibles.setStrokeWidth(4);
            btnNoDisponibles.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
            btnNoDisponibles.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_borde)));
            btnNoDisponibles.setStrokeWidth(2);
        } else {
            btnNoDisponibles.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnNoDisponibles.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnNoDisponibles.setStrokeWidth(4);
            btnDisponibles.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
            btnDisponibles.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_borde)));
            btnDisponibles.setStrokeWidth(2);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductAdminAdapter(this, filteredProducts, new ProductAdminAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                Intent intent = new Intent(VendedorProductosAdminActivity.this, AgregarEditarProductoActivity.class);
                intent.putExtra("producto_id", product.getProductoId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Product product) {
                confirmDelete(product);
            }

            @Override
            public void onRestore(Product product) {
                confirmRestore(product);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    for (Product p : response.body()) {
                        if (p.getUsuarioId() != null && p.getUsuarioId() == vendedorId) {
                            allProducts.add(p);
                        }
                    }
                    filter(searchView.getQuery().toString());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(VendedorProductosAdminActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String query) {
        filteredProducts.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (Product p : allProducts) {
            boolean matchesStatus = showingAvailable ? (p.getEstado() == null || p.getEstado()) : (p.getEstado() != null && !p.getEstado());
            String nombreP = p.getName() != null ? p.getName().toLowerCase() : "";
            boolean matchesSearch = nombreP.contains(lowerQuery);

            if (matchesStatus && matchesSearch) {
                filteredProducts.add(p);
            }
        }
        adapter.updateList(filteredProducts);

        if (filteredProducts.isEmpty()) {
            if (allProducts.isEmpty()) {
                tvEmpty.setText("EL VENDEDOR NO TIENE PRODUCTOS AGREGADOS");
            } else {
                tvEmpty.setText("No se encontraron resultados");
            }
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_products);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_admin_users) {
                Intent intent = new Intent(this, UsuarioActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return id == R.id.nav_admin_products;
        });
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Desactivar Producto")
                .setMessage("¿Estás seguro de marcar '" + product.getName() + "' como No Disponible?")
                .setPositiveButton("Desactivar", (dialog, which) -> logicalDelete(product))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void logicalDelete(Product product) {
        product.setEstado(false);
        productApi.actualizarProducto(product.getProductoId(), product).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VendedorProductosAdminActivity.this, "Producto desactivado", Toast.LENGTH_SHORT).show();
                    loadProducts();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(VendedorProductosAdminActivity.this, "Error al desactivar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmRestore(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Restaurar Producto")
                .setMessage("¿Estás seguro de marcar '" + product.getName() + "' como Disponible?")
                .setPositiveButton("Restaurar", (dialog, which) -> restoreProduct(product))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void restoreProduct(Product product) {
        product.setEstado(true);
        productApi.actualizarProducto(product.getProductoId(), product).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VendedorProductosAdminActivity.this, "Producto restaurado", Toast.LENGTH_SHORT).show();
                    loadProducts();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(VendedorProductosAdminActivity.this, "Error al restaurar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
