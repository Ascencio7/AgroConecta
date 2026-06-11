package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class PerfilClienteActivity extends AppCompatActivity {

    private static final int REQ_GALERIA  = 5001;
    private static final int REQ_CAMERA   = 5002;
    private static final int REQ_CAM_PERM = 5003;

    private SessionManager sessionManager;
    private UsuarioApi usuarioApi;

    private ImageView ivFoto;
    private TextView tvAvatar, tvRol, btnCambiarFoto;
    private TextInputEditText etNombre, etCorreo, etTelefono;
    private TextInputEditText etNuevaPassword, etConfirmarPassword;
    private Button btnGuardar, btnCambiarPassword, btnLogout;
    private ImageButton btnBack;
    private Uri fotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_cliente);

        sessionManager = new SessionManager(this);
        usuarioApi     = ApiClient.getClient().create(UsuarioApi.class);

        ivFoto              = findViewById(R.id.ivFotoPerfilCliente);
        tvAvatar            = findViewById(R.id.tvAvatarClientePerfil);
        tvRol               = findViewById(R.id.tvRolClientePerfil);
        btnCambiarFoto      = findViewById(R.id.btnCambiarFotoCliente);
        etNombre            = findViewById(R.id.etNombreClientePerfil);
        etCorreo            = findViewById(R.id.etCorreoClientePerfil);
        etTelefono          = findViewById(R.id.etTelefonoClientePerfil);
        etNuevaPassword     = findViewById(R.id.etNuevaPasswordCliente);
        etConfirmarPassword = findViewById(R.id.etConfirmarPasswordCliente);
        btnGuardar          = findViewById(R.id.btnGuardarPerfilCliente);
        btnCambiarPassword  = findViewById(R.id.btnCambiarPasswordCliente);
        btnLogout           = findViewById(R.id.btnLogoutCliente);
        btnBack             = findViewById(R.id.btnBackPerfilCliente);

        cargarDatosSesion();
        cargarDatosBackend();

        btnBack.setOnClickListener(v -> finish());
        setupPhoneFormatting();
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
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            etNombre.setText(nombre);
        }
        if (correo != null) etCorreo.setText(correo);
        tvRol.setText("CLIENTE");
        String foto = sessionManager.getFotoPerfil();
        if (foto != null && !foto.isEmpty()) mostrarFoto(foto);
    }

    private void cargarDatosBackend() {
        int myId = sessionManager.getUserId();
        if (myId < 0) return;
        usuarioApi.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                for (Usuario u : response.body()) {
                    if (u.getUsuarioId() == myId) {
                        runOnUiThread(() -> {
                            if (u.getNombre() != null && !u.getNombre().isEmpty()) {
                                etNombre.setText(u.getNombre());
                                tvAvatar.setText(String.valueOf(u.getNombre().charAt(0)).toUpperCase());
                            }
                            if (u.getCorreo()   != null) etCorreo.setText(u.getCorreo());
                            if (u.getTelefono() != null) etTelefono.setText(u.getTelefono());
                        });
                        break;
                    }
                }
            }
            @Override public void onFailure(Call<List<Usuario>> call, Throwable t) {}
        });
    }

    private void mostrarDialogoFoto() {
        new AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setItems(new String[]{"📷 Tomar foto", "🖼️ Elegir de galería"}, (d, which) -> {
                    if (which == 0) verificarPermisosCamara(); else abrirGaleria();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
        else abrirCamara();
    }

    private void abrirCamara() {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File foto = File.createTempFile("PERFIL_" + ts, ".jpg", dir);
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) { Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show(); }
    }

    private void abrirGaleria() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQ_GALERIA);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        Uri uri = req == REQ_CAMERA ? fotoUri : (data != null ? data.getData() : null);
        if (uri != null) {
            Glide.with(this).load(uri).transform(new CircleCrop()).into(ivFoto);
            ivFoto.setVisibility(View.VISIBLE); tvAvatar.setVisibility(View.GONE);
            subirFoto(uri);
        }
    }

    @Override public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_CAM_PERM && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) abrirCamara();
    }

    private void subirFoto(Uri uri) {
        btnCambiarFoto.setText("⏳"); btnCambiarFoto.setClickable(false);
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[4096]; int n;
                while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
                SupabaseImageHelper.subirImagen(buf.toByteArray(), new SupabaseImageHelper.UploadCallback() {
                    @Override public void onSuccess(String url) {
                        sessionManager.setFotoPerfil(url);
                        guardarFotoEnBackend(url);
                        runOnUiThread(() -> { btnCambiarFoto.setText("📷"); btnCambiarFoto.setClickable(true);
                            Toast.makeText(PerfilClienteActivity.this, "✅ Foto actualizada", Toast.LENGTH_SHORT).show(); });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> { btnCambiarFoto.setText("📷"); btnCambiarFoto.setClickable(true); });
                    }
                });
            } catch (Exception e) { runOnUiThread(() -> { btnCambiarFoto.setText("📷"); btnCambiarFoto.setClickable(true); }); }
        }).start();
    }

    private void guardarFotoEnBackend(String url) {
        int myId = sessionManager.getUserId();
        if (myId < 0) return;
        Usuario u = new Usuario();
        u.setNombre(sessionManager.getNombre());
        u.setCorreo(sessionManager.getCorreo());
        u.setFotoPerfil(url);
        u.setRolId(3);
        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {}
            @Override public void onFailure(Call<Usuario> c, Throwable t) {}
        });
    }

    private void mostrarFoto(String url) {
        Glide.with(this).load(url).transform(new CircleCrop()).into(ivFoto);
        ivFoto.setVisibility(View.VISIBLE); tvAvatar.setVisibility(View.GONE);
    }

    private void guardarCambios() {
        String nombre   = etNombre.getText()   != null ? etNombre.getText().toString().trim()   : "";
        String correo   = etCorreo.getText()   != null ? etCorreo.getText().toString().trim()   : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) { etNombre.setError("Requerido"); etNombre.requestFocus(); return; }
        if (TextUtils.isEmpty(correo)) { etCorreo.setError("Requerido"); etCorreo.requestFocus(); return; }
        if (!TextUtils.isEmpty(telefono)) {
            if (telefono.length() < 9) {
                etTelefono.setError("Formato XXXX-XXXX"); etTelefono.requestFocus(); return;
            }
        }

        btnGuardar.setEnabled(false); btnGuardar.setText("Guardando...");
        int myId = sessionManager.getUserId();
        Usuario u = new Usuario();
        u.setNombre(nombre); u.setCorreo(correo);
        if (!TextUtils.isEmpty(telefono)) u.setTelefono(telefono);
        String fp = sessionManager.getFotoPerfil();
        if (fp != null) u.setFotoPerfil(fp);
        u.setRolId(3);

        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {
                runOnUiThread(() -> {
                    btnGuardar.setEnabled(true); btnGuardar.setText("💾 Guardar cambios");
                    if (r.isSuccessful()) {
                        sessionManager.createSession(myId, nombre, correo, "CLIENTE");
                        tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
                        Toast.makeText(PerfilClienteActivity.this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(PerfilClienteActivity.this, "❌ Error al guardar", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(Call<Usuario> c, Throwable t) {
                runOnUiThread(() -> { btnGuardar.setEnabled(true); btnGuardar.setText("💾 Guardar cambios");
                    Toast.makeText(PerfilClienteActivity.this, "❌ Sin conexión", Toast.LENGTH_SHORT).show(); });
            }
        });
    }

    private void cambiarPassword() {
        String nueva = etNuevaPassword.getText() != null ? etNuevaPassword.getText().toString().trim() : "";
        String conf  = etConfirmarPassword.getText() != null ? etConfirmarPassword.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nueva)) { etNuevaPassword.setError("Ingresa la nueva contraseña"); etNuevaPassword.requestFocus(); return; }
        if (nueva.length() < 8) { etNuevaPassword.setError("Mínimo 8 caracteres"); etNuevaPassword.requestFocus(); return; }
        if (!nueva.equals(conf)) { etConfirmarPassword.setError("No coinciden"); etConfirmarPassword.requestFocus(); return; }

        btnCambiarPassword.setEnabled(false); btnCambiarPassword.setText("Actualizando...");
        int myId = sessionManager.getUserId();
        Usuario u = new Usuario();
        u.setNombre(sessionManager.getNombre()); u.setCorreo(sessionManager.getCorreo());
        u.setPassword(nueva); u.setRolId(3);

        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {
                runOnUiThread(() -> {
                    btnCambiarPassword.setEnabled(true); btnCambiarPassword.setText("🔒 Cambiar contraseña");
                    if (r.isSuccessful()) { etNuevaPassword.setText(""); etConfirmarPassword.setText("");
                        Toast.makeText(PerfilClienteActivity.this, "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(PerfilClienteActivity.this, "❌ Error", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(Call<Usuario> c, Throwable t) {
                runOnUiThread(() -> { btnCambiarPassword.setEnabled(true); btnCambiarPassword.setText("🔒 Cambiar contraseña"); });
            }
        });
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this).setTitle("Cerrar Sesión").setMessage("¿Seguro?")
                .setPositiveButton("Sí", (d, w) -> {
                    sessionManager.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i); finish();
                }).setNegativeButton("No", null).show();
    }
}
