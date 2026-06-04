package sv.edu.agroconecta.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.ui.LoginActivity;

public class AgroConectaFirebaseService extends FirebaseMessagingService {

    private static final String CHANNEL_PEDIDOS  = "pedidos_channel";
    private static final String CHANNEL_CHAT     = "chat_channel";
    private static final String CHANNEL_PRODUCTOS = "productos_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo  = "AgroConecta";
        String cuerpo  = "Tienes una notificación nueva";
        String tipo    = "general";

        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            cuerpo = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("titulo"))
                titulo = remoteMessage.getData().get("titulo");
            if (remoteMessage.getData().containsKey("cuerpo"))
                cuerpo = remoteMessage.getData().get("cuerpo");
            if (remoteMessage.getData().containsKey("tipo"))
                tipo = remoteMessage.getData().get("tipo");
        }

        mostrarNotificacion(titulo, cuerpo, tipo);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Aquí guardarías el token en el backend para enviar notificaciones
        // al usuario específico
    }

    private void mostrarNotificacion(String titulo, String cuerpo, String tipo) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId;
        int icono;
        switch (tipo) {
            case "pedido":
                channelId = CHANNEL_PEDIDOS;
                icono = R.mipmap.ic_launcher;
                break;
            case "chat":
                channelId = CHANNEL_CHAT;
                icono = R.mipmap.ic_launcher;
                break;
            default:
                channelId = CHANNEL_PRODUCTOS;
                icono = R.mipmap.ic_launcher;
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_PEDIDOS, "Pedidos", NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_CHAT, "Mensajes", NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_PRODUCTOS, "Productos", NotificationManager.IMPORTANCE_DEFAULT));
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(icono)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
