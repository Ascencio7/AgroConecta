package sv.edu.agroconecta.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.GeneralResponse;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.AuthApi;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etNewPassword;
    private Button btnReset;
    private TextView txtBackToLogin;
    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnReset = findViewById(R.id.btnReset);
        txtBackToLogin = findViewById(R.id.txtBackToLogin);

        authApi = ApiClient.getClient().create(AuthApi.class);

        btnReset.setOnClickListener(v -> updatePassword());

        txtBackToLogin.setOnClickListener(v -> finish());
    }

    private void updatePassword() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Ingresa tu correo");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo inválido");
            etEmail.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("Ingresa la nueva contraseña");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError("Mínimo 8 caracteres");
            etNewPassword.requestFocus();
            return;
        }

        btnReset.setEnabled(false);
        btnReset.setText("Actualizando...");

        authApi.updatePassword(email, newPassword).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                btnReset.setEnabled(true);
                btnReset.setText("Actualizar contraseña");

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Contraseña actualizada exitosamente.",
                                Toast.LENGTH_LONG).show();
                        
                        showSuccessNotification();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                response.body().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Error en el servidor o el correo no existe.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                btnReset.setEnabled(true);
                btnReset.setText("Actualizar contraseña");
                Toast.makeText(ForgotPasswordActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessNotification() {
        String channelId = "password_channel";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notificaciones de Seguridad",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Seguridad AgroConecta")
                .setContentText("Tu contraseña ha sido actualizada correctamente.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(2, builder.build());
    }
}
