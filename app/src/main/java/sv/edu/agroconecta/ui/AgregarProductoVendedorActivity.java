package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.google.android.gms.location.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Categoria;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.CategoriaApi;
import sv.edu.agroconecta.network.ProductApi;
import sv.edu.agroconecta.service.NotificacionNuevoProductoService;
import sv.edu.agroconecta.utils.SessionManager;
import sv.edu.agroconecta.utils.SupabaseImageHelper;

import android.content.res.ColorStateList;
import android.graphics.Color;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AlertDialog;

public class AgregarProductoVendedorActivity extends AppCompatActivity {

    private static final int REQ_CAMERA   = 2001;
    private static final int REQ_GALERIA  = 2002;
    private static final int REQ_LOCATION = 2003;
    private static final int REQ_CAM_PERM = 2004;

    private EditText etNombre, etDescripcion, etPrecio, etExistencia,
            etDireccion, etLatitud, etLongitud, etTelefono;
    private CheckBox cbEfectivo, cbTransferencia, cbTarjeta;
    private Spinner  spCategoria;
    private TextView tvUbicacionStatus;
    private Button   btnTomarFoto, btnElegirGaleria, btnUsarUbicacion, btnPublicar;
    private ImageView ivFotoProducto;
    private TextView tvAvatar;

    private List<Categoria> categorias = new ArrayList<>();
    private FusedLocationProviderClient locationClient;
    private double latSeleccionada = 0, lonSeleccionada = 0;
    private int    productoId  = -1;
    private String imagenUrl   = null;   // URL pública ya subida a Supabase
    private Uri    fotoUri     = null;
    private boolean subiendoFoto = false;

    private SessionManager sessionManager;
    private ProductApi     productApi;
    private CategoriaApi   categoriaApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_producto_vendedor);

        sessionManager = new SessionManager(this);
        productApi     = ApiClient.getClient().create(ProductApi.class);
        categoriaApi   = ApiClient.getClient().create(CategoriaApi.class);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        productoId     = getIntent().getIntExtra("producto_id", -1);

        bindViews();
        cargarCategorias();

        if (productoId != -1) {
            ((TextView) findViewById(R.id.tvTituloFormVendedor)).setText("Editar producto");
            cargarDatosProducto();
        }

        // Header Avatar / Logout logic
        if (sessionManager.isLoggedIn()) {
            String nom = sessionManager.getNombre();
            if (nom != null && !nom.isEmpty()) {
                tvAvatar.setText(String.valueOf(nom.charAt(0)).toUpperCase());
            }
        }
        tvAvatar.setOnClickListener(this::showProfileMenu);

        btnUsarUbicacion.setOnClickListener(v -> obtenerUbicacion());
        btnPublicar.setOnClickListener(v -> publicarProducto());

        btnTomarFoto.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
            } else {
                abrirCamara();
            }
        });

        btnElegirGaleria.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_GALERIA);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavVendedorAdd);

        bottomNav.setSelectedItemId(R.id.nav_seller_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_seller_products) {
                irADashboard("productos");
                return true;
            } else if (id == R.id.nav_seller_orders) {
                irADashboard("pedidos");
                return true;
            }
            return id == R.id.nav_seller_add;
        });
    }

    private void irADashboard(String tab) {
        Intent i = new Intent(this, VendedorDashboardActivity.class);
        i.putExtra("nav_to", tab);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void actualizarBadge(TextView badge) {
        if (badge == null) return;
        int total = 0;
        // Si el vendedor tuviera carrito, pero usualmente no. 
        // Si quieres mostrar el badge en el icono de carrito del header:
        // Pero el vendedor no tiene carrito en su header segun la imagen.
        // Solo tiene el avatar.
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
        Intent intent = new Intent(this, PerfilAdminActivity.class);
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
                    finish();
                })
                .setNegativeButton("No", null).show();
    }

    private void bindViews() {
        etNombre         = findViewById(R.id.etVNombre);
        etDescripcion    = findViewById(R.id.etVDescripcion);
        etPrecio         = findViewById(R.id.etVPrecio);
        etExistencia     = findViewById(R.id.etVExistencia);
        etDireccion      = findViewById(R.id.etVDireccion);
        etLatitud        = findViewById(R.id.etVLatitud);
        etLongitud       = findViewById(R.id.etVLongitud);
        etTelefono       = findViewById(R.id.etVTelefono);
        cbEfectivo       = findViewById(R.id.cbEfectivo);
        cbTransferencia  = findViewById(R.id.cbTransferencia);
        cbTarjeta        = findViewById(R.id.cbTarjeta);
        spCategoria      = findViewById(R.id.spVCategoria);
        tvUbicacionStatus= findViewById(R.id.tvUbicacionStatus);
        btnUsarUbicacion = findViewById(R.id.btnUsarMiUbicacion);
        btnPublicar      = findViewById(R.id.btnPublicarProducto);
        btnTomarFoto     = findViewById(R.id.btnTomarFoto);
        btnElegirGaleria = findViewById(R.id.btnElegirGaleria);
        ivFotoProducto   = findViewById(R.id.ivFotoProducto);
        tvAvatar         = findViewById(R.id.tvAvatarVendedorAdd);
    }

    private void abrirCamara() {
        // No usar resolveActivity() - falla en Android 11+ por Package Visibility
        // Verificar directamente si el dispositivo tiene hardware de cámara
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Este dispositivo no tiene cámara", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File fotoFile = crearArchivoFoto();
            fotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", fotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo de foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("FOTO_" + timestamp, ".jpg", dir);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == REQ_CAMERA && fotoUri != null) {
            ivFotoProducto.setImageURI(fotoUri);
            procesarYSubirImagen(fotoUri);

        } else if (req == REQ_GALERIA && data != null && data.getData() != null) {
            Uri uri = data.getData();
            fotoUri = uri;
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ivFotoProducto.setImageBitmap(bmp);
            } catch (IOException e) {
                ivFotoProducto.setImageURI(uri);
            }
            procesarYSubirImagen(uri);
        }
    }

    // ── Leer bytes del Uri y subir a Supabase Storage ─────────────────────
    private void procesarYSubirImagen(Uri imageUri) {
        subiendoFoto = true;
        btnPublicar.setEnabled(false);
        btnPublicar.setText("Subiendo imagen...");
        Toast.makeText(this, "⬆️ Subiendo foto...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                byte[] bytes = leerBytes(is);

                SupabaseImageHelper.subirImagen(bytes, new SupabaseImageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        imagenUrl    = publicUrl;
                        subiendoFoto = false;
                        runOnUiThread(() -> {
                            btnPublicar.setEnabled(true);
                            btnPublicar.setText("🌿  Publicar producto");
                            Toast.makeText(AgregarProductoVendedorActivity.this,
                                    "✅ Foto lista", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        subiendoFoto = false;
                        runOnUiThread(() -> {
                            btnPublicar.setEnabled(true);
                            btnPublicar.setText("🌿  Publicar producto");
                            Toast.makeText(AgregarProductoVendedorActivity.this,
                                    "Error al subir foto: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } catch (Exception e) {
                subiendoFoto = false;
                runOnUiThread(() -> {
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("🌿  Publicar producto");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private byte[] leerBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[4096];
        int n;
        while ((n = is.read(temp)) != -1) buffer.write(temp, 0, n);
        return buffer.toByteArray();
    }
    // ─────────────────────────────────────────────────────────────────────

    private void cargarCategorias() {
        categoriaApi.getCategorias().enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> c, Response<List<Categoria>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    categorias = r.body();
                    ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(
                            AgregarProductoVendedorActivity.this,
                            android.R.layout.simple_spinner_item, categorias);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spCategoria.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Categoria>> c, Throwable t) {}
        });
    }

    private void cargarDatosProducto() {
        productApi.getProductoPorId(productoId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> c, Response<Product> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Product p = r.body();
                    etNombre.setText(p.getNombre());
                    etDescripcion.setText(p.getDescripcion());
                    etPrecio.setText(String.valueOf(p.getPrecio()));
                    etExistencia.setText(String.valueOf(p.getExistencia()));
                    if (p.getTelefonoVendedor() != null) etTelefono.setText(p.getTelefonoVendedor());
                    if (p.getDireccion() != null) etDireccion.setText(p.getDireccion());
                    if (p.getLatitud() != null) {
                        latSeleccionada = p.getLatitud();
                        lonSeleccionada = p.getLongitud() != null ? p.getLongitud() : 0;
                        etLatitud.setText(String.format("%.6f", latSeleccionada));
                        etLongitud.setText(String.format("%.6f", lonSeleccionada));
                        tvUbicacionStatus.setText("✅ Ubicación guardada");
                    }
                    cbEfectivo.setChecked(Boolean.TRUE.equals(p.getAceptaEfectivo()));
                    cbTransferencia.setChecked(Boolean.TRUE.equals(p.getAceptaTransferencia()));
                    cbTarjeta.setChecked(Boolean.TRUE.equals(p.getAceptaTarjeta()));

                    // Cargar imagen existente desde productos.imagen
                    if (p.getImagen() != null && p.getImagen().startsWith("https://")) {
                        imagenUrl = p.getImagen();
                        com.bumptech.glide.Glide.with(AgregarProductoVendedorActivity.this)
                                .load(imagenUrl).into(ivFotoProducto);
                    }
                }
            }
            @Override public void onFailure(Call<Product> c, Throwable t) {}
        });
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        tvUbicacionStatus.setText("📡 Obteniendo ubicación...");
        locationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                latSeleccionada = loc.getLatitude();
                lonSeleccionada = loc.getLongitude();
                etLatitud.setText(String.format("%.6f", latSeleccionada));
                etLongitud.setText(String.format("%.6f", lonSeleccionada));
                tvUbicacionStatus.setText("✅ Ubicación obtenida correctamente");
            } else {
                tvUbicacionStatus.setText("⚠️ Activa el GPS e intenta de nuevo");
            }
        });
    }

    private void publicarProducto() {
        if (subiendoFoto) {
            Toast.makeText(this, "⏳ Espera a que termine de subir la foto", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre  = etNombre.getText().toString().trim();
        String precioS = etPrecio.getText().toString().trim();
        String existS  = etExistencia.getText().toString().trim();

        if (nombre.isEmpty() || precioS.isEmpty() || existS.isEmpty()) {
            Toast.makeText(this, "Nombre, precio y existencia son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Espera a que carguen las categorías", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublicar.setEnabled(false);
        btnPublicar.setText("Publicando...");

        Product p = new Product();
        p.setNombre(nombre);
        p.setDescripcion(etDescripcion.getText().toString().trim());
        p.setPrecio(Double.parseDouble(precioS));
        p.setExistencia(Integer.parseInt(existS));
        p.setEstado(true);
        p.setUsuarioId(sessionManager.getUserId());
        p.setCategoriaId(categorias.get(spCategoria.getSelectedItemPosition()).getCategoriaId());
        p.setTelefonoVendedor(etTelefono.getText().toString().trim());
        p.setDireccion(etDireccion.getText().toString().trim());
        if (latSeleccionada != 0) {
            p.setLatitud(latSeleccionada);
            p.setLongitud(lonSeleccionada);
        }
        p.setAceptaEfectivo(cbEfectivo.isChecked());
        p.setAceptaTransferencia(cbTransferencia.isChecked());
        p.setAceptaTarjeta(cbTarjeta.isChecked());
        p.setNombreVendedor(sessionManager.getNombre());
        // Imagen subida a Supabase Storage — URL pública
        if (imagenUrl != null && imagenUrl.startsWith("https://")) p.setImagen(imagenUrl);

        Callback<Product> cb = new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> c, Response<Product> r) {
                if (r.isSuccessful() && r.body() != null) {
                    int nuevoProductoId = r.body().getProductoId();

                    if (productoId == -1)
                        NotificacionNuevoProductoService.enviarNotificacion(
                                AgregarProductoVendedorActivity.this, nombre);

                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("🌿  Publicar producto");
                    Toast.makeText(AgregarProductoVendedorActivity.this,
                            productoId == -1 ? "✅ Producto publicado" : "✅ Producto actualizado",
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("🌿  Publicar producto");
                    Toast.makeText(AgregarProductoVendedorActivity.this,
                            "Error al publicar. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Product> c, Throwable t) {
                btnPublicar.setEnabled(true);
                btnPublicar.setText("🌿  Publicar producto");
                Toast.makeText(AgregarProductoVendedorActivity.this,
                        "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        };

        if (productoId == -1) productApi.crearProducto(p).enqueue(cb);
        else                  productApi.actualizarProducto(productoId, p).enqueue(cb);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            if (req == REQ_LOCATION) obtenerUbicacion();
            if (req == REQ_CAM_PERM) abrirCamara();
        }
    }
}
