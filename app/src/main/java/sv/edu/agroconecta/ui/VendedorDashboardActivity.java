package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import sv.edu.agroconecta.ChatManager;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.ProductApi;
import sv.edu.agroconecta.utils.FCMHelper;
import sv.edu.agroconecta.utils.SessionManager;

import android.content.res.ColorStateList;
import android.graphics.Color;

public class VendedorDashboardActivity extends AppCompatActivity {

    interface VendedorApi {
        @GET("pedidos/vendedor/{id}")
        Call<List<Pedido>> getPedidosVendedor(@Path("id") int vendedorId);
    }

    private SessionManager sessionManager;
    private ProductApi productApi;
    private VendedorApi vendedorApi;
    private List<Product> misProductos = new ArrayList<>();
    private List<Product> misProductosFiltrados = new ArrayList<>();
    private List<Pedido> misPedidos = new ArrayList<>();
    private RecyclerView rvMisProductos, rvPedidosVendedor;
    private TextView tvTotalProd, tvActivos, tvAgotados, tvAvatar, tvWelcomeVendedor;
    private LinearLayout panelProductos, panelPedidos;
    private BottomNavigationView bottomNav;
    private androidx.appcompat.widget.SearchView searchView;
    private android.widget.ProgressBar progressProductos, progressPedidos;
    private android.widget.TextView tvEmptyProductos, tvEmptyPedidos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendedor_dashboard);

        sessionManager = new SessionManager(this);
        productApi    = ApiClient.getClient().create(ProductApi.class);
        vendedorApi   = ApiClient.getClient().create(VendedorApi.class);

        tvWelcomeVendedor = findViewById(R.id.tvWelcomeVendedor);
        String nombre = sessionManager.getNombre();
        if (nombre != null) {
            tvWelcomeVendedor.setText("¡Hola, " + nombre + "! 👋");
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("Vendedor - AgroConecta");
        } else {
            tvWelcomeVendedor.setText("AgroConecta");
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("Panel de Vendedor");
        }

        tvTotalProd    = findViewById(R.id.tvTotalProductos);
        tvActivos      = findViewById(R.id.tvProductosActivos);
        tvAgotados     = findViewById(R.id.tvProductosAgotados);
        panelProductos = findViewById(R.id.panelMisProductos);
        panelPedidos   = findViewById(R.id.panelMisPedidosVendedor);
        rvMisProductos = findViewById(R.id.rvMisProductos);
        rvPedidosVendedor = findViewById(R.id.rvPedidosVendedor);
        tvAvatar       = findViewById(R.id.tvAvatarVendedor);
        bottomNav      = findViewById(R.id.bottomNavVendedor);
        progressProductos = findViewById(R.id.progressProductosVendedor);
        progressPedidos   = findViewById(R.id.progressPedidosVendedor);
        tvEmptyProductos  = findViewById(R.id.tvEmptyProductosVendedor);
        tvEmptyPedidos    = findViewById(R.id.tvEmptyPedidosVendedor);

        rvMisProductos.setLayoutManager(new LinearLayoutManager(this));
        rvPedidosVendedor.setLayoutManager(new LinearLayoutManager(this));

        // Header Avatar / Logout
        if (sessionManager.isLoggedIn()) {
            String nom = sessionManager.getNombre();
            if (nom != null && !nom.isEmpty()) {
                tvAvatar.setText(String.valueOf(nom.charAt(0)).toUpperCase());
            }
        }
        tvAvatar.setOnClickListener(v -> confirmarLogout());

        // ── AgroBot IA flotante para el vendedor ──────────────────────────
        // CoordinatorLayout root = findViewById(R.id.coordinatorVendedor);
        // new ChatManager(this, root);
        // ──────────────────────────────────────────────────────────────────

        searchView = findViewById(R.id.searchMisProductos);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filtrarProductos(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filtrarProductos(newText);
                    return true;
                }
            });
        }

        // Bottom Nav logic
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_seller_products) {
                mostrarTab("productos");
                return true;
            } else if (id == R.id.nav_seller_orders) {
                mostrarTab("pedidos");
                return true;
            } else if (id == R.id.nav_seller_add) {
                Intent i = new Intent(this, AgregarProductoVendedorActivity.class);
                startActivity(i);
                return true;
            }
            return false;
        });

        handleIntent(getIntent());
        cargarMisProductos();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("nav_to")) {
            String target = intent.getStringExtra("nav_to");
            if ("productos".equals(target)) {
                bottomNav.setSelectedItemId(R.id.nav_seller_products);
            } else if ("pedidos".equals(target)) {
                bottomNav.setSelectedItemId(R.id.nav_seller_orders);
            }
        } else {
            bottomNav.setSelectedItemId(R.id.nav_seller_products);
        }
    }

    private void mostrarTab(String tab) {
        String nombre = sessionManager.getNombre();
        if ("productos".equals(tab)) {
            panelProductos.setVisibility(View.VISIBLE);
            panelPedidos.setVisibility(View.GONE);
            if (nombre != null) {
                tvWelcomeVendedor.setText("¡Hola, " + nombre + "! 👋");
            }
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("Vendedor - AgroConecta");
        } else {
            panelProductos.setVisibility(View.GONE);
            panelPedidos.setVisibility(View.VISIBLE);
            tvWelcomeVendedor.setText("AgroConecta");
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("Gestión de Pedidos 🛒");
            cargarPedidosVendedor();
        }
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres salir?")
                .setPositiveButton("Sí", (d, w) -> {
                    sessionManager.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("No", null).show();
    }

    // ── Mis Productos ─────────────────────────────────────
    private void cargarMisProductos() {
        int userId = sessionManager.getUserId();
        if (progressProductos != null) progressProductos.setVisibility(android.view.View.VISIBLE);
        if (tvEmptyProductos != null) tvEmptyProductos.setVisibility(android.view.View.GONE);
        rvMisProductos.setVisibility(android.view.View.GONE);
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> r) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    misProductos.clear();
                    for (Product p : r.body())
                        if (p.getUsuarioId() != null && p.getUsuarioId() == userId)
                            misProductos.add(p);
                    misProductosFiltrados.clear();
                    misProductosFiltrados.addAll(misProductos);
                    actualizarStats();
                    if (misProductos.isEmpty()) {
                        if (tvEmptyProductos != null) { tvEmptyProductos.setText("No tienes productos publicados aún"); tvEmptyProductos.setVisibility(android.view.View.VISIBLE); }
                    } else {
                        rvMisProductos.setVisibility(android.view.View.VISIBLE);
                        rvMisProductos.setAdapter(new ProductoVendedorAdapter(misProductosFiltrados));
                    }
                }
            }
            @Override public void onFailure(Call<List<Product>> c, Throwable t) {
                if (progressProductos != null) progressProductos.setVisibility(android.view.View.GONE);
                if (tvEmptyProductos != null) { tvEmptyProductos.setText("Sin conexión. Intenta de nuevo."); tvEmptyProductos.setVisibility(android.view.View.VISIBLE); }
                Toast.makeText(VendedorDashboardActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarProductos(String texto) {
        misProductosFiltrados.clear();
        if (texto.isEmpty()) {
            misProductosFiltrados.addAll(misProductos);
        } else {
            String query = texto.toLowerCase().trim();
            for (Product p : misProductos) {
                if (p.getNombre().toLowerCase().contains(query)) {
                    misProductosFiltrados.add(p);
                }
            }
        }
        
        View tvNoRes = findViewById(R.id.tvNoResultadosVendedor);
        if (tvNoRes != null) {
            tvNoRes.setVisibility(misProductosFiltrados.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (rvMisProductos.getAdapter() != null) {
            rvMisProductos.getAdapter().notifyDataSetChanged();
        }
    }

    private void actualizarStats() {
        int total   = misProductos.size();
        int activos = 0;
        for (Product p : misProductos) if (p.getExistencia() > 0) activos++;
        if (tvTotalProd != null) tvTotalProd.setText(String.valueOf(total));
        if (tvActivos != null) tvActivos.setText(String.valueOf(activos));
        if (tvAgotados != null) tvAgotados.setText(String.valueOf(total - activos));
    }

    // ── Pedidos recibidos ─────────────────────────────────
    private void cargarPedidosVendedor() {
        int userId = sessionManager.getUserId();
        if (progressPedidos != null) progressPedidos.setVisibility(android.view.View.VISIBLE);
        if (tvEmptyPedidos != null) tvEmptyPedidos.setVisibility(android.view.View.GONE);
        rvPedidosVendedor.setVisibility(android.view.View.GONE);
        vendedorApi.getPedidosVendedor(userId).enqueue(new Callback<List<Pedido>>() {
            @Override
            public void onResponse(Call<List<Pedido>> call, Response<List<Pedido>> r) {
                if (progressPedidos != null) progressPedidos.setVisibility(android.view.View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    misPedidos.clear();
                    misPedidos.addAll(r.body());
                    if (misPedidos.isEmpty()) {
                        if (tvEmptyPedidos != null) { tvEmptyPedidos.setText("No tienes pedidos aún"); tvEmptyPedidos.setVisibility(android.view.View.VISIBLE); }
                    } else {
                        rvPedidosVendedor.setVisibility(android.view.View.VISIBLE);
                        rvPedidosVendedor.setAdapter(new PedidoVendedorAdapter());
                    }
                } else {
                    if (tvEmptyPedidos != null) { tvEmptyPedidos.setText("Error al cargar pedidos"); tvEmptyPedidos.setVisibility(android.view.View.VISIBLE); }
                    Toast.makeText(VendedorDashboardActivity.this,
                            "Error al cargar pedidos", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<Pedido>> c, Throwable t) {
                if (progressPedidos != null) progressPedidos.setVisibility(android.view.View.GONE);
                if (tvEmptyPedidos != null) { tvEmptyPedidos.setText("Sin conexión. Intenta de nuevo."); tvEmptyPedidos.setVisibility(android.view.View.VISIBLE); }
                Toast.makeText(VendedorDashboardActivity.this,
                        "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Adapter Productos del vendedor ────────────────────
    class ProductoVendedorAdapter extends RecyclerView.Adapter<ProductoVendedorAdapter.VH> {
        private List<Product> lista;
        ProductoVendedorAdapter(List<Product> lista) { this.lista = lista; }
        
        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_producto_vendedor, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Product p = lista.get(pos);
            ((TextView) h.itemView.findViewById(R.id.tvNombreVendedor)).setText(p.getNombre());
            ((TextView) h.itemView.findViewById(R.id.tvPrecioVendedor))
                    .setText(String.format("$%.2f", p.getPrecio()));
            ((TextView) h.itemView.findViewById(R.id.tvStockVendedor))
                    .setText("Stock: " + p.getExistencia());

            TextView tvEstado = h.itemView.findViewById(R.id.tvEstadoVendedor);
            if (p.getExistencia() > 0) {
                tvEstado.setText("Activo");
                tvEstado.setTextColor(0xFF27AE60);
                tvEstado.setBackgroundResource(R.drawable.bg_badge_verde);
            } else {
                tvEstado.setText("Agotado");
                tvEstado.setTextColor(0xFFC0392B);
                tvEstado.setBackgroundResource(R.drawable.bg_badge_rojo);
            }

            if (p.getImagen() != null && !p.getImagen().isEmpty()) {
                Glide.with(h.itemView).load(p.getImagen())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into((android.widget.ImageView) h.itemView.findViewById(R.id.ivProductoVendedor));
            }

            h.itemView.findViewById(R.id.btnEditarProductoV).setOnClickListener(v -> {
                Intent i = new Intent(VendedorDashboardActivity.this,
                        AgregarProductoVendedorActivity.class);
                i.putExtra("producto_id", p.getProductoId());
                startActivity(i);
            });

            h.itemView.findViewById(R.id.btnEliminarProductoV).setOnClickListener(v ->
                    new AlertDialog.Builder(VendedorDashboardActivity.this)
                            .setTitle("Eliminar producto")
                            .setMessage("¿Eliminar \"" + p.getNombre() + "\"?")
                            .setPositiveButton("Eliminar", (d, w) -> eliminarProducto(p))
                            .setNegativeButton("Cancelar", null).show());
        }

        @Override public int getItemCount() { return lista.size(); }
    }

    // ── Adapter Pedidos recibidos ─────────────────────────
    class PedidoVendedorAdapter extends RecyclerView.Adapter<PedidoVendedorAdapter.VH> {
        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_pedido, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Pedido p = misPedidos.get(pos);

            ((TextView) h.itemView.findViewById(R.id.tvPedidoId))
                    .setText("Pedido #" + p.getPedidoId());
            ((TextView) h.itemView.findViewById(R.id.tvPedidoTotal))
                    .setText(String.format("$%.2f", p.getTotal()));
            ((TextView) h.itemView.findViewById(R.id.tvPedidoFecha))
                    .setText("📅 " + (p.getFecha() != null ?
                            p.getFecha().substring(0, 10) : "Hoy"));

            // Items del pedido
            TextView tvItems = h.itemView.findViewById(R.id.tvPedidoItems);
            if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                StringBuilder sb = new StringBuilder("🛒 ");
                for (int i = 0; i < p.getDetalles().size(); i++) {
                    sb.append(p.getDetalles().get(i).getNombre())
                      .append(" x").append(p.getDetalles().get(i).getCantidad());
                    if (i < p.getDetalles().size() - 1) sb.append(", ");
                }
                tvItems.setText(sb.toString());
            } else {
                tvItems.setText("Ver detalles");
            }

            // Estado con color
            TextView tvEstado = h.itemView.findViewById(R.id.tvPedidoEstado);
            String estado = p.getEstado() != null ? p.getEstado() : p.getEstadoTexto();
            tvEstado.setText(estado);
            switch (p.getEstadoId()) {
                case 3:
                    tvEstado.setTextColor(0xFF27AE60);
                    tvEstado.setBackgroundResource(R.drawable.bg_badge_verde); break;
                case 4:
                    tvEstado.setTextColor(0xFFC0392B);
                    tvEstado.setBackgroundResource(R.drawable.bg_badge_rojo); break;
                default:
                    tvEstado.setTextColor(0xFFB7770D);
                    tvEstado.setBackgroundResource(R.drawable.bg_badge_amber); break;
            }

            // Botón cambiar estado del pedido
            android.view.View btnSeg = h.itemView.findViewById(R.id.btnSeguimiento);
            if (btnSeg != null) {
                btnSeg.setOnClickListener(v -> mostrarDialogoCambioEstado(p, pos));
            }

            // Ocultar botón calificar en panel vendedor
            android.view.View btnCal = h.itemView.findViewById(R.id.btnCalificar);
            if (btnCal != null) btnCal.setVisibility(android.view.View.GONE);
        }

        @Override public int getItemCount() { return misPedidos.size(); }
    }

    private void mostrarDialogoCambioEstado(Pedido pedido, int pos) {
        String[] estados = {"Pendiente", "En preparacion", "En camino", "Entregado"};
        String[] emojis  = {"⏳", "🔧", "🚚", "✅"};
        int estadoActual = pedido.getEstadoId() - 1;

        new AlertDialog.Builder(this)
            .setTitle("Cambiar estado del Pedido #" + pedido.getPedidoId())
            .setSingleChoiceItems(
                new String[]{
                    emojis[0] + " " + estados[0],
                    emojis[1] + " " + estados[1],
                    emojis[2] + " " + estados[2],
                    emojis[3] + " " + estados[3]
                },
                estadoActual >= 0 ? estadoActual : 0,
                null
            )
            .setPositiveButton("Confirmar", (dialog, which) -> {
                android.widget.ListView lv = ((AlertDialog) dialog).getListView();
                int seleccionado = lv.getCheckedItemPosition();
                int nuevoEstadoId = seleccionado + 1;
                actualizarEstadoPedido(pedido, nuevoEstadoId, pos);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    interface PedidoUpdateApi {
        @PUT("pedidos/{id}")
        retrofit2.Call<Void> actualizarEstado(@Path("id") int id, @Body Map<String, Object> body);
    }

    private void actualizarEstadoPedido(Pedido pedido, int nuevoEstadoId, int pos) {
        Map<String, Object> body = new HashMap<>();
        body.put("estado_id", nuevoEstadoId);

        ApiClient.getClient()
            .create(PedidoUpdateApi.class)
            .actualizarEstado(pedido.getPedidoId(), body)
            .enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> c, retrofit2.Response<Void> r) {
                    if (r.isSuccessful()) {
                        // Actualizar en Firebase para seguimiento en tiempo real
                        Map<String, Object> firebaseData = new HashMap<>();
                        firebaseData.put("estado_id", nuevoEstadoId);
                        firebaseData.put("fecha", new java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date()));
                        com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("pedidos_estado")
                            .child(String.valueOf(pedido.getPedidoId()))
                            .setValue(firebaseData);

                        // ── Notificar al comprador del cambio de estado ──────
                        String[] mensajes = {
                            "Tu pedido #" + pedido.getPedidoId() + " está pendiente ⏳",
                            "Tu pedido #" + pedido.getPedidoId() + " está en preparación 🔧",
                            "Tu pedido #" + pedido.getPedidoId() + " va en camino 🚚",
                            "Tu pedido #" + pedido.getPedidoId() + " fue entregado ✅"
                        };
                        int idx = Math.min(nuevoEstadoId - 1, mensajes.length - 1);
                        FCMHelper.notificarUsuario(
                            String.valueOf(pedido.getUsuarioId()),
                            "📦 Actualización de tu pedido",
                            mensajes[idx],
                            "pedido"
                        );
                        // ────────────────────────────────────────────────────

                        // Actualizar lista local
                        pedido.setEstadoId(nuevoEstadoId);
                        misPedidos.set(pos, pedido);
                        rvPedidosVendedor.getAdapter().notifyItemChanged(pos);

                        Toast.makeText(VendedorDashboardActivity.this,
                            "Estado actualizado a: " + pedido.getEstadoTexto(),
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VendedorDashboardActivity.this,
                            "Error al actualizar", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<Void> c, Throwable t) {
                    Toast.makeText(VendedorDashboardActivity.this,
                        "Sin conexion", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void eliminarProducto(Product p) {
        productApi.eliminarProducto(p.getProductoId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(VendedorDashboardActivity.this,
                            "Producto eliminado", Toast.LENGTH_SHORT).show();
                    cargarMisProductos();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                Toast.makeText(VendedorDashboardActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
