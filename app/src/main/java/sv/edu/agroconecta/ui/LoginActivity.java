package sv.edu.agroconecta.ui;
// Imports para notificaciones
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// Imports para notificaciones
import androidx.core.app.NotificationCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.MainActivity;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.LoginResponse;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.AuthApi;
// Import para el admin de las sesiones
import sv.edu.agroconecta.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;
import sv.edu.agroconecta.utils.FCMHelper;


public class LoginActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    AuthApi authApi;
    SessionManager sessionManager;
    GoogleSignInClient googleSignInClient;
    FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si ya hay una sesión activa, redirigir al dashboard correspondiente
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard(sessionManager.getRol(), sessionManager.getNombre(), sessionManager.getUserId());
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        TextView txtRegister = findViewById(R.id.txtRegister);
        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);

        // Enlace hacia la actividad de registro de los usuarios
        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Enlace hacia la actividad de restablecer contraseña
        txtForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Mostrara el campo con el error y obtendra el mensaje para ser mostrado
        etEmail.setOnFocusChangeListener((v, hasFocus) ->{
            if (hasFocus) etEmail.setError(null);
        });

        // Mostrara el campo con el error y obtendra el mensaje para ser mostrado
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etPassword.setError(null);
        });

        authApi = ApiClient.getClient().create(AuthApi.class);
        firebaseAuth = FirebaseAuth.getInstance();

        // Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Buscar botón de Google en el layout
        android.view.View btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) btnGoogle.setOnClickListener(v -> iniciarGoogleSignIn());

        btnLogin.setOnClickListener(v -> loginUser());

        // Verificar permisos de notificación para Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM_TOKEN", "Token: " + token);
                    }
                });
    }

    private void iniciarGoogleSignIn() {
        // Forzamos el cierre de sesión previo para limpiar el estado y que aparezca el selector
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Log.d("GOOGLE_AUTH", "Result Code: " + resultCode);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken(), account);
            } catch (ApiException e) {
                int code = e.getStatusCode();
                Log.e("GOOGLE_AUTH", "Error Técnico: " + code, e);
                String msg = "Error: " + code;
                if (code == 12501) msg = "Inicio de sesión cancelado.";
                else if (code == 10) msg = "Error de Desarrollador (Verifica SHA-1).";
                else if (code == 7) msg = "Error de red.";
                
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount account) {
        btnLogin.setEnabled(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    String nombre = account.getDisplayName() != null ? account.getDisplayName() : user.getEmail();
                    String correo = user.getEmail();
                    String foto   = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
                    final String fotoFinal = foto;
                    // Buscar o crear usuario en la BD
                    buscarOCrearUsuarioGoogle(nombre, correo, fotoFinal);
                }
            } else {
                btnLogin.setEnabled(true);
                Toast.makeText(this, "Error con Google Sign-In", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buscarOCrearUsuarioGoogle(String nombre, String correo, String foto) {
        sv.edu.agroconecta.network.UsuarioApi usuarioApi =
            sv.edu.agroconecta.network.ApiClient.getClient()
                .create(sv.edu.agroconecta.network.UsuarioApi.class);

        // Buscar si ya existe en la BD
        usuarioApi.getUsuarios().enqueue(new Callback<java.util.List<sv.edu.agroconecta.model.Usuario>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<sv.edu.agroconecta.model.Usuario>> call,
                                   retrofit2.Response<java.util.List<sv.edu.agroconecta.model.Usuario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (sv.edu.agroconecta.model.Usuario u : response.body()) {
                        if (correo.equalsIgnoreCase(u.getCorreo())) {
                            // Usuario ya existe — iniciar sesión
                            runOnUiThread(() -> {
                                String rolBackend = (u.getRol() != null) ? u.getRol() : "CLIENTE";
                                sessionManager.createSession(u.getUsuarioId(), nombre, correo, rolBackend);
                                if (foto != null) sessionManager.setFotoPerfil(foto);
                                showWelcomeNotification(nombre);
                                navigateToDashboard(rolBackend, nombre, u.getUsuarioId());
                                finish();
                            });
                            return;
                        }
                    }
                }
                // No existe — crear usuario nuevo con rol CLIENTE (3)
                crearUsuarioGoogle(nombre, correo, foto, usuarioApi);
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.List<sv.edu.agroconecta.model.Usuario>> call, Throwable t) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void crearUsuarioGoogle(String nombre, String correo, String foto,
                                     sv.edu.agroconecta.network.UsuarioApi usuarioApi) {
        // Preguntar qué rol quiere antes de crear
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Cómo quieres usar AgroConecta?")
            .setMessage("Selecciona tu rol para continuar")
            .setCancelable(false)
            .setPositiveButton("🛒 Soy Comprador", (d, w) ->
                registrarConGoogle(nombre, correo, foto, usuarioApi, 3))
            .setNegativeButton("🌾 Soy Vendedor", (d, w) ->
                registrarConGoogle(nombre, correo, foto, usuarioApi, 2))
            .show();
    }

    private void registrarConGoogle(String nombre, String correo, String foto,
                                     sv.edu.agroconecta.network.UsuarioApi usuarioApi, int rolId) {
        sv.edu.agroconecta.model.Usuario nuevoUsuario = new sv.edu.agroconecta.model.Usuario();
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setPassword("google_" + System.currentTimeMillis());
        nuevoUsuario.setRolId(rolId);

        usuarioApi.createUsuario(nuevoUsuario).enqueue(new Callback<sv.edu.agroconecta.model.Usuario>() {
            @Override
            public void onResponse(retrofit2.Call<sv.edu.agroconecta.model.Usuario> call,
                                   retrofit2.Response<sv.edu.agroconecta.model.Usuario> response) {
                // Buscar el ID del usuario recién creado
                usuarioApi.getUsuarios().enqueue(new Callback<java.util.List<sv.edu.agroconecta.model.Usuario>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.List<sv.edu.agroconecta.model.Usuario>> c2,
                                           retrofit2.Response<java.util.List<sv.edu.agroconecta.model.Usuario>> r2) {
                        int userId = -1;
                        if (r2.isSuccessful() && r2.body() != null) {
                            for (sv.edu.agroconecta.model.Usuario u : r2.body()) {
                                if (correo.equalsIgnoreCase(u.getCorreo())) {
                                    userId = u.getUsuarioId();
                                    break;
                                }
                            }
                        }
                        final int uid = userId;
                        runOnUiThread(() -> {
                            if (uid > 0) {
                                String rolStr = (rolId == 2) ? "VENDEDOR" : "CLIENTE";
                                sessionManager.createSession(uid, nombre, correo, rolStr);
                                if (foto != null) sessionManager.setFotoPerfil(foto);
                                showWelcomeNotification(nombre);
                                navigateToDashboard(rolStr, nombre, uid);
                                finish();
                            } else {
                                btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this,
                                    "Error al crear cuenta Google", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override public void onFailure(retrofit2.Call<java.util.List<sv.edu.agroconecta.model.Usuario>> c2, Throwable t) {
                        runOnUiThread(() -> { btnLogin.setEnabled(true); });
                    }
                });
            }
            @Override
            public void onFailure(retrofit2.Call<sv.edu.agroconecta.model.Usuario> call, Throwable t) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Error al registrar con Google", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loginUser() {
        String correo = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones para los campos
        if (correo.isEmpty()) {
            etEmail.setError("Ingresa tu correo");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
            etEmail.setError("Correo inválido");
            etEmail.requestFocus();
            return;
        }

        if(password.isEmpty()){
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return;
        }

        if(password.length() < 8){
            etPassword.setError("Minimo 8 caracteres");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        // Cambia el estado de texto del boton, mas estetico
        btnLogin.setText("Validando...");

        Call<LoginResponse> call = authApi.login(correo, password);

        call.enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");

                Log.e("LOGIN_DEBUG", "Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    LoginResponse user = response.body();

                    // Logs importantes para ver si todo fue correcto. Esto se ve en Logcats de Android, con el simbolo del gato :D
                    Log.e("LOGIN_DEBUG", "Body: " + user.toString());
                    Log.e("LOGIN_DEBUG", "Success: " + user.isSuccess());
                    Log.e("LOGIN_DEBUG", "Nombre: " + user.getNombre());
                    Log.e("LOGIN_DEBUG", "Rol: " + user.getRol());

                    // Validar el usuario
                    if (!user.isSuccess()) {
                        Toast.makeText(LoginActivity.this,
                                user.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(LoginActivity.this,
                            "Bienvenido " + user.getNombre(),
                            Toast.LENGTH_SHORT).show();

                    // Guardar la sesión para que se mantenga abierta
                    sessionManager.createSession(user.getUsuarioId(), user.getNombre(), user.getCorreo(), user.getRol());
                    // Guardar foto de perfil si el backend la devuelve
                    if (user.getFotoPerfil() != null && !user.getFotoPerfil().isEmpty()) {
                        sessionManager.setFotoPerfil(user.getFotoPerfil());
                    }

                    // Se muestra la notificiacion en la Barra de notificacion del celular,
                    // muestra un mensaje de bienvenida con el nombre de Usuario y AgroConecta
                    showWelcomeNotification(user.getNombre());

                    navigateToDashboard(user.getRol(), user.getNombre(), user.getUsuarioId());
                    finish();

                } else {

                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Error desconocido";

                        Log.e("LOGIN_DEBUG", "ErrorBody: " + error);

                        Toast.makeText(LoginActivity.this,
                                "Error del servidor",
                                Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this,
                                "Error al procesar respuesta",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");
                Toast.makeText(LoginActivity.this,
                        "Sin conexión. Verifica tu red.",
                        Toast.LENGTH_SHORT).show();
                Log.e("LOGIN", "ERROR:", t);
            }
        });
    }

    // Metodo privado para mostrar la notificacion
    private void showWelcomeNotification(String nombre) {
        String channelId = "login_channel";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notificaciones de Inicio de Sesión",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        // Se crea la notificacion que vera el usuario al iniciar la sesion exitosamente
        // El mensaje no caera al correo, sino de manera local
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¡Bienvenido, " + nombre + "!")
                .setContentText("Has iniciado sesión correctamente en AgroConecta.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }

    // Método para centralizar la navegación según el rol del usuario
    private void navigateToDashboard(String rol, String nombre, int usuarioId) {
        // Verificar si se deben mostrar los términos para este usuario específico
        if (sessionManager.shouldShowTerms(usuarioId)) {
            Intent intentTerms = new Intent(LoginActivity.this, TermsActivity.class);
            intentTerms.putExtra("nombre", nombre);
            intentTerms.putExtra("usuario_id", usuarioId);
            intentTerms.putExtra("rol", rol);
            startActivity(intentTerms);
            return;
        }

        String rolUpper = rol != null ? rol.toUpperCase() : "";
        Intent intent;

        switch (rolUpper) {
            case "ADMIN":
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                break;
            case "CLIENTE":
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
            case "VENDEDOR":
                intent = new Intent(LoginActivity.this, VendedorDashboardActivity.class);
                break;
            default:
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
        }

        intent.putExtra("nombre", nombre);
        intent.putExtra("usuario_id", usuarioId);
        intent.putExtra("rol", rol);
        // Guardar token FCM
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
            FCMHelper.guardarToken(String.valueOf(sessionManager.getUserId()), token));
        startActivity(intent);
    }
}