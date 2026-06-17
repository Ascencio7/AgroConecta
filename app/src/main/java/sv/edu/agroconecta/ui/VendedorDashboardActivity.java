package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
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
    private List<Pedido> misPedidosFiltrados = new ArrayList<>();
    private RecyclerView rvMisProductos, rvPedidosVendedor;
    private TextView tvTotalProd, tvActivos, tvAgotados, tvAvatar, tvWelcomeVendedor;
    private ImageView ivAvatarFoto;
    private LinearLayout panelProductos, panelPedidos;
    private BottomNavigationView bottomNav;
    private androidx.appcompat.widget.SearchView searchView;
    private android.widget.ProgressBar progressProductos, progressPedidos;
    private android.widget.TextView tvEmptyProductos, tvEmptyPedidos;
    private Spinner spFiltroEstadoPedido;
    private com.google.android.material.button.MaterialButton btnFiltroTodos, btnFiltroActivos, btnFiltroNoDisp, btnFiltroAgotados;
    private String filtroActualProductos = "Todos";

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
        }

        tvTotalProd    = findViewById(R.id.tvTotalProductos);
        tvActivos      = findViewById(R.id.tvProductosActivos);
        tvAgotados     = findViewById(R.id.tvProductosAgotados);
        panelProductos = findViewById(R.id.panelMisProductos);
        panelPedidos   = findViewById(R.id.panelMisPedidosVendedor);
        rvMisProductos = findViewById(R.id.rvMisProductos);
        rvPedidosVendedor = findViewById(R.id.rvPedidosVendedor);
        tvAvatar       = findViewById(R.id.tvAvatarVendedor);
        ivAvatarFoto   = findViewById(R.id.ivAvatarFotoVendedor);
        bottomNav      = findViewById(R.id.bottomNavVendedor);
        progressProductos = findViewById(R.id.progressProductosVendedor);
        progressPedidos   = findViewById(R.id.progressPedidosVendedor);
        tvEmptyProductos  = findViewById(R.id.tvEmptyProductosVendedor);
        tvEmptyPedidos    = findViewById(R.id.tvEmptyPedidosVendedor);
        spFiltroEstadoPedido = findViewById(R.id.spFiltroEstadoPedido);

        btnFiltroTodos    = findViewById(R.id.btnFiltroTodosV);
        btnFiltroActivos  = findViewById(R.id.btnFiltroActivosV);
        btnFiltroNoDisp   = findViewById(R.id.btnFiltroNoDispV);
        btnFiltroAgotados = findViewById(R.id.btnFiltroAgotadosV);

        rvMisProductos.setLayoutManager(new LinearLayoutManager(this));
        rvPedidosVendedor.setLayoutManager(new LinearLayoutManager(this));

        setupHeaderProfile();
        setupSearch();
        setupBottomNav();
        setupPedidosFilter();
        setupProductosFilters();

        // Logo del header -> ir a la pantalla principal del vendedor (Productos)
        View ivHeaderLogo = findViewById(R.id.ivHeaderLogoVendedor);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> {
                bottomNav.setSelectedItemId(R.id.nav_seller_products);
            });
        }

        CoordinatorLayout root = findViewById(R.id.coordinatorVendedor);
        new ChatManager(this, root);

        handleIntent(getIntent());
        cargarMisProductos();
    }

    private void setupProductosFilters() {
        btnFiltroTodos.setOnClickListener(v -> { filtroActualProductos = "Todos"; updateFiltroButtonsUI(); filtrarProductos(searchView.getQuery().toString()); });
        btnFiltroActivos.setOnClickListener(v -> { filtroActualProductos = "Disponibles"; updateFiltroButtonsUI(); filtrarProductos(searchView.getQuery().toString()); });
        btnFiltroNoDisp.setOnClickListener(v -> { filtroActualProductos = "No Disponibles"; updateFiltroButtonsUI(); filtrarProductos(searchView.getQuery().toString()); });
        btnFiltroAgotados.setOnClickListener(v -> { filtroActualProductos = "Agotados"; updateFiltroButtonsUI(); filtrarProductos(searchView.getQuery().toString()); });
        updateFiltroButtonsUI();
    }

    private void updateFiltroButtonsUI() {
        resetFiltroButton(btnFiltroTodos);
        resetFiltroButton(btnFiltroActivos);
        resetFiltroButton(btnFiltroNoDisp);
        resetFiltroButton(btnFiltroAgotados);

        com.google.android.material.button.MaterialButton selected = btnFiltroTodos;
        if ("Disponibles".equals(filtroActualProductos)) selected = btnFiltroActivos;
        else if ("No Disponibles".equals(filtroActualProductos)) selected = btnFiltroNoDisp;
        else if ("Agotados".equals(filtroActualProductos)) selected = btnFiltroAgotados;

        selected.setStrokeWidth(4);
        selected.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.verde_primario)));
        selected.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.verde_primario));
    }

    private void resetFiltroButton(com.google.android.material.button.MaterialButton btn) {
        btn.setStrokeWidth(2);
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.gris_borde)));
        btn.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.texto_secundario));
    }

    private void setupHeaderProfile() {
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }
        String fotoPerfil = sessionManager.getFotoPerfil();
        if (fotoPerfil != null && !fotoPerfil.isEmpty() && ivAvatarFoto != null) {
            Glide.with(this).load(fotoPerfil).transform(new CircleCrop()).into(ivAvatarFoto);
            ivAvatarFoto.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
        }
        tvAvatar.setOnClickListener(this::showProfileMenu);
        if (ivAvatarFoto != null) ivAvatarFoto.setOnClickListener(this::showProfileMenu);
    }

    private void setupPedidosFilter() {
        String[] estados = {"Todos", "Pendiente", "En preparación", "En camino", "Entregado", "Pagado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, estados);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltroEstadoPedido.setAdapter(adapter);
        spFiltroEstadoPedido.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtrarPedidos(estados[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        searchView = findViewById(R.id.searchMisProductos);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) { filtrarProductos(query); return true; }
                @Override public boolean onQueryTextChange(String newText) { filtrarProductos(newText); return true; }
            });
        }
    }

    private void setupBottomNav() {
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
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("nav_to")) {
            String target = intent.getStringExtra("nav_to");
            if ("productos".equals(target)) bottomNav.setSelectedItemId(R.id.nav_seller_products);
            else if ("pedidos".equals(target)) bottomNav.setSelectedItemId(R.id.nav_seller_orders);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_seller_products);
        }
    }

    private void mostrarTab(String tab) {
        if ("productos".equals(tab)) {
            panelProductos.setVisibility(View.VISIBLE);
            panelPedidos.setVisibility(View.GONE);
            String n = sessionManager.getNombre();
            if (n != null) tvWelcomeVendedor.setText("¡Hola, " + n + "! 👋");
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("MIS PRODUCTOS");
        } else {
            panelProductos.setVisibility(View.GONE);
            panelPedidos.setVisibility(View.VISIBLE);
            tvWelcomeVendedor.setText("AgroConecta");
            ((TextView) findViewById(R.id.tvVendedorNombre)).setText("GESTIÓN DE PEDIDOS");
            cargarPedidosVendedor();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMisProductos();
        setupHeaderProfile();
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_view_profile) {
                startActivity(new Intent(this, PerfilVendedorActivity.class));
                return true;
            } else if (id == R.id.menu_soporte) {
                startActivity(new Intent(this, SoporteActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                confirmarLogout();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres salir?")
                .setPositiveButton("Sí, salir", (d, w) -> {
                    sessionManager.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("No", null).show();
    }

    private void cargarMisProductos() {
        int userId = sessionManager.getUserId();
        if (progressProductos != null) progressProductos.setVisibility(View.VISIBLE);
        if (tvEmptyProductos != null) tvEmptyProductos.setVisibility(View.GONE);
        rvMisProductos.setVisibility(View.GONE);
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> r) {
                if (progressProductos != null) progressProductos.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    misProductos.clear();
                    for (Product p : r.body())
                        if (p.getUsuarioId() != null && p.getUsuarioId() == userId)
                            misProductos.add(p);
                    filtrarProductos("");
                    actualizarStats();
                }
            }
            @Override public void onFailure(Call<List<Product>> c, Throwable t) {
                if (progressProductos != null) progressProductos.setVisibility(View.GONE);
                Toast.makeText(VendedorDashboardActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarProductos(String texto) {
        misProductosFiltrados.clear();
        String query = texto.toLowerCase().trim();
        for (Product p : misProductos) {
            String nombreP = p.getNombre() != null ? p.getNombre().toLowerCase() : "";
            boolean matchesSearch = nombreP.contains(query);
            boolean matchesFilter = false;

            if ("Todos".equals(filtroActualProductos)) {
                matchesFilter = true;
            } else if ("Disponibles".equals(filtroActualProductos)) {
                matchesFilter = (p.getEstado() == null || p.getEstado()) && p.getExistencia() > 0;
            } else if ("No Disponibles".equals(filtroActualProductos)) {
                matchesFilter = p.getEstado() != null && !p.getEstado();
            } else if ("Agotados".equals(filtroActualProductos)) {
                matchesFilter = (p.getEstado() == null || p.getEstado()) && p.getExistencia() == 0;
            }

            if (matchesSearch && matchesFilter) misProductosFiltrados.add(p);
        }

        if (misProductosFiltrados.isEmpty()) {
            tvEmptyProductos.setVisibility(View.VISIBLE);
            tvEmptyProductos.setText("No se encontraron productos");
            rvMisProductos.setVisibility(View.GONE);
        } else {
            tvEmptyProductos.setVisibility(View.GONE);
            rvMisProductos.setVisibility(View.VISIBLE);
            rvMisProductos.setAdapter(new ProductoVendedorAdapter(misProductosFiltrados));
        }
    }

    private void actualizarStats() {
        int total = misProductos.size();
        int activos = 0;
        int agotados = 0;
        for (Product p : misProductos) {
            if ((p.getEstado() == null || p.getEstado()) && p.getExistencia() > 0) activos++;
            if ((p.getEstado() == null || p.getEstado()) && p.getExistencia() == 0) agotados++;
        }
        if (tvTotalProd != null) tvTotalProd.setText(String.valueOf(total));
        if (tvActivos != null) tvActivos.setText(String.valueOf(activos));
        if (tvAgotados != null) tvAgotados.setText(String.valueOf(agotados));
    }

    private void cargarPedidosVendedor() {
        int userId = sessionManager.getUserId();
        if (progressPedidos != null) progressPedidos.setVisibility(View.VISIBLE);
        vendedorApi.getPedidosVendedor(userId).enqueue(new Callback<List<Pedido>>() {
            @Override
            public void onResponse(Call<List<Pedido>> call, Response<List<Pedido>> r) {
                if (progressPedidos != null) progressPedidos.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    misPedidos = r.body();
                    filtrarPedidos(spFiltroEstadoPedido.getSelectedItem().toString());
                }
            }
            @Override public void onFailure(Call<List<Pedido>> c, Throwable t) {
                if (progressPedidos != null) progressPedidos.setVisibility(View.GONE);
                Toast.makeText(VendedorDashboardActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarPedidos(String estado) {
        misPedidosFiltrados.clear();
        for (Pedido p : misPedidos) {
            String estTxt = p.getEstadoTexto();
            if (estado.equals("Todos") || (estTxt != null && estTxt.equalsIgnoreCase(estado))) {
                misPedidosFiltrados.add(p);
            }
        }
        if (misPedidosFiltrados.isEmpty()) {
            tvEmptyPedidos.setVisibility(View.VISIBLE);
            rvPedidosVendedor.setVisibility(View.GONE);
        } else {
            tvEmptyPedidos.setVisibility(View.GONE);
            rvPedidosVendedor.setVisibility(View.VISIBLE);
            rvPedidosVendedor.setAdapter(new PedidoVendedorAdapter());
        }
    }

    class ProductoVendedorAdapter extends RecyclerView.Adapter<ProductoVendedorAdapter.VH> {
        private List<Product> lista;
        ProductoVendedorAdapter(List<Product> lista) { this.lista = lista; }
        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_producto_vendedor, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Product p = lista.get(pos);
            ((TextView) h.itemView.findViewById(R.id.tvNombreVendedor)).setText(p.getNombre());
            ((TextView) h.itemView.findViewById(R.id.tvPrecioVendedor)).setText(String.format("$%.2f", p.getPrecio()));
            ((TextView) h.itemView.findViewById(R.id.tvStockVendedor)).setText("Stock: " + p.getExistencia());
            TextView tvE = h.itemView.findViewById(R.id.tvEstadoVendedor);
            com.google.android.material.button.MaterialButton btnDesactivar = h.itemView.findViewById(R.id.btnEliminarProductoV);
            ImageView ivProducto = h.itemView.findViewById(R.id.ivProductoVendedor);
            View llFotoNoDisponible = h.itemView.findViewById(R.id.llFotoNoDisponible);

            if (p.getEstado() != null && !p.getEstado()) {
                tvE.setText("No Disponible");
                tvE.setTextColor(0xFF7F8C8D); // Gris
                tvE.setBackgroundResource(R.drawable.bg_badge_rojo);
                
                btnDesactivar.setText("Activar");
                btnDesactivar.setIconResource(android.R.drawable.ic_menu_revert);
                btnDesactivar.setIconTint(android.content.res.ColorStateList.valueOf(0xFF27AE60));
                btnDesactivar.setTextColor(0xFF27AE60);
                btnDesactivar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                btnDesactivar.setOnClickListener(v -> confirmarCambioEstado(p, true));
            } else {
                if (p.getExistencia() > 0) {
                    tvE.setText("Activo"); tvE.setTextColor(0xFF27AE60); tvE.setBackgroundResource(R.drawable.bg_badge_verde);
                } else {
                    tvE.setText("Agotado"); tvE.setTextColor(0xFFC0392B); tvE.setBackgroundResource(R.drawable.bg_badge_rojo);
                }
                
                btnDesactivar.setText("Desactivar");
                btnDesactivar.setIconResource(android.R.drawable.ic_lock_power_off);
                btnDesactivar.setIconTint(android.content.res.ColorStateList.valueOf(0xFFC0392B));
                btnDesactivar.setTextColor(0xFFC0392B);
                btnDesactivar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF1F0));
                btnDesactivar.setOnClickListener(v -> confirmarCambioEstado(p, false));
            }

            if (p.getImagen() != null && !p.getImagen().isEmpty()) {
                llFotoNoDisponible.setVisibility(View.GONE);
                ivProducto.setVisibility(View.VISIBLE);
                Glide.with(h.itemView)
                        .load(p.getImagen())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.logoapp)
                        .into(ivProducto);
            } else {
                ivProducto.setVisibility(View.GONE);
                llFotoNoDisponible.setVisibility(View.VISIBLE);
            }

            h.itemView.findViewById(R.id.btnEditarProductoV).setOnClickListener(v -> {
                Intent i = new Intent(VendedorDashboardActivity.this, AgregarProductoVendedorActivity.class);
                i.putExtra("producto_id", p.getProductoId());
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return lista.size(); }
    }

    private void confirmarCambioEstado(Product p, boolean activar) {
        String msg = activar ? "¿Deseas activar este producto?" : "¿Deseas desactivar este producto?";
        new AlertDialog.Builder(this)
                .setTitle(activar ? "Activar" : "Desactivar")
                .setMessage(msg)
                .setPositiveButton("Sí", (d, w) -> cambiarEstadoProducto(p, activar))
                .setNegativeButton("No", null).show();
    }

    private void cambiarEstadoProducto(Product p, boolean activo) {
        p.setEstado(activo);
        productApi.actualizarProducto(p.getProductoId(), p).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VendedorDashboardActivity.this, activo ? "Producto activado" : "Producto desactivado", Toast.LENGTH_SHORT).show();
                    cargarMisProductos();
                } else {
                    p.setEstado(!activo); // Revertir si falla
                    Toast.makeText(VendedorDashboardActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                p.setEstado(!activo);
                Toast.makeText(VendedorDashboardActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class PedidoVendedorAdapter extends RecyclerView.Adapter<PedidoVendedorAdapter.VH> {
        class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_pedido_vendedor, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Pedido p = misPedidosFiltrados.get(pos);
            ((TextView) h.itemView.findViewById(R.id.tvPedidoId)).setText("Pedido #" + p.getPedidoId());
            ((TextView) h.itemView.findViewById(R.id.tvPedidoTotal)).setText(String.format("$%.2f", p.getTotal()));
            ((TextView) h.itemView.findViewById(R.id.tvPedidoFecha)).setText("📅 " + (p.getFecha() != null ? p.getFecha().substring(0, 10) : "Hoy"));
            TextView tvE = h.itemView.findViewById(R.id.tvPedidoEstado);
            String est = p.getEstadoTexto();
            if (p.getMetodoPago() != null && !p.getMetodoPago().isEmpty()) {
                est += " [" + p.getMetodoPago() + "]";
            }
            tvE.setText(est);
            
            if (p.getEstadoId() == 4) { // Entregado
                tvE.setTextColor(0xFF27AE60); 
                tvE.setBackgroundResource(R.drawable.bg_badge_verde);
            } else if (p.getEstadoId() == 5) { // Pagado
                tvE.setTextColor(0xFF2980B9); // Azul
                tvE.setBackgroundResource(R.drawable.bg_badge_verde); // Usar verde o uno neutro si no hay azul
            } else {
                tvE.setTextColor(0xFFB7770D);
                tvE.setBackgroundResource(R.drawable.bg_badge_amber);
            }
            
            // Foto y Nombre del Cliente
            TextView tvAvatarC = h.itemView.findViewById(R.id.tvAvatarCliente);
            ImageView ivFotoC = h.itemView.findViewById(R.id.ivFotoCliente);
            TextView tvNombreC = h.itemView.findViewById(R.id.tvNombreCliente);
            
            if (p.getNombreCliente() != null && !p.getNombreCliente().isEmpty()) {
                tvNombreC.setText(p.getNombreCliente());
                if (tvAvatarC != null) tvAvatarC.setText(String.valueOf(p.getNombreCliente().charAt(0)).toUpperCase());
            } else {
                tvNombreC.setText("Cliente");
                if (tvAvatarC != null) tvAvatarC.setText("C");
            }
            
            if (p.getFotoCliente() != null && !p.getFotoCliente().isEmpty()) {
                Glide.with(h.itemView.getContext())
                    .load(p.getFotoCliente())
                    .transform(new CircleCrop())
                    .into(ivFotoC);
                ivFotoC.setVisibility(View.VISIBLE);
                if (tvAvatarC != null) tvAvatarC.setVisibility(View.GONE);
            } else {
                ivFotoC.setVisibility(View.GONE);
                if (tvAvatarC != null) tvAvatarC.setVisibility(View.VISIBLE);
            }

            // Mostrar items del pedido
            TextView tvItems = h.itemView.findViewById(R.id.tvPedidoItems);
            if (tvItems != null) {
                if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < p.getDetalles().size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(p.getDetalles().get(i).getNombre());
                        if (p.getDetalles().get(i).getCantidad() > 1) {
                            sb.append(" (x").append(p.getDetalles().get(i).getCantidad()).append(")");
                        }
                    }
                    tvItems.setText(sb.toString());
                } else {
                    tvItems.setText("Sin detalles de productos");
                }
            }

            h.itemView.findViewById(R.id.btnSeg).setOnClickListener(v -> mostrarDialogoCambioEstado(p, pos));

            // Botón WhatsApp Cliente
            h.itemView.findViewById(R.id.btnWhatsAppCliente).setOnClickListener(v -> {
                String tel = p.getTelefonoCliente();
                if (tel != null && !tel.isEmpty()) {
                    // Limpiar el número de caracteres no numéricos
                    String numLimpio = tel.replaceAll("[^0-9]", "");
                    if (!numLimpio.startsWith("503") && numLimpio.length() == 8) {
                        numLimpio = "503" + numLimpio;
                    }
                    
                    String msg = "Hola " + p.getNombreCliente() + ", te contacto de AgroConecta por tu pedido #" + p.getPedidoId();
                    String url = "https://wa.me/" + numLimpio + "?text=" + Uri.encode(msg);
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        v.getContext().startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(v.getContext(), "El cliente no tiene teléfono registrado", Toast.LENGTH_SHORT).show();
                }
            });
        }
        @Override public int getItemCount() { return misPedidosFiltrados.size(); }
    }

    private void mostrarDialogoCambioEstado(Pedido pedido, int pos) {
        String[] estados = {"Pendiente", "En preparación", "En camino", "Entregado", "Pagado"};
        int actual = pedido.getEstadoId() - 1;
        new AlertDialog.Builder(this).setTitle("Estado Pedido #" + pedido.getPedidoId()).setSingleChoiceItems(estados, actual >= 0 ? actual : 0, null)
            .setPositiveButton("Confirmar", (dialog, which) -> {
                int sel = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                actualizarEstadoPedido(pedido, sel + 1, pos);
            }).setNegativeButton("Cancelar", null).show();
    }

    interface PedidoUpdateApi { @PUT("pedidos/{id}") Call<Void> actualizarEstado(@Path("id") int id, @Body Map<String, Object> body); }

    private void actualizarEstadoPedido(Pedido pedido, int nuevoEstadoId, int pos) {
        Map<String, Object> body = new HashMap<>(); body.put("estado_id", nuevoEstadoId);
        ApiClient.getClient().create(PedidoUpdateApi.class).actualizarEstado(pedido.getPedidoId(), body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    pedido.setEstadoId(nuevoEstadoId);
                    
                    // Actualizar en Firebase para seguimiento en tiempo real
                    Map<String, Object> fbData = new HashMap<>();
                    fbData.put("estado_id", nuevoEstadoId);
                    fbData.put("fecha", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                    
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("pedidos_estado")
                        .child(String.valueOf(pedido.getPedidoId()))
                        .setValue(fbData);

                    // Notificar al cliente sobre el cambio de estado
                    String tit = "Actualización de Pedido #" + pedido.getPedidoId();
                    String msg = "Tu pedido ahora está: " + (nuevoEstadoId == 1 ? "Pendiente" : 
                                 nuevoEstadoId == 2 ? "En preparación" : 
                                 nuevoEstadoId == 3 ? "En camino" : 
                                 nuevoEstadoId == 4 ? "Entregado" : "Pagado");
                    
                    FCMHelper.notificarUsuario(String.valueOf(pedido.getUsuarioId()), tit, msg, "pedido");

                    cargarPedidosVendedor();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    private void eliminarProducto(Product p) {
        productApi.eliminarProducto(p.getProductoId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) { Toast.makeText(VendedorDashboardActivity.this, "Eliminado", Toast.LENGTH_SHORT).show(); cargarMisProductos(); }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }
}
