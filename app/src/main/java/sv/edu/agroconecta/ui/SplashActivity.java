package sv.edu.agroconecta.ui;

import android.animation.AnimatorSet;
import sv.edu.agroconecta.MainActivity;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sin ActionBar en splash
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_splash);

        ImageView logo     = findViewById(R.id.ivSplashLogo);
        TextView nombre    = findViewById(R.id.tvSplashNombre);
        TextView slogan    = findViewById(R.id.tvSplashSlogan);
        ProgressBar progress = findViewById(R.id.progressSplash);

        // Animación logo: escala + fade in
        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.3f, 1f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.3f, 1f);
        ObjectAnimator fadeLog = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f);
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(scaleX, scaleY, fadeLog);
        logoAnim.setDuration(700);
        logoAnim.setInterpolator(new OvershootInterpolator(1.2f));

        // Animación nombre
        ObjectAnimator fadeNombre = ObjectAnimator.ofFloat(nombre, View.ALPHA, 0f, 1f);
        ObjectAnimator slideNombre = ObjectAnimator.ofFloat(nombre, View.TRANSLATION_Y, 30f, 0f);
        AnimatorSet nombreAnim = new AnimatorSet();
        nombreAnim.playTogether(fadeNombre, slideNombre);
        nombreAnim.setDuration(500);
        nombreAnim.setStartDelay(600);

        // Animación slogan
        ObjectAnimator fadeSlogan = ObjectAnimator.ofFloat(slogan, View.ALPHA, 0f, 1f);
        fadeSlogan.setDuration(400);
        fadeSlogan.setStartDelay(1000);

        // Animación progress
        ObjectAnimator fadeProgress = ObjectAnimator.ofFloat(progress, View.ALPHA, 0f, 1f);
        fadeProgress.setDuration(300);
        fadeProgress.setStartDelay(1200);

        // Ejecutar todo
        logoAnim.start();
        nombreAnim.start();
        fadeSlogan.start();
        fadeProgress.start();

        // Decidir a dónde ir después de 2.5 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            Intent intent;
            if (session.isLoggedIn()) {
                // Ya tiene sesión — ir directo al dashboard
                String rol = session.getRol();
                if ("ADMIN".equalsIgnoreCase(rol)) {
                    intent = new Intent(this, AdminDashboardActivity.class);
                } else if ("VENDEDOR".equalsIgnoreCase(rol)) {
                    intent = new Intent(this, VendedorDashboardActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
            } else {
                // Primera vez o no logueado — ver si ya vio onboarding
                boolean yaVioOnboarding = getSharedPreferences("AgroPrefs", MODE_PRIVATE)
                        .getBoolean("onboarding_done", false);
                if (yaVioOnboarding) {
                    intent = new Intent(this, LoginActivity.class);
                } else {
                    intent = new Intent(this, OnboardingActivity.class);
                }
            }

            startActivity(intent);
            // Transición suave
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}
