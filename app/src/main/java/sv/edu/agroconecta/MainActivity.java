package sv.edu.agroconecta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

import sv.edu.agroconecta.adapter.CarritoAdapter;
import sv.edu.agroconecta.adapter.ProductAdapter;
import sv.edu.agroconecta.modelo.DetallePedido;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.repository.PedidoRepository;
import sv.edu.agroconecta.ui.LoginActivity;
import sv.edu.agroconecta.ui.ProductoDetalleActivity;
import sv.edu.agroconecta.utils.CarritoManager;
import sv.edu.agroconecta.utils.FCMHelper;
import sv.edu.agroconecta.utils.SessionManager;

import android.content.res.ColorStateList;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private SessionManager sessionManager;
    private String nombreUsuario;
    private int usuarioId;

    private BottomNavigationView bottomNav;
    private FrameLayout frameContenido;
    private TextView tvHeaderSubtitle, tvAvatar, tvCarritoBadge, tvWelcome;

    private View viewCatalogo, viewPedidos, viewMapa, viewCarrito;

    // Catálogo
    private RecyclerView rvCatalogo;
    private ProductAdapter productAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private String filtroCategoria = "Todos";
    private SearchView searchViewCatalogo;

    // Carrito
    private RecyclerView rvCarritoTab;
    private CarritoAdapter carritoAdapter;
    private TextView tvResumenSubtotal, tvResumenIva, tvResumenTotal;
    private static final double IVA = 0.13;

    // Mapa
    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERM = 1001;

    private static final double[][] FINCAS = {
        {13.6929, -89.2182}, {13.7034, -89.2245},
        {13.6850, -89.2100}, {13.7200, -89.1900},
        {13.6600, -89.2400}, {13.7100, -89.2600}
    };
    private static final String[] NOMBRES_FINCAS = {
        "Finca El Roble", "Finca La Esperanza",
        "Finca San José", "Finca Los Pinos",
        "Finca El Rosal", "Rancho El Sol"
    };
    private static final String[] PRODUCTOS_FINCAS = {
        "Maíz, Frijol", "Café, Caña",
        "Tomate, Chile", "Verduras orgánicas",
        "Maíz blanco, Sorgo", "Frutas tropicales"
    };
    private static final String[] EMOJIS_FINCAS = {"🌽","☕","🍅","🥦","🌾","🍌"};

    interface ProductoApi {
        @GET("productos")
        Call<List<Product>> getProductos();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        nombreUsuario = getIntent().getStringExtra("nombre");
        usuarioId     = getIntent().getIntExtra("usuario_id", -1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvAvatar         = findViewById(R.id.tvAvatar);
        tvCarritoBadge   = findViewById(R.id.tvCarritoBadge);
        tvWelcome        = findViewById(R.id.tvWelcome);
        frameContenido   = findViewById(R.id.frameContenido);
        bottomNav        = findViewById(R.id.bottomNav);

        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombreUsuario.charAt(0)).toUpperCase());
            tvWelcome.setText("¡Hola, " + nombreUsuario + "! 👋");
        }

        tvAvatar.setOnClickListener(v -> confirmarLogout());
        findViewById(R.id.btnHeaderCarrito).setOnClickListener(v ->
                bottomNav.setSelectedItemId(R.id.nav_carrito));

        // Chat flotante
        CoordinatorLayout root = findViewById(R.id.coordinatorMain);
        new ChatManager(this, root);

        // Inflar pestañas
        LayoutInflater inf = LayoutInflater.from(this);
        viewCatalogo = inf.inflate(R.layout.fragment_catalogo, frameContenido, false);
        viewPedidos  = inf.inflate(R.layout.fragment_pedidos,  frameContenido, false);
        viewMapa     = inf.inflate(R.layout.fragment_mapa,     frameContenido, false);
        viewCarrito  = inf.inflate(R.layout.fragment_carrito,  frameContenido, false);

        frameContenido.addView(viewCatalogo);
        frameContenido.addView(viewPedidos);
        frameContenido.addView(viewMapa);
        frameContenido.addView(viewCarrito);

        // Inicializar MapView (requiere lifecycle)
        mapView = viewMapa.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        initCatalogo();
        initPedidos();
        initMapa();
        initCarrito();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_catalogo) {
                mostrarPestana(viewCatalogo, "Catálogo de productos");
            } else if (id == R.id.nav_pedidos) {
                mostrarPestana(viewPedidos, "Mis Pedidos");
                cargarPedidos();
            } else if (id == R.id.nav_mapa) {
                mostrarPestana(viewMapa, "Mapa de Fincas");
                mapView.onResume();
                if (mMap == null) mapView.getMapAsync(this);
            } else if (id == R.id.nav_carrito) {
                mostrarPestana(viewCarrito, "Mi Carrito 🛒");
                actualizarCarritoTab();
            }
            return true;
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("nav_to")) {
            String target = intent.getStringExtra("nav_to");
            if ("catalogo".equals(target)) bottomNav.setSelectedItemId(R.id.nav_catalogo);
            else if ("pedidos".equals(target)) bottomNav.setSelectedItemId(R.id.nav_pedidos);
            else if ("mapa".equals(target)) bottomNav.setSelectedItemId(R.id.nav_mapa);
            else if ("carrito".equals(target)) bottomNav.setSelectedItemId(R.id.nav_carrito);
        } else {
            mostrarPestana(viewCatalogo, "Catálogo de productos");
        }
    }

    private void mostrarPestana(View target, String subtitulo) {
        viewCatalogo.setVisibility(View.GONE);
        viewPedidos.setVisibility(View.GONE);
        viewMapa.setVisibility(View.GONE);
        viewCarrito.setVisibility(View.GONE);
        target.setVisibility(View.VISIBLE);
        
        String nombre = sessionManager.getNombre();
        if (target == viewCatalogo) {
            if (nombre != null) {
                tvWelcome.setText("¡Hola, " + nombre + "! 👋");
            }
            tvHeaderSubtitle.setText("Catálogo de productos");
        } else {
            tvWelcome.setText("AgroConecta");
            tvHeaderSubtitle.setText(subtitulo);
        }
    }

    // ── CATÁLOGO ─────────────────────────────────────────
    private void initCatalogo() {
        rvCatalogo         = viewCatalogo.findViewById(R.id.rvCatalogo);
        searchViewCatalogo = viewCatalogo.findViewById(R.id.searchViewCatalogo);
        LinearLayout llCats= viewCatalogo.findViewById(R.id.llCategorias);

        // Asegurar que el SearchView esté expandido y listo
        if (searchViewCatalogo != null) {
            searchViewCatalogo.setIconifiedByDefault(false);
            searchViewCatalogo.setFocusable(true);
            searchViewCatalogo.setIconified(false);
            searchViewCatalogo.requestFocusFromTouch();
        }

        productAdapter = new ProductAdapter(this, filteredProducts,
            product -> {
                Intent i = new Intent(this, ProductoDetalleActivity.class);
                i.putExtra("producto_id",      product.getProductoId());
                i.putExtra("usuario_id",       product.getUsuarioId() != null ? product.getUsuarioId() : -1);
                i.putExtra("nombre",           product.getNombre());
                i.putExtra("descripcion",      product.getDescripcion());
                i.putExtra("precio",           product.getPrecio());
                i.putExtra("imagen",           product.getImagen());
                i.putExtra("categoria",        product.getCategoria());
                i.putExtra("existencia",       product.getExistencia());
                i.putExtra("telefono_vendedor",product.getTelefonoVendedor() != null ? product.getTelefonoVendedor() : "");
                i.putExtra("nombre_vendedor",  product.getNombreVendedor() != null ? product.getNombreVendedor() : "");
                i.putExtra("metodos_pago",     product.getMetodosPagoTexto());
                i.putExtra("direccion",        product.getDireccion() != null ? product.getDireccion() : "");
                if (product.getLatitud() != null) {
                    i.putExtra("latitud",  product.getLatitud());
                    i.putExtra("longitud", product.getLongitud() != null ? product.getLongitud() : 0.0);
                }
                startActivity(i);
            },
            (product, rating) -> {}
        );
        rvCatalogo.setLayoutManager(new LinearLayoutManager(this));
        rvCatalogo.setAdapter(productAdapter);

        String[] cats   = {"Todos","Verduras","Frutas","Granos","Lácteos","Hierbas"};
        String[] emojis = {"🌿","🥦","🍊","🌾","🧀","🌱"};
        for (int i = 0; i < cats.length; i++) {
            TextView chip = crearChip(cats[i], emojis[i]);
            final String cat = cats[i];
            final TextView fc = chip;
            chip.setOnClickListener(v -> {
                filtroCategoria = cat;
                for (int j = 0; j < llCats.getChildCount(); j++)
                    llCats.getChildAt(j).setSelected(false);
                fc.setSelected(true);
                aplicarFiltro();
            });
            if (i == 0) chip.setSelected(true);
            llCats.addView(chip);
        }

        searchViewCatalogo.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) { aplicarFiltro(); return true; }
        });

        cargarProductos();
    }

    private TextView crearChip(String texto, String emoji) {
        TextView tv = new TextView(this);
        tv.setText(emoji + " " + texto);
        tv.setTextSize(13f);
        tv.setPadding(28, 14, 28, 14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(6, 0, 6, 0);
        tv.setLayoutParams(lp);
        tv.setBackgroundResource(R.drawable.bg_chip_selector);
        // Usar selector de color de texto para que cambie automáticamente
        tv.setTextColor(getResources().getColorStateList(R.color.chip_text_selector, getTheme()));
        return tv;
    }

    private void cargarProductos() {
        ProductoApi api = ApiClient.getClient().create(ProductoApi.class);
        api.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    allProducts.clear();
                    allProducts.addAll(r.body());
                    aplicarFiltro();
                }
            }
            @Override public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarFiltro() {
        String q = searchViewCatalogo != null
            ? searchViewCatalogo.getQuery().toString().toLowerCase().trim() : "";
        filteredProducts.clear();
        for (Product p : allProducts) {
            boolean matchCat = filtroCategoria.equals("Todos") ||
                (p.getCategoria() != null && p.getCategoria().contains(filtroCategoria));
            boolean matchQ = q.isEmpty() ||
                p.getNombre().toLowerCase().contains(q) ||
                (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(q));
            if (matchCat && matchQ) filteredProducts.add(p);
        }
        if (productAdapter != null) productAdapter.notifyDataSetChanged();

        TextView tv = viewCatalogo.findViewById(R.id.tvContador);
        TextView tvNoRes = viewCatalogo.findViewById(R.id.tvNoResultados);

        if (filteredProducts.isEmpty()) {
            if (tv != null) tv.setText("0 productos");
            if (tvNoRes != null) tvNoRes.setVisibility(View.VISIBLE);
        } else {
            if (tv != null) tv.setText(filteredProducts.size() + " productos");
            if (tvNoRes != null) tvNoRes.setVisibility(View.GONE);
        }
    }

    // ── PEDIDOS ──────────────────────────────────────────
    private RecyclerView rvPedidos;

    private void initPedidos() {
        rvPedidos = viewPedidos.findViewById(R.id.rvPedidos);
        rvPedidos.setLayoutManager(new LinearLayoutManager(this));
    }

    private void cargarPedidos() {
        int userId = sessionManager.getUserId();
        sv.edu.agroconecta.network.ApiClient.getClient()
            .create(sv.edu.agroconecta.network.PedidoApi.class)
            .getPedidosPorUsuario(userId)
            .enqueue(new retrofit2.Callback<java.util.List<sv.edu.agroconecta.modelo.Pedido>>() {
                @Override
                public void onResponse(retrofit2.Call<java.util.List<sv.edu.agroconecta.modelo.Pedido>> call,
                                       retrofit2.Response<java.util.List<sv.edu.agroconecta.modelo.Pedido>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        mostrarPedidos(r.body());
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<java.util.List<sv.edu.agroconecta.modelo.Pedido>> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Error al cargar pedidos", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void mostrarPedidos(java.util.List<sv.edu.agroconecta.modelo.Pedido> pedidos) {
        rvPedidos.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_pedido, p, false);
                return new RecyclerView.ViewHolder(v){};
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                sv.edu.agroconecta.modelo.Pedido p = pedidos.get(pos);

                ((TextView) h.itemView.findViewById(R.id.tvPedidoId))
                        .setText("Pedido #" + p.getPedidoId());
                ((TextView) h.itemView.findViewById(R.id.tvPedidoTotal))
                        .setText(String.format("$%.2f", p.getTotal()));
                ((TextView) h.itemView.findViewById(R.id.tvPedidoFecha))
                        .setText("📅 " + (p.getFecha() != null ? p.getFecha().substring(0, 10) : "Hoy"));

                // Items
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

                // Estado con color
                TextView tvE = h.itemView.findViewById(R.id.tvPedidoEstado);
                String estado = p.getEstadoTexto();
                tvE.setText(estado);
                switch (p.getEstadoId()) {
                    case 3:
                        tvE.setTextColor(0xFF27AE60);
                        tvE.setBackgroundResource(R.drawable.bg_badge_verde); break;
                    case 4:
                        tvE.setTextColor(0xFFC0392B);
                        tvE.setBackgroundResource(R.drawable.bg_badge_rojo); break;
                    default:
                        tvE.setTextColor(0xFFB7770D);
                        tvE.setBackgroundResource(R.drawable.bg_badge_amber); break;
                }

                // Botón Seguimiento
                View btnSeg = h.itemView.findViewById(R.id.btnSeguimiento);
                if (btnSeg != null) {
                    btnSeg.setOnClickListener(v -> {
                        Intent i = new Intent(MainActivity.this,
                                sv.edu.agroconecta.ui.SeguimientoPedidoActivity.class);
                        i.putExtra("pedido_id", p.getPedidoId());
                        i.putExtra("estado_id", p.getEstadoId());
                        startActivity(i);
                    });
                }

                // Botón Calificar — solo disponible cuando el pedido fue entregado (estado 4)
                View btnCal = h.itemView.findViewById(R.id.btnCalificar);
                if (btnCal != null) {
                    if (p.getEstadoId() == 4) {
                        btnCal.setEnabled(true);
                        btnCal.setAlpha(1f);
                        btnCal.setOnClickListener(v -> {
                            Intent i = new Intent(MainActivity.this,
                                    sv.edu.agroconecta.ui.CalificacionActivity.class);
                            i.putExtra("pedido_id", p.getPedidoId());
                            if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                                sv.edu.agroconecta.modelo.DetallePedido det = p.getDetalles().get(0);
                                i.putExtra("producto_id", det.getProductoId());
                                i.putExtra("nombre_producto", det.getNombre());
                            }
                            startActivity(i);
                        });
                    } else {
                        btnCal.setEnabled(false);
                        btnCal.setAlpha(0.4f);
                    }
                }
            }
            @Override public int getItemCount() { return pedidos.size(); }
        });
    }

    // ── MAPA ─────────────────────────────────────────────
    private void initMapa() {
        viewMapa.findViewById(R.id.fabMiUbicacion).setOnClickListener(v -> irAMiUbicacion());
        poblarFincasPanel();
    }

    private void poblarFincasPanel() {
        LinearLayout ll = viewMapa.findViewById(R.id.llFincas);
        ll.removeAllViews();
        for (int i = 0; i < NOMBRES_FINCAS.length; i++)
            ll.addView(crearFilaFinca(EMOJIS_FINCAS[i], NOMBRES_FINCAS[i], PRODUCTOS_FINCAS[i]));
    }

    private View crearFilaFinca(String emoji, String nombre, String productos) {
        LinearLayout cont = new LinearLayout(this);
        cont.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView emo = new TextView(this);
        emo.setText(emoji); emo.setTextSize(22f);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(56, LinearLayout.LayoutParams.WRAP_CONTENT);
        emo.setLayoutParams(elp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvN = new TextView(this);
        tvN.setText(nombre); tvN.setTextSize(14f);
        tvN.setTypeface(null, Typeface.BOLD);
        tvN.setTextColor(0xFF1A1A18);

        TextView tvP = new TextView(this);
        tvP.setText(productos); tvP.setTextSize(12f);
        tvP.setTextColor(0xFF5A5A50);

        info.addView(tvN); info.addView(tvP);
        row.addView(emo); row.addView(info);
        cont.addView(row);

        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(0xFFE0DDD5);
        cont.addView(div);
        return cont;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        for (int i = 0; i < FINCAS.length; i++) {
            LatLng pos = new LatLng(FINCAS[i][0], FINCAS[i][1]);
            mMap.addMarker(new MarkerOptions()
                .position(pos).title(NOMBRES_FINCAS[i])
                .snippet("Productos: " + PRODUCTOS_FINCAS[i])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(13.6929, -89.2182), 11));
        habilitarUbicacion();
    }

    private void habilitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERM);
        }
    }

    private void irAMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && mMap != null)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(loc.getLatitude(), loc.getLongitude()), 15));
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == LOCATION_PERM && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED)
            habilitarUbicacion();
    }

    // ── CARRITO ──────────────────────────────────────────
    private void initCarrito() {
        rvCarritoTab      = viewCarrito.findViewById(R.id.rvCarritoTab);
        tvResumenSubtotal = viewCarrito.findViewById(R.id.tvResumenSubtotal);
        tvResumenIva      = viewCarrito.findViewById(R.id.tvResumenIva);
        tvResumenTotal    = viewCarrito.findViewById(R.id.tvResumenTotal);

        carritoAdapter = new CarritoAdapter(this,
            CarritoManager.getInstance().getItems(),
            new CarritoAdapter.OnItemChangedListener() {
                @Override public void onItemRemoved(int pos) { actualizarCarritoTab(); }
                @Override public void onCantidadChanged()    { actualizarCarritoTab(); }
            });
        rvCarritoTab.setLayoutManager(new LinearLayoutManager(this));
        rvCarritoTab.setAdapter(carritoAdapter);

        viewCarrito.findViewById(R.id.btnConfirmarTab).setOnClickListener(v -> confirmarPedidoTab());
    }

    private void actualizarCarritoTab() {
        if (carritoAdapter != null) carritoAdapter.notifyDataSetChanged();
        double sub = CarritoManager.getInstance().getTotal();
        double iva = sub * IVA;
        if (tvResumenSubtotal != null) tvResumenSubtotal.setText(String.format("$%.2f", sub));
        if (tvResumenIva      != null) tvResumenIva.setText(String.format("$%.2f", iva));
        if (tvResumenTotal    != null) tvResumenTotal.setText(String.format("$%.2f", sub + iva));
        // Contar unidades totales (no tipos de producto)
        int totalUnidades = 0;
        for (sv.edu.agroconecta.modelo.DetallePedido item : CarritoManager.getInstance().getItems()) {
            totalUnidades += item.getCantidad();
        }
        if (tvCarritoBadge != null) {
            if (totalUnidades > 0) {
                tvCarritoBadge.setText(String.valueOf(totalUnidades));
                tvCarritoBadge.setVisibility(View.VISIBLE);
            } else {
                tvCarritoBadge.setVisibility(View.GONE);
            }
        }
    }

    private void confirmarPedidoTab() {
        List<DetallePedido> items = CarritoManager.getInstance().getItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "🛒 El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        double subtotal = CarritoManager.getInstance().getTotal();
        double total    = subtotal * (1 + IVA);

        // Mostrar resumen antes de confirmar
        StringBuilder resumen = new StringBuilder();
        for (DetallePedido item : items) {
            resumen.append("• ").append(item.getNombre())
                   .append(" x").append(item.getCantidad())
                   .append(" = $").append(String.format("%.2f", item.getSubtotal()))
                   .append("\n");
        }
        resumen.append("\nTotal (con IVA 13%): $").append(String.format("%.2f", total));

        new AlertDialog.Builder(this)
            .setTitle("Confirmar pedido")
            .setMessage(resumen.toString())
            .setPositiveButton("✅ Confirmar", (d, w) -> enviarPedido(items, total))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void enviarPedido(List<DetallePedido> items, double total) {
        // Mostrar progreso
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Enviando pedido...");
        progress.setCancelable(false);
        progress.show();

        Pedido pedido = new Pedido();
        pedido.setDetalles(items);
        pedido.setTotal(total);
        pedido.setEstadoId(1);
        pedido.setUsuarioId(sessionManager.getUserId());

        new PedidoRepository().crearPedido(pedido).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> c, Response<Pedido> r) {
                progress.dismiss();
                if (r.isSuccessful()) {
                    // Pedido confirmado
                    CarritoManager.getInstance().limpiar();
                    actualizarCarritoTab();

                    // Notificar al vendedor por FCM (le llega a su celular, no al comprador)
                    java.util.Set<Integer> notificados = new java.util.HashSet<>();
                    for (sv.edu.agroconecta.modelo.DetallePedido item : items) {
                        int vid = item.getVendedorId();
                        if (vid > 0 && !notificados.contains(vid)) {
                            notificados.add(vid);
                            sv.edu.agroconecta.utils.FCMHelper.notificarUsuario(
                                String.valueOf(vid),
                                "🛒 ¡Nuevo pedido recibido!",
                                "Un comprador realizó un pedido. Revisa tu dashboard.",
                                "pedido"
                            );
                        }
                    }

                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Pedido confirmado!")
                        .setMessage("Tu pedido fue enviado exitosamente.\n\nEl vendedor recibirá una notificación.\nPuedes ver el estado en Mis Pedidos.")
                        .setPositiveButton("Ver mis pedidos", (dd, ww) ->
                            bottomNav.setSelectedItemId(R.id.nav_pedidos))
                        .setNegativeButton("OK", null)
                        .show();
                } else {
                    // Mostrar error real del backend
                    String errorMsg = "Error " + r.code();
                    try {
                        if (r.errorBody() != null)
                            errorMsg += ": " + r.errorBody().string();
                    } catch (Exception ignored) {}

                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error al confirmar")
                        .setMessage("No se pudo enviar el pedido.\n\n" + errorMsg + "\n\nVerifica tu conexion e intenta de nuevo.")
                        .setPositiveButton("OK", null)
                        .show();
                }
            }

            @Override
            public void onFailure(Call<Pedido> c, Throwable t) {
                progress.dismiss();
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Sin conexion")
                    .setMessage("No se pudo conectar al servidor.\n\nDetalle: " + t.getMessage() + "\n\nVerifica tu internet e intenta de nuevo.")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });
    }


    // ── LOGOUT ───────────────────────────────────────────
    private void confirmarLogout() {
        new AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Sí", (d, w) -> {
                sessionManager.logout();
                Intent i = new Intent(this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            })
            .setNegativeButton("No", null)
            .show();
    }

    // ── LIFECYCLE MapView ─────────────────────────────────
    @Override protected void onResume()  { super.onResume();  if(mapView!=null) mapView.onResume();  actualizarCarritoTab(); }
    @Override protected void onPause()   { super.onPause();   if(mapView!=null) mapView.onPause();   }
    @Override protected void onDestroy() { super.onDestroy(); if(mapView!=null) mapView.onDestroy(); }
    @Override protected void onStop()    { super.onStop();    if(mapView!=null) mapView.onStop();    }
    @Override public void onLowMemory() { super.onLowMemory(); if(mapView!=null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if(mapView!=null) mapView.onSaveInstanceState(out);
    }
}
