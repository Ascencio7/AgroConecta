package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsuarios, cardProductos, cardReportes;
    private TextView txtTitulo, tvAvatarAdmin, tvWelcomeAdmin;
    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);

        // Vistas
        cardUsuarios    = findViewById(R.id.cardUsuarios);
        cardProductos   = findViewById(R.id.cardProductos);
        cardReportes    = findViewById(R.id.cardReportes);
        txtTitulo       = findViewById(R.id.txtTitulo);
        tvAvatarAdmin   = findViewById(R.id.tvAvatarAdmin);
        tvWelcomeAdmin  = findViewById(R.id.tvWelcomeAdmin);
        bottomNavAdmin  = findViewById(R.id.bottomNavAdmin);

        // Avatar
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            tvWelcomeAdmin.setText("¡Hola, " + nombre + "! 👋");
            txtTitulo.setText("Panel de Control ⚙️");
        }
        tvAvatarAdmin.setOnClickListener(v -> confirmarLogout());

        // Dashboard Cards
        cardUsuarios.setOnClickListener(v -> startActivity(new Intent(this, UsuarioActivity.class)));
        cardProductos.setOnClickListener(v -> startActivity(new Intent(this, ProductosAdminActivity.class)));
        cardReportes.setOnClickListener(v -> startActivity(new Intent(this, ReportesActivity.class)));

        setupBottomNav();
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
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

}
