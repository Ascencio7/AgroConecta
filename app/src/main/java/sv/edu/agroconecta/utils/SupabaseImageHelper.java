package sv.edu.agroconecta.utils;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseImageHelper {

    private static final String TAG          = "SupabaseImg";
    public  static final String SUPABASE_URL = "https://dkyaxrfnlstsmdqizugk.supabase.co";
    public  static final String SUPABASE_ANON= "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRreWF4cmZubHN0c21kcWl6dWdrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYyNzcxMDQsImV4cCI6MjA5MTg1MzEwNH0.UmindpU1oTGl2YsvOQgHqIZT7Ml5Ks4pBs1yeac8y7g";
    private static final String BUCKET       = "productos";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    public interface UrlCallback {
        void onResult(String url);
    }

    // ── 1. Subir bytes al Storage ─────────────────────────────────────────
    public static void subirImagen(byte[] bytes, UploadCallback cb) {
        new Thread(() -> {
            try {
                String fileName  = UUID.randomUUID().toString() + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + fileName;

                Log.d(TAG, "Subiendo " + bytes.length + " bytes a: " + uploadUrl);

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON)
                        .addHeader("Content-Type", "image/jpeg")
                        .addHeader("x-upsert", "true")
                        .build();

                Response response = client.newCall(request).execute();
                int code = response.code();
                String body = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "Upload response code: " + code);
                Log.d(TAG, "Upload response body: " + body);

                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/"
                            + BUCKET + "/" + fileName;
                    Log.d(TAG, "URL pública: " + publicUrl);
                    cb.onSuccess(publicUrl);
                } else {
                    Log.e(TAG, "Error upload " + code + ": " + body);
                    cb.onError("Error " + code + ": " + body);
                }
            } catch (Exception e) {
                Log.e(TAG, "Excepción al subir imagen", e);
                cb.onError(e.getMessage() != null ? e.getMessage() : "Error desconocido");
            }
        }).start();
    }

    // ── 2. Guardar URL en tabla imagenes_productos ───────────────────────
    public static void guardarUrlEnTabla(int productoId, String url) {
        // Nunca guardar URIs locales
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            Log.e(TAG, "URL inválida, no se guarda: " + url);
            return;
        }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("producto_id", productoId);
                body.put("url", url);

                Log.d(TAG, "Guardando en imagenes_productos: productoId=" + productoId + " url=" + url);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/imagenes_productos")
                        .post(RequestBody.create(body.toString(),
                                MediaType.parse("application/json")))
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON)
                        .addHeader("apikey", SUPABASE_ANON)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build();

                Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "guardarUrl response " + response.code() + ": " + respBody);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Error guardando URL en BD: " + response.code() + " " + respBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "Excepción guardando URL", e);
            }
        }).start();
    }

    // ── 3. Obtener URL de imagen por producto_id ─────────────────────────
    public static void obtenerUrl(int productoId, UrlCallback cb) {
        new Thread(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/imagenes_productos"
                        + "?producto_id=eq." + productoId
                        + "&select=url&order=imagen_id.desc&limit=1";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON)
                        .addHeader("apikey", SUPABASE_ANON)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JSONArray arr = new JSONArray(json);
                    if (arr.length() > 0) {
                        String imageUrl = arr.getJSONObject(0).getString("url");
                        // Solo retornar URLs válidas, nunca content://
                        if (imageUrl.startsWith("http")) {
                            cb.onResult(imageUrl);
                            return;
                        }
                    }
                }
                cb.onResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo URL", e);
                cb.onResult(null);
            }
        }).start();
    }
}
