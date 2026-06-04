package sv.edu.agroconecta.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.ui.LoginActivity;

public class NotificacionNuevoProductoService {

    private static final String CHANNEL_ID  = "nuevo_producto_channel";
    private static final String CHANNEL_NAME = "Nuevos Productos";
    private static int notifId = 1000;

    public static void enviarNotificacion(Context ctx, String nombreProducto) {
        NotificationManager manager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Alertas de nuevos productos en AgroConecta");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🌿 ¡Nuevo producto disponible!")
                .setContentText("\"" + nombreProducto + "\" ya está en el catálogo de AgroConecta")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Un vendedor acaba de publicar \"" + nombreProducto +
                                "\". ¡Entra al catálogo y consíguelo antes de que se agote!"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pending)
                .setAutoCancel(true);

        manager.notify(notifId++, builder.build());
    }
}
