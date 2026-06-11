package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.DetallePedido;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.repository.PedidoRepository;
import sv.edu.agroconecta.utils.CarritoManager;
import sv.edu.agroconecta.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PagoTransferenciaActivity extends AppCompatActivity {

    private TextInputEditText etBanco, etReferencia, etMonto;
    private MaterialButton btnConfirmar;
    private double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_transferencia);

        total = getIntent().getDoubleExtra("total", 0.0);

        etBanco = findViewById(R.id.etBancoEmisor);
        etReferencia = findViewById(R.id.etReferencia);
        etMonto = findViewById(R.id.etMontoTransferencia);
        btnConfirmar = findViewById(R.id.btnConfirmarTransferencia);

        findViewById(R.id.btnBackTransferencia).setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(v -> validarYConfirmar());
        
        etMonto.setText(String.format("%.2f", total));
    }

    private void validarYConfirmar() {
        String banco = etBanco.getText().toString().trim();
        String ref = etReferencia.getText().toString().trim();
        String monto = etMonto.getText().toString().trim();

        if (banco.isEmpty()) { etBanco.setError("Requerido"); return; }
        if (ref.isEmpty()) { etReferencia.setError("Requerido"); return; }
        if (monto.isEmpty()) { etMonto.setError("Requerido"); return; }

        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("Confirmando...");

        SessionManager session = new SessionManager(this);
        List<DetallePedido> items = new ArrayList<>(CarritoManager.getInstance().getItems());

        Pedido p = new Pedido();
        p.setUsuarioId(session.getUserId());
        p.setTotal(total);
        p.setEstadoId(1);
        p.setMetodoPago("Transferencia (" + banco + " #" + ref + ")");
        p.setDetalles(items);

        new PedidoRepository().crearPedido(p).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                if (response.isSuccessful()) {
                    CarritoManager.getInstance().limpiar();
                    Toast.makeText(PagoTransferenciaActivity.this, "¡Transferencia reportada exitosamente!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnConfirmar.setEnabled(true);
                    btnConfirmar.setText("CONFIRMAR PAGO");
                    Toast.makeText(PagoTransferenciaActivity.this, "Error al guardar pedido", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Pedido> call, Throwable t) {
                btnConfirmar.setEnabled(true);
                btnConfirmar.setText("CONFIRMAR PAGO");
                Toast.makeText(PagoTransferenciaActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
