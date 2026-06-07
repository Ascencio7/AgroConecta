package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
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
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.ProductAdminAdapter;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.ProductApi;
import sv.edu.agroconecta.utils.SessionManager;

public class ProductosAdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdminAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private ProductApi productApi;
    private TextView tvEmpty, tvAvatarAdmin;
    private android.widget.ProgressBar progressProductos;
    private SearchView searchView;
    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos_admin);

        sessionManager = new SessionManager(this);
        productApi = ApiClient.getClient().create(ProductApi.class);

        initViews();
        setupRecyclerView();
        setupSearch();
        loadProducts();

        // Avatar
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }
        tvAvatarAdmin.setOnClickListener(this::showProfileMenu);

        findViewById(R.id.fabAgregarProducto).setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarEditarProductoActivity.class);
            startActivity(intent);
        });

        setupBottomNav();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerProductos);
        tvEmpty = findViewById(R.id.tvEmptyProducts);
        searchView = findViewById(R.id.searchProducts);
        progressProductos = findViewById(R.id.progressProductosAdmin);
        tvAvatarAdmin = findViewById(R.id.tvAvatarAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
    }

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_products);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                finish();
                return true;
            } else if (id == R.id.nav_admin_users) {
                startActivity(new Intent(this, UsuarioActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_admin_products;
        });
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_view_profile) {
                mostrarPerfil();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void mostrarPerfil() {
        Intent intent = new Intent(this, PerfilAdminActivity.class);
        startActivity(intent);
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupRecyclerView() {
        adapter = new ProductAdminAdapter(this, filteredProducts, new ProductAdminAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                Intent intent = new Intent(ProductosAdminActivity.this, AgregarEditarProductoActivity.class);
                intent.putExtra("producto_id", product.getProductoId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Product product) {
                confirmDelete(product);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadProducts() {
        if (progressProductos != null) progressProductos.setVisibility(android.view.View.VISIBLE);
        recyclerView.setVisibility(android.view.View.GONE);
        tvEmpty.setVisibility(android.view.View.GONE);
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    for (Product p : response.body()) {
                        if (p.getEstado() == null || p.isActivo()) allProducts.add(p);
                    }
                    filter(searchView.getQuery().toString());
                } else {
                    tvEmpty.setText("Error al cargar productos");
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(ProductosAdminActivity.this, "Error cargando productos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                tvEmpty.setText("Sin conexión. Intenta de nuevo.");
                tvEmpty.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(ProductosAdminActivity.this, "Error cargando productos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String query) {
        filteredProducts.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (Product p : allProducts) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            String price = String.valueOf(p.getPrice());
            
            if (name.contains(lowerQuery) || price.contains(lowerQuery)) {
                filteredProducts.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        
        if (filteredProducts.isEmpty()) {
            tvEmpty.setText("SIN RESULTADOS");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Producto")
                .setMessage("¿Estás seguro de eliminar '" + product.getName() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> logicalDelete(product))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void logicalDelete(Product product) {
        product.setEstado(false);
        productApi.actualizarProducto(product.getProductoId(), product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductosAdminActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                    loadProducts();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(ProductosAdminActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }
}