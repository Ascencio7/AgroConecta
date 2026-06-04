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
import com.google.firebase.messaging.FirebaseMessaging;
import sv.edu.agroconecta.utils.FCMHelper;


public class LoginActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    AuthApi authApi;
    SessionManager sessionManager;

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

        // Enlace hacia la actividad de registro de los usuarios
        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
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
                    sessionManager.createSession(user.getUsuarioId(), user.getNombre(), user.getRol());

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