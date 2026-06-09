package sv.edu.agroconecta.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Categoria;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.network.ApiClient;
import sv.edu.agroconecta.network.CategoriaApi;
import sv.edu.agroconecta.network.ProductApi;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AgregarEditarProductoActivity extends AppCompatActivity {

    private EditText etNombre, etDescripcion, etPrecio, etExistencia, etUsuarioId, etImagenUrl;
    private Spinner spCategoria;
    private Button btnGuardar;
    private ProductApi productApi;
    private CategoriaApi categoriaApi;
    private int productoId = -1;
    private List<Categoria> categoriasList = new ArrayList<>();
    private BottomNavigationView bottomNavAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_editar_producto);

        productApi = ApiClient.getClient().create(ProductApi.class);
        categoriaApi = ApiClient.getClient().create(CategoriaApi.class);
        productoId = getIntent().getIntExtra("producto_id", -1);

        initViews();
        loadCategorias();
        setupBottomNav();

        if (productoId != -1) {
            ((TextView) findViewById(R.id.txtTitulo)).setText("Editar Producto");
        }

        //findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> saveProduct());
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etPrecio = findViewById(R.id.etPrecio);
        etExistencia = findViewById(R.id.etExistencia);
        etUsuarioId = findViewById(R.id.etUsuarioId);
        etImagenUrl = findViewById(R.id.etImagenUrl);
        spCategoria = findViewById(R.id.spCategoria);
        btnGuardar = findViewById(R.id.btnGuardar);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
    }

    private void setupBottomNav() {
        bottomNavAdmin.setSelectedItemId(R.id.nav_admin_products);
        bottomNavAdmin.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_admin_users) {
                startActivity(new Intent(this, UsuarioActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_admin_products;
        });
    }

    private void loadCategorias() {
        categoriaApi.getCategorias().enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoriasList = response.body();
                    ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(AgregarEditarProductoActivity.this, android.R.layout.simple_spinner_item, categoriasList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spCategoria.setAdapter(adapter);
                    if (productoId != -1) loadProductData();
                }
            }
            @Override
            public void onFailure(Call<List<Categoria>> call, Throwable t) {
                Toast.makeText(AgregarEditarProductoActivity.this, "Error cargando categorías", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProductData() {
        productApi.getProductoPorId(productoId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product p = response.body();
                    etNombre.setText(p.getNombre());
                    etDescripcion.setText(p.getDescripcion());
                    etPrecio.setText(String.valueOf(p.getPrecio()));
                    etExistencia.setText(String.valueOf(p.getExistencia()));
                    etUsuarioId.setText(String.valueOf(p.getUsuarioId()));
                    etImagenUrl.setText(p.getImagen());
                    for (int i = 0; i < categoriasList.size(); i++) {
                        if (categoriasList.get(i).getCategoriaId() == p.getCategoriaId()) {
                            spCategoria.setSelection(i);
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(AgregarEditarProductoActivity.this, "Error cargando datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProduct() {
        String n = etNombre.getText().toString().trim();
        String d = etDescripcion.getText().toString().trim();
        String pStr = etPrecio.getText().toString().trim();
        String eStr = etExistencia.getText().toString().trim();
        String uIdStr = etUsuarioId.getText().toString().trim();
        String img = etImagenUrl.getText().toString().trim();

        if (n.isEmpty() || pStr.isEmpty() || eStr.isEmpty() || uIdStr.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Categoria cat = (Categoria) spCategoria.getSelectedItem();
        Product product = new Product();
        product.setNombre(n);
        product.setDescripcion(d);
        product.setPrecio(Double.parseDouble(pStr));
        product.setExistencia(Integer.parseInt(eStr));
        product.setUsuarioId(Integer.parseInt(uIdStr));
        product.setCategoriaId(cat != null ? cat.getCategoriaId() : 1);
        product.setImagen(img.isEmpty() ? null : img);
        product.setEstado(true);

        if (productoId == -1) {
            Log.d("API_SAVE", "Enviando nuevo producto: " + n);
            productApi.crearProducto(product).enqueue(new Callback<Product>() {
                @Override
                public void onResponse(Call<Product> call, Response<Product> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AgregarEditarProductoActivity.this, "Producto creado con éxito", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = "Error al crear: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.e("API_SAVE", errorMsg);
                        Toast.makeText(AgregarEditarProductoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Product> call, Throwable t) {
                    Log.e("API_SAVE", "Falla de red: " + t.getMessage());
                    Toast.makeText(AgregarEditarProductoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            product.setProductoId(productoId);
            Log.d("API_SAVE", "Actualizando producto ID: " + productoId);
            productApi.actualizarProducto(productoId, product).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AgregarEditarProductoActivity.this, "Producto actualizado", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AgregarEditarProductoActivity.this, "Error al actualizar: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(AgregarEditarProductoActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
