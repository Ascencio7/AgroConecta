package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
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

import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.utils.SessionManager;
import sv.edu.agroconecta.utils.SupabaseImageHelper;

public class PerfilAdminActivity extends AppCompatActivity {

    private static final int REQ_GALERIA  = 4001;
    private static final int REQ_CAMERA   = 4002;
    private static final int REQ_CAM_PERM = 4003;

    private TextView tvAvatarLarge, tvRolAdmin;
    private TextInputEditText etNombre, etCorreo, etTelefono, etNuevaPassword, etConfirmarPassword;
    private ImageView ivFoto;
    private TextView btnCambiarFoto;
    private ImageButton btnBackProfile;
    private Button btnLogoutProfile, btnGuardar, btnCambiarPassword;
    private SessionManager sessionManager;
    private UsuarioApi usuarioApi;
    private Uri fotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Evitar que el teclado tape los campos
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_perfil_admin);

        sessionManager = new SessionManager(this);
        usuarioApi = ApiClient.getClient().create(UsuarioApi.class);

        ivFoto              = findViewById(R.id.ivFotoPerfilAdmin);
        tvAvatarLarge       = findViewById(R.id.tvAvatarLarge);
        etNombre            = findViewById(R.id.etNombreAdminPerfil);
        etCorreo            = findViewById(R.id.etCorreoAdminPerfil);
        etTelefono          = findViewById(R.id.etTelefonoAdminPerfil);
        tvRolAdmin          = findViewById(R.id.tvRolAdminPerfil);
        etNuevaPassword     = findViewById(R.id.etNuevaPasswordAdmin);
        etConfirmarPassword = findViewById(R.id.etConfirmarPasswordAdmin);
        btnCambiarFoto      = findViewById(R.id.btnCambiarFotoAdmin);
        btnBackProfile      = findViewById(R.id.btnBackProfile);
        btnLogoutProfile    = findViewById(R.id.btnLogoutProfile);
        btnGuardar          = findViewById(R.id.btnGuardarPerfilAdmin);
        btnCambiarPassword  = findViewById(R.id.btnCambiarPasswordAdmin);

        cargarDatosSesion();
        cargarDatosBackend();

        btnBackProfile.setOnClickListener(v -> finish());
        btnCambiarFoto.setOnClickListener(v -> mostrarDialogoFoto());
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());
        btnLogoutProfile.setOnClickListener(v -> confirmarLogout());
        
        setupPhoneFormatting();
    }

    private void setupPhoneFormatting() {
        if (etTelefono == null) return;
        etTelefono.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
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
            tvAvatarLarge.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            etNombre.setText(nombre);
        }
        if (correo != null) etCorreo.setText(correo);
        if (tvRolAdmin != null) tvRolAdmin.setText("ADMINISTRADOR");

        // Cargar foto guardada
        String fotoGuardada = sessionManager.getFotoPerfil();
        if (fotoGuardada != null && !fotoGuardada.isEmpty()) {
            mostrarFoto(fotoGuardada);
        }
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
                                tvAvatarLarge.setText(String.valueOf(u.getNombre().charAt(0)).toUpperCase());
                            }
                            if (u.getCorreo() != null) etCorreo.setText(u.getCorreo());
                            if (u.getTelefono() != null) etTelefono.setText(u.getTelefono());
                        });
                        break;
                    }
                }
            }
            @Override public void onFailure(Call<List<Usuario>> call, Throwable t) {}
        });
    }

    private void guardarCambios() {
        String nombre   = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String correo   = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) { etNombre.setError("Requerido"); return; }
        if (TextUtils.isEmpty(correo)) { etCorreo.setError("Requerido"); return; }

        btnGuardar.setEnabled(false); btnGuardar.setText("Guardando...");
        int myId = sessionManager.getUserId();
        Usuario u = new Usuario();
        u.setNombre(nombre); u.setCorreo(correo);
        u.setTelefono(telefono);
        String fp = sessionManager.getFotoPerfil();
        if (fp != null) u.setFotoPerfil(fp);
        u.setRolId(1); // ADMIN

        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {
                runOnUiThread(() -> {
                    btnGuardar.setEnabled(true); btnGuardar.setText("ACTUALIZAR");
                    if (r.isSuccessful()) {
                        sessionManager.createSession(myId, nombre, correo, "ADMIN");
                        tvAvatarLarge.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
                        Toast.makeText(PerfilAdminActivity.this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(PerfilAdminActivity.this, "❌ Error al guardar", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(Call<Usuario> c, Throwable t) {
                runOnUiThread(() -> { btnGuardar.setEnabled(true); btnGuardar.setText("ACTUALIZAR");
                    Toast.makeText(PerfilAdminActivity.this, "❌ Sin conexión", Toast.LENGTH_SHORT).show(); });
            }
        });
    }

    private void cambiarPassword() {
        String nueva = etNuevaPassword.getText() != null ? etNuevaPassword.getText().toString().trim() : "";
        String conf  = etConfirmarPassword.getText() != null ? etConfirmarPassword.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nueva)) { etNuevaPassword.setError("Ingresa la nueva contraseña"); return; }
        if (nueva.length() < 8) { etNuevaPassword.setError("Mínimo 8 caracteres"); return; }
        if (!nueva.equals(conf)) { etConfirmarPassword.setError("No coinciden"); return; }

        btnCambiarPassword.setEnabled(false); btnCambiarPassword.setText("Actualizando...");
        int myId = sessionManager.getUserId();
        Usuario u = new Usuario();
        u.setNombre(sessionManager.getNombre()); u.setCorreo(sessionManager.getCorreo());
        u.setPassword(nueva); u.setRolId(1);

        usuarioApi.updateUsuario(myId, u).enqueue(new Callback<Usuario>() {
            @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {
                runOnUiThread(() -> {
                    btnCambiarPassword.setEnabled(true); btnCambiarPassword.setText("🔒 Cambiar contraseña");
                    if (r.isSuccessful()) { etNuevaPassword.setText(""); etConfirmarPassword.setText("");
                        Toast.makeText(PerfilAdminActivity.this, "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(PerfilAdminActivity.this, "❌ Error", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onFailure(Call<Usuario> c, Throwable t) {
                runOnUiThread(() -> { btnCambiarPassword.setEnabled(true); btnCambiarPassword.setText("🔒 Cambiar contraseña"); });
            }
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
            String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File   dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File   foto = File.createTempFile("PERFIL_" + ts, ".jpg", dir);
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_GALERIA);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        Uri uri = null;
        if (req == REQ_CAMERA && fotoUri != null) uri = fotoUri;
        else if (req == REQ_GALERIA && data != null && data.getData() != null) uri = data.getData();

        if (uri != null) {
            Glide.with(this).load(uri).transform(new CircleCrop()).into(ivFoto);
            ivFoto.setVisibility(View.VISIBLE);
            tvAvatarLarge.setVisibility(View.GONE);
            subirFoto(uri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_CAM_PERM && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) abrirCamara();
    }

    private void subirFoto(Uri uri) {
        btnCambiarFoto.setText("⏳");
        btnCambiarFoto.setClickable(false);
        new Thread(() -> {
            try {
                InputStream is    = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] temp = new byte[4096]; int n;
                while ((n = is.read(temp)) != -1) buf.write(temp, 0, n);
                byte[] bytes = buf.toByteArray();

                SupabaseImageHelper.subirImagen(bytes, new SupabaseImageHelper.UploadCallback() {
                    @Override public void onSuccess(String publicUrl) {
                        sessionManager.setFotoPerfil(publicUrl);
                        // Persistir en backend
                        guardarFotoEnBackend(publicUrl);
                        runOnUiThread(() -> {
                            btnCambiarFoto.setText("📷");
                            btnCambiarFoto.setClickable(true);
                            Toast.makeText(PerfilAdminActivity.this,
                                    "✅ Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> {
                            btnCambiarFoto.setText("📷");
                            btnCambiarFoto.setClickable(true);
                            Toast.makeText(PerfilAdminActivity.this,
                                    "❌ Error al subir foto", Toast.LENGTH_SHORT).show();
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


    private void guardarFotoEnBackend(String fotoUrl) {
        int myId = sessionManager.getUserId();
        String rol = sessionManager.getRol();
        if (myId < 0) return;
        Usuario u = new Usuario();
        u.setNombre(sessionManager.getNombre());
        u.setCorreo(sessionManager.getCorreo());
        u.setFotoPerfil(fotoUrl);
        if ("ADMIN".equalsIgnoreCase(rol)) u.setRolId(1);
        else if ("VENDEDOR".equalsIgnoreCase(rol)) u.setRolId(2);
        else u.setRolId(3);
        ApiClient.getClient().create(UsuarioApi.class)
                .updateUsuario(myId, u)
                .enqueue(new Callback<Usuario>() {
                    @Override public void onResponse(Call<Usuario> c, Response<Usuario> r) {}
                    @Override public void onFailure(Call<Usuario> c, Throwable t) {}
                });
    }

    private void mostrarFoto(String url) {
        Glide.with(this).load(url).transform(new CircleCrop()).into(ivFoto);
        ivFoto.setVisibility(View.VISIBLE);
        tvAvatarLarge.setVisibility(View.GONE);
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
