package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.agroconecta.MainActivity;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class TermsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String rol, nombre;
    private int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        sessionManager = new SessionManager(this);

        // Obtener datos pasados desde Login
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");
        usuarioId = getIntent().getIntExtra("usuario_id", -1);

        CheckBox cbAcepto = findViewById(R.id.cbAcepto);
        Button btnSiguiente = findViewById(R.id.btnSiguiente);

        // El botón solo se activa si el CheckBox está marcado
        cbAcepto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnSiguiente.setEnabled(isChecked);
            if (isChecked) {
                btnSiguiente.setAlpha(1.0f);
            } else {
                btnSiguiente.setAlpha(0.5f);
            }
        });

        // Configuración inicial del botón (por si acaso)
        btnSiguiente.setAlpha(0.5f);

        btnSiguiente.setOnClickListener(v -> {
            // Guardar que este usuario específico ya aceptó los términos
            sessionManager.setHideTerms(usuarioId, true);
            navigateToDashboard();
        });
    }

    private void navigateToDashboard() {
        String rolUpper = rol != null ? rol.toUpperCase() : "";
        Intent intent;

        switch (rolUpper) {
            case "ADMIN":
                intent = new Intent(TermsActivity.this, AdminDashboardActivity.class);
                break;
            case "CLIENTE":
                intent = new Intent(TermsActivity.this, MainActivity.class);
                break;
            case "VENDEDOR":
                intent = new Intent(TermsActivity.this, VendedorDashboardActivity.class);
                break;
            default:
                intent = new Intent(TermsActivity.this, MainActivity.class);
                break;
        }

        intent.putExtra("nombre", nombre);
        intent.putExtra("usuario_id", usuarioId);
        intent.putExtra("rol", rol);
        startActivity(intent);
        finish();
    }
}