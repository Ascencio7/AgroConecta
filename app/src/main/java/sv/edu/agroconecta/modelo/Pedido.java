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

    @SerializedName("nombre_cliente")
    private String nombreCliente;

    @SerializedName("vendedor_id")
    private int vendedorId;

    @SerializedName("nombre_vendedor")
    private String nombreVendedor;

    @SerializedName("foto_perfil_vendedor")
    private String fotoVendedor;

    @SerializedName("telefono_vendedor")
    private String telefonoVendedor;

    @SerializedName("foto_perfil_cliente")
    private String fotoCliente;

    @SerializedName("telefono_cliente")
    private String telefonoCliente;

    @SerializedName("detalles")
    private List<DetallePedido> detalles;

    public int getPedidoId()   { return pedidoId; }
    public int getUsuarioId()  { return usuarioId; }
    public String getFecha()   { return fecha; }
    public String getTelefonoVendedor() { return telefonoVendedor; }
    public int getEstadoId()   { return estadoId; }
    public String getEstado()  { return estado != null ? estado : getEstadoTexto(); }
    public double getTotal()   { return total; }
    public String getNombreCliente()   { return nombreCliente; }
    public String getFotoCliente()     { return fotoCliente; }
    public String getTelefonoCliente() { return telefonoCliente; }
    public void setNombreCliente(String v)   { nombreCliente = v; }
    public void setFotoCliente(String v)     { fotoCliente = v; }
    public void setTelefonoCliente(String v) { telefonoCliente = v; }
    public int getVendedorId()    { return vendedorId; }
    public void setVendedorId(int v) { vendedorId = v; }
    public String getNombreVendedor()   { return nombreVendedor; }
    public String getFotoVendedor()     { return fotoVendedor; }
    public String getTelefonoVendedor2(){ return telefonoVendedor; }
    public void setNombreVendedor(String v)   { nombreVendedor = v; }
    public void setFotoVendedor(String v)     { fotoVendedor = v; }
    public void setTelefonoVendedor2(String v){ telefonoVendedor = v; }
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
