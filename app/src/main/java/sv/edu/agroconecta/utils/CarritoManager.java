package sv.edu.agroconecta.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import sv.edu.agroconecta.modelo.DetallePedido;

public class CarritoManager {

    private static final String PREF_NAME = "CarritoPrefs";
    private static final String KEY_ITEMS = "carrito_items_";
    private static CarritoManager instance;
    private List<DetallePedido> items;

    private CarritoManager() {
        items = new ArrayList<>();
    }

    public static synchronized CarritoManager getInstance() {
        if (instance == null) {
            instance = new CarritoManager();
        }
        return instance;
    }

    public void agregarItem(DetallePedido item, Context context, int userId) {
        boolean encontrado = false;
        for (DetallePedido existente : items) {
            if (existente.getProductoId() == item.getProductoId()) {
                existente.setCantidad(existente.getCantidad() + item.getCantidad());
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            items.add(item);
        }
        guardarCarrito(context, userId);
    }

    // Mantener para compatibilidad si es necesario, pero no persistirá
    public void agregarItem(DetallePedido item) {
        for (DetallePedido existente : items) {
            if (existente.getProductoId() == item.getProductoId()) {
                existente.setCantidad(existente.getCantidad() + item.getCantidad());
                return;
            }
        }
        items.add(item);
    }

    public List<DetallePedido> getItems() {
        return items;
    }

    public double getTotal() {
        double total = 0;
        for (DetallePedido item : items) {
            total += (item.getPrecio() * item.getCantidad());
        }
        return total;
    }

    public int getCantidadItems() {
        int count = 0;
        for (DetallePedido item : items) {
            count += item.getCantidad();
        }
        return count;
    }

    public void limpiar() {
        items.clear();
    }

    public void limpiarYPersistir(Context context, int userId) {
        items.clear();
        guardarCarrito(context, userId);
    }

    public void guardarCarrito(Context context, int userId) {
        if (userId == -1) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(items);
        editor.putString(KEY_ITEMS + userId, json);
        editor.apply();
    }

    public void cargarCarrito(Context context, int userId) {
        if (userId == -1) {
            items.clear();
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS + userId, null);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<DetallePedido>>() {}.getType();
        List<DetallePedido> cargados = gson.fromJson(json, type);
        if (cargados != null) {
            items = cargados;
        } else {
            items = new ArrayList<>();
        }
    }
}
