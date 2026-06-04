package sv.edu.agroconecta.modelo;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Pedido {

    @SerializedName("pedido_id")
    private int pedidoId;

    @SerializedName("usuario_id")
    private int usuarioId;

    @SerializedName("fecha")
    private String fecha;

    @SerializedName("estado_id")
    private int estadoId;  // 1=pendiente, 2=en proceso, 3=entregado, 4=cancelado

    @SerializedName("estado")
    private String estado; // nombre del estado (viene del JOIN)

    @SerializedName("total")
    private double total;

    @SerializedName("detalles")
    private List<DetallePedido> detalles;

    public int getPedidoId()   { return pedidoId; }
    public int getUsuarioId()  { return usuarioId; }
    public String getFecha()   { return fecha; }
    public int getEstadoId()   { return estadoId; }
    public String getEstado()  { return estado != null ? estado : getEstadoTexto(); }
    public double getTotal()   { return total; }
    public List<DetallePedido> getDetalles() { return detalles; }

    public void setPedidoId(int v)              { pedidoId = v; }
    public void setUsuarioId(int v)             { usuarioId = v; }
    public void setFecha(String v)              { fecha = v; }
    public void setEstadoId(int v)              { estadoId = v; }
    public void setEstado(String v)             { estado = v; }
    public void setTotal(double v)              { total = v; }
    public void setDetalles(List<DetallePedido> v) { detalles = v; }

    // Convierte estado_id a texto legible
    public String getEstadoTexto() {
        switch (estadoId) {
            case 1: return "Pendiente";
            case 2: return "En proceso";
            case 3: return "Entregado";
            case 4: return "Cancelado";
            default: return "Pendiente";
        }
    }
}
