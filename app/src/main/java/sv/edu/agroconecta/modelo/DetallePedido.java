package sv.edu.agroconecta.modelo;

import com.google.gson.annotations.SerializedName;

public class DetallePedido {

    @SerializedName("detalle_id")
    private int detalleId;

    @SerializedName("producto_id")
    private int productoId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("precio")
    private double precio;

    @SerializedName("cantidad")
    private int cantidad;

    @SerializedName("imagen")
    private String imagen;

    // ID del vendedor dueño del producto — se usa para notificarle al hacer pedido
    private int vendedorId;

    // Compatibilidad de métodos de pago
    private boolean aceptaEfectivo;
    private boolean aceptaTransferencia;
    private boolean aceptaTarjeta;

    // Constructor para agregar al carrito localmente (sin vendedorId — compatibilidad)
    public DetallePedido(int productoId, String nombre, double precio, int cantidad, String imagen) {
        this.productoId = productoId;
        this.nombre     = nombre;
        this.precio     = precio;
        this.cantidad   = cantidad;
        this.imagen     = imagen;
        this.vendedorId = -1;
        this.aceptaEfectivo = true; // Por defecto
    }

    // Constructor completo para el carrito
    public DetallePedido(int productoId, String nombre, double precio, int cantidad, String imagen, 
                         int vendedorId, boolean efectivo, boolean transferencia, boolean tarjeta) {
        this.productoId = productoId;
        this.nombre     = nombre;
        this.precio     = precio;
        this.cantidad   = cantidad;
        this.imagen     = imagen;
        this.vendedorId = vendedorId;
        this.aceptaEfectivo = efectivo;
        this.aceptaTransferencia = transferencia;
        this.aceptaTarjeta = tarjeta;
    }

    // Constructor anterior (mantener compatibilidad si se usa en otros lados)
    public DetallePedido(int productoId, String nombre, double precio, int cantidad, String imagen, int vendedorId) {
        this(productoId, nombre, precio, cantidad, imagen, vendedorId, true, false, false);
    }

    public int getDetalleId()   { return detalleId; }
    public int getProductoId()  { return productoId; }
    public String getNombre()   { return nombre; }
    public double getPrecio()   { return precio; }
    public int getCantidad()    { return cantidad; }
    public String getImagen()   { return imagen; }
    public int getVendedorId()  { return vendedorId; }
    public double getSubtotal() { return precio * cantidad; }

    public boolean isAceptaEfectivo()      { return aceptaEfectivo; }
    public boolean isAceptaTransferencia() { return aceptaTransferencia; }
    public boolean isAceptaTarjeta()       { return aceptaTarjeta; }

    public void setCantidad(int cantidad)   { this.cantidad = cantidad; }
    public void setPrecio(double precio)    { this.precio = precio; }
    public void setNombre(String nombre)    { this.nombre = nombre; }
    public void setImagen(String imagen)    { this.imagen = imagen; }
    public void setVendedorId(int id)       { this.vendedorId = id; }
}
