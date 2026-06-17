package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Rol;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.repository.UsuarioRepository;

public class EditarUsuarioActivity extends AppCompatActivity {

    int usuarioId, rolIdOriginal;

    EditText etNombre, etCorreo, etTelefono;
    Spinner spRol;
    MaterialSwitch switchEstado;
    TextView txtEstadoLabel;
    Button btnGuardar, btnCancelar;
    BottomNavigationView bottomNavAdmin;

    UsuarioRepository repository;
    List<Rol> rolesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_usuario);

        repository = new UsuarioRepository();

        // Vistas
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        spRol = findViewById(R.id.spRol);
        switchEstado = findViewById(R.id.switchEstado);
        txtEstadoLabel = findViewById(R.id.txtEstadoLabel);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);

        // Aqui se reciben los datos
        usuarioId = getIntent().getIntExtra("usuario_id", -1);
        rolIdOriginal = getIntent().getIntExtra("rol_id", -1);
        etNombre.setText(getIntent().getStringExtra("nombre"));
        etCorreo.setText(getIntent().getStringExtra("correo"));
        etTelefono.setText(getIntent().getStringExtra("telefono"));
        boolean estado = getIntent().getBooleanExtra("estado", true);
        switchEstado.setChecked(estado);
        actualizarLabelEstado(estado);

        cargarRoles();
        setupPhoneFormatting();

        switchEstado.setOnCheckedChangeListener((buttonView, isChecked) -> actualizarLabelEstado(isChecked));

        // Logo del header -> ir a la pantalla principal del admin (Dashboard)
        android.view.View ivHeaderLogo = findViewById(R.id.ivHeaderLogoEditarUsuario);
        if (ivHeaderLogo != null) {
            ivHeaderLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Se guardan los datos y se actualizan
        btnGuardar.setText("ACTUALIZAR");
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

    private void cargarRoles() {
        repository.getRoles().enqueue(new Callback<List<Rol>>() {
            @Override
            public void onResponse(Call<List<Rol>> call, Response<List<Rol>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rolesList = response.body();
                    List<String> nombres = new ArrayList<>();
                    int selectionIndex = 0;
                    for (int i = 0; i < rolesList.size(); i++) {
                        Rol r = rolesList.get(i);
                        nombres.add(r.getNombre().toUpperCase());
                        if (r.getRolId() == rolIdOriginal) {
                            selectionIndex = i;
                        }
                    }
                    ArrayAdapter<String> adapterRol = new ArrayAdapter<>(
                            EditarUsuarioActivity.this,
                            android.R.layout.simple_spinner_item,
                            nombres
                    );
                    adapterRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spRol.setAdapter(adapterRol);
                    spRol.setSelection(selectionIndex);
                }
            }

            @Override
            public void onFailure(Call<List<Rol>> call, Throwable t) {
                Toast.makeText(EditarUsuarioActivity.this, "Error al cargar roles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPhoneFormatting() {
        etTelefono.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                String original = s.toString();
                String digits = original.replaceAll("[^0-9]", "");
                StringBuilder formatted = new StringBuilder();
                
                for (int i = 0; i < digits.length() && i < 8; i++) {
                    formatted.append(digits.charAt(i));
                    if (i == 3 && digits.length() > 4) {
                        formatted.append("-");
                    }
                }

                String result = formatted.toString();
                if (!result.equals(original)) {
                    isUpdating = true;
                    // Usar replace de forma segura para mantener el foco y evitar crashes en WordIterator
                    etTelefono.setText(result);
                    etTelefono.setSelection(result.length());
                    isUpdating = false;
                }
            }
        });
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
        
        // El teléfono debe tener 9 caracteres (8 dígitos + 1 guion)
        if (telefono.length() < 9) {
            etTelefono.setError("Teléfono incompleto (formato XXXX-XXXX)");
            etTelefono.requestFocus();
            return;
        }

        if (rolesList.isEmpty()) {
            Toast.makeText(this, "Espera a que carguen los roles", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        int rolId = rolesList.get(spRol.getSelectedItemPosition()).getRolId();

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(usuarioId);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuario.setEstado(estado);
        usuario.setRolId(rolId);

        repository.actualizar(usuarioId, usuario)
                .enqueue(new Callback<Usuario>() {
                    @Override
                    public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("ACTUALIZAR");
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
                        btnGuardar.setText("ACTUALIZAR");
                        Toast.makeText(EditarUsuarioActivity.this,
                                "Sin conexión. Verifica tu red.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}