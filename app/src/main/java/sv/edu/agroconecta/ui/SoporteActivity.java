package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class SoporteActivity extends AppCompatActivity {

    // Número de WhatsApp del equipo de soporte
    private static final String NUMERO_SOPORTE = "50378564202";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soporte);

        ImageButton btnBack = findViewById(R.id.btnBackSoporte);
        TextInputEditText etTipo = findViewById(R.id.etTipoProblema);
        TextInputEditText etDesc = findViewById(R.id.etDescripcionProblema);
        Button btnEnviar = findViewById(R.id.btnEnviarWhatsApp);

        btnBack.setOnClickListener(v -> finish());

        SessionManager session = new SessionManager(this);
        String nombreUsuario = session.getNombre();
        String rolUsuario    = session.getRol();

        btnEnviar.setOnClickListener(v -> {
            String tipo = etTipo.getText() != null ? etTipo.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

            if (TextUtils.isEmpty(tipo)) {
                etTipo.setError("Indica el tipo de problema");
                etTipo.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(desc)) {
                etDesc.setError("Describe el problema");
                etDesc.requestFocus();
                return;
            }

            String mensaje = "🛠️ *SOPORTE AGROCONECTA*\n\n" +
                    "👤 *Usuario:* " + (nombreUsuario != null ? nombreUsuario : "No identificado") + "\n" +
                    "🎭 *Rol:* " + (rolUsuario != null ? rolUsuario : "Desconocido") + "\n\n" +
                    "📋 *Tipo de problema:* " + tipo + "\n\n" +
                    "📝 *Descripción:*\n" + desc + "\n\n" +
                    "─────────────────\n" +
                    "_Enviado desde AgroConecta App_";

            String url = "https://wa.me/" + NUMERO_SOPORTE + "?text=" + Uri.encode(mensaje);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
