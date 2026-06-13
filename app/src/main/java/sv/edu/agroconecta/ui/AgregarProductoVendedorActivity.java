package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import com.google.android.material.checkbox.MaterialCheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
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
import okhttp3.ResponseBody;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AlertDialog;

public class AgregarProductoVendedorActivity extends AppCompatActivity {

    private static final int REQ_CAMERA   = 2001;
    private static final int REQ_GALERIA  = 2002;
    private static final int REQ_LOCATION = 2003;
    private static final int REQ_CAM_PERM = 2004;

    private EditText etNombre, etDescripcion, etPrecio, etExistencia, etDireccion, etLatitud, etLongitud, etTelefono;
    private MaterialCheckBox cbEfectivo, cbTransferencia, cbTarjeta;
    private Spinner spCategoria;
    private TextView tvUbicacionStatus, tvAvatar;
    private Button btnTomarFoto, btnElegirGaleria, btnUsarMiUbicacion;
    private com.google.android.material.button.MaterialButton btnPublicar, btnCancelar;
    private ImageView ivFotoProducto, ivAvatarFoto;

    private List<Categoria> categorias = new ArrayList<>();
    private FusedLocationProviderClient locationClient;
    private double latSeleccionada = 0, lonSeleccionada = 0;
    private int productoId = -1;
    private String imagenUrl = null;
    private Uri fotoUri = null;
    private boolean subiendoFoto = false;

    private SessionManager sessionManager;
    private ProductApi productApi;
    private CategoriaApi categoriaApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_producto_vendedor);

        sessionManager = new SessionManager(this);
        productApi    = ApiClient.getClient().create(ProductApi.class);
        categoriaApi  = ApiClient.getClient().create(CategoriaApi.class);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        productoId    = getIntent().getIntExtra("producto_id", -1);

        bindViews();
        setupPhoneFormatting();
        cargarCategorias();

        if (productoId != -1) {
            ((TextView) findViewById(R.id.tvTituloFormVendedor)).setText("Editar producto");
            btnPublicar.setText("ACTUALIZAR");
            cargarDatosProducto();
        } else {
            ((TextView) findViewById(R.id.tvTituloFormVendedor)).setText("Agregar Producto");
            btnPublicar.setText("PUBLICAR");
            cbEfectivo.setChecked(true);
            cbTransferencia.setChecked(false);
            cbTarjeta.setChecked(false);
        }

        setupHeaderProfile();
        
        btnUsarMiUbicacion.setOnClickListener(v -> obtenerUbicacion());
        btnPublicar.setOnClickListener(v -> publicarProducto());
        btnCancelar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar")
                    .setMessage("¿Estás seguro que deseas cancelar?")
                    .setPositiveButton("Sí", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        });

        btnTomarFoto.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
            } else { abrirCamara(); }
        });

        btnElegirGaleria.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_GALERIA);
        });

        setupBottomNav();

        // Logo del header -> ir a la pantalla principal del vendedor (Productos)
        View ivHeaderLogo = findViewById(R.id.ivHeaderLogoAddProducto);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> irADashboard("productos"));
        }
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

    private void setupPhoneFormatting() {
        etTelefono.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;
                String str = s.toString().replaceAll("[^0-9]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < str.length() && i < 8; i++) {
                    formatted.append(str.charAt(i));
                    if (i == 3 && str.length() > 4) formatted.append("-");
                }
                s.replace(0, s.length(), formatted.toString());
                isUpdating = false;
            }
        });
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
        btnUsarMiUbicacion = findViewById(R.id.btnUsarMiUbicacion);
        btnPublicar      = findViewById(R.id.btnPublicarProducto);
        btnCancelar      = findViewById(R.id.btnCancelarProductoV);
        btnTomarFoto     = findViewById(R.id.btnTomarFoto);
        btnElegirGaleria = findViewById(R.id.btnElegirGaleria);
        ivFotoProducto   = findViewById(R.id.ivFotoProducto);
        tvAvatar         = findViewById(R.id.tvAvatarVendedorAdd);
        ivAvatarFoto     = findViewById(R.id.ivAvatarFotoVendedorAdd);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavVendedorAdd);
        bottomNav.setSelectedItemId(R.id.nav_seller_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_seller_products) { irADashboard("productos"); return true; }
            if (id == R.id.nav_seller_orders) { irADashboard("pedidos"); return true; }
            return id == R.id.nav_seller_add;
        });
    }

    private void irADashboard(String tab) {
        Intent i = new Intent(this, VendedorDashboardActivity.class);
        i.putExtra("nav_to", tab);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        overridePendingTransition(0, 0);
        finish();
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_view_profile) { startActivity(new Intent(this, PerfilVendedorActivity.class)); return true; }
            if (id == R.id.menu_soporte) { startActivity(new Intent(this, SoporteActivity.class)); return true; }
            if (id == R.id.menu_logout) { confirmarLogout(); return true; }
            return false;
        });
        popupMenu.show();
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this).setTitle("Salir").setMessage("¿Seguro?").setPositiveButton("Sí", (d, w) -> {
            sessionManager.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }).setNegativeButton("No", null).show();
    }

    private void cargarCategorias() {
        categoriaApi.getCategorias().enqueue(new Callback<List<Categoria>>() {
            @Override public void onResponse(Call<List<Categoria>> c, Response<List<Categoria>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    categorias = r.body();
                    ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(AgregarProductoVendedorActivity.this, android.R.layout.simple_spinner_item, categorias);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spCategoria.setAdapter(adapter);
                    if (productoId != -1) cargarDatosProducto();
                }
            }
            @Override public void onFailure(Call<List<Categoria>> c, Throwable t) {}
        });
    }

    private void cargarDatosProducto() {
        productApi.getProductoPorId(productoId).enqueue(new Callback<Product>() {
            @Override public void onResponse(Call<Product> c, Response<Product> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Product p = r.body();
                    etNombre.setText(p.getNombre());
                    etDescripcion.setText(p.getDescripcion());
                    etPrecio.setText(String.valueOf(p.getPrecio()));
                    etExistencia.setText(String.valueOf(p.getExistencia()));
                    etTelefono.setText(p.getTelefonoVendedor());
                    etDireccion.setText(p.getDireccion());
                    if (p.getLatitud() != null) {
                        latSeleccionada = p.getLatitud(); lonSeleccionada = p.getLongitud();
                        etLatitud.setText(String.format("%.6f", latSeleccionada)); etLongitud.setText(String.format("%.6f", lonSeleccionada));
                        tvUbicacionStatus.setText("✅ Ubicación guardada");
                    }
                    cbEfectivo.setChecked(Boolean.TRUE.equals(p.getAceptaEfectivo()));
                    cbTransferencia.setChecked(Boolean.TRUE.equals(p.getAceptaTransferencia()));
                    cbTarjeta.setChecked(Boolean.TRUE.equals(p.getAceptaTarjeta()));
                    if (p.getImagen() != null) {
                        imagenUrl = p.getImagen();
                        Glide.with(AgregarProductoVendedorActivity.this).load(imagenUrl).into(ivFotoProducto);
                    }
                    for (int i = 0; i < categorias.size(); i++) {
                        if (categorias.get(i).getCategoriaId() == p.getCategoriaId()) {
                            spCategoria.setSelection(i); break;
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Product> c, Throwable t) {}
        });
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        tvUbicacionStatus.setText("📡 Buscando...");
        locationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                latSeleccionada = loc.getLatitude(); lonSeleccionada = loc.getLongitude();
                etLatitud.setText(String.format("%.6f", latSeleccionada)); etLongitud.setText(String.format("%.6f", lonSeleccionada));
                tvUbicacionStatus.setText("✅ Ubicación lista");
            }
        });
    }

    private void publicarProducto() {
        if (subiendoFoto) { Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show(); return; }
        String n = etNombre.getText().toString().trim();
        String pStr = etPrecio.getText().toString().trim();
        String eStr = etExistencia.getText().toString().trim();
        String tel = etTelefono.getText().toString().trim();

        if (n.isEmpty() || pStr.isEmpty() || eStr.isEmpty()) { Toast.makeText(this, "Campos obligatorios", Toast.LENGTH_SHORT).show(); return; }
        
        if (!cbEfectivo.isChecked() && !cbTransferencia.isChecked() && !cbTarjeta.isChecked()) {
            Toast.makeText(this, "DEBES SELECCIONAR AL MENOS UN MÉTODO DE PAGO", Toast.LENGTH_LONG).show();
            return;
        }

        if (!tel.isEmpty() && tel.length() < 9) {
            etTelefono.setError("Teléfono incompleto (formato XXXX-XXXX)");
            etTelefono.requestFocus();
            return;
        }

        Product p = new Product();
        p.setNombre(n); p.setDescripcion(etDescripcion.getText().toString().trim());
        p.setPrecio(Double.parseDouble(pStr)); p.setExistencia(Integer.parseInt(eStr));
        p.setEstado(true); p.setUsuarioId(sessionManager.getUserId());
        p.setCategoriaId(categorias.get(spCategoria.getSelectedItemPosition()).getCategoriaId());
        p.setTelefonoVendedor(tel); p.setDireccion(etDireccion.getText().toString().trim());
        p.setLatitud(latSeleccionada); p.setLongitud(lonSeleccionada);
        p.setAceptaEfectivo(cbEfectivo.isChecked()); p.setAceptaTransferencia(cbTransferencia.isChecked()); p.setAceptaTarjeta(cbTarjeta.isChecked());
        p.setImagen(imagenUrl);

        if (productoId == -1) {
            productApi.crearProducto(p).enqueue(new Callback<Product>() {
                @Override public void onResponse(Call<Product> c, Response<Product> r) {
                    if (r.isSuccessful()) { Toast.makeText(AgregarProductoVendedorActivity.this, "🌿 Publicado", Toast.LENGTH_SHORT).show(); finish(); }
                }
                @Override public void onFailure(Call<Product> c, Throwable t) {}
            });
        } else {
            p.setProductoId(productoId);
            productApi.actualizarProducto(productoId, p).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                    if (r.isSuccessful()) { Toast.makeText(AgregarProductoVendedorActivity.this, "✅ Actualizado", Toast.LENGTH_SHORT).show(); finish(); }
                }
                @Override public void onFailure(Call<ResponseBody> c, Throwable t) {}
            });
        }
    }

    private void abrirCamara() {
        try {
            File f = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        if (req == REQ_CAMERA) { ivFotoProducto.setImageURI(fotoUri); procesarYSubirImagen(fotoUri); }
        else if (req == REQ_GALERIA && data != null) { ivFotoProducto.setImageURI(data.getData()); procesarYSubirImagen(data.getData()); }
    }

    private void procesarYSubirImagen(Uri uri) {
        subiendoFoto = true; btnPublicar.setEnabled(false);
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                byte[] bytes = leerBytes(is);
                SupabaseImageHelper.subirImagen(bytes, new SupabaseImageHelper.UploadCallback() {
                    @Override public void onSuccess(String url) { imagenUrl = url; subiendoFoto = false; runOnUiThread(() -> { btnPublicar.setEnabled(true); Toast.makeText(AgregarProductoVendedorActivity.this, "Foto lista", Toast.LENGTH_SHORT).show(); }); }
                    @Override public void onError(String e) { subiendoFoto = false; runOnUiThread(() -> btnPublicar.setEnabled(true)); }
                });
            } catch (Exception ignored) { subiendoFoto = false; runOnUiThread(() -> btnPublicar.setEnabled(true)); }
        }).start();
    }

    private byte[] leerBytes(InputStream is) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] t = new byte[4096]; int n;
        while ((n = is.read(t)) != -1) b.write(t, 0, n);
        return b.toByteArray();
    }
}
