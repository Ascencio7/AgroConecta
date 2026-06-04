package sv.edu.agroconecta.utils;

import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class FCMHelper {

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "unbSPAjX3_gOjlvFHufp4moBm7PGILz97RQlF6cZNoo";
    private static final OkHttpClient client = new OkHttpClient();

    // Enviar notificación a un token específico
    public static void enviarNotificacion(String tokenDestino, String titulo,
                                          String cuerpo, String tipo) {
        try {
            JSONObject data = new JSONObject();
            data.put("titulo", titulo);
            data.put("cuerpo", cuerpo);
            data.put("tipo",   tipo);

            JSONObject notification = new JSONObject();
            notification.put("title", titulo);
            notification.put("body",  cuerpo);
            notification.put("sound", "default");

            JSONObject body = new JSONObject();
            body.put("to",           tokenDestino);
            body.put("notification", notification);
            body.put("data",         data);
            body.put("priority",     "high");

            RequestBody requestBody = RequestBody.create(
                    body.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(FCM_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e("FCM", "Error enviando notificacion: " + e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    Log.d("FCM", "Notificacion enviada: " + response.code());
                }
            });

        } catch (Exception e) {
            Log.e("FCM", "Error: " + e.getMessage());
        }
    }

    // Guardar token del usuario en Firebase Realtime Database
    public static void guardarToken(String userId, String token) {
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("tokens")
                    .child(userId)
                    .setValue(token);
        } catch (Exception e) {
            Log.e("FCM", "Error guardando token: " + e.getMessage());
        }
    }

    // Obtener token de un usuario y enviarle notificación
    public static void notificarUsuario(String userId, String titulo,
                                        String cuerpo, String tipo) {
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("tokens")
                .child(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String token = snapshot.getValue(String.class);
                        if (token != null && !token.isEmpty()) {
                            enviarNotificacion(token, titulo, cuerpo, tipo);
                        }
                    }
                });
    }
}
