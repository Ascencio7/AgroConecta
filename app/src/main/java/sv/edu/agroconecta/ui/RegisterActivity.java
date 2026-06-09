package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;

public class RegisterActivity extends AppCompatActivity {

    EditText etNombre, etCorreo, etPassword, etTelefono;
    Spinner spinnerRol;
    Button btnRegistrar, btnCancelar;

    UsuarioApi usuarioApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);

        etCorreo = findViewById(R.id.etCorreo);
        limpiarError(etCorreo, "correo"); // Valida el campo en tiempo real

        etPassword = findViewById(R.id.etPassword);
        limpiarError(etPassword, "password"); // Valida el campo en tiempo real

        etTelefono = findViewById(R.id.etTelefono);
        limpiarError(etTelefono, "telefono"); // Valida el campo en tiempo real

        spinnerRol = findViewById(R.id.spinnerRol);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnCancelar = findViewById(R.id.btnCancelar);

        usuarioApi = ApiClient.getClient().create(UsuarioApi.class);

        // Opciones del spinner
        String[] roles = {"Cliente", "Vendedor"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerRol.setAdapter(adapter);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        // Boton de cancelar
        btnCancelar.setOnClickListener(v ->{
            new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                    .setTitle("Cancelar registro")
                    .setMessage("¿Estás seguro que deseas cancelar?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        finish(); // regresa al Login
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

    }

    // Metodo para limpiar los errores y validar los campos en tiempo real
    private void limpiarError(EditText campo, String tipo){
        campo.addTextChangedListener(new android.text.TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){
                String valor = s.toString().trim();

                switch (tipo){

                    case "correo":
                        if(!valor.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(valor).matches()){
                            campo.setError("Correo inválido");
                        } else {
                            campo.setError(null);
                        }
                        break;

                    case "password":
                        if(!valor.isEmpty() && valor.length() < 8){
                            campo.setError("Mínimo 8 caracteres");
                        } else {
                            campo.setError(null);
                        }
                        break;

                    case "telefono":
                        if(!valor.isEmpty() && valor.length() < 8){
                            campo.setError("Teléfono inválido");
                        } else {
                            campo.setError(null);
                        }
                        break;
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s){}
        });
    }

    private void registrarUsuario() {

        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        // Validaciones para validar los campos al registrar
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
            etCorreo.setError("Correo inválido");
            return;
        }

        if(password.length() < 8){
            etPassword.setError("Mínimo de 8 caracteres");
            return;
        }

        String soloDigitosReg = telefono.replaceAll("[^0-9]", "");
        if (soloDigitosReg.length() != 8) {
            etTelefono.setError("Debe tener 8 dígitos");
            etTelefono.requestFocus();
            return;
        }
        if (!soloDigitosReg.matches("^[2678]\\d{7}$")) {
            etTelefono.setError("Número salvadoreño inválido (empieza en 2,6,7 u 8)");
            etTelefono.requestFocus();
            return;
        }

        int rolId;
        String tipo = spinnerRol.getSelectedItem().toString();

        if (tipo.equals("Vendedor")) {
            rolId = 2;
        } else {
            rolId = 3; // Cliente
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPassword(password);
        usuario.setTelefono(telefono);
        usuario.setRolId(rolId);

        btnRegistrar.setEnabled(false);

        usuarioApi.createUsuario(usuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {

                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Registrarse");

                // Condicion para validar si el correo ingresado ya existe y le impide registrarse
                if(response.isSuccessful()){
                    Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    try{
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "";
                        if(error.toLowerCase().contains("email_exists")){
                            etCorreo.setError("El correo ya esta en uso");
                            etCorreo.requestFocus();
                        }else{
                            Toast.makeText(RegisterActivity.this, "Error al registrarse", Toast.LENGTH_SHORT).show();
                        }

                    }catch (Exception e){
                        Toast.makeText(RegisterActivity.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Registrarse");
                Toast.makeText(RegisterActivity.this,
                        "Sin conexión. Verifica tu red.",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }
}