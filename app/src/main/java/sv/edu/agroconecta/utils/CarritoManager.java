package sv.edu.agroconecta.utils;

import java.util.ArrayList;
import java.util.List;
import sv.edu.agroconecta.modelo.DetallePedido;

public class CarritoManager {

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
            total += item.getSubtotal();
        }
        return total;
    }

    public int getCantidadItems() {
        return items.size();
    }

    public void limpiar() {
        items.clear();
    }
}