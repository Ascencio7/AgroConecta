package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.repository.UsuarioRepository;

public class EditarUsuarioActivity extends AppCompatActivity {

    int usuarioId;

    EditText etNombre, etCorreo, etTelefono;
    MaterialSwitch switchEstado;
    TextView txtEstadoLabel;
    Button btnGuardar, btnCancelar;
    BottomNavigationView bottomNavAdmin;

    UsuarioRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_usuario);

        repository = new UsuarioRepository();

        // Vistas
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        switchEstado = findViewById(R.id.switchEstado);
        txtEstadoLabel = findViewById(R.id.txtEstadoLabel);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);

        // Aqui se reciben los datos
        usuarioId = getIntent().getIntExtra("usuario_id", -1);
        etNombre.setText(getIntent().getStringExtra("nombre"));
        etCorreo.setText(getIntent().getStringExtra("correo"));
        etTelefono.setText(getIntent().getStringExtra("telefono"));
        boolean estado = getIntent().getBooleanExtra("estado", true);
        switchEstado.setChecked(estado);
        actualizarLabelEstado(estado);

        switchEstado.setOnCheckedChangeListener((buttonView, isChecked) -> actualizarLabelEstado(isChecked));

        // Se guardan los datos y se actualizan
        btnGuardar.setOnClickListener(v -> actualizarUsuario());

        // Agregar validación en tiempo real del correo
        etCorreo.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String v = s.toString().trim();
                if (!v.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches())
                    etCorreo.setError("Correo inválido");
                else etCorreo.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnCancelar.setOnClickListener(v ->{
            new androidx.appcompat.app.AlertDialog.Builder(EditarUsuarioActivity.this)
                    .setTitle("Cancelar edición")
                    .setMessage("¿Estás seguro que deseas cancelar?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        setupBottomNav();
    }

    private void actualizarLabelEstado(boolean isChecked) {
        if (isChecked) {
            txtEstadoLabel.setText("Usuario Activo");
            txtEstadoLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.verde_primario));
        } else {
            txtEstadoLabel.setText("Usuario Inactivo");
            txtEstadoLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.rojo_error));
        }
    }

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_users);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_admin_products) {
                startActivity(new Intent(this, ProductosAdminActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_admin_users;
        });
    }

    // Actualizar los datos
    private void actualizarUsuario() {

        String nombre   = etNombre.getText().toString().trim();
        String correo   = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        boolean estado  = switchEstado.isChecked();

        if (nombre.isEmpty()) {
            etNombre.setError("Ingresa el nombre"); etNombre.requestFocus(); return;
        }
        if (correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo inválido"); etCorreo.requestFocus(); return;
        }
        if (telefono.isEmpty()) {
            etTelefono.setError("Ingresa el teléfono"); etTelefono.requestFocus(); return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(usuarioId);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuario.setEstado(estado);

        repository.actualizar(usuarioId, usuario)
                .enqueue(new Callback<Usuario>() {
                    @Override
                    public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar cambios");
                        if (response.isSuccessful()) {
                            Toast.makeText(EditarUsuarioActivity.this,
                                    "Usuario actualizado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditarUsuarioActivity.this,
                                    "Error al actualizar. Intenta de nuevo.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Usuario> call, Throwable t) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar cambios");
                        Toast.makeText(EditarUsuarioActivity.this,
                                "Sin conexión. Verifica tu red.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}