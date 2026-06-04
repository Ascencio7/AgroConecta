package sv.edu.agroconecta.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "AgroConectaSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "usuario_id";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_ROL = "rol";
    private static final String KEY_HIDE_TERMS = "hideTerms";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createSession(int userId, String nombre, String rol) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_NOMBRE, nombre);
        editor.putString(KEY_ROL, rol);
        editor.apply();
    }

    public void setHideTerms(int userId, boolean hide) {
        editor.putBoolean(KEY_HIDE_TERMS + "_" + userId, hide);
        editor.apply();
    }

    public boolean shouldShowTerms(int userId) {
        if (userId == -1) return true;
        return !pref.getBoolean(KEY_HIDE_TERMS + "_" + userId, false);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getNombre() {
        return pref.getString(KEY_NOMBRE, "");
    }

    public String getRol() {
        return pref.getString(KEY_ROL, "");
    }

    public void logout() {
        // Limpiar carrito en memoria antes de cerrar sesión
        sv.edu.agroconecta.utils.CarritoManager.getInstance().limpiar();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_NOMBRE);
        editor.remove(KEY_ROL);
        editor.apply();
    }
}