package sv.edu.agroconecta.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.agroconecta.R;

public class ContactarVendedorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactar_vendedor);

        String nombreVendedor = getIntent().getStringExtra("nombre_vendedor");
        String telefono       = getIntent().getStringExtra("telefono");
        String nombreProducto = getIntent().getStringExtra("nombre_producto");
        String metodosPago    = getIntent().getStringExtra("metodos_pago");
        double latitud        = getIntent().getDoubleExtra("latitud", 0);
        double longitud       = getIntent().getDoubleExtra("longitud", 0);

        // Usar variables final para los lambdas
        final String nombreV = (nombreVendedor != null && !nombreVendedor.isEmpty()) ? nombreVendedor : "Vendedor";
        final String tel     = (telefono != null)       ? telefono       : "";
        final String prod    = (nombreProducto != null) ? nombreProducto : "Producto";
        final String pagos   = (metodosPago != null)    ? metodosPago    : "💵 Efectivo";

        // Avatar inicial
        TextView tvAvatar = findViewById(R.id.tvAvatarVendedor);
        tvAvatar.setText(String.valueOf(nombreV.charAt(0)).toUpperCase());

        ((TextView) findViewById(R.id.tvNombreVendedorContact)).setText(nombreV);
        ((TextView) findViewById(R.id.tvProductoContact)).setText("Producto: " + prod);
        ((TextView) findViewById(R.id.tvMetodosPago)).setText(pagos);

        findViewById(R.id.btnBackContactar).setOnClickListener(v -> finish());

        // WhatsApp
        Button btnWA = findViewById(R.id.btnContactarWhatsApp);
        if (tel.isEmpty()) {
            btnWA.setEnabled(false);
            btnWA.setText("💬 WhatsApp no disponible");
        } else {
            btnWA.setOnClickListener(v -> {
                String num = tel.replaceAll("[^0-9]", "");
                String msg = Uri.encode("Hola, estoy interesado en tu producto \"" + prod + "\" de AgroConecta. ¿Sigue disponible?");
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/503" + num + "?text=" + msg));
                try { startActivity(i); }
                catch (Exception e) {
                    Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Llamada
        Button btnLlamar = findViewById(R.id.btnContactarLlamada);
        if (tel.isEmpty()) {
            btnLlamar.setEnabled(false);
            btnLlamar.setText("📞 Teléfono no disponible");
        } else {
            btnLlamar.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel))));
        }

        // Ver en mapa
        Button btnMapa = findViewById(R.id.btnVerEnMapa);
        if (latitud == 0 && longitud == 0) {
            btnMapa.setEnabled(false);
            btnMapa.setText("🗺️ Ubicación no disponible");
        } else {
            final double lat = latitud, lon = longitud;
            btnMapa.setOnClickListener(v -> {
                Uri geoUri = Uri.parse("geo:" + lat + "," + lon +
                        "?q=" + lat + "," + lon + "(" + Uri.encode(nombreV) + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    String url = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            });
        }
    }
}
