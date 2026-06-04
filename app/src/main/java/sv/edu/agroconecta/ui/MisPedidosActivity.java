package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.PedidoApi;
import sv.edu.agroconecta.utils.SessionManager;

public class MisPedidosActivity extends AppCompatActivity {

    private RecyclerView rvMisPedidos;
    private ProgressBar progressPedidos;
    private TextView tvEmptyPedidos;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pedidos);

        sessionManager  = new SessionManager(this);
        rvMisPedidos    = findViewById(R.id.rvMisPedidos);
        progressPedidos = findViewById(R.id.progressMisPedidos);
        tvEmptyPedidos  = findViewById(R.id.tvEmptyMisPedidos);

        rvMisPedidos.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Pedidos");
        }

        cargarPedidos();
    }

    private void cargarPedidos() {
        if (progressPedidos != null) progressPedidos.setVisibility(View.VISIBLE);
        rvMisPedidos.setVisibility(View.GONE);
        if (tvEmptyPedidos != null) tvEmptyPedidos.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();
        ApiClient.getClient()
                .create(PedidoApi.class)
                .getPedidosPorUsuario(userId)
                .enqueue(new Callback<List<Pedido>>() {
            @Override
            public void onResponse(Call<List<Pedido>> call, Response<List<Pedido>> response) {
                if (progressPedidos != null) progressPedidos.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Pedido> pedidos = response.body();
                    if (pedidos.isEmpty()) {
                        if (tvEmptyPedidos != null) {
                            tvEmptyPedidos.setText("No tienes pedidos aún");
                            tvEmptyPedidos.setVisibility(View.VISIBLE);
                        }
                    } else {
                        rvMisPedidos.setVisibility(View.VISIBLE);
                        rvMisPedidos.setAdapter(new PedidoAdapter(pedidos));
                    }
                } else {
                    if (tvEmptyPedidos != null) {
                        tvEmptyPedidos.setText("Error al cargar pedidos");
                        tvEmptyPedidos.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Pedido>> call, Throwable t) {
                if (progressPedidos != null) progressPedidos.setVisibility(View.GONE);
                if (tvEmptyPedidos != null) {
                    tvEmptyPedidos.setText("Sin conexión. Intenta de nuevo.");
                    tvEmptyPedidos.setVisibility(View.VISIBLE);
                }
                Toast.makeText(MisPedidosActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.VH> {
        private List<Pedido> lista;
        PedidoAdapter(List<Pedido> lista) { this.lista = lista; }

        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pedido, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Pedido p = lista.get(pos);

            ((TextView) h.itemView.findViewById(R.id.tvPedidoId))
                    .setText("Pedido #" + p.getPedidoId());
            ((TextView) h.itemView.findViewById(R.id.tvPedidoTotal))
                    .setText(String.format("$%.2f", p.getTotal()));
            ((TextView) h.itemView.findViewById(R.id.tvPedidoFecha))
                    .setText("📅 " + (p.getFecha() != null ? p.getFecha().substring(0, 10) : "Hoy"));

            TextView tvItems = h.itemView.findViewById(R.id.tvPedidoItems);
            if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                StringBuilder sb = new StringBuilder("🛒 ");
                for (int i = 0; i < p.getDetalles().size(); i++) {
                    sb.append(p.getDetalles().get(i).getNombre())
                      .append(" ×").append(p.getDetalles().get(i).getCantidad());
                    if (i < p.getDetalles().size() - 1) sb.append(", ");
                }
                tvItems.setText(sb.toString());
            } else {
                tvItems.setText("🛒 Ver detalles");
            }

            TextView tvE = h.itemView.findViewById(R.id.tvPedidoEstado);
            tvE.setText(p.getEstadoTexto());
            switch (p.getEstadoId()) {
                case 3: tvE.setTextColor(0xFF27AE60); tvE.setBackgroundResource(R.drawable.bg_badge_verde); break;
                case 4: tvE.setTextColor(0xFFC0392B); tvE.setBackgroundResource(R.drawable.bg_badge_rojo); break;
                default: tvE.setTextColor(0xFFB7770D); tvE.setBackgroundResource(R.drawable.bg_badge_amber); break;
            }

            // Seguimiento
            View btnSeg = h.itemView.findViewById(R.id.btnSeguimiento);
            if (btnSeg != null) btnSeg.setOnClickListener(v -> {
                Intent i = new Intent(MisPedidosActivity.this, SeguimientoPedidoActivity.class);
                i.putExtra("pedido_id", p.getPedidoId());
                i.putExtra("estado_id", p.getEstadoId());
                startActivity(i);
            });

            // Calificar — solo si fue entregado (estado 4)
            View btnCal = h.itemView.findViewById(R.id.btnCalificar);
            if (btnCal != null) {
                if (p.getEstadoId() == 4) {
                    btnCal.setEnabled(true);
                    btnCal.setAlpha(1f);
                    btnCal.setOnClickListener(v -> {
                        Intent i = new Intent(MisPedidosActivity.this, CalificacionActivity.class);
                        i.putExtra("pedido_id", p.getPedidoId());
                        if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                            i.putExtra("producto_id", p.getDetalles().get(0).getProductoId());
                            i.putExtra("nombre_producto", p.getDetalles().get(0).getNombre());
                        }
                        startActivity(i);
                    });
                } else {
                    btnCal.setEnabled(false);
                    btnCal.setAlpha(0.4f);
                }
            }
        }

        @Override public int getItemCount() { return lista.size(); }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
