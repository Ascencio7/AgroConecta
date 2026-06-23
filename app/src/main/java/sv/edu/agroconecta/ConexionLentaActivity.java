package sv.edu.agroconecta;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class ConexionLentaActivity extends AppCompatActivity {

    private MaterialCardView cardMessage;
    private Button btnAceptar;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion_lenta);

        cardMessage = findViewById(R.id.cardMessage);
        btnAceptar = findViewById(R.id.btnAceptar);
        tvMessage = findViewById(R.id.tvMessage);

        // Ocultamos inicialmente los elementos para cumplir con la "espera" de 12 segundos
        // si se desea que la advertencia aparezca después de un tiempo.
        // Opcional: Podrías mostrar un ProgressBar aquí.
        
        cardMessage.setVisibility(View.GONE);
        btnAceptar.setVisibility(View.GONE);

        // Simulamos la espera de 12 segundos para informar sobre la conexión lenta
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cardMessage.setVisibility(View.VISIBLE);
                btnAceptar.setVisibility(View.VISIBLE);
            }
        }, 12000); // 12 segundos

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra la actividad al aceptar
            }
        });
    }
}
