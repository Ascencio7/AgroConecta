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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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
import sv.edu.agroconecta.modelo.Categoria;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.CategoriaApi;
import sv.edu.agroconecta.network.ProductApi;
import sv.edu.agroconecta.utils.SessionManager;

public class EstadisticaProductosActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "downloads_channel";
    private BarChart barChart;
    private Spinner spFiltroCategoria;
    private ProductApi productApi;
    private CategoriaApi categoriaApi;
    private List<Product> allProducts = new ArrayList<>();
    private List<Categoria> allCategories = new ArrayList<>();
    private int currentCategoryId = -1;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadistica_productos);

        sessionManager = new SessionManager(this);
        barChart = findViewById(R.id.barChart);
        spFiltroCategoria = findViewById(R.id.spFiltroCategoria);
        productApi = ApiClient.getClient().create(ProductApi.class);
        categoriaApi = ApiClient.getClient().create(CategoriaApi.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDescargarPDF).setOnClickListener(v -> generatePDF());

        createNotificationChannel();
        loadCategories();
        loadProducts();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Descargas", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void loadCategories() {
        categoriaApi.getCategorias().enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();
                    setupSpinner();
                }
            }
            @Override public void onFailure(Call<List<Categoria>> call, Throwable t) {}
        });
    }

    private void setupSpinner() {
        List<String> names = new ArrayList<>();
        names.add("Todas");
        for (Categoria c : allCategories) names.add(c.getNombre());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltroCategoria.setAdapter(adapter);

        spFiltroCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) currentCategoryId = -1;
                else currentCategoryId = allCategories.get(position - 1).getCategoriaId();
                updateChart(currentCategoryId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadProducts() {
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body();
                    updateChart(currentCategoryId);
                }
            }
            @Override public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
    }

    private void updateChart(int categoryId) {
        int disponible = 0, agotado = 0;

        for (Product p : allProducts) {
            if (categoryId == -1 || p.getCategoriaId() == categoryId) {
                if (p.getExistencia() > 0) disponible++;
                else agotado++;
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, disponible));
        entries.add(new BarEntry(1, agotado));

        BarDataSet dataSet = new BarDataSet(entries, "Estado del Stock");
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"));
        dataSet.setValueTextSize(12f);
        
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Disponible", "Agotado"}));
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.animateY(1000);
        barChart.invalidate();
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
        String catName = "Todas";
        if (currentCategoryId != -1) {
            for (Categoria c : allCategories) if (c.getCategoriaId() == currentCategoryId) catName = c.getNombre();
        }
        canvas.drawText("Estadísticas de Productos (Categoría: " + catName + ")", 40, 150, paint);

        // --- AGREGAR GRÁFICA ---
        Bitmap chartBitmap = barChart.getChartBitmap();
        if (chartBitmap != null) {
            Bitmap scaledChart = Bitmap.createScaledBitmap(chartBitmap, 400, 300, false);
            canvas.drawBitmap(scaledChart, (595 - 400) / 2f, 180, paint);
        }

        // Datos
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        int y = 520;
        
        int disponible = 0, agotado = 0;
        for (Product p : allProducts) {
            if (currentCategoryId == -1 || p.getCategoriaId() == currentCategoryId) {
                if (p.getExistencia() > 0) disponible++;
                else agotado++;
            }
        }
        int total = disponible + agotado;
        
        canvas.drawText("Resumen de Inventario:", 40, y, paint); y += 30;
        canvas.drawText("Total de productos en esta categoría: " + total, 40, y, paint); y += 25;
        if (total > 0) {
            canvas.drawText("- Disponibles: " + disponible + " (" + (disponible * 100 / total) + "%)", 60, y, paint); y += 25;
            canvas.drawText("- Agotados: " + agotado + " (" + (agotado * 100 / total) + "%)", 60, y, paint); y += 25;
        }

        // 4. Pagina n de n
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        canvas.drawText("Página 1 de 1", 500, 810, paint);

        pdfDocument.finishPage(page);

        // Guardar archivo
        String fileName = "Estadisticas_Productos.pdf";
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
                .setContentTitle("Estadísticas de Productos")
                .setContentText("Guardado: " + fileName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
