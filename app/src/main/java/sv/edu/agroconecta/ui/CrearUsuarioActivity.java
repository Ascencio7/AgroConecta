package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Rol;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.repository.UsuarioRepository;

public class CrearUsuarioActivity extends AppCompatActivity {
    EditText etNombre, etCorreo, etPassword, etTelefono;
    Spinner spRol;
    Button btnGuardar, btnCancelar;
    UsuarioRepository repo;
    List<Rol> rolesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_usuario);

        repo = new UsuarioRepository();

        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        etTelefono = findViewById(R.id.etTelefono);
        spRol = findViewById(R.id.spRol);
        btnGuardar = findViewById(R.id.btnGuardar);

        btnCancelar = findViewById(R.id.btnCancelar);

        cargarRoles();

        btnGuardar.setOnClickListener(v -> crearUsuario());

        btnCancelar.setOnClickListener(v ->{
            new androidx.appcompat.app.AlertDialog.Builder(CrearUsuarioActivity.this)
                    .setTitle("Cancelar")
                    .setMessage("¿Estás seguro que deseas cancelar?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

    }

    private void cargarRoles() {
        repo.getRoles() .enqueue(new Callback<List<Rol>>() {
            @Override
            public void onResponse(Call<List<Rol>> call, Response<List<Rol>> response) {
                rolesList = response.body();
                if (rolesList == null) {
                    Toast.makeText(CrearUsuarioActivity.this, "Error roles", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> nombres = new ArrayList<>();
                for (Rol r : rolesList) {
                    nombres.add(r.getNombre());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        CrearUsuarioActivity.this,
                        android.R.layout.simple_spinner_item,
                        nombres
                );
                spRol.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<Rol>> call, Throwable t) {
                Toast.makeText(CrearUsuarioActivity.this, "Error roles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void crearUsuario() {
        String nombre   = etNombre.getText().toString().trim();
        String correo   = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("Ingresa el nombre"); etNombre.requestFocus(); return;
        }
        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo inválido"); etCorreo.requestFocus(); return;
        }
        if (password.length() < 8) {
            etPassword.setError("Mínimo 8 caracteres"); etPassword.requestFocus(); return;
        }

        if (rolesList.isEmpty()) {
            Toast.makeText(this, "Espera a que carguen los roles", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        int rolSeleccionado = rolesList.get(spRol.getSelectedItemPosition()).getRolId();

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setPassword(password);
        u.setTelefono(telefono);
        u.setRolId(rolSeleccionado);

        repo.crear(u).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("Guardar");
                if (response.isSuccessful()) {
                    Toast.makeText(CrearUsuarioActivity.this, "Usuario creado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CrearUsuarioActivity.this, "Error al crear usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("Guardar");
                Toast.makeText(CrearUsuarioActivity.this, "Sin conexión. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}