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

    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;
    private TextView tvAvatarAdmin;
    private MaterialButton btnActivos, btnInactivos;
    private android.widget.ProgressBar progressUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario);

        sessionManager = new SessionManager(this);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        searchUsuarios = findViewById(R.id.searchUsuarios);
        tvAvatarAdmin  = findViewById(R.id.tvAvatarAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
        btnActivos     = findViewById(R.id.btnActivos);
        btnInactivos   = findViewById(R.id.btnInactivos);
        progressUsuarios = findViewById(R.id.progressUsuarios);
        setupSearch();

        // Avatar
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }
        tvAvatarAdmin.setOnClickListener(this::showProfileMenu);

        com.google.android.material.floatingactionbutton.FloatingActionButton fabAgregar =
                findViewById(R.id.fabAgregar);

        fabAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(UsuarioActivity.this, CrearUsuarioActivity.class);
            startActivity(intent);
        });

        usuarioRepository = new UsuarioRepository();

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);
        recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));

        btnActivos.setOnClickListener(v -> listarUsuariosActivos());
        btnInactivos.setOnClickListener(v -> listarUsuariosInactivos());

        setupBottomNav();
        listarUsuariosActivos();
    }

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_users);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                finish(); // Volver al dashboard
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

    @Override
    protected void onResume() {
        super.onResume();
        //listarUsuarios(true);
    }

    private void listarUsuariosActivos() {
        updateFilterButtons(true);
        if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.VISIBLE);
        recyclerUsuarios.setVisibility(android.view.View.GONE);
        tvEmptyMessage.setVisibility(android.view.View.GONE);
        usuarioRepository.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    currentFullList.clear();
                    for (Usuario u : response.body()) {
                        if (u.isActivo()) currentFullList.add(u);
                    }
                    filter(searchUsuarios.getQuery().toString());
                } else {
                    tvEmptyMessage.setText("Error al cargar usuarios");
                    tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                tvEmptyMessage.setText("Sin conexión. Intenta de nuevo.");
                tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(UsuarioActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listarUsuariosInactivos() {
        updateFilterButtons(false);
        if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.VISIBLE);
        recyclerUsuarios.setVisibility(android.view.View.GONE);
        tvEmptyMessage.setVisibility(android.view.View.GONE);
        usuarioRepository.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    currentFullList.clear();
                    for (Usuario u : response.body()) {
                        if (u.isInactivo()) currentFullList.add(u);
                    }
                    filter(searchUsuarios.getQuery().toString());
                } else {
                    tvEmptyMessage.setText("Error al cargar usuarios");
                    tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                if (progressUsuarios != null) progressUsuarios.setVisibility(android.view.View.GONE);
                tvEmptyMessage.setText("Sin conexión. Intenta de nuevo.");
                tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(UsuarioActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFilterButtons(boolean isActivosSelected) {
        if (isActivosSelected) {
            // Activos seleccionado
            btnActivos.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnActivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnActivos.setStrokeWidth(4);

            // Inactivos deseleccionado
            btnInactivos.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
            btnInactivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_borde)));
            btnInactivos.setStrokeWidth(2);
        } else {
            // Inactivos seleccionado
            btnInactivos.setTextColor(ContextCompat.getColor(this, R.color.verde_primario));
            btnInactivos.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_primario)));
            btnInactivos.setStrokeWidth(4);

            // Activos deseleccionado
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

    private void setupSearch() {
        searchUsuarios.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String text) {
        List<Usuario> filteredList = new ArrayList<>();
        String query = text.toLowerCase().trim();
        for (Usuario item : currentFullList) {
            if (item.getNombre().toLowerCase().contains(query) ||
                    item.getCorreo().toLowerCase().contains(query)) {
                filteredList.add(item);
            }
        }
        actualizarRecycler(filteredList, "SIN RESULTADOS");
    }
}