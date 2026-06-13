package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.VendedorAdapter;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;
import sv.edu.agroconecta.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class ProductosAdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VendedorAdapter vendedorAdapter;
    private List<Usuario> allVendedores = new ArrayList<>();
    private List<Usuario> filteredVendedores = new ArrayList<>();
    private UsuarioApi usuarioApi;
    private TextView tvEmpty, tvAvatarAdmin;
    private android.widget.ImageView ivAvatarFotoAdmin;
    private android.widget.ProgressBar progressProductos;
    private SearchView searchView;
    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;
    
    private MaterialButton btnActivos, btnInactivos;
    private boolean showingActivos = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos_admin);

        sessionManager = new SessionManager(this);
        usuarioApi = ApiClient.getClient().create(UsuarioApi.class);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        loadVendedores();

        // Avatar
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }
        // Foto de perfil
        String fotoAdmin = sessionManager.getFotoPerfil();
        if (fotoAdmin != null && !fotoAdmin.isEmpty() && ivAvatarFotoAdmin != null) {
            com.bumptech.glide.Glide.with(this)
                    .load(fotoAdmin)
                    .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                    .into(ivAvatarFotoAdmin);
            ivAvatarFotoAdmin.setVisibility(View.VISIBLE);
            tvAvatarAdmin.setVisibility(View.GONE);
        }

        tvAvatarAdmin.setOnClickListener(this::showProfileMenu);
        if (ivAvatarFotoAdmin != null) ivAvatarFotoAdmin.setOnClickListener(this::showProfileMenu);

        // Logo del header -> ir a la pantalla principal del admin (Dashboard)
        View ivHeaderLogo = findViewById(R.id.ivHeaderLogoProductosAdmin);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // El FAB para agregar productos generales lo ocultamos o lo dejamos para redirección a agregar sin vendedor predefinido
        View fab = findViewById(R.id.fabAgregarProducto);
        if (fab != null) fab.setVisibility(View.GONE);

        setupBottomNav();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerProductos);
        tvEmpty = findViewById(R.id.tvEmptyProducts);
        searchView = findViewById(R.id.searchProducts);
        progressProductos = findViewById(R.id.progressProductosAdmin);
        tvAvatarAdmin = findViewById(R.id.tvAvatarAdmin);
        ivAvatarFotoAdmin = findViewById(R.id.ivAvatarFotoAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
        
        btnActivos = findViewById(R.id.btnVendedoresActivos);
        btnInactivos = findViewById(R.id.btnVendedoresInactivos);
        
        ((TextView) findViewById(R.id.tvHeaderSubtitle)).setText("Gestión de Productos 📦");
    }

    private void setupFilters() {
        btnActivos.setOnClickListener(v -> {
            showingActivos = true;
            filterVendedores(searchView.getQuery().toString());
        });
        btnInactivos.setOnClickListener(v -> {
            showingActivos = false;
            filterVendedores(searchView.getQuery().toString());
        });
    }

    private void updateFilterButtons() {
        if (showingActivos) {
            btnActivos.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnActivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnActivos.setStrokeWidth(4);
            btnInactivos.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
            btnInactivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_borde)));
            btnInactivos.setStrokeWidth(2);
        } else {
            btnInactivos.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnInactivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnInactivos.setStrokeWidth(4);
            btnActivos.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
            btnActivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_borde)));
            btnActivos.setStrokeWidth(2);
        }
    }

    private void setupRecyclerView() {
        vendedorAdapter = new VendedorAdapter(this, filteredVendedores, new VendedorAdapter.OnVendedorActionListener() {
            @Override
            public void onVendedorClick(Usuario vendedor) {
                Intent intent = new Intent(ProductosAdminActivity.this, VendedorProductosAdminActivity.class);
                intent.putExtra("vendedor_id", vendedor.getUsuarioId());
                intent.putExtra("vendedor_nombre", vendedor.getNombre());
                startActivity(intent);
            }

            @Override
            public void onStatusChange(Usuario vendedor, boolean activate) {
                confirmarCambioEstado(vendedor, activate);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(vendedorAdapter);
    }

    private void confirmarCambioEstado(Usuario v, boolean activate) {
        String titulo = activate ? "Activar Vendedor" : "Desactivar Vendedor";
        String mensaje = activate ? "¿Deseas permitir que este vendedor publique productos?" : "¿Deseas desactivar este vendedor?";
        
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Sí", (dialog, which) -> cambiarEstadoVendedor(v, activate))
                .setNegativeButton("No", null)
                .show();
    }

    private void cambiarEstadoVendedor(Usuario v, boolean activate) {
        v.setEstado(activate);
        // Para actualizar usamos el mismo endpoint de actualizar usuario
        // pero solo mandamos los campos necesarios si el backend lo permite,
        // o mandamos el objeto completo.
        usuarioApi.updateUsuario(v.getUsuarioId(), v).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductosAdminActivity.this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                    loadVendedores();
                } else {
                    Toast.makeText(ProductosAdminActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(ProductosAdminActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVendedores() {
        if (progressProductos != null) progressProductos.setVisibility(android.view.View.VISIBLE);
        usuarioApi.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allVendedores.clear();
                    for (Usuario u : response.body()) {
                        String rol = u.getRol();
                        if ("VENDEDOR".equalsIgnoreCase(rol) || u.getRolId() == 2) {
                            allVendedores.add(u);
                        }
                    }
                    filterVendedores(searchView.getQuery().toString());
                }
            }
            @Override public void onFailure(Call<List<Usuario>> call, Throwable t) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                Toast.makeText(ProductosAdminActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filterVendedores(newText);
                return true;
            }
        });
    }

    private void filterVendedores(String query) {
        updateFilterButtons();
        filteredVendedores.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (Usuario v : allVendedores) {
            boolean matchesStatus = (showingActivos && v.isActivo()) || (!showingActivos && v.isInactivo());
            boolean matchesSearch = v.getNombre().toLowerCase().contains(lowerQuery) || v.getCorreo().toLowerCase().contains(lowerQuery);
            
            if (matchesStatus && matchesSearch) {
                filteredVendedores.add(v);
            }
        }
        vendedorAdapter.updateList(filteredVendedores);

        if (filteredVendedores.isEmpty()) {
            tvEmpty.setText("No se encontraron vendedores");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNav() {
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
            } else if (id == R.id.menu_soporte) {
                startActivity(new Intent(this, SoporteActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                confirmarLogout();
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
}
