package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Product;
import sv.edu.agroconecta.ui.ProductoDetalleActivity;

public class ProductoCompradorAdapter extends RecyclerView.Adapter<ProductoCompradorAdapter.ViewHolder> {

    private Context context;
    private List<Product> productos;

    public ProductoCompradorAdapter(Context context, List<Product> productos) {
        this.context = context;
        this.productos = productos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_producto_comprador, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_in));
        Product producto = productos.get(position);

        holder.tvNombre.setText(producto.getNombre());
        holder.tvPrecio.setText(String.format("$%.2f", producto.getPrecio()));
        holder.tvCategoria.setText(producto.getCategoria());

        Glide.with(context)
                .load(producto.getImagen())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivImagen);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductoDetalleActivity.class);
            intent.putExtra("producto_id",      producto.getProductoId());
            intent.putExtra("usuario_id",       producto.getUsuarioId());
            intent.putExtra("nombre",           producto.getNombre());
            intent.putExtra("descripcion",      producto.getDescripcion());
            intent.putExtra("precio",           producto.getPrecio());
            intent.putExtra("imagen",           producto.getImagen());
            intent.putExtra("categoria",        producto.getCategoria());
            intent.putExtra("existencia",       producto.getExistencia());
            // FIX: pasar datos del vendedor para que funcione WhatsApp
            intent.putExtra("telefono_vendedor", producto.getTelefonoVendedor());
            intent.putExtra("nombre_vendedor",  producto.getNombreVendedor());
            intent.putExtra("foto_perfil_vendedor", producto.getFotoPerfilVendedor());
            String metodos = producto.getMetodosPagoTexto();
            intent.putExtra("metodos_pago", metodos != null ? metodos : "💵 Efectivo");
            Double lat = producto.getLatitud();
            Double lon = producto.getLongitud();
            intent.putExtra("latitud",  lat  != null ? lat  : 0.0);
            intent.putExtra("longitud", lon  != null ? lon  : 0.0);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return productos.size(); }

    public void setProductos(List<Product> productos) {
        this.productos = productos;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImagen;
        TextView tvNombre, tvPrecio, tvCategoria;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen    = itemView.findViewById(R.id.ivImagenProducto);
            tvNombre    = itemView.findViewById(R.id.tvNombreProducto);
            tvPrecio    = itemView.findViewById(R.id.tvPrecioProducto);
            tvCategoria = itemView.findViewById(R.id.tvCategoriaProducto);
        }
    }
}
