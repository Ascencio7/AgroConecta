package sv.edu.agroconecta.modelo;

import com.google.gson.annotations.SerializedName;

public class Product {

    @SerializedName("producto_id")
    private int productoId;
    @SerializedName("usuario_id")
    private Integer usuarioId;
    @SerializedName("categoria_id")
    private Integer categoriaId;
    @SerializedName("nombre")
    private String nombre;
    @SerializedName("descripcion")
    private String descripcion;
    @SerializedName("precio")
    private double precio;
    @SerializedName("existencia")
    private int existencia;
    @SerializedName("estado")
    private Boolean estado;
    @SerializedName("imagen")
    private String imagen;
    @SerializedName("nombre_categoria")
    private String categoriaNombre;

    // Campos de vendedor (se guardan en SharedPreferences localmente
    // y se envían como parte de la descripción extendida al backend)
    @SerializedName("telefono_vendedor")
    private String telefonoVendedor;
    @SerializedName("latitud")
    private Double latitud;
    @SerializedName("longitud")
    private Double longitud;
    @SerializedName("direccion")
    private String direccion;
    @SerializedName("acepta_efectivo")
    private Boolean aceptaEfectivo;
    @SerializedName("acepta_transferencia")
    private Boolean aceptaTransferencia;
    @SerializedName("acepta_tarjeta")
    private Boolean aceptaTarjeta;
    @SerializedName("nombre_vendedor")
    private String nombreVendedor;

    private float userRating;

    // Getters / Setters estándar
    public int getProductoId() { return productoId; }
    public void setProductoId(int v) { productoId = v; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer v) { usuarioId = v; }
    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer v) { categoriaId = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
    public double getPrecio() { return precio; }
    public void setPrecio(double v) { precio = v; }
    public int getExistencia() { return existencia; }
    public void setExistencia(int v) { existencia = v; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean v) { estado = v; }
    public String getImagen() { return imagen; }
    public void setImagen(String v) { imagen = v; }
    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String v) { categoriaNombre = v; }
    public boolean isActivo() { return Boolean.TRUE.equals(estado); }
    public float getUserRating() { return userRating; }
    public void setUserRating(float v) { userRating = v; }

    // Vendedor
    public String getTelefonoVendedor() { return telefonoVendedor; }
    public void setTelefonoVendedor(String v) { telefonoVendedor = v; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double v) { latitud = v; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double v) { longitud = v; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = v; }
    public Boolean getAceptaEfectivo() { return aceptaEfectivo; }
    public void setAceptaEfectivo(Boolean v) { aceptaEfectivo = v; }
    public Boolean getAceptaTransferencia() { return aceptaTransferencia; }
    public void setAceptaTransferencia(Boolean v) { aceptaTransferencia = v; }
    public Boolean getAceptaTarjeta() { return aceptaTarjeta; }
    public void setAceptaTarjeta(Boolean v) { aceptaTarjeta = v; }
    public String getNombreVendedor() { return nombreVendedor; }
    public void setNombreVendedor(String v) { nombreVendedor = v; }

    // Compatibilidad adaptadores
    public String getName()        { return nombre; }
    public double getPrice()       { return precio; }
    public String getDescription() { return descripcion; }
    public String getCategoria()   { return categoriaNombre; }

    public String getMetodosPagoTexto() {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(aceptaEfectivo))      sb.append("💵 Efectivo\n");
        if (Boolean.TRUE.equals(aceptaTransferencia)) sb.append("🏦 Transferencia bancaria\n");
        if (Boolean.TRUE.equals(aceptaTarjeta))       sb.append("💳 Tarjeta débito/crédito");
        return sb.length() > 0 ? sb.toString().trim() : "💵 Efectivo";
    }
}
