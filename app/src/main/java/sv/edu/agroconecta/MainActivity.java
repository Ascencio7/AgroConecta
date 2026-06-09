package sv.edu.agroconecta;

import android.Manifest;
import sv.edu.agroconecta.ui.SoporteActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

import sv.edu.agroconecta.adapter.CarritoAdapter;
import sv.edu.agroconecta.adapter.ProductAdapter;
import sv.edu.agroconecta.adapter.PedidoClienteAdapter;
import sv.edu.agroconecta.modelo.DetallePedido;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.CategoriaApi;
import sv.edu.agroconecta.modelo.Categoria;
import sv.edu.agroconecta.repository.PedidoRepository;
import sv.edu.agroconecta.ui.LoginActivity;
import sv.edu.agroconecta.ui.ProductoDetalleActivity;
import sv.edu.agroconecta.ui.PerfilClienteActivity;
import sv.edu.agroconecta.utils.CarritoManager;
import sv.edu.agroconecta.utils.FCMHelper;
import sv.edu.agroconecta.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private SessionManager sessionManager;
    private String nombreUsuario;
    private int usuarioId;

    private BottomNavigationView bottomNav;
    private FrameLayout frameContenido;
    private TextView tvHeaderSubtitle, tvAvatar, tvCarritoBadge, tvWelcome;
    private android.widget.ImageView ivAvatarFoto;

    private View viewCatalogo, viewPedidos, viewMapa, viewCarrito;

    // ── Catálogo ──────────────────────────────────
    private RecyclerView rvCatalogo;
    private ProductAdapter productAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private String filtroCategoria = "Todos";
    private SearchView searchViewCatalogo;

    // ── Carrito ───────────────────────────────────
    private RecyclerView rvCarritoTab;
    private CarritoAdapter carritoAdapter;
    private TextView tvResumenSubtotal, tvResumenIva, tvResumenTotal;
    private static final double IVA = 0.13;

    // ── Mapa ──────────────────────────────────────
    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERM = 1001;
    private boolean mapaInicializado = false;

    private static final double[][] FINCAS = {
        {13.6929, -89.2182},
        {13.7034, -89.2245}
    };
    private static final String[] NOMBRES_FINCAS  = { "Finca El Roble", "Finca La Esperanza" };
    private static final String[] PRODUCTOS_FINCAS = { "Maíz, Frijol", "Café, Caña" };
    private static final String[] EMOJIS_FINCAS    = { "🌽", "☕" };

    // ── Pedidos ───────────────────────────────────
    private RecyclerView rvPedidos;

    // ── Chat flotante ─────────────────────────────
    private ChatManager chatManager;

    interface ProductoApi {
        @GET("productos")
        Call<List<Product>> getProductos();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager  = new SessionManager(this);
        nombreUsuario   = getIntent().getStringExtra("nombre");
        usuarioId       = getIntent().getIntExtra("usuario_id", -1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvAvatar         = findViewById(R.id.tvAvatar);
        ivAvatarFoto     = findViewById(R.id.ivAvatarFoto);
        tvCarritoBadge   = findViewById(R.id.tvCarritoBadge);
        tvWelcome        = findViewById(R.id.tvWelcome);
        frameContenido   = findViewById(R.id.frameContenido);
        bottomNav        = findViewById(R.id.bottomNav);

        // Saludo y avatar
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombreUsuario.charAt(0)).toUpperCase());
            tvWelcome.setText("¡Bienvenido!");
        }

        String fotoPerfil = sessionManager.getFotoPerfil();
        if (fotoPerfil != null && !fotoPerfil.isEmpty() && ivAvatarFoto != null) {
            Glide.with(this).load(fotoPerfil).transform(new CircleCrop()).into(ivAvatarFoto);
            ivAvatarFoto.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
        }

        // Avatar click → menú perfil
        View avatarContainer = ivAvatarFoto.getParent() instanceof View
                ? (View) ivAvatarFoto.getParent() : ivAvatarFoto;
        ivAvatarFoto.setOnClickListener(v -> showProfileMenu(v));
        tvAvatar.setOnClickListener(v -> showProfileMenu(v));

        // Icono carrito del header → ir a tab carrito
        View btnHeaderCarrito = findViewById(R.id.btnHeaderCarrito);
        if (btnHeaderCarrito != null) {
            btnHeaderCarrito.setOnClickListener(v -> {
                bottomNav.setSelectedItemId(R.id.nav_carrito);
            });
        }

        // Inflar fragmentos
        LayoutInflater inf = LayoutInflater.from(this);
        viewCatalogo = inf.inflate(R.layout.fragment_catalogo, frameContenido, false);
        viewPedidos  = inf.inflate(R.layout.fragment_pedidos,  frameContenido, false);
        viewMapa     = inf.inflate(R.layout.fragment_mapa,     frameContenido, false);
        viewCarrito  = inf.inflate(R.layout.fragment_carrito,  frameContenido, false);

        frameContenido.addView(viewCatalogo);
        frameContenido.addView(viewPedidos);
        frameContenido.addView(viewMapa);
        frameContenido.addView(viewCarrito);

        mapView = viewMapa.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        initCatalogo();
        initPedidos();
        initMapa();
        initCarrito();

        // Chat flotante
        CoordinatorLayout root = findViewById(R.id.coordinatorMain);
        if (root != null) {
            chatManager = new ChatManager(this, root);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_catalogo) {
                mostrarPestana(viewCatalogo, "Catálogo de productos");
            } else if (id == R.id.nav_pedidos) {
                mostrarPestana(viewPedidos, "Mis Pedidos");
                cargarPedidos();
            } else if (id == R.id.nav_mapa) {
                mostrarPestana(viewMapa, "Mapa");
                mapView.onResume();
                if (mMap == null) mapView.getMapAsync(this);
                else mostrarFincasEnLista();
            } else if (id == R.id.nav_carrito) {
                mostrarPestana(viewCarrito, "Carrito");
                actualizarCarritoTab();
            }
            return true;
        });

        // Mostrar catálogo por defecto
        mostrarPestana(viewCatalogo, "Catálogo de productos");
    }

    private void mostrarPestana(View target, String subtitulo) {
        viewCatalogo.setVisibility(View.GONE);
        viewPedidos.setVisibility(View.GONE);
        viewMapa.setVisibility(View.GONE);
        viewCarrito.setVisibility(View.GONE);
        target.setVisibility(View.VISIBLE);
        tvHeaderSubtitle.setText(subtitulo);
    }

    // ─────────────────────────── CATÁLOGO ────────────────────────────────────
    private android.widget.LinearLayout llCategorias;
    private android.widget.TextView tvContador;

    private void initCatalogo() {
        rvCatalogo         = viewCatalogo.findViewById(R.id.rvCatalogo);
        searchViewCatalogo = viewCatalogo.findViewById(R.id.searchViewCatalogo);
        llCategorias       = viewCatalogo.findViewById(R.id.llCategorias);
        tvContador         = viewCatalogo.findViewById(R.id.tvContador);

        productAdapter = new ProductAdapter(this, filteredProducts,
            product -> abrirDetalle(product),
            (product, rating) -> {}
        );
        rvCatalogo.setLayoutManager(new LinearLayoutManager(this));
        rvCatalogo.setAdapter(productAdapter);

        if (searchViewCatalogo != null) {
            searchViewCatalogo.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) { return false; }
                @Override public boolean onQueryTextChange(String q) {
                    filtrarPorTexto(q); return true;
                }
            });
        }

        cargarProductos();
    }

    private void cargarProductos() {
        ApiClient.getClient().create(ProductoApi.class).getProductos()
            .enqueue(new Callback<List<Product>>() {
                @Override
                public void onResponse(Call<List<Product>> call, Response<List<Product>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        allProducts.clear();
                        allProducts.addAll(r.body());
                        filteredProducts.clear();
                        filteredProducts.addAll(allProducts);
                        productAdapter.notifyDataSetChanged();
                        actualizarContador();
                        generarChipsCategorias();
                    }
                }
                @Override public void onFailure(Call<List<Product>> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Error cargando productos", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void generarChipsCategorias() {
        if (llCategorias == null) return;
        llCategorias.removeAllViews();

        // Recopilar categorías únicas de los productos
        java.util.LinkedHashSet<String> cats = new java.util.LinkedHashSet<>();
        cats.add("Todos");
        for (Product p : allProducts) {
            String cat = p.getCategoria();
            if (cat != null && !cat.isEmpty()) cats.add(cat);
        }

        for (String cat : cats) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals(filtroCategoria));
            chip.setChipBackgroundColorResource(R.color.blanco);
            chip.setTextColor(getResources().getColor(R.color.texto_primario, null));
            chip.setChipStrokeColorResource(R.color.verde_primario);
            chip.setChipStrokeWidth(2f);
            chip.setEnsureMinTouchTargetSize(false);

            android.view.ViewGroup.MarginLayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            ((android.widget.LinearLayout.LayoutParams) lp).setMargins(0, 0, 8, 0);
            chip.setLayoutParams(lp);

            final String catFinal = cat;
            chip.setOnClickListener(v -> {
                filtroCategoria = catFinal;
                // Desmarcar todos y marcar el seleccionado
                for (int i = 0; i < llCategorias.getChildCount(); i++) {
                    com.google.android.material.chip.Chip c =
                        (com.google.android.material.chip.Chip) llCategorias.getChildAt(i);
                    boolean sel = c.getText().toString().equals(catFinal);
                    c.setChecked(sel);
                    c.setChipBackgroundColorResource(sel ? R.color.verde_primario : R.color.blanco);
                    c.setTextColor(getResources().getColor(sel ? R.color.blanco : R.color.texto_primario, null));
                }
                aplicarFiltros();
            });

            // Chip "Todos" empieza seleccionado visualmente
            if (cat.equals(filtroCategoria)) {
                chip.setChipBackgroundColorResource(R.color.verde_primario);
                chip.setTextColor(getResources().getColor(R.color.blanco, null));
            }

            llCategorias.addView(chip);
        }
    }

    private void aplicarFiltros() {
        String query = searchViewCatalogo != null
            ? searchViewCatalogo.getQuery().toString().trim()
            : "";
        filteredProducts.clear();
        for (Product p : allProducts) {
            boolean matchCat  = filtroCategoria.equals("Todos") ||
                (p.getCategoria() != null && p.getCategoria().equalsIgnoreCase(filtroCategoria));
            boolean matchText = query.isEmpty() ||
                (p.getName() != null && p.getName().toLowerCase().contains(query.toLowerCase()));
            if (matchCat && matchText) filteredProducts.add(p);
        }
        productAdapter.notifyDataSetChanged();
        actualizarContador();

        // Mostrar/ocultar "sin resultados"
        android.widget.TextView tvNo = viewCatalogo.findViewById(R.id.tvNoResultados);
        if (tvNo != null) tvNo.setVisibility(filteredProducts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void filtrarPorTexto(String query) {
        aplicarFiltros();
    }

    private void filtrarProductos(String query) {
        aplicarFiltros();
    }

    private void actualizarContador() {
        if (tvContador != null)
            tvContador.setText(filteredProducts.size() + " productos");
    }

    private void abrirDetalle(Product product) {
        Intent intent = new Intent(this, ProductoDetalleActivity.class);
        intent.putExtra("producto_id",          product.getProductoId());
        intent.putExtra("nombre",               product.getNombre());
        intent.putExtra("descripcion",          product.getDescripcion());
        intent.putExtra("precio",               product.getPrecio());
        intent.putExtra("imagen",               product.getImagen());
        intent.putExtra("categoria",            product.getCategoria());
        intent.putExtra("existencia",           product.getExistencia());
        // Datos del vendedor — necesarios para mostrar en ProductoDetalleActivity
        intent.putExtra("usuario_id",           product.getUsuarioId() != null ? product.getUsuarioId() : -1);
        intent.putExtra("nombre_vendedor",      product.getNombreVendedor());
        intent.putExtra("telefono_vendedor",    product.getTelefonoVendedor());
        intent.putExtra("foto_perfil_vendedor", product.getFotoPerfilVendedor());
        intent.putExtra("metodos_pago",         product.getMetodosPagoTexto());
        intent.putExtra("direccion",            product.getDireccion());
        intent.putExtra("latitud",              product.getLatitud()  != null ? product.getLatitud()  : 0.0);
        intent.putExtra("longitud",             product.getLongitud() != null ? product.getLongitud() : 0.0);
        startActivity(intent);
    }

    // ─────────────────────────── PEDIDOS ─────────────────────────────────────
    private void initPedidos() {
        rvPedidos = viewPedidos.findViewById(R.id.rvPedidos);
        rvPedidos.setLayoutManager(new LinearLayoutManager(this));
    }

    private void cargarPedidos() {
        int userId = sessionManager.getUserId();
        ApiClient.getClient()
            .create(sv.edu.agroconecta.network.PedidoApi.class)
            .getPedidosPorUsuario(userId)
            .enqueue(new Callback<List<Pedido>>() {
                @Override
                public void onResponse(Call<List<Pedido>> call, Response<List<Pedido>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        rvPedidos.setAdapter(new PedidoClienteAdapter(r.body(), MainActivity.this));
                    } else {
                        Toast.makeText(MainActivity.this, "Sin pedidos por el momento", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<List<Pedido>> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Error al cargar pedidos", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // ─────────────────────────── MAPA ────────────────────────────────────────
    private void initMapa() {
        FloatingActionButton fabMiUbicacion = viewMapa.findViewById(R.id.fabMiUbicacion);
        if (fabMiUbicacion != null) {
            fabMiUbicacion.setOnClickListener(v -> obtenerUbicacion());
        }
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERM);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mMap != null) {
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14));
                mMap.addMarker(new MarkerOptions().position(pos).title("Mi ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                Toast.makeText(this, "No se pudo obtener tu ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Centrar en El Salvador
        LatLng salvador = new LatLng(13.6929, -89.2182);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(salvador, 12));

        // Añadir marcadores de fincas
        for (int i = 0; i < FINCAS.length; i++) {
            LatLng pos = new LatLng(FINCAS[i][0], FINCAS[i][1]);
            mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(NOMBRES_FINCAS[i])
                .snippet(EMOJIS_FINCAS[i] + " " + PRODUCTOS_FINCAS[i])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        mostrarFincasEnLista();

        // Activar ubicación si tiene permiso
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void mostrarFincasEnLista() {
        LinearLayout llFincas = viewMapa.findViewById(R.id.llFincas);
        if (llFincas == null) return;
        llFincas.removeAllViews();

        for (int i = 0; i < NOMBRES_FINCAS.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText(EMOJIS_FINCAS[i] + "  " + NOMBRES_FINCAS[i] + "  ·  " + PRODUCTOS_FINCAS[i]);
            tv.setTextSize(14);
            tv.setPadding(0, 8, 0, 8);
            tv.setOnClickListener(v -> {
                if (mMap != null) {
                    LatLng pos = new LatLng(FINCAS[idx][0], FINCAS[idx][1]);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                }
            });
            llFincas.addView(tv);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERM && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        }
    }

    // ─────────────────────────── CARRITO ─────────────────────────────────────
    private void initCarrito() {
        rvCarritoTab    = viewCarrito.findViewById(R.id.rvCarritoTab);
        tvResumenSubtotal = viewCarrito.findViewById(R.id.tvResumenSubtotal);
        tvResumenIva      = viewCarrito.findViewById(R.id.tvResumenIva);
        tvResumenTotal    = viewCarrito.findViewById(R.id.tvResumenTotal);

        List<DetallePedido> items = CarritoManager.getInstance().getItems();
        carritoAdapter = new CarritoAdapter(this, items, new CarritoAdapter.OnItemChangedListener() {
            @Override public void onItemRemoved(int pos) { actualizarCarritoTab(); }
            @Override public void onCantidadChanged()    { actualizarCarritoTab(); }
        });
        rvCarritoTab.setLayoutManager(new LinearLayoutManager(this));
        rvCarritoTab.setAdapter(carritoAdapter);

        MaterialButton btnConfirmar = viewCarrito.findViewById(R.id.btnConfirmarTab);
        if (btnConfirmar != null) {
            btnConfirmar.setOnClickListener(v -> confirmarPedidoTab());
        }

        actualizarCarritoTab();
    }

    private void actualizarCarritoTab() {
        List<DetallePedido> items = CarritoManager.getInstance().getItems();

        double subtotal = CarritoManager.getInstance().getTotal();
        double iva      = subtotal * IVA;
        double total    = subtotal + iva;

        if (tvResumenSubtotal != null) tvResumenSubtotal.setText(String.format("$%.2f", subtotal));
        if (tvResumenIva      != null) tvResumenIva.setText(String.format("$%.2f", iva));
        if (tvResumenTotal    != null) tvResumenTotal.setText(String.format("$%.2f", total));

        // Badge del carrito en el header
        int count = items.size();
        if (tvCarritoBadge != null) {
            if (count > 0) {
                tvCarritoBadge.setText(String.valueOf(count));
                tvCarritoBadge.setVisibility(View.VISIBLE);
            } else {
                tvCarritoBadge.setVisibility(View.GONE);
            }
        }

        // Habilitar/deshabilitar botón confirmar
        MaterialButton btnConfirmar = viewCarrito.findViewById(R.id.btnConfirmarTab);
        if (btnConfirmar != null) btnConfirmar.setEnabled(!items.isEmpty());

        if (carritoAdapter != null) carritoAdapter.notifyDataSetChanged();
    }

    private void confirmarPedidoTab() {
        List<DetallePedido> items = CarritoManager.getInstance().getItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "🛒 El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = CarritoManager.getInstance().getTotal();
        double total    = subtotal * (1 + IVA);

        new AlertDialog.Builder(this)
            .setTitle("Confirmar pedido")
            .setMessage(String.format("¿Confirmar pedido por $%.2f (incluye IVA 13%%)?", total))
            .setPositiveButton("✅ Confirmar", (d, w) -> enviarPedido(new ArrayList<>(items), total))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void enviarPedido(List<DetallePedido> items, double total) {
        MaterialButton btnConfirmar = viewCarrito.findViewById(R.id.btnConfirmarTab);
        if (btnConfirmar != null) { btnConfirmar.setEnabled(false); btnConfirmar.setText("Enviando…"); }

        Pedido pedido = new Pedido();
        pedido.setDetalles(items);
        pedido.setTotal(total);
        pedido.setEstadoId(1);
        pedido.setUsuarioId(sessionManager.getUserId());

        new PedidoRepository().crearPedido(pedido).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                if (response.isSuccessful()) {
                    // Notificar vendedores únicos
                    Set<Integer> notificados = new HashSet<>();
                    for (DetallePedido item : items) {
                        int vId = item.getVendedorId();
                        if (vId > 0 && !notificados.contains(vId)) {
                            notificados.add(vId);
                            FCMHelper.notificarUsuario(
                                String.valueOf(vId),
                                "🛒 ¡Nuevo pedido recibido!",
                                "Tienes un nuevo pedido. Revisa tu dashboard.",
                                "pedido"
                            );
                        }
                    }

                    CarritoManager.getInstance().limpiar();
                    actualizarCarritoTab();
                    Toast.makeText(MainActivity.this,
                        "🎉 ¡Pedido confirmado! Total: " + String.format("$%.2f", total),
                        Toast.LENGTH_LONG).show();

                    if (btnConfirmar != null) {
                        btnConfirmar.setEnabled(false);
                        btnConfirmar.setText("+ CONFIRMAR PEDIDO");
                    }
                } else {
                    if (btnConfirmar != null) { btnConfirmar.setEnabled(true); btnConfirmar.setText("+ CONFIRMAR PEDIDO"); }
                    Toast.makeText(MainActivity.this, "Error al confirmar. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Pedido> call, Throwable t) {
                if (btnConfirmar != null) { btnConfirmar.setEnabled(true); btnConfirmar.setText("+ CONFIRMAR PEDIDO"); }
                Toast.makeText(MainActivity.this, "Sin conexión. Verifica tu internet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────── PERFIL ──────────────────────────────────────
    private void showProfileMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        // Construir el menú programáticamente para evitar problemas con inflate
        popup.getMenu().add(0, 1, 0, "👤 Mi Perfil");
        popup.getMenu().add(0, 2, 1, "🛠️ Soporte técnico");
        popup.getMenu().add(0, 3, 2, "🚪 Cerrar Sesión");
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { mostrarPerfil(); return true; }
            if (id == 2) { startActivity(new Intent(MainActivity.this, sv.edu.agroconecta.ui.SoporteActivity.class)); return true; }
            if (id == 3) { cerrarSesion(); return true; }
            return false;
        });
        popup.show();
    }

    private void mostrarPerfil() {
        startActivity(new Intent(this, PerfilClienteActivity.class));
    }

    private void cerrarSesion() {
        new AlertDialog.Builder(MainActivity.this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas salir?")
            .setPositiveButton("Sí, salir", (d, w) -> {
                sessionManager.logout();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", null)
            .show();
    }

    // ─────────────────────────── LIFECYCLE del MAPA ───────────────────────────
    @Override protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        // Actualizar badge del carrito al volver de ProductoDetalleActivity
        actualizarCarritoTab();
    }
    @Override protected void onPause()   { super.onPause();   if (mapView != null) mapView.onPause();   }
    @Override protected void onStop()    { super.onStop();    if (mapView != null) mapView.onStop();    }
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (mapView != null) mapView.onSaveInstanceState(out);
    }
}
