package sv.edu.agroconecta.ui;

import android.content.Intent;
import sv.edu.agroconecta.MainActivity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.modelo.DetallePedido;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;
import sv.edu.agroconecta.utils.CarritoManager;
import sv.edu.agroconecta.utils.SessionManager;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProductoDetalleActivity extends AppCompatActivity {

    private int    cantidad = 1;
    private int    stock;
    private double precio;
    private String nombre;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto_detalle);

        sessionManager = new SessionManager(this);

        // Vistas producto
        ImageView   ivImagen   = findViewById(R.id.ivImagenDetalle);
        TextView    tvNombre   = findViewById(R.id.tvNombreDetalle);
        TextView    tvCategoria= findViewById(R.id.tvCategoriaDetalle);
        TextView    tvPrecio   = findViewById(R.id.tvPrecioDetalle);
        TextView    tvStock    = findViewById(R.id.tvStockDetalle);
        TextView    tvDesc     = findViewById(R.id.tvDescripcionDetalle);
        TextView    tvCantidad = findViewById(R.id.tvCantidadDetalle);
        ImageButton btnMas     = findViewById(R.id.btnMasCantidad);
        ImageButton btnMenos   = findViewById(R.id.btnMenosCantidad);
        MaterialButton btnAgregar = (MaterialButton) findViewById(R.id.btnAgregarCarrito);

        // Header views
        ImageButton btnBack = findViewById(R.id.btnBackDetalle);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        // Removed elements from header (Avatar and Cart icon)
        /*
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        TextView tvCarritoBadge = findViewById(R.id.tvCarritoBadge);
        
        if (sessionManager.isLoggedIn() && tvAvatar != null) {
            String nombreU = sessionManager.getNombre();
            if (nombreU != null && !nombreU.isEmpty()) {
                tvAvatar.setText(String.valueOf(nombreU.charAt(0)).toUpperCase());
            }
            tvAvatar.setOnClickListener(this::showProfileMenu);
        }

        View btnCarrito = findViewById(R.id.btnHeaderCarrito);
        if (btnCarrito != null) {
            btnCarrito.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("nav_to", "carrito");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        actualizarBadge(tvCarritoBadge);
        */

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_catalogo); // Detalle es parte del catálogo
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            String target = null;
            if (id == R.id.nav_catalogo) target = "catalogo";
            else if (id == R.id.nav_pedidos) target = "pedidos";
            else if (id == R.id.nav_mapa) target = "mapa";
            else if (id == R.id.nav_carrito) target = "carrito";

            if (target != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("nav_to", target);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // Datos del Intent
        int    productoId = getIntent().getIntExtra("producto_id", 0);
        int    usuarioId  = getIntent().getIntExtra("usuario_id", -1);
        nombre            = getIntent().getStringExtra("nombre");
        String descripcion= getIntent().getStringExtra("descripcion");
        precio            = getIntent().getDoubleExtra("precio", 0);
        String imagen     = getIntent().getStringExtra("imagen");
        String categoria  = getIntent().getStringExtra("categoria");
        stock             = getIntent().getIntExtra("existencia", 0);
        final String tel  = getIntent().getStringExtra("telefono_vendedor");
        final String nomV = getIntent().getStringExtra("nombre_vendedor");
        final String metP = getIntent().getStringExtra("metodos_pago");
        final String dir  = getIntent().getStringExtra("direccion");
        final double lat  = getIntent().getDoubleExtra("latitud", 0);
        final double lon  = getIntent().getDoubleExtra("longitud", 0);
        final String fotoVendedor = getIntent().getStringExtra("foto_perfil_vendedor");

        // Producto
        if (tvNombre   != null) tvNombre.setText(nombre != null ? nombre : "");
        if (tvCategoria!= null) tvCategoria.setText(categoria != null ? categoria.toUpperCase() : "");
        if (tvPrecio   != null) tvPrecio.setText(String.format("$%.2f", precio));
        if (tvDesc     != null) tvDesc.setText(descripcion != null ? descripcion : "");
        if (tvCantidad != null) tvCantidad.setText("1");

        if (tvStock != null) {
            if (stock > 0) {
                tvStock.setText(stock + " disponibles");
            } else {
                tvStock.setText("Agotado");
                if (btnAgregar != null) btnAgregar.setEnabled(false);
            }
        }

        if (imagen != null && !imagen.isEmpty() && ivImagen != null) {
            Glide.with(this).load(imagen)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivImagen);
        }

        // Cantidad
        if (btnMas != null) btnMas.setOnClickListener(v -> {
            if (cantidad < stock) {
                cantidad++;
                if (tvCantidad != null) tvCantidad.setText(String.valueOf(cantidad));
            } else {
                Toast.makeText(this, "Stock máximo: " + stock, Toast.LENGTH_SHORT).show();
            }
        });

        if (btnMenos != null) btnMenos.setOnClickListener(v -> {
            if (cantidad > 1) {
                cantidad--;
                if (tvCantidad != null) tvCantidad.setText(String.valueOf(cantidad));
            }
        });

        // Agregar al carrito — incluimos vendedorId para poder notificarle al confirmar pedido
        if (btnAgregar != null) btnAgregar.setOnClickListener(v -> {
            CarritoManager.getInstance().agregarItem(
                    new DetallePedido(productoId, nombre, precio, cantidad, imagen, usuarioId));
            btnAgregar.setEnabled(false);
            Toast.makeText(this, "🛒 " + nombre + " ×" + cantidad + " al carrito",
                    Toast.LENGTH_SHORT).show();
            v.postDelayed(this::finish, 500);
        });

        // Back — puede existir o no en el nuevo XML
        //View btnBack = findViewById();
        //if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // ── Sección vendedor (IDs opcionales — solo si existen en el XML) ──
        ImageView    ivFotoV    = findViewById(R.id.ivFotoVendedorDetalle);
        TextView     tvAvatarV  = findViewById(R.id.tvAvatarVendedorDetalle);
        TextView     tvNombreV  = findViewById(R.id.tvNombreVendedorDetalle);
        TextView     tvTelV     = findViewById(R.id.tvTelefonoVendedorDetalle);
        TextView     tvMetV     = findViewById(R.id.tvMetodosPagoDetalle);
        LinearLayout rowDir     = findViewById(R.id.rowDireccion);
        TextView     tvDirV     = findViewById(R.id.tvDireccionVendedorDetalle);
        Button       btnWA      = findViewById(R.id.btnContactarVendedor);
        Button       btnMapaBtn = findViewById(R.id.btnVerUbicacion);

        // Si el XML tiene la sección vendedor la llenamos
        if (tvNombreV != null) {
            String nv = (nomV != null && !nomV.isEmpty()) ? nomV : "Vendedor";
            tvNombreV.setText(nv);
            if (tvAvatarV != null)
                tvAvatarV.setText(String.valueOf(nv.charAt(0)).toUpperCase());
            if (tvTelV != null)
                tvTelV.setText(tel != null && !tel.isEmpty() ? "📞 " + tel : "📞 No disponible");
            if (tvMetV != null)
                tvMetV.setText(metP != null && !metP.isEmpty() ? metP : "💵 Efectivo");
            if (rowDir != null && dir != null && !dir.isEmpty()) {
                rowDir.setVisibility(View.VISIBLE);
                if (tvDirV != null) tvDirV.setText(dir);
            }
            configurarBotonesVendedor(btnWA, btnMapaBtn, tel, lat, lon, nv);
            // Intentar cargar foto del vendedor guardada localmente
            // Si el intent trae la foto del vendedor directamente (desde backend), usarla
            if (fotoVendedor != null && !fotoVendedor.isEmpty() && ivFotoV != null) {
                Glide.with(this).load(fotoVendedor)
                        .transform(new CircleCrop())
                        .placeholder(android.R.color.transparent)
                        .into(ivFotoV);
                ivFotoV.setVisibility(android.view.View.VISIBLE);
                if (tvAvatarV != null) tvAvatarV.setVisibility(android.view.View.GONE);
            } else {
                cargarFotoVendedor(ivFotoV, tvAvatarV, usuarioId);
            }
        }

        // Buscar datos del vendedor por usuarioId si no vinieron en el intent
        if ((nomV == null || nomV.isEmpty()) && usuarioId > 0) {
            buscarVendedor(usuarioId, tvAvatarV, tvNombreV, tvTelV, btnWA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBadge(findViewById(R.id.tvCarritoBadge));
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_view_profile) {
                mostrarPerfil();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void mostrarPerfil() {
        String rol = sessionManager.getRol();
        Intent intent;
        if ("VENDEDOR".equalsIgnoreCase(rol)) {
            intent = new Intent(this, PerfilVendedorActivity.class);
        } else {
            intent = new Intent(this, PerfilAdminActivity.class);
        }
        startActivity(intent);
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
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void actualizarBadge(TextView badge) {
        if (badge == null) return;
        int count = 0;
        for (DetallePedido item : CarritoManager.getInstance().getItems()) {
            count += item.getCantidad();
        }
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void configurarBotonesVendedor(Button btnWA, Button btnMapa,
                                           String tel, double lat, double lon, String nv) {
        if (btnWA != null) {
            if (tel != null && !tel.isEmpty()) {
                btnWA.setEnabled(true); btnWA.setAlpha(1f);
                final String telFinal = tel;
                btnWA.setOnClickListener(v -> {
                    String num = telFinal.replaceAll("[^0-9]", "");
                    String msg = Uri.encode("Hola! Vi \"" + nombre + "\" en AgroConecta. ¿Disponible?");
                    try { startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/503" + num + "?text=" + msg)));
                    } catch (Exception e) {
                        Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show();
                    }
                });
            } else { btnWA.setEnabled(false); btnWA.setAlpha(0.4f); }
        }
        if (btnMapa != null) {
            if (lat != 0 || lon != 0) {
                btnMapa.setEnabled(true); btnMapa.setAlpha(1f);
                final double la = lat, lo = lon; final String nFin = nv;
                btnMapa.setOnClickListener(v -> {
                    // Intentar geo: primero, fallback a Google Maps en navegador
                    Uri geoUri = Uri.parse("geo:" + la + "," + lo + "?q=" + la + "," + lo +
                            "(" + Uri.encode(nFin) + ")");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        // Fallback: abrir en navegador
                        String url = "https://www.google.com/maps/search/?api=1&query=" + la + "," + lo;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                });
            } else { btnMapa.setEnabled(false); btnMapa.setAlpha(0.4f); }
        }
    }


    private void cargarFotoVendedor(ImageView ivFoto, TextView tvAvatar, int usuarioId) {
        if (ivFoto == null || usuarioId <= 0) return;
        // Buscar foto guardada en SharedPreferences con key foto_perfil_<userId>
        android.content.SharedPreferences prefs = getSharedPreferences("AgroConectaSession",
                android.content.Context.MODE_PRIVATE);
        String fotoUrl = prefs.getString("foto_perfil_" + usuarioId, null);
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this).load(fotoUrl)
                    .transform(new CircleCrop())
                    .placeholder(android.R.color.transparent)
                    .into(ivFoto);
            ivFoto.setVisibility(android.view.View.VISIBLE);
            if (tvAvatar != null) tvAvatar.setVisibility(android.view.View.GONE);
        }
    }

    private void buscarVendedor(int uid, TextView tvAvatar, TextView tvNombre,
                                TextView tvTel, Button btnWA) {
        ApiClient.getClient().create(UsuarioApi.class).getUsuarios()
                .enqueue(new Callback<List<Usuario>>() {
                    @Override
                    public void onResponse(Call<List<Usuario>> c, Response<List<Usuario>> r) {
                        if (!r.isSuccessful() || r.body() == null) return;
                        for (Usuario u : r.body()) {
                            if (u.getUsuarioId() == uid) {
                                final String nv  = u.getNombre() != null ? u.getNombre() : "Vendedor";
                                final String tel = u.getTelefono() != null ? u.getTelefono() : "";
                                runOnUiThread(() -> {
                                    if (tvNombre != null) tvNombre.setText(nv);
                                    if (tvAvatar != null)
                                        tvAvatar.setText(String.valueOf(nv.charAt(0)).toUpperCase());
                                    if (tvTel != null)
                                        tvTel.setText(!tel.isEmpty() ? "📞 " + tel : "📞 No disponible");
                                    // Cargar foto del vendedor si tiene
                                    String fotoU = u.getFotoPerfil();
                                    ImageView ivFotoDetalle = findViewById(R.id.ivFotoVendedorDetalle);
                                    if (fotoU != null && !fotoU.isEmpty() && ivFotoDetalle != null) {
                                        Glide.with(ProductoDetalleActivity.this).load(fotoU)
                                                .transform(new CircleCrop()).into(ivFotoDetalle);
                                        ivFotoDetalle.setVisibility(android.view.View.VISIBLE);
                                        if (tvAvatar != null) tvAvatar.setVisibility(android.view.View.GONE);
                                    }
                                    if (btnWA != null && !tel.isEmpty()) {
                                        btnWA.setEnabled(true); btnWA.setAlpha(1f);
                                        btnWA.setOnClickListener(v -> {
                                            String num = tel.replaceAll("[^0-9]", "");
                                            String msg = Uri.encode("Hola! Vi \"" + nombre + "\" en AgroConecta. ¿Disponible?");
                                            try { startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://wa.me/503" + num + "?text=" + msg)));
                                            } catch (Exception e) { }
                                        });
                                    }
                                });
                                break;
                            }
                        }
                    }
                    @Override public void onFailure(Call<List<Usuario>> c, Throwable t) {}
                });
    }
}
