package sv.edu.agroconecta.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import sv.edu.agroconecta.R;

public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Fincas de ejemplo — luego puedes cargarlas desde tu API
    private static final double[][] FINCAS = {
            {13.6929, -89.2182, 0}, // San Salvador
            {13.7034, -89.2245, 1},
            {13.6850, -89.2100, 2},
    };
    private static final String[] NOMBRES_FINCAS = {
            "Finca El Roble",
            "Finca La Esperanza",
            "Finca San José"
    };
    private static final String[] PRODUCTOS_FINCAS = {
            "Maíz, Frijol",
            "Café, Caña",
            "Tomate, Chile"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fabMiUbicacion).setOnClickListener(v -> irAMiUbicacion());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Agregar marcadores de fincas
        for (int i = 0; i < FINCAS.length; i++) {
            LatLng pos = new LatLng(FINCAS[i][0], FINCAS[i][1]);
            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(NOMBRES_FINCAS[i])
                    .snippet("Productos: " + PRODUCTOS_FINCAS[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN)));
        }

        // Centrar en El Salvador
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(13.6929, -89.2182), 12));

        // Activar ubicación si tiene permiso
        habilitarMiUbicacion();
    }

    private void habilitarMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void irAMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng mi = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mi, 15));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            habilitarMiUbicacion();
        }
    }
}