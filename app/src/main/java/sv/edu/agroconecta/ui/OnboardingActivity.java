package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import sv.edu.agroconecta.R;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnSiguiente;
    private TextView btnSaltar;
    private View dot1, dot2, dot3;

    private final String[] emojis  = {"🌾", "🛒", "📍"};
    private final String[] titulos = {
            "Conecta con agricultores",
            "Compra productos frescos",
            "Encuentra vendedores cerca"
    };
    private final String[] descs   = {
            "AgroConecta une a agricultores salvadoreños con compradores. Productos frescos directo del campo a tu mesa.",
            "Explora el catálogo, agrega al carrito y confirma tu pedido en segundos. Fácil, rápido y seguro.",
            "Usa el mapa para encontrar vendedores cerca de ti y contáctalos directamente por WhatsApp."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_onboarding);

        viewPager    = findViewById(R.id.viewPagerOnboarding);
        btnSiguiente = findViewById(R.id.btnOnboardingSiguiente);
        btnSaltar    = findViewById(R.id.btnOnboardingSaltar);
        dot1         = findViewById(R.id.dot1);
        dot2         = findViewById(R.id.dot2);
        dot3         = findViewById(R.id.dot3);

        viewPager.setAdapter(new OnboardingAdapter());
        viewPager.setPageTransformer(new ZoomOutTransformer());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarDots(position);
                if (position == 2) {
                    btnSiguiente.setText("¡Empezar!");
                    btnSaltar.setVisibility(View.GONE);
                } else {
                    btnSiguiente.setText("Siguiente");
                    btnSaltar.setVisibility(View.VISIBLE);
                }
            }
        });

        btnSiguiente.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < 2) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                irAlLogin();
            }
        });

        btnSaltar.setOnClickListener(v -> irAlLogin());
    }

    private void actualizarDots(int pos) {
        dot1.setBackgroundResource(pos == 0 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        dot2.setBackgroundResource(pos == 1 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        dot3.setBackgroundResource(pos == 2 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
    }

    private void irAlLogin() {
        // Marcar que ya vio el onboarding
        SharedPreferences prefs = getSharedPreferences("AgroPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_done", true).apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── Adapter ──────────────────────────────────────────────────
    class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvTitulo, tvDesc;
            VH(View v) {
                super(v);
                tvEmoji  = v.findViewById(R.id.tvOnboardingEmoji);
                tvTitulo = v.findViewById(R.id.tvOnboardingTitulo);
                tvDesc   = v.findViewById(R.id.tvOnboardingDesc);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.tvEmoji.setText(emojis[pos]);
            h.tvTitulo.setText(titulos[pos]);
            h.tvDesc.setText(descs[pos]);
        }

        @Override public int getItemCount() { return 3; }
    }

    // ── Zoom transition ───────────────────────────────────────────
    static class ZoomOutTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.90f;
        private static final float MIN_ALPHA = 0.7f;

        @Override
        public void transformPage(@NonNull View page, float pos) {
            if (pos < -1 || pos > 1) {
                page.setAlpha(MIN_ALPHA);
            } else if (pos <= 0 || pos <= 1) {
                float scale = Math.max(MIN_SCALE, 1 - Math.abs(pos) * (1 - MIN_SCALE));
                page.setScaleX(scale);
                page.setScaleY(scale);
                page.setAlpha(MIN_ALPHA + (scale - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            }
        }
    }
}
