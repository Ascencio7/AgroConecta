package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.PedidoClienteAdapter;
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
                                rvMisPedidos.setAdapter(new PedidoClienteAdapter(pedidos, MisPedidosActivity.this));
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

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}