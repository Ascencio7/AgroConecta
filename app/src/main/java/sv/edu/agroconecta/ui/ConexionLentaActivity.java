package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import sv.edu.agroconecta.R;

public class ConexionLentaActivity extends AppCompatActivity {

    private MaterialCardView cardMessage;
    private Button btnAceptar;
    private ProgressBar pbCargando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion_lenta);

        cardMessage = findViewById(R.id.cardMessage);
        btnAceptar = findViewById(R.id.btnAceptar);
        pbCargando = findViewById(R.id.pbCargando);

        // Iniciamos la espera de 12 segundos
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Ocultamos el progress bar y mostramos el mensaje de error
                pbCargando.setVisibility(View.GONE);
                cardMessage.setVisibility(View.VISIBLE);
                btnAceptar.setVisibility(View.VISIBLE);
            }
        }, 12000); // 12000 ms = 12 segundos

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Al presionar aceptar, regresamos a la pantalla anterior
                finish();
            }
        });
    }
}
