package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.adapter.CarritoAdapter;
import sv.edu.agroconecta.modelo.DetallePedido;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.repository.PedidoRepository;
import sv.edu.agroconecta.utils.CarritoManager;
import sv.edu.agroconecta.utils.FCMHelper;
import sv.edu.agroconecta.utils.SessionManager;

public class CarritoActivity extends AppCompatActivity implements CarritoAdapter.OnItemChangedListener {

    private static final double IVA = 0.13;

    private RecyclerView rvCarrito;
    private TextView tvTotal, tvCantidadItems;
    private Button btnConfirmar;
    private CarritoAdapter adapter;
    private PedidoRepository pedidoRepository;
    private List<DetallePedido> items;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrito);

        rvCarrito       = findViewById(R.id.rvCarrito);
        tvTotal         = findViewById(R.id.tvTotalCarrito);
        tvCantidadItems = findViewById(R.id.tvCantidadItems);
        btnConfirmar    = findViewById(R.id.btnConfirmarPedido);

        pedidoRepository = new PedidoRepository();
        sessionManager = new SessionManager(this);
        items = CarritoManager.getInstance().getItems();

        adapter = new CarritoAdapter(this, items, this);
        rvCarrito.setLayoutManager(new LinearLayoutManager(this));
        rvCarrito.setAdapter(adapter);

        actualizarResumen();

        // Deshabilitar botón si el carrito está vacío
        btnConfirmar.setEnabled(!items.isEmpty());

        btnConfirmar.setOnClickListener(v -> confirmarPedido());
    }

    private void actualizarResumen() {
        double subtotal = CarritoManager.getInstance().getTotal();
        double total    = subtotal * (1 + IVA);
        int count       = items.stream().mapToInt(DetallePedido::getCantidad).sum();

        if (tvTotal != null) tvTotal.setText(String.format("$%.2f", total));
        if (tvCantidadItems != null)
            tvCantidadItems.setText(count + " " + (count == 1 ? "item" : "items"));

        // Persistir cambios
        CarritoManager.getInstance().guardarCarrito(this, sessionManager.getUserId());

        // Actualizar estado del botón según si hay items
        if (btnConfirmar != null)
            btnConfirmar.setEnabled(!items.isEmpty());
    }

    private void confirmarPedido() {
        if (items.isEmpty()) {
            Toast.makeText(this, "🛒 El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = CarritoManager.getInstance().getTotal();
        double total = subtotal * (1 + IVA);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar pedido")
                .setMessage(String.format("¿Confirmar pedido por $%.2f (incluye IVA)?", total))
                .setPositiveButton("✅ Confirmar", (dialog, which) -> enviarPedido(total))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void enviarPedido(double total) {
        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("Enviando…");

        Pedido pedido = new Pedido();
        pedido.setDetalles(items);
        pedido.setTotal(total);
        pedido.setEstadoId(1);
        pedido.setUsuarioId(sessionManager.getUserId());

        pedidoRepository.crearPedido(pedido).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                if (response.isSuccessful()) {

                    // ── Notificar a cada vendedor único del pedido ──────────────
                    Set<Integer> vendedoresNotificados = new HashSet<>();
                    for (DetallePedido item : items) {
                        int vendedorId = item.getVendedorId();
                        if (vendedorId > 0 && !vendedoresNotificados.contains(vendedorId)) {
                            vendedoresNotificados.add(vendedorId);
                            FCMHelper.notificarUsuario(
                                String.valueOf(vendedorId),
                                "🛒 ¡Nuevo pedido recibido!",
                                "Tienes un nuevo pedido. Revisa tu dashboard para verlo.",
                                "pedido"
                            );
                        }
                    }
                    // ───────────────────────────────────────────────────────────

                    CarritoManager.getInstance().limpiarYPersistir(CarritoActivity.this, sessionManager.getUserId());
                    actualizarResumen(); // badge a 0
                    Toast.makeText(CarritoActivity.this,
                            "🎉 ¡Pedido confirmado! Total: " + String.format("$%.2f", total),
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    btnConfirmar.setEnabled(true);
                    btnConfirmar.setText("✅  Confirmar pedido");
                    Toast.makeText(CarritoActivity.this,
                            "Error al confirmar. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Pedido> call, Throwable t) {
                btnConfirmar.setEnabled(true);
                btnConfirmar.setText("✅  Confirmar pedido");
                Toast.makeText(CarritoActivity.this,
                        "Sin conexión. Verifica tu internet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemRemoved(int position) {
        actualizarResumen();
    }

    @Override
    public void onCantidadChanged() {
        actualizarResumen();
    }
}
