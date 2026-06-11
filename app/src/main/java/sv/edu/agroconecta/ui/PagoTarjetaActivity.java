package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class PagoTarjetaActivity extends AppCompatActivity {

    private TextInputEditText etNumero, etNombre, etExpiracion, etCVV;
    private MaterialButton btnPagar;
    private double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_tarjeta);

        total = getIntent().getDoubleExtra("total", 0.0);

        etNumero = findViewById(R.id.etNumeroTarjeta);
        etNombre = findViewById(R.id.etNombreTarjeta);
        etExpiracion = findViewById(R.id.etExpiracion);
        etCVV = findViewById(R.id.etCVV);
        btnPagar = findViewById(R.id.btnValidarTarjeta);

        findViewById(R.id.btnBackTarjeta).setOnClickListener(v -> finish());

        setupFormatters();

        btnPagar.setOnClickListener(v -> validarYProcesar());
    }

    private void setupFormatters() {
        // Formateador de Número de Tarjeta (XXXX XXXX XXXX XXXX)
        etNumero.addTextChangedListener(new TextWatcher() {
            private boolean isDeleting = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { isDeleting = count > after; }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isDeleting) return;
                String original = s.toString().replaceAll(" ", "");
                if (original.length() > 16) return;
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < original.length(); i++) {
                    if (i > 0 && i % 4 == 0) formatted.append(" ");
                    formatted.append(original.charAt(i));
                }
                
                etNumero.removeTextChangedListener(this);
                etNumero.setText(formatted.toString());
                etNumero.setSelection(formatted.length());
                etNumero.addTextChangedListener(this);
            }
        });

        // Formateador de Fecha de Expiración (MM/YY)
        etExpiracion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (before == 0 && s.length() == 2) {
                    etExpiracion.setText(s + "/");
                    etExpiracion.setSelection(3);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validarYProcesar() {
        String num = etNumero.getText().toString().replaceAll(" ", "");
        String nom = etNombre.getText().toString().trim();
        String exp = etExpiracion.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();

        if (num.length() < 16) { etNumero.setError("Se requieren 16 dígitos"); return; }
        if (nom.isEmpty()) { etNombre.setError("Nombre requerido"); return; }
        if (exp.length() < 5) { etExpiracion.setError("Formato MM/YY requerido"); return; }
        if (cvv.length() < 3) { etCVV.setError("Mínimo 3 dígitos"); return; }

        btnPagar.setEnabled(false);
        btnPagar.setText("Procesando...");

        // Simular procesamiento y enviar pedido
        SessionManager session = new SessionManager(this);
        List<DetallePedido> items = new ArrayList<>(CarritoManager.getInstance().getItems());

        Pedido p = new Pedido();
        p.setUsuarioId(session.getUserId());
        p.setTotal(total);
        p.setEstadoId(1);
        p.setMetodoPago("Tarjeta (" + num.substring(num.length() - 4) + ")");
        p.setDetalles(items);

        new PedidoRepository().crearPedido(p).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                if (response.isSuccessful()) {
                    CarritoManager.getInstance().limpiar();
                    Toast.makeText(PagoTarjetaActivity.this, "¡Pago exitoso y pedido confirmado!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnPagar.setEnabled(true);
                    btnPagar.setText("VALIDAR Y PAGAR");
                    Toast.makeText(PagoTarjetaActivity.this, "Error al procesar pedido", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Pedido> call, Throwable t) {
                btnPagar.setEnabled(true);
                btnPagar.setText("VALIDAR Y PAGAR");
                Toast.makeText(PagoTarjetaActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
