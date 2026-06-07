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
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.ProductApi;
import sv.edu.agroconecta.network.UsuarioApi;
import sv.edu.agroconecta.utils.SessionManager;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;

public class ReportesActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "downloads_channel";
    private UsuarioApi usuarioApi;
    private ProductApi productApi;
    private SessionManager sessionManager;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavAdmin;
    private TextView tvAvatarAdmin;
    private android.widget.ProgressBar progressReporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        progressReporte = findViewById(R.id.progressReporte);
        createNotificationChannel();

        sessionManager = new SessionManager(this);
        usuarioApi = ApiClient.getClient().create(UsuarioApi.class);
        productApi = ApiClient.getClient().create(ProductApi.class);

        CardView btnReporteUsuarios = findViewById(R.id.btnReporteUsuarios);
        CardView btnReporteProductos = findViewById(R.id.btnReporteProductos);
        tvAvatarAdmin = findViewById(R.id.tvAvatarAdmin);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);

        // Avatar
        String nombre = sessionManager.getNombre();
        if (nombre != null && !nombre.isEmpty()) {
            tvAvatarAdmin.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }
        tvAvatarAdmin.setOnClickListener(this::showProfileMenu);

        btnReporteUsuarios.setOnClickListener(v -> showUserReportMenu(v));
        btnReporteProductos.setOnClickListener(v -> showProductReportMenu(v));

        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                finish();
                return true;
            } else if (id == R.id.nav_admin_users) {
                startActivity(new Intent(this, UsuarioActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_admin_products) {
                startActivity(new Intent(this, ProductosAdminActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void showProfileMenu(android.view.View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_view_profile) {
                mostrarPerfil();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void mostrarPerfil() {
        Intent intent = new Intent(this, PerfilAdminActivity.class);
        startActivity(intent);
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showUserReportMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Todos los Usuarios");
        popup.getMenu().add("Usuarios Activos");
        popup.getMenu().add("Usuarios Inactivos");
        popup.getMenu().add("Administradores");
        popup.getMenu().add("Clientes");
        popup.getMenu().add("Vendedores");
        popup.getMenu().add("Estado y Fecha de Creación");

        popup.setOnMenuItemClickListener(item -> {
            fetchUsuariosAndGeneratePDF(item.getTitle().toString());
            return true;
        });
        popup.show();
    }

    private void showProductReportMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Todos los Productos");
        popup.getMenu().add("Productos con Stock (Activos)");
        popup.getMenu().add("Productos sin Stock (Inactivos)");

        popup.setOnMenuItemClickListener(item -> {
            fetchProductosAndGeneratePDF(item.getTitle().toString());
            return true;
        });
        popup.show();
    }

    private void fetchUsuariosAndGeneratePDF(String subType) {
        if (progressReporte != null) progressReporte.setVisibility(android.view.View.VISIBLE);
        Toast.makeText(this, "Generando reporte...", Toast.LENGTH_SHORT).show();
        usuarioApi.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (progressReporte != null) progressReporte.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    generateDetailedUsersPDF(response.body(), subType);
                } else {
                    Toast.makeText(ReportesActivity.this, "Error al obtener usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                if (progressReporte != null) progressReporte.setVisibility(android.view.View.GONE);
                Toast.makeText(ReportesActivity.this, "Sin conexión. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProductosAndGeneratePDF(String subType) {
        if (progressReporte != null) progressReporte.setVisibility(android.view.View.VISIBLE);
        Toast.makeText(this, "Generando reporte...", Toast.LENGTH_SHORT).show();
        productApi.getProductos().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (progressReporte != null) progressReporte.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    generateDetailedProductsPDF(response.body(), subType);
                } else {
                    Toast.makeText(ReportesActivity.this, "Error al obtener productos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (progressReporte != null) progressReporte.setVisibility(android.view.View.GONE);
                Toast.makeText(ReportesActivity.this, "Sin conexión. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Descargas de Reportes",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void generateDetailedUsersPDF(List<Usuario> usuarios, String subType) {
        // Ordenar usuarios por ID de menor a mayor
        Collections.sort(usuarios, (u1, u2) -> Integer.compare(u1.getUsuarioId(), u2.getUsuarioId()));

        // Filtrar según el subTipo
        List<Usuario> filtrados = new java.util.ArrayList<>();
        String tituloReporte = "REPORTE DE USUARIOS";

        for (Usuario u : usuarios) {
            boolean agregar = false;
            String userRol = u.getRol() != null ? u.getRol().toUpperCase().trim() : "";
            int rId = u.getRolId();
            
            switch (subType) {
                case "Usuarios Activos":
                    agregar = u.isActivo();
                    tituloReporte = "REPORTE DE USUARIOS ACTIVOS";
                    break;
                case "Usuarios Inactivos":
                    agregar = u.isInactivo();
                    tituloReporte = "REPORTE DE USUARIOS INACTIVOS";
                    break;
                case "Administradores":
                    agregar = (rId == 1 || userRol.equals("ADMIN"));
                    tituloReporte = "REPORTE DE ADMINISTRADORES";
                    break;
                case "Vendedores":
                    agregar = (rId == 2 || userRol.equals("VENDEDOR"));
                    tituloReporte = "REPORTE DE VENDEDORES";
                    break;
                case "Clientes":
                    agregar = (rId == 3 || userRol.equals("CLIENTE"));
                    tituloReporte = "REPORTE DE CLIENTES";
                    break;
                case "Estado y Fecha de Creación":
                    agregar = true;
                    tituloReporte = "REPORTE DE ESTADO Y REGISTRO";
                    break;
                default:
                    agregar = true;
                    tituloReporte = "REPORTE GENERAL DE USUARIOS";
                    break;
            }
            if (agregar) filtrados.add(u);
        }

        Log.d("REPORTE", "Filtrados para " + subType + ": " + filtrados.size());

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Logo
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logoapp);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80, false);
            canvas.drawBitmap(scaledLogo, 40, 40, paint);
        }

        // Encabezado
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.parseColor("#25632D"));
        canvas.drawText("AGROCONECTA", 140, 70, titlePaint);

        paint.setTextSize(12);
        paint.setColor(Color.GRAY);
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Fecha de generación: " + dateStr, 140, 95, paint);

        paint.setStrokeWidth(2);
        paint.setColor(Color.parseColor("#25632D"));
        canvas.drawLine(40, 130, 555, 130, paint);

        headerPaint.setTextSize(18);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(Color.BLACK);
        canvas.drawText(tituloReporte, 40, 170, headerPaint);

        paint.setTextSize(10);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);

        int startY = 210;
        
        if (subType.equals("Estado y Fecha de Creación")) {
            drawTableRow(canvas, paint, "ID", "Nombre", "Rol", "Fecha Registro", startY);
        } else {
            drawTableRow5(canvas, paint, "ID", "Nombre", "Correo", "Teléfono", "Rol", startY);
        }
        
        paint.setStrokeWidth(1);
        canvas.drawLine(40, startY + 5, 555, startY + 5, paint);
        
        paint.setFakeBoldText(false);
        startY += 30;
        
        for (Usuario u : filtrados) {
            if (startY > 780) break; 
            
            String rolTexto = "N/A";
            if (u.getRol() != null && !u.getRol().isEmpty()) {
                rolTexto = u.getRol().toUpperCase();
            } else if (u.getRolId() == 1) {
                rolTexto = "ADMIN";
            } else if (u.getRolId() == 2) {
                rolTexto = "VENDEDOR";
            } else if (u.getRolId() == 3) {
                rolTexto = "CLIENTE";
            } else if (u.getRolId() != 0) {
                rolTexto = String.valueOf(u.getRolId());
            }

            if (subType.equals("Estado y Fecha de Creación")) {
                String fechaOriginal = u.getFechaRegistro() != null ? u.getFechaRegistro() : "N/A";
                String fechaFormateada = fechaOriginal;
                if (!fechaOriginal.equals("N/A") && fechaOriginal.contains("T")) {
                    try {
                        fechaFormateada = fechaOriginal.split("T")[0];
                    } catch (Exception e) {
                        fechaFormateada = fechaOriginal;
                    }
                }
                drawTableRow(canvas, paint, 
                        String.valueOf(u.getUsuarioId()), 
                        u.getNombre(), 
                        rolTexto,
                        fechaFormateada, 
                        startY);
            } else {
                drawTableRow5(canvas, paint, 
                        String.valueOf(u.getUsuarioId()), 
                        u.getNombre(), 
                        u.getCorreo(), 
                        u.getTelefono() != null ? u.getTelefono() : "N/A", 
                        rolTexto,
                        startY);
            }
            startY += 25;
        }

        drawFooter(canvas, paint, 1, 1);
        pdfDocument.finishPage(page);

        String safeFileName = tituloReporte.replace(" ", "_").toLowerCase();
        saveAndNotify(pdfDocument, safeFileName + ".pdf");
        pdfDocument.close();
    }

    private void generateDetailedProductsPDF(List<Product> productos, String subType) {
        // Ordenar productos por ID de menor a mayor
        Collections.sort(productos, (p1, p2) -> Integer.compare(p1.getProductoId(), p2.getProductoId()));

        List<Product> filtrados = new java.util.ArrayList<>();
        String tituloReporte = "REPORTE DE PRODUCTOS";

        for (Product p : productos) {
            boolean agregar = false;
            switch (subType) {
                case "Productos con Stock (Activos)":
                    agregar = p.getExistencia() > 0;
                    tituloReporte = "PRODUCTOS CON EXISTENCIA";
                    break;
                case "Productos sin Stock (Inactivos)":
                    agregar = p.getExistencia() == 0;
                    tituloReporte = "PRODUCTOS SIN EXISTENCIA";
                    break;
                default:
                    agregar = true;
                    tituloReporte = "REPORTE GENERAL DE PRODUCTOS";
                    break;
            }
            if (agregar) filtrados.add(p);
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Logo
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logoapp);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80, false);
            canvas.drawBitmap(scaledLogo, 40, 40, paint);
        }

        // Encabezado
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.parseColor("#25632D"));
        canvas.drawText("AGROCONECTA", 140, 70, titlePaint);

        paint.setTextSize(12);
        paint.setColor(Color.GRAY);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Fecha de generación: " + date, 140, 95, paint);

        paint.setStrokeWidth(2);
        paint.setColor(Color.parseColor("#25632D"));
        canvas.drawLine(40, 130, 555, 130, paint);

        headerPaint.setTextSize(18);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(Color.BLACK);
        canvas.drawText(tituloReporte, 40, 170, headerPaint);

        paint.setTextSize(10);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);

        int startY = 210;
        drawTableRow(canvas, paint, "ID", "Nombre del Producto", "Precio", "Existencia", startY);
        paint.setStrokeWidth(1);
        canvas.drawLine(40, startY + 5, 555, startY + 5, paint);
        
        paint.setFakeBoldText(false);
        startY += 30;
        
        for (Product p : filtrados) {
            if (startY > 780) break;
            drawTableRow(canvas, paint, 
                    String.valueOf(p.getProductoId()), 
                    p.getName(), 
                    String.format(Locale.getDefault(), "$%.2f", p.getPrice()), 
                    String.valueOf(p.getExistencia()), 
                    startY);
            startY += 25;
        }

        drawFooter(canvas, paint, 1, 1);
        pdfDocument.finishPage(page);

        String safeFileName = tituloReporte.replace(" ", "_").toLowerCase();
        saveAndNotify(pdfDocument, safeFileName + ".pdf");
        pdfDocument.close();
    }

    private void drawFooter(Canvas canvas, Paint paint, int currentPage, int totalPages) {
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("Este documento es un reporte oficial generado por el sistema AgroConecta.", 40, 800, paint);
        canvas.drawText("Página " + currentPage + " de " + totalPages, 500, 800, paint);
    }

    private void drawTableRow(Canvas canvas, Paint paint, String c1, String c2, String c3, String c4, int y) {
        canvas.drawText(c1, 40, y, paint);
        canvas.drawText(c2, 90, y, paint);
        canvas.drawText(c3, 260, y, paint);
        canvas.drawText(c4, 480, y, paint);
    }

    private void drawTableRow5(Canvas canvas, Paint paint, String c1, String c2, String c3, String c4, String c5, int y) {
        canvas.drawText(c1, 40, y, paint);
        canvas.drawText(c2, 75, y, paint);
        canvas.drawText(c3, 190, y, paint);
        canvas.drawText(c4, 380, y, paint);
        canvas.drawText(c5, 485, y, paint);
    }

    private void saveAndNotify(PdfDocument pdfDocument, String fileName) {
        try {
            Uri fileUri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                fileUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (fileUri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
                    pdfDocument.writeTo(outputStream);
                    if (outputStream != null) outputStream.close();
                }
            }

            if (fileUri != null) {
                showDownloadNotification(fileName, fileUri);
                Toast.makeText(this, "Reporte guardado en descargas", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDownloadNotification(String fileName, Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Descarga completada")
                .setContentText("Toca para abrir: " + fileName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}