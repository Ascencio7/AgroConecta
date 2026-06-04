package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMensajes;
    private EditText etMensaje;
    private ImageButton btnEnviar;
    private SessionManager sessionManager;
    private DatabaseReference chatRef;
    private List<Map<String, Object>> mensajes = new ArrayList<>();
    private MensajeAdapter adapter;

    private int miId, otroId;
    private String miNombre, otroNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sessionManager = new SessionManager(this);
        miId     = sessionManager.getUserId();
        miNombre = sessionManager.getNombre();
        otroId   = getIntent().getIntExtra("otro_usuario_id", -1);
        otroNombre = getIntent().getStringExtra("otro_nombre");

        // Header
        TextView tvTitulo = findViewById(R.id.tvChatTitulo);
        TextView tvSubtitulo = findViewById(R.id.tvChatSubtitulo);
        if (tvTitulo != null) tvTitulo.setText(otroNombre != null ? otroNombre : "Chat");
        if (tvSubtitulo != null) tvSubtitulo.setText("En línea");

        findViewById(R.id.btnBackChat).setOnClickListener(v -> finish());

        rvMensajes = findViewById(R.id.rvMensajes);
        etMensaje  = findViewById(R.id.etMensajeChat);
        btnEnviar  = findViewById(R.id.btnEnviarChat);

        adapter = new MensajeAdapter();
        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        rvMensajes.setAdapter(adapter);

        // Generar ID de sala único (menor_id_mayor_id)
        String salaId = Math.min(miId, otroId) + "_" + Math.max(miId, otroId);
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(salaId);

        // Escuchar mensajes en tiempo real
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                Map<String, Object> msg = (Map<String, Object>) snapshot.getValue();
                if (msg != null) {
                    mensajes.add(msg);
                    adapter.notifyItemInserted(mensajes.size() - 1);
                    rvMensajes.scrollToPosition(mensajes.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            enviarMensaje();
            return true;
        });
    }

    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;

        String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("texto",    texto);
        mensaje.put("emisorId", miId);
        mensaje.put("emisor",   miNombre);
        mensaje.put("hora",     hora);
        mensaje.put("timestamp", System.currentTimeMillis());

        chatRef.push().setValue(mensaje)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show());

        etMensaje.setText("");
    }

    // ── Adapter mensajes ─────────────────────────────────
    class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == 1
                    ? R.layout.item_mensaje_enviado
                    : R.layout.item_mensaje_recibido;
            return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> msg = mensajes.get(pos);
            String texto = (String) msg.get("texto");
            String hora  = (String) msg.get("hora");

            TextView tvTexto = h.itemView.findViewById(R.id.tvMensajeTexto);
            TextView tvHora  = h.itemView.findViewById(R.id.tvMensajeHora);
            if (tvTexto != null) tvTexto.setText(texto);
            if (tvHora  != null) tvHora.setText(hora);
        }

        @Override
        public int getItemViewType(int pos) {
            Map<String, Object> msg = mensajes.get(pos);
            Object emisorId = msg.get("emisorId");
            long id = emisorId instanceof Long ? (Long) emisorId : ((Number) emisorId).longValue();
            return id == miId ? 1 : 0; // 1=enviado, 0=recibido
        }

        @Override public int getItemCount() { return mensajes.size(); }
    }
}
