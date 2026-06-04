package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.PedidoApi;

public class CalificacionActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText  etComentario;
    private Button    btnEnviar;
    private int       pedidoId, productoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificacion);

        pedidoId   = getIntent().getIntExtra("pedido_id", 0);
        productoId = getIntent().getIntExtra("producto_id", 0);
        String nombreProducto = getIntent().getStringExtra("nombre_producto");

        TextView tvProducto = findViewById(R.id.tvProductoCalificar);
        if (tvProducto != null && nombreProducto != null)
            tvProducto.setText("Califica: " + nombreProducto);

        ratingBar    = findViewById(R.id.ratingBarCalificacion);
        etComentario = findViewById(R.id.etComentarioCalificacion);
        btnEnviar    = findViewById(R.id.btnEnviarCalificacion);

        findViewById(R.id.btnBackCalificacion).setOnClickListener(v -> finish());
        btnEnviar.setOnClickListener(v -> enviarCalificacion());
    }

    private void enviarCalificacion() {
        int puntuacion = (int) ratingBar.getRating();
        String comentario = etComentario.getText().toString().trim();

        if (puntuacion == 0) {
            Toast.makeText(this, "Selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        Map<String, Object> data = new HashMap<>();
        data.put("pedido_id", pedidoId);
        data.put("producto_id", productoId);
        data.put("puntuacion", puntuacion);
        data.put("comentario", comentario);

        ApiClient.getClient().create(PedidoApi.class).calificar(data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar calificación");

                if (response.isSuccessful()) {
                    Toast.makeText(CalificacionActivity.this,
                            "⭐ ¡Gracias por tu calificación!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String error = "Error " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            error += ": " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(CalificacionActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar calificación");
                Toast.makeText(CalificacionActivity.this,
                        "Sin conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
