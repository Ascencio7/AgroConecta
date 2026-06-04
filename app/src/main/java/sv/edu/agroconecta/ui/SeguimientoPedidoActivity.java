package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import sv.edu.agroconecta.R;

public class SeguimientoPedidoActivity extends AppCompatActivity {

    private TextView tvEstadoActual, tvFechaEstado, tvMensajeEstado;
    private View step1, step2, step3, step4;
    private View line1, line2, line3;
    private int pedidoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_pedido);

        pedidoId = getIntent().getIntExtra("pedido_id", 0);

        tvEstadoActual  = findViewById(R.id.tvEstadoActual);
        tvFechaEstado   = findViewById(R.id.tvFechaEstado);
        tvMensajeEstado = findViewById(R.id.tvMensajeEstado);
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        line1 = findViewById(R.id.line1);
        line2 = findViewById(R.id.line2);
        line3 = findViewById(R.id.line3);

        ((ImageButton) findViewById(R.id.btnBackSeguimiento))
                .setOnClickListener(v -> finish());

        // Escuchar cambios de estado en Firebase Realtime Database
        FirebaseDatabase.getInstance()
                .getReference("pedidos_estado")
                .child(String.valueOf(pedidoId))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Integer estadoId = snapshot.child("estado_id").getValue(Integer.class);
                            String fecha = snapshot.child("fecha").getValue(String.class);
                            if (estadoId != null) actualizarUI(estadoId, fecha);
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });

        // Estado inicial del intent
        int estadoInicial = getIntent().getIntExtra("estado_id", 1);
        actualizarUI(estadoInicial, null);
    }

    private void actualizarUI(int estadoId, String fecha) {
        int colorActivo   = 0xFF25632D;
        int colorInactivo = 0xFFCCCCCC;

        // Resetear todo
        step1.setBackgroundColor(colorInactivo);
        step2.setBackgroundColor(colorInactivo);
        step3.setBackgroundColor(colorInactivo);
        step4.setBackgroundColor(colorInactivo);
        line1.setBackgroundColor(colorInactivo);
        line2.setBackgroundColor(colorInactivo);
        line3.setBackgroundColor(colorInactivo);

        String estado, mensaje;
        switch (estadoId) {
            case 1:
                estado  = "Pedido recibido";
                mensaje = "Tu pedido fue recibido y está siendo revisado por el vendedor.";
                step1.setBackgroundColor(colorActivo);
                break;
            case 2:
                estado  = "En preparacion";
                mensaje = "El vendedor está preparando tu pedido para el envío.";
                step1.setBackgroundColor(colorActivo);
                step2.setBackgroundColor(colorActivo);
                line1.setBackgroundColor(colorActivo);
                break;
            case 3:
                estado  = "En camino";
                mensaje = "Tu pedido está en camino. Pronto llegará a tu destino.";
                step1.setBackgroundColor(colorActivo);
                step2.setBackgroundColor(colorActivo);
                step3.setBackgroundColor(colorActivo);
                line1.setBackgroundColor(colorActivo);
                line2.setBackgroundColor(colorActivo);
                break;
            case 4:
                estado  = "Entregado";
                mensaje = "¡Tu pedido fue entregado exitosamente. Gracias por comprar en AgroConecta!";
                step1.setBackgroundColor(colorActivo);
                step2.setBackgroundColor(colorActivo);
                step3.setBackgroundColor(colorActivo);
                step4.setBackgroundColor(colorActivo);
                line1.setBackgroundColor(colorActivo);
                line2.setBackgroundColor(colorActivo);
                line3.setBackgroundColor(colorActivo);
                break;
            default:
                estado  = "Pendiente";
                mensaje = "Tu pedido está pendiente de confirmación.";
                step1.setBackgroundColor(colorActivo);
        }

        tvEstadoActual.setText(estado);
        tvMensajeEstado.setText(mensaje);
        if (fecha != null && tvFechaEstado != null)
            tvFechaEstado.setText("Actualizado: " + fecha.substring(0, 10));
    }
}
