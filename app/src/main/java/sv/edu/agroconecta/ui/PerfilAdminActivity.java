package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private TextView tvAvatarLarge, tvProfileName, tvProfileRole, tvProfileEmail;
    private ImageView ivFoto;
    private TextView btnCambiarFoto;
    private ImageButton btnBackProfile;
    private Button btnLogoutProfile;
    private SessionManager sessionManager;
    private Uri fotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_admin);

        sessionManager = new SessionManager(this);

        ivFoto          = findViewById(R.id.ivFotoPerfilAdmin);
        tvAvatarLarge   = findViewById(R.id.tvAvatarLarge);
        tvProfileName   = findViewById(R.id.tvProfileName);
        tvProfileRole   = findViewById(R.id.tvProfileRole);
        tvProfileEmail  = findViewById(R.id.tvProfileEmail);
        btnCambiarFoto  = findViewById(R.id.btnCambiarFotoAdmin);
        btnBackProfile  = findViewById(R.id.btnBackProfile);
        btnLogoutProfile= findViewById(R.id.btnLogoutProfile);

        String nombre = sessionManager.getNombre();
        String rol    = sessionManager.getRol();
        String correo = sessionManager.getCorreo();

        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarLarge.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            tvProfileName.setText(nombre);
        }
        tvProfileRole.setText(rol);
        tvProfileEmail.setText(correo);

        if ("ADMIN".equalsIgnoreCase(rol)) {
            tvProfileRole.setTextColor(getResources().getColor(R.color.dorado, getTheme()));
        } else if ("VENDEDOR".equalsIgnoreCase(rol)) {
            tvProfileRole.setTextColor(getResources().getColor(R.color.verde_primario, getTheme()));
        } else {
            tvProfileRole.setTextColor(getResources().getColor(R.color.texto_secundario, getTheme()));
        }

        // Cargar foto guardada
        String fotoGuardada = sessionManager.getFotoPerfil();
        if (fotoGuardada != null && !fotoGuardada.isEmpty()) {
            mostrarFoto(fotoGuardada);
        }

        btnBackProfile.setOnClickListener(v -> finish());
        btnCambiarFoto.setOnClickListener(v -> mostrarDialogoFoto());
        btnLogoutProfile.setOnClickListener(v -> confirmarLogout());
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
