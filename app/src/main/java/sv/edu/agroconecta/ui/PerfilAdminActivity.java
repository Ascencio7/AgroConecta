package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class PerfilAdminActivity extends AppCompatActivity {

    private TextView tvAvatarLarge, tvProfileName, tvProfileRole, tvProfileEmail;
    private ImageButton btnBackProfile;
    private Button btnLogoutProfile;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_admin);

        sessionManager = new SessionManager(this);

        tvAvatarLarge = findViewById(R.id.tvAvatarLarge);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);

        // Cargar datos
        String nombre = sessionManager.getNombre();
        String rol = sessionManager.getRol();
        String correo = sessionManager.getCorreo();

        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarLarge.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            tvProfileName.setText(nombre);
        }

        tvProfileRole.setText(rol);
        tvProfileEmail.setText(correo);

        // Estilo dinámico para el rol
        if ("ADMIN".equalsIgnoreCase(rol)) {
            tvProfileRole.setTextColor(getResources().getColor(R.color.dorado, getTheme()));
        } else if ("VENDEDOR".equalsIgnoreCase(rol)) {
            tvProfileRole.setTextColor(getResources().getColor(R.color.verde_primario, getTheme()));
        } else {
            tvProfileRole.setTextColor(getResources().getColor(R.color.texto_secundario, getTheme()));
        }

        // Listeners
        btnBackProfile.setOnClickListener(v -> finish());
        
        btnLogoutProfile.setOnClickListener(v -> confirmarLogout());
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
