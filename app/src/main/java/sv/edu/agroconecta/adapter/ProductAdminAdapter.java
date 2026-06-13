package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.List;
import java.util.Locale;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Product;

public class ProductAdminAdapter extends RecyclerView.Adapter<ProductAdminAdapter.ViewHolder> {

    private List<Product> lista;
    private Context context;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEdit(Product product);
        void onDelete(Product product);
        void onRestore(Product product);
    }

    public ProductAdminAdapter(Context context, List<Product> lista, OnProductActionListener listener) {
        this.context = context;
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPrecio, txtStock, txtStatusBadge;
        com.google.android.material.button.MaterialButton btnEdit, btnDelete;
        ImageView imgProducto;

        public ViewHolder(View view) {
            super(view);
            txtNombre = view.findViewById(R.id.txtNombreProducto);
            txtPrecio = view.findViewById(R.id.txtPrecioProducto);
            txtStock = view.findViewById(R.id.txtStock);
            txtStatusBadge = view.findViewById(R.id.txtStatusBadge);
            btnEdit = view.findViewById(R.id.btnEdit);
            btnDelete = view.findViewById(R.id.btnDelete);
            imgProducto = view.findViewById(R.id.imgProducto);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = lista.get(position);
        holder.txtNombre.setText(p.getName());
        holder.txtPrecio.setText(String.format(Locale.getDefault(), "$%.2f", p.getPrice()));
        holder.txtStock.setText(p.getExistencia() + " unidades");

        if (p.getEstado() != null && !p.getEstado()) {
            holder.txtStatusBadge.setText("NO DISPONIBLE");
            holder.txtStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_tenue));
            holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_rojo);
            
            // UI para Restaurar
            holder.btnDelete.setText("Activar");
            holder.btnDelete.setIconResource(android.R.drawable.ic_menu_revert);
            holder.btnDelete.setIconTint(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario)));
            holder.btnDelete.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
            holder.btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.verde_superficial)));
            holder.btnDelete.setOnClickListener(v -> listener.onRestore(p));
        } else {
            if (p.getExistencia() > 0) {
                holder.txtStatusBadge.setText("DISPONIBLE");
                holder.txtStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
                holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_verde);
            } else {
                holder.txtStatusBadge.setText("AGOTADO");
                holder.txtStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
                holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_badge_rojo);
            }
            
            // UI para Desactivar
            holder.btnDelete.setText("Desactivar");
            holder.btnDelete.setIconResource(android.R.drawable.ic_lock_power_off);
            holder.btnDelete.setIconTint(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error)));
            holder.btnDelete.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
            holder.btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF1F0")));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(p));
        }

        // Carga de imagen
        String imagen = p.getImagen();
        if (imagen != null && imagen.startsWith("https://")) {
            Glide.with(context)
                    .load(imagen)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.logoapp)
                    .error(R.drawable.logoapp)
                    .centerCrop()
                    .into(holder.imgProducto);
        } else {
            holder.imgProducto.setImageResource(R.drawable.logoapp);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(p));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void updateList(List<Product> newList) {
        this.lista = newList;
        notifyDataSetChanged();
    }
}