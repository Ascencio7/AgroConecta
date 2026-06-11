package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import sv.edu.agroconecta.R;
import sv.edu.agroconecta.ui.SoporteActivity;
import sv.edu.agroconecta.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import sv.edu.agroconecta.ChatManager;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsuarios, cardProductos, cardReportes;
    private TextView txtTitulo, tvAvatarAdmin, tvWelcomeAdmin;
    private android.widget.ImageView ivAvatarFotoAdmin;
    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);

        // Vistas
        cardUsuarios = findViewById(R.id.cardUsuarios);
        cardProductos = findViewById(R.id.cardProductos);
        cardReportes = findViewById(R.id.cardReportes);
        txtTitulo = findViewById(R.id.txtTitulo);
        tvAvatarAdmin = findViewById(R.id.tvAvatarAdmin);
        ivAvatarFotoAdmin = findViewById(R.id.ivAvatarFotoAdmin);
        tvWelcomeAdmin = findViewById(R.id.tvWelcomeAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);

        // Inicializar datos del Header
        cargarDatosHeader();

        tvAvatarAdmin.setOnClickListener(this::showProfileMenu);
        if (ivAvatarFotoAdmin != null) ivAvatarFotoAdmin.setOnClickListener(this::showProfileMenu);

        // Dashboard Cards
        cardUsuarios.setOnClickListener(v -> startActivity(new Intent(this, UsuarioActivity.class)));
        cardProductos.setOnClickListener(v -> startActivity(new Intent(this, ProductosAdminActivity.class)));
        cardReportes.setOnClickListener(v -> startActivity(new Intent(this, ReportesActivity.class)));

        setupBottomNav();

        // AgroBot IA flotante para el Admin
        CoordinatorLayout rootAdmin = findViewById(R.id.coordinatorAdmin);
        new ChatManager(this, rootAdmin);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar datos de perfil (nombre, foto) al volver
        cargarDatosHeader();
    }

    private void cargarDatosHeader() {
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            tvWelcomeAdmin.setText("¡Hola, " + nombre + "! 👋");
            txtTitulo.setText("Panel de Control ⚙️");
        }
        
        String fotoAdmin = sessionManager.getFotoPerfil();
        if (fotoAdmin != null && !fotoAdmin.isEmpty() && ivAvatarFotoAdmin != null) {
            Glide.with(this).load(fotoAdmin)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(ivAvatarFotoAdmin);
            ivAvatarFotoAdmin.setVisibility(View.VISIBLE);
            tvAvatarAdmin.setVisibility(View.GONE);
        } else {
            if (ivAvatarFotoAdmin != null) ivAvatarFotoAdmin.setVisibility(View.GONE);
            tvAvatarAdmin.setVisibility(View.VISIBLE);
        }
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(AdminDashboardActivity.this, v);
        popupMenu.getMenu().add(0, 1, 0, "👤 Mi Perfil");
        popupMenu.getMenu().add(0, 2, 1, "🛠️ Soporte técnico");
        popupMenu.getMenu().add(0, 3, 2, "🚪 Cerrar Sesión");
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                mostrarPerfil();
                return true;
            }
            if (id == 2) {
                startActivity(new Intent(AdminDashboardActivity.this, SoporteActivity.class));
                return true;
            }
            if (id == 3) {
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

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_dashboard);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_users) {
                startActivity(new Intent(this, UsuarioActivity.class));
                return true;
            } else if (id == R.id.nav_admin_products) {
                startActivity(new Intent(this, ProductosAdminActivity.class));
                return true;
            }
            return id == R.id.nav_admin_dashboard;
        });
    }

    private void confirmarLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AdminDashboardActivity.this);
        builder.setTitle("Cerrar Sesión");
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?");
        builder.setPositiveButton("Sí", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                sessionManager.logout();
                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
}
