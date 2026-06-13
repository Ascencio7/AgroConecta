package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
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
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.UsuarioAdapter;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.repository.UsuarioRepository;
import sv.edu.agroconecta.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import androidx.core.content.ContextCompat;

public class UsuarioActivity extends AppCompatActivity {

    private UsuarioRepository usuarioRepository;
    private RecyclerView recyclerUsuarios;
    private UsuarioAdapter adapter;
    private List<Usuario> currentFullList = new ArrayList<>();
    private SearchView searchUsuarios;
    private TextView tvEmptyMessage;
    private Spinner spFiltroRol;

    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;
    private TextView tvAvatarAdmin;
    private android.widget.ImageView ivAvatarFotoAdmin;
    private MaterialButton btnActivos, btnInactivos;
    private android.widget.ProgressBar progressUsuarios;
    private boolean showingActivos = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario);

        sessionManager = new SessionManager(this);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        searchUsuarios = findViewById(R.id.searchUsuarios);
        tvAvatarAdmin  = findViewById(R.id.tvAvatarAdmin);
        ivAvatarFotoAdmin = findViewById(R.id.ivAvatarFotoAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
        btnActivos     = findViewById(R.id.btnActivos);
        btnInactivos   = findViewById(R.id.btnInactivos);
        progressUsuarios = findViewById(R.id.progressUsuarios);
        spFiltroRol = findViewById(R.id.spFiltroRol);
        
        setupSearch();
        setupRoleFilter();

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
        View ivHeaderLogo = findViewById(R.id.ivHeaderLogoUsuarios);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        com.google.android.material.floatingactionbutton.FloatingActionButton fabAgregar =
                findViewById(R.id.fabAgregar);

        fabAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(UsuarioActivity.this, CrearUsuarioActivity.class);
            startActivity(intent);
        });

        usuarioRepository = new UsuarioRepository();

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);
        recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));

        btnActivos.setOnClickListener(v -> {
            showingActivos = true;
            filter();
        });
        btnInactivos.setOnClickListener(v -> {
            showingActivos = false;
            filter();
        });

        setupBottomNav();
        cargarUsuarios();
    }

    private void setupRoleFilter() {
        String[] roles = {"Todos", "Admin", "Vendedor", "Cliente"};
        android.widget.ArrayAdapter<String> adapterRole = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapterRole.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltroRol.setAdapter(adapterRole);
        spFiltroRol.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { filter(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }

    private void cargarUsuarios() {
        if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.VISIBLE);
        usuarioRepository.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    currentFullList = response.body();
                    filter();
                }
            }
            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                Toast.makeText(UsuarioActivity.this, "Error al cargar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    private void filter() {
        updateFilterButtons(showingActivos);
        String query = searchUsuarios.getQuery().toString().toLowerCase().trim();
        String selectedRole = spFiltroRol.getSelectedItem().toString();
        
        List<Usuario> filteredList = new ArrayList<>();
        for (Usuario u : currentFullList) {
            boolean matchesStatus = (showingActivos && u.isActivo()) || (!showingActivos && u.isInactivo());
            boolean matchesSearch = u.getNombre().toLowerCase().contains(query) || u.getCorreo().toLowerCase().contains(query);
            
            // Lógica de Rol para el filtro
            String userRol = u.getRol();
            if (userRol == null || userRol.isEmpty()) {
                if (u.getRolId() == 1) userRol = "Admin";
                else if (u.getRolId() == 2) userRol = "Vendedor";
                else if (u.getRolId() == 3) userRol = "Cliente";
                else userRol = "Usuario";
            }
            
            boolean matchesRole = selectedRole.equals("Todos") || (userRol != null && userRol.equalsIgnoreCase(selectedRole));
            
            if (matchesStatus && matchesSearch && matchesRole) {
                filteredList.add(u);
            }
        }
        actualizarRecycler(filteredList, "SIN RESULTADOS");
    }

    private void setupSearch() {
        searchUsuarios.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filter(); return true; }
        });
    }

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_users);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                finish();
                return true;
            } else if (id == R.id.nav_admin_products) {
                startActivity(new Intent(this, ProductosAdminActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_admin_users;
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

    private void updateFilterButtons(boolean isActivosSelected) {
        if (isActivosSelected) {
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

    private void actualizarRecycler(List<Usuario> filtrados, String mensajeVacio) {
        if (adapter == null) {
            adapter = new UsuarioAdapter(filtrados, UsuarioActivity.this);
            recyclerUsuarios.setAdapter(adapter);
        } else {
            adapter.updateList(filtrados);
        }

        if (filtrados.isEmpty()) {
            tvEmptyMessage.setText(mensajeVacio);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerUsuarios.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerUsuarios.setVisibility(View.VISIBLE);
        }
    }
}
