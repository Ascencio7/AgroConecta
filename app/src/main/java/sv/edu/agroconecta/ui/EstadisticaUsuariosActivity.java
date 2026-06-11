package sv.edu.agroconecta.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.UsuarioApi;
import sv.edu.agroconecta.utils.SessionManager;

public class EstadisticaUsuariosActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "downloads_channel";
    private PieChart pieChart;
    private Spinner spFiltroEstado;
    private UsuarioApi usuarioApi;
    private List<Usuario> allUsuarios = new ArrayList<>();
    private String currentFilter = "Todos";
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadistica_usuarios);

        sessionManager = new SessionManager(this);
        pieChart = findViewById(R.id.pieChart);
        spFiltroEstado = findViewById(R.id.spFiltroEstado);
        usuarioApi = ApiClient.getClient().create(UsuarioApi.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDescargarPDF).setOnClickListener(v -> generatePDF());

        createNotificationChannel();
        setupSpinner();
        loadData();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Descargas", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void setupSpinner() {
        String[] options = {"Todos", "Activos", "Inactivos"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltroEstado.setAdapter(adapter);

        spFiltroEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = options[position];
                updateChart(currentFilter);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadData() {
        usuarioApi.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsuarios = response.body();
                    updateChart(currentFilter);
                }
            }
            @Override public void onFailure(Call<List<Usuario>> call, Throwable t) {
                Toast.makeText(EstadisticaUsuariosActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateChart(String filter) {
        int admins = 0, vendedores = 0, clientes = 0;

        for (Usuario u : allUsuarios) {
            boolean matches = filter.equals("Todos") || 
                             (filter.equals("Activos") && u.isActivo()) || 
                             (filter.equals("Inactivos") && u.isInactivo());

            if (matches) {
                String r = u.getRol() != null ? u.getRol().toLowerCase() : "";
                if (r.contains("admin") || u.getRolId() == 1) admins++;
                else if (r.contains("vendedor") || u.getRolId() == 2) vendedores++;
                else clientes++;
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (admins > 0) entries.add(new PieEntry(admins, "Admins"));
        if (vendedores > 0) entries.add(new PieEntry(vendedores, "Vendedores"));
        if (clientes > 0) entries.add(new PieEntry(clientes, "Clientes"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));
        
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Usuarios\n" + filter);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void generatePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 1. Logo
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logoapp);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 60, 60, false);
            canvas.drawBitmap(scaledLogo, 40, 40, paint);
        }

        // 2. AgroConecta
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.parseColor("#25632D"));
        canvas.drawText("AgroConecta", 110, 65, titlePaint);

        // 3. Fecha y Hora (12h format)
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        String dateTime = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generado: " + dateTime, 110, 85, paint);

        // Creado por
        String adminName = sessionManager.getNombre() != null ? sessionManager.getNombre() : "Administrador";
        canvas.drawText("Creado por: " + adminName, 110, 100, paint);

        // Linea divisora
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        canvas.drawLine(40, 120, 555, 120, paint);

        // Título del reporte
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Estadísticas de Usuarios (" + currentFilter + ")", 40, 150, paint);

        // --- AGREGAR GRÁFICA ---
        Bitmap chartBitmap = pieChart.getChartBitmap();
        if (chartBitmap != null) {
            Bitmap scaledChart = Bitmap.createScaledBitmap(chartBitmap, 400, 300, false);
            canvas.drawBitmap(scaledChart, (595 - 400) / 2f, 180, paint);
        }

        // Datos numéricos
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        int y = 520;
        
        int admins = 0, vendedores = 0, clientes = 0;
        for (Usuario u : allUsuarios) {
            boolean matches = currentFilter.equals("Todos") || 
                             (currentFilter.equals("Activos") && u.isActivo()) || 
                             (currentFilter.equals("Inactivos") && u.isInactivo());
            if (matches) {
                String r = u.getRol() != null ? u.getRol().toLowerCase() : "";
                if (r.contains("admin") || u.getRolId() == 1) admins++;
                else if (r.contains("vendedor") || u.getRolId() == 2) vendedores++;
                else clientes++;
            }
        }
        int total = admins + vendedores + clientes;
        
        canvas.drawText("Resumen Estadístico:", 40, y, paint); y += 30;
        canvas.drawText("Total de usuarios analizados: " + total, 40, y, paint); y += 25;
        if (total > 0) {
            canvas.drawText("- Administradores: " + admins + " (" + (admins * 100 / total) + "%)", 60, y, paint); y += 25;
            canvas.drawText("- Vendedores: " + vendedores + " (" + (vendedores * 100 / total) + "%)", 60, y, paint); y += 25;
            canvas.drawText("- Clientes: " + clientes + " (" + (clientes * 100 / total) + "%)", 60, y, paint); y += 25;
        }

        // 4. Pagina n de n
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        canvas.drawText("Página 1 de 1", 500, 810, paint);

        pdfDocument.finishPage(page);

        // Guardar archivo
        String fileName = "Estadisticas_Usuarios.pdf";
        savePdfToDownloads(pdfDocument, fileName);
    }

    private void savePdfToDownloads(PdfDocument pdfDocument, String fileName) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri fileUri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            }

            if (fileUri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
                pdfDocument.writeTo(outputStream);
                if (outputStream != null) outputStream.close();
                
                showDownloadNotification(fileName, fileUri);
                Toast.makeText(this, "Reporte guardado en descargas", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    private void showDownloadNotification(String fileName, Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Estadísticas de Usuarios")
                .setContentText("Guardado: " + fileName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
