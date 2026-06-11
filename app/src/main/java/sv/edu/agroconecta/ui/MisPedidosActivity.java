package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
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
    private Spinner spFiltro;
    private List<Pedido> listaCompleta = new ArrayList<>();
    private PedidoClienteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pedidos);

        sessionManager  = new SessionManager(this);
        rvMisPedidos    = findViewById(R.id.rvMisPedidos);
        progressPedidos = findViewById(R.id.progressMisPedidos);
        tvEmptyPedidos  = findViewById(R.id.tvEmptyMisPedidos);
        spFiltro        = findViewById(R.id.spFiltroEstadoPedidos);

        rvMisPedidos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidoClienteAdapter(new ArrayList<>(), this);
        rvMisPedidos.setAdapter(adapter);

        configurarFiltro();
        cargarPedidos();
    }

    private void configurarFiltro() {
        String[] opciones = {"Todos", "Pendiente", "En preparacion", "En camino", "Entregado"};
        ArrayAdapter<String> adapterF = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        adapterF.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltro.setAdapter(adapterF);

        spFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtrarPedidos(opciones[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filtrarPedidos(String estado) {
        if (listaCompleta == null || listaCompleta.isEmpty()) return;
        
        if (estado.equals("Todos")) {
            adapter.updateList(listaCompleta);
            tvEmptyPedidos.setVisibility(View.GONE);
        } else {
            List<Pedido> filtrados = new ArrayList<>();
            for (Pedido p : listaCompleta) {
                if (p.getEstadoTexto().equalsIgnoreCase(estado)) {
                    filtrados.add(p);
                }
            }
            adapter.updateList(filtrados);
            tvEmptyPedidos.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
            if (filtrados.isEmpty()) tvEmptyPedidos.setText("No hay pedidos con este estado");
        }
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
                            listaCompleta = response.body();
                            if (listaCompleta.isEmpty()) {
                                if (tvEmptyPedidos != null) {
                                    tvEmptyPedidos.setText("No tienes pedidos aún");
                                    tvEmptyPedidos.setVisibility(View.VISIBLE);
                                }
                            } else {
                                rvMisPedidos.setVisibility(View.VISIBLE);
                                // Aplicar filtro actual al cargar
                                if (spFiltro.getSelectedItem() != null) {
                                    filtrarPedidos(spFiltro.getSelectedItem().toString());
                                } else {
                                    adapter.updateList(listaCompleta);
                                }
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