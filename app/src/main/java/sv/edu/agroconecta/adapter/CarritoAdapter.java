package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.DetallePedido;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    private Context context;
    private List<DetallePedido> items;
    private OnItemChangedListener listener;

    public interface OnItemChangedListener {
        void onItemRemoved(int position);
        void onCantidadChanged();
    }

    public CarritoAdapter(Context context, List<DetallePedido> items, OnItemChangedListener listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        DetallePedido item = items.get(position);

        h.tvNombre.setText(item.getNombre());
        h.tvPrecio.setText(String.format("$%.2f", item.getPrecio()));
        h.tvCantidad.setText(String.valueOf(item.getCantidad()));
        h.tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));

        if (item.getImagen() != null && !item.getImagen().isEmpty()) {
            Glide.with(context).load(item.getImagen())
                    .placeholder(R.drawable.ic_launcher_background).into(h.ivImagen);
        }

        h.btnMas.setOnClickListener(v -> {
            item.setCantidad(item.getCantidad() + 1);
            h.tvCantidad.setText(String.valueOf(item.getCantidad()));
            h.tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));
            listener.onCantidadChanged();
        });

        h.btnMenos.setOnClickListener(v -> {
            if (item.getCantidad() > 1) {
                item.setCantidad(item.getCantidad() - 1);
                h.tvCantidad.setText(String.valueOf(item.getCantidad()));
                h.tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));
                listener.onCantidadChanged();
            }
        });

        h.btnEliminar.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Eliminar producto")
                .setMessage("¿Estás seguro que deseas eliminar \"" + item.getNombre() + "\" del carrito?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    int pos = h.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        items.remove(pos);
                        notifyItemRemoved(pos);
                        notifyItemRangeChanged(pos, items.size());
                        listener.onItemRemoved(pos);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
        });
    }

    @Override public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView   ivImagen;
        TextView    tvNombre, tvPrecio, tvCantidad, tvSubtotal;
        TextView    btnMas, btnMenos;   // son TextView en el XML
        ImageButton btnEliminar;

        public ViewHolder(@NonNull View v) {
            super(v);
            ivImagen   = v.findViewById(R.id.ivImagenCarrito);
            tvNombre   = v.findViewById(R.id.tvNombreCarrito);
            tvPrecio   = v.findViewById(R.id.tvPrecioCarrito);
            tvCantidad = v.findViewById(R.id.tvCantidadCarrito);
            tvSubtotal = v.findViewById(R.id.tvSubtotalCarrito);
            btnMas     = v.findViewById(R.id.btnMas);
            btnMenos   = v.findViewById(R.id.btnMenos);
            btnEliminar= v.findViewById(R.id.btnEliminarCarrito);
        }
    }
}
