package sv.edu.agroconecta;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class AgroConectaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Forzar modo claro en toda la aplicación
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
