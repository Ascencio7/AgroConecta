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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;
import sv.edu.agroconecta.utils.SessionManager;
import sv.edu.agroconecta.utils.SupabaseImageHelper;

public class PerfilVendedorActivity extends AppCompatActivity {

    private static final int REQ_GALERIA  = 3001;
    private static final int REQ_CAMERA   = 3002;
    private static final int REQ_CAM_PERM = 3003;

    private SessionManager sessionManager;
    private UsuarioApi usuarioApi;

    private ImageView ivFoto;
    private TextView tvAvatar, tvRol, btnCambiarFoto;
    private TextInputEditText etNombre, etCorreo, etTelefono;
    private TextInputEditText etNuevaPassword, etConfirmarPassword;
    private Button btnGuardar, btnCambiarPassword, btnLogout;
    private ImageButton btnBack;

    private Uri fotoUri = null;
    private String fotoUrlSubida = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_vendedor);

        sessionManager = new SessionManager(this);
        usuarioApi     = ApiClient.getClient().create(UsuarioApi.class);

        ivFoto               = findViewById(R.id.ivFotoPerfilVendedor);
        tvAvatar             = findViewById(R.id.tvAvatarVendedorPerfil);
        tvRol                = findViewById(R.id.tvRolVendedorPerfil);
        btnCambiarFoto       = findViewById(R.id.btnCambiarFotoVendedor);
        etNombre             = findViewById(R.id.etNombreVendedorPerfil);
        etCorreo             = findViewById(R.id.etCorreoVendedorPerfil);
        etTelefono           = findViewById(R.id.etTelefonoVendedorPerfil);
        etNuevaPassword      = findViewById(R.id.etNuevaPasswordVendedor);
        etConfirmarPassword  = findViewById(R.id.etConfirmarPasswordVendedor);
        btnGuardar           = findViewById(R.id.btnGuardarPerfilVendedor);
        btnCambiarPassword   = findViewById(R.id.btnCambiarPasswordVendedor);
        btnLogout            = findViewById(R.id.btnLogoutVendedor);
        btnBack              = findViewById(R.id.btnBackPerfilVendedor);

        setupPhoneFormatting();
        cargarDatosSesion();
        cargarDatosBackend();

        btnBack.setOnClickListener(v -> finish());
        btnCambiarFoto.setOnClickListener(v -> mostrarDialogoFoto());
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());
        btnLogout.setOnClickListener(v -> confirmarLogout());
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

    private void cargarDatosSesion() {
        String nombre = sessionManager.getNombre();
        String correo = sessionManager.getCorreo();
        String tel    = sessionManager.getTelefono();
        String rol    = sessionManager.getRol();

        if (nombre != null && !nombre.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            etNombre.setText(nombre);
        }
        if (correo != null) etCorreo.setText(correo);
        if (tel != null && !tel.isEmpty()) etTelefono.setText(tel);
        tvRol.setText(rol != null ? rol : "VENDEDOR");

        String fotoGuardada = sessionManager.getFotoPerfil();
        if (fotoGuardada != null && !fotoGuardada.isEmpty()) {
            mostrarFoto(fotoGuardada);
        }
    }

    private void cargarDatosBackend() {
        int myId = sessionManager.getUserId();
        if (myId < 0) return;
        usuarioApi.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                for (Usuario u : response.body()) {
                    if (u.getUsuarioId() == myId) {
                        runOnUiThread(() -> {
                            if (u.getNombre() != null) {
                                etNombre.setText(u.getNombre());
                                tvAvatar.setText(String.valueOf(u.getNombre().charAt(0)).toUpperCase());
                            }
                            if (u.getCorreo()   != null) etCorreo.setText(u.getCorreo());
                            if (u.getTelefono() != null) {
                                etTelefono.setText(u.getTelefono());
                                sessionManager.setTelefono(u.getTelefono());
                            }
                            if (u.getFotoPerfil() != null && !u.getFotoPerfil().isEmpty()) {
                                mostrarFoto(u.getFotoPerfil());
                                sessionManager.setFotoPerfil(u.getFotoPerfil());
                            }
                        });
                        break;
                    }
                }
            }
            @Override public void onFailure(Call<List<Usuario>> call, Throwable t) { }
        });
    }

    private void mostrarDialogoFoto() {
        new AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setItems(new String[]{"📷 Tomar foto", "🖼️ Elegir de galería"}, (dialog, which) -> {
                    if (which == 0) verificarPermisosCamara();
                    else            abrirGaleria();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
        } else {
            abrirCamara();
        }
    }

    private void abrirCamara() {
        try {
            File foto = crearArchivoFoto();
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_GALERIA);
    }

    private File crearArchivoFoto() throws IOException {
        String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File   dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("PERFIL_" + ts, ".jpg", dir);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        Uri uri = null;
        if (req == REQ_CAMERA && fotoUri != null) uri = fotoUri;
        else if (req == REQ_GALERIA && data != null && data.getData() != null) uri = data.getData();

        if (uri != null) {
            mostrarFoto(uri.toString());
            subirFoto(uri);
        }
    }

    private void subirFoto(Uri uri) {
        btnCambiarFoto.setText("⏳");
        btnCambiarFoto.setClickable(false);
        new Thread(() -> {
            try {
                InputStream is    = getContentResolver().openInputStream(uri);
                byte[]      bytes = leerBytes(is);
                SupabaseImageHelper.subirImagen(bytes, new SupabaseImageHelper.UploadCallback() {
                    @Override public void onSuccess(String publicUrl) {
                        fotoUrlSubida = publicUrl;
                        sessionManager.setFotoPerfil(publicUrl);
                        runOnUiThread(() -> {
                            btnCambiarFoto.setText("📷");
                            btnCambiarFoto.setClickable(true);
                            Toast.makeText(PerfilVendedorActivity.this, "✅ Foto lista", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> {
                            btnCambiarFoto.setText("📷");
                            btnCambiarFoto.setClickable(true);
                            Toast.makeText(PerfilVendedorActivity.this, "❌ Error al subir", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnCambiarFoto.setText("📷");
                    btnCambiarFoto.setClickable(true);
                });
            }
        }).start();
    }

    private void mostrarFoto(String url) {
        Glide.with(this).load(url).transform(new CircleCrop()).placeholder(R.drawable.bg_avatar_circle).into(ivFoto);
        ivFoto.setVisibility(View.VISIBLE);
        tvAvatar.setVisibility(View.GONE);
    }

    private void guardarCambios() {
        Editable nomEditable = etNombre.getText();
        Editable telEditable = etTelefono.getText();
        Editable corEditable = etCorreo.getText();
        
        String nombre   = nomEditable != null ? nomEditable.toString().trim()   : "";
        String correo   = corEditable != null ? corEditable.toString().trim()   : "";
        String telefono = telEditable != null ? telEditable.toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) { etNombre.setError("Requerido"); return; }
        if (telefono.length() < 9) { etTelefono.setError("Formato XXXX-XXXX"); return; }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        int myId = sessionManager.getUserId();
        String rol = sessionManager.getRol();

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        String fotoPerfil = sessionManager.getFotoPerfil();
        if (fotoPerfil != null) u.setFotoPerfil(fotoPerfil);
        if ("ADMIN".equalsIgnoreCase(rol)) u.setRolId(1);
        else if ("VENDEDOR".equalsIgnoreCase(rol)) u.setRolId(2);
        else u.setRolId(3);

        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                runOnUiThread(() -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("💾 Guardar cambios");
                    if (response.isSuccessful()) {
                        sessionManager.createSession(myId, nombre, correo, rol);
                        sessionManager.setTelefono(telefono);
                        Toast.makeText(PerfilVendedorActivity.this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(Call<Usuario> call, Throwable t) {
                runOnUiThread(() -> { btnGuardar.setEnabled(true); btnGuardar.setText("💾 Guardar cambios"); });
            }
        });
    }

    private void cambiarPassword() {
        String nueva = etNuevaPassword.getText().toString().trim();
        String conf = etConfirmarPassword.getText().toString().trim();
        if (TextUtils.isEmpty(nueva) || nueva.length() < 6 || !nueva.equals(conf)) { Toast.makeText(this, "Verifica la contraseña", Toast.LENGTH_SHORT).show(); return; }
        btnCambiarPassword.setEnabled(false);
        int myId = sessionManager.getUserId();
        Usuario u = new Usuario();
        u.setNombre(sessionManager.getNombre());
        u.setCorreo(sessionManager.getCorreo());
        u.setPassword(nueva);
        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                runOnUiThread(() -> { btnCambiarPassword.setEnabled(true); if (response.isSuccessful()) Toast.makeText(PerfilVendedorActivity.this, "✅ Actualizada", Toast.LENGTH_SHORT).show(); });
            }
            @Override public void onFailure(Call<Usuario> call, Throwable t) { runOnUiThread(() -> btnCambiarPassword.setEnabled(true)); }
        });
    }

    private byte[] leerBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] temp = new byte[4096]; int n;
        while ((n = is.read(temp)) != -1) buf.write(temp, 0, n);
        return buf.toByteArray();
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this).setTitle("Salir").setMessage("¿Cerrar sesión?").setPositiveButton("Sí", (d, w) -> {
            sessionManager.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }).setNegativeButton("No", null).show();
    }
}
