package sv.edu.agroconecta;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.animation.ObjectAnimator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import io.noties.markwon.Markwon;

public class ChatManager {

    private static final String API_KEY = "AIzaSyCniouH9ONUtzpARHttjGT6bw-RjXS03W8";
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + API_KEY;

    // Colores extraídos del tema de la app
    private static final int COLOR_VERDE = 0xFF25632D;
    private static final int COLOR_CREMA = 0xFFF7F2E8;
    private static final int COLOR_BLANCO = 0xFFFFFFFF;
    private static final int COLOR_TEXTO = 0xFF1A1A18;
    private static final int COLOR_TEXTO_S = 0xFF5A5A50;

    private Activity activity;
    private View panelChat;
    private LinearLayout contenedorMensajes;
    private ScrollView scrollChat;
    private EditText etMensaje;
    private Markwon markwon;
    private OkHttpClient client = new OkHttpClient();

    private List<JSONObject> historial = new ArrayList<>();
    private static final String SYSTEM_PROMPT =
            "Eres AgroBot, el asistente agrícola de AgroConecta en El Salvador. " +
            "Ayuda con preguntas sobre productos agrícolas, precios de mercado, cultivos, temporadas de cosecha, " +
            "manejo de plagas, abonos y fincas salvadoreñas. " +
            "Responde siempre en español, de manera amigable, concisa y con emojis relevantes. " +
            "Recuerda el contexto de la conversación. Si no sabes algo, dilo honestamente.";

    public ChatManager(Activity activity, ViewGroup rootView) {
        this.activity = activity;

        LayoutInflater inflater = LayoutInflater.from(activity);
        View chatView = inflater.inflate(R.layout.chat_flotante, rootView, false);
        rootView.addView(chatView);

        panelChat = chatView.findViewById(R.id.panelChat);
        contenedorMensajes = chatView.findViewById(R.id.contenedorMensajes);
        scrollChat = chatView.findViewById(R.id.scrollChat);
        etMensaje = chatView.findViewById(R.id.etMensaje);

        markwon = Markwon.create(activity);

        // FAB abrir/cerrar
        chatView.findViewById(R.id.fabChat).setOnClickListener(v -> {
            boolean visible = panelChat.getVisibility() == View.VISIBLE;
            if (visible) {
                panelChat.animate().alpha(0f).translationY(40f).setDuration(200)
                        .withEndAction(() -> panelChat.setVisibility(View.GONE)).start();
            } else {
                panelChat.setVisibility(View.VISIBLE);
                panelChat.setAlpha(0f);
                panelChat.setTranslationY(40f);
                panelChat.animate().alpha(1f).translationY(0f).setDuration(250).start();
            }
        });

        chatView.findViewById(R.id.btnCerrarChat).setOnClickListener(v -> {
            panelChat.animate().alpha(0f).translationY(40f).setDuration(200)
                    .withEndAction(() -> panelChat.setVisibility(View.GONE)).start();
        });

        chatView.findViewById(R.id.btnEnviar).setOnClickListener(v -> {
            String mensaje = etMensaje.getText().toString().trim();
            if (!mensaje.isEmpty()) {
                enviarMensaje(mensaje);
                etMensaje.setText("");
            }
        });

        // Mensaje de bienvenida
        agregarMensaje("¡Hola! 👋 Soy **AgroBot**, tu asistente agrícola de AgroConecta.\n\n" +
                "Puedo ayudarte con:\n• 🌽 Precios de productos\n• 🌱 Consejos de cultivo\n• 📅 Temporadas de cosecha\n• 🐛 Manejo de plagas\n\n¿En qué te puedo ayudar hoy?", false);
    }

    private void enviarMensaje(String texto) {
        agregarMensaje(texto, true);
        agregarTypingIndicator();

        try {
            JSONObject userMsg = new JSONObject();
            JSONArray userParts = new JSONArray();
            userParts.put(new JSONObject().put("text", texto));
            userMsg.put("role", "user");
            userMsg.put("parts", userParts);
            historial.add(userMsg);

            JSONArray contentsArray = new JSONArray();

            // System prompt
            JSONObject systemMsg = new JSONObject();
            JSONArray systemParts = new JSONArray();
            systemParts.put(new JSONObject().put("text", SYSTEM_PROMPT));
            systemMsg.put("role", "user");
            systemMsg.put("parts", systemParts);
            contentsArray.put(systemMsg);

            JSONObject systemResp = new JSONObject();
            JSONArray systemRespParts = new JSONArray();
            systemRespParts.put(new JSONObject().put("text", "Entendido. Soy AgroBot de AgroConecta, listo para ayudar."));
            systemResp.put("role", "model");
            systemResp.put("parts", systemRespParts);
            contentsArray.put(systemResp);

            for (JSONObject msg : historial) {
                contentsArray.put(msg);
            }

            JSONObject body = new JSONObject();
            body.put("contents", contentsArray);

            RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(URL).post(requestBody).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        quitarTypingIndicator();
                        agregarMensaje("⚠️ Sin conexión. Verifica tu internet e intenta de nuevo.", false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) {
                        activity.runOnUiThread(() -> {
                            quitarTypingIndicator();
                            agregarMensaje("⚠️ Sin respuesta del servidor. Intenta de nuevo.", false);
                        });
                        return;
                    }
                    String res = response.body().string();
                    activity.runOnUiThread(() -> {
                        quitarTypingIndicator();
                        try {
                            JSONObject json = new JSONObject(res);
                            String respuesta = json
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            try {
                                JSONObject modelMsg = new JSONObject();
                                JSONArray modelParts = new JSONArray();
                                modelParts.put(new JSONObject().put("text", respuesta));
                                modelMsg.put("role", "model");
                                modelMsg.put("parts", modelParts);
                                historial.add(modelMsg);
                            } catch (Exception ignored) {}

                            agregarMensaje(respuesta, false);
                        } catch (Exception e) {
                            agregarMensaje("⚠️ Error procesando la respuesta. Intenta de nuevo.", false);
                        }
                    });
                }
            });

        } catch (Exception e) {
            quitarTypingIndicator();
            agregarMensaje("⚠️ Error: " + e.getMessage(), false);
        }
    }

    private View typingView = null;

    private void agregarTypingIndicator() {
        TextView tv = new TextView(activity);
        tv.setText("✍️ Escribiendo…");
        tv.setPadding(20, 10, 20, 10);
        tv.setTextSize(13);
        tv.setTextColor(COLOR_TEXTO_S);
        tv.setTag("typing");

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadii(new float[]{24, 24, 24, 24, 4, 4, 24, 24});
        bg.setColor(COLOR_CREMA);
        tv.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);
        params.gravity = android.view.Gravity.START;
        tv.setLayoutParams(params);

        typingView = tv;
        contenedorMensajes.addView(tv);
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private void quitarTypingIndicator() {
        if (typingView != null) {
            contenedorMensajes.removeView(typingView);
            typingView = null;
        }
    }

    private void agregarMensaje(String texto, boolean esUsuario) {
        TextView tv = new TextView(activity);

        if (!esUsuario) {
            markwon.setMarkdown(tv, texto);
        } else {
            tv.setText(texto);
        }

        tv.setPadding(20, 12, 20, 12);
        tv.setTextSize(13.5f);
        tv.setLineSpacing(0, 1.4f);

        // Burbuja con bordes redondeados asimétricos
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);

        if (esUsuario) {
            bg.setCornerRadii(new float[]{24, 24, 4, 4, 24, 24, 24, 24});
            bg.setColor(COLOR_VERDE);
            tv.setTextColor(COLOR_BLANCO);
        } else {
            bg.setCornerRadii(new float[]{4, 4, 24, 24, 24, 24, 24, 24});
            bg.setColor(COLOR_BLANCO);
            bg.setStroke(2, 0xFFE0DDD5);
            tv.setTextColor(COLOR_TEXTO);
        }

        tv.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);

        if (esUsuario) {
            params.gravity = android.view.Gravity.END;
            params.setMarginStart(60);
        } else {
            params.gravity = android.view.Gravity.START;
            params.setMarginEnd(60);
        }

        tv.setLayoutParams(params);

        // Animación de entrada
        tv.setAlpha(0f);
        tv.setTranslationY(10f);
        contenedorMensajes.addView(tv);
        tv.animate().alpha(1f).translationY(0f).setDuration(200).start();

        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }
}
