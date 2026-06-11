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
        // Colores de los estados
        int colorActivo   = 0xFF25632D; // Verde primario
        int colorInactivo = 0xFFCCCCCC; // Gris claro

        // Resetear todos los pasos a inactivo
        if (step1 != null) step1.setBackgroundColor(colorInactivo);
        if (step2 != null) step2.setBackgroundColor(colorInactivo);
        if (step3 != null) step3.setBackgroundColor(colorInactivo);
        if (step4 != null) step4.setBackgroundColor(colorInactivo);
        if (line1 != null) line1.setBackgroundColor(colorInactivo);
        if (line2 != null) line2.setBackgroundColor(colorInactivo);
        if (line3 != null) line3.setBackgroundColor(colorInactivo);

        String estado = "Pendiente";
        String mensaje = "Tu pedido está siendo procesado.";

        // Lógica de progreso acumulativo
        if (estadoId >= 1) {
            estado  = "Pedido recibido";
            mensaje = "Tu pedido fue recibido y está siendo revisado por el vendedor.";
            if (step1 != null) step1.setBackgroundColor(colorActivo);
        }
        
        if (estadoId >= 2) {
            estado  = "En preparación";
            mensaje = "El vendedor está preparando tu pedido para el envío.";
            if (step2 != null) step2.setBackgroundColor(colorActivo);
            if (line1 != null) line1.setBackgroundColor(colorActivo);
        }
        
        if (estadoId >= 3) {
            estado  = "En camino";
            mensaje = "Tu pedido está en camino. Pronto llegará a tu destino.";
            if (step3 != null) step3.setBackgroundColor(colorActivo);
            if (line2 != null) line2.setBackgroundColor(colorActivo);
        }
        
        if (estadoId >= 4) {
            estado  = "Entregado";
            mensaje = "¡Tu pedido fue entregado exitosamente. Gracias por comprar en AgroConecta!";
            if (step4 != null) step4.setBackgroundColor(colorActivo);
            if (line3 != null) line3.setBackgroundColor(colorActivo);
        }

        if (estadoId >= 5) {
            estado  = "Pagado";
            mensaje = "Tu pedido ha sido pagado y entregado. ¡Gracias por tu preferencia!";
            // El estado 5 (Pagado) mantiene el paso 4 (Entregado) iluminado
        }

        if (tvEstadoActual != null) tvEstadoActual.setText(estado);
        if (tvMensajeEstado != null) tvMensajeEstado.setText(mensaje);
        
        if (fecha != null && !fecha.isEmpty() && tvFechaEstado != null) {
            try {
                String fechaCorta = fecha.length() >= 10 ? fecha.substring(0, 10) : fecha;
                tvFechaEstado.setText("Actualizado: " + fechaCorta);
            } catch (Exception e) {
                tvFechaEstado.setText("Actualizado hoy");
            }
        } else if (tvFechaEstado != null) {
            tvFechaEstado.setText("");
        }
    }
}
