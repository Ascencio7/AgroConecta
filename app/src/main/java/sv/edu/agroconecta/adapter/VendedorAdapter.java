package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import java.util.List;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;

public class VendedorAdapter extends RecyclerView.Adapter<VendedorAdapter.ViewHolder> {

    private List<Usuario> lista;
    private Context context;
    private OnVendedorActionListener listener;

    public interface OnVendedorActionListener {
        void onVendedorClick(Usuario vendedor);
        void onStatusChange(Usuario vendedor, boolean activate);
    }

    public VendedorAdapter(Context context, List<Usuario> lista, OnVendedorActionListener listener) {
        this.context = context;
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtCorreo, txtAvatar, txtRol, txtEstado, txtTelefono;
        ImageView imgFoto;
        com.google.android.material.button.MaterialButton btnEditar, btnEliminar;

        public ViewHolder(View view) {
            super(view);
            txtNombre = view.findViewById(R.id.txtNombre);
            txtCorreo = view.findViewById(R.id.txtCorreo);
            txtAvatar = view.findViewById(R.id.txtAvatar);
            imgFoto = view.findViewById(R.id.ivFotoUsuario);
            txtRol = view.findViewById(R.id.txtRol);
            txtEstado = view.findViewById(R.id.txtEstado);
            txtTelefono = view.findViewById(R.id.txtTelefono);
            btnEditar = view.findViewById(R.id.btnEditar);
            btnEliminar = view.findViewById(R.id.btnEliminar);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario v = lista.get(position);
        holder.txtNombre.setText(v.getNombre());
        holder.txtCorreo.setText(v.getCorreo());
        holder.txtTelefono.setText(v.getTelefono() != null ? v.getTelefono() : "N/A");
        holder.txtRol.setText("VENDEDOR");
        
        holder.btnEditar.setVisibility(View.GONE); // No editamos datos aquí, solo productos al entrar

        if (v.isActivo()) {
            holder.txtEstado.setText("ACTIVO");
            holder.txtEstado.setTextColor(ContextCompat.getColor(context, R.color.verde_primario));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_verde);
            
            holder.btnEliminar.setText("Desactivar");
            holder.btnEliminar.setIconResource(android.R.drawable.ic_lock_power_off);
            holder.btnEliminar.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rojo_error)));
            holder.btnEliminar.setTextColor(ContextCompat.getColor(context, R.color.rojo_error));
            holder.btnEliminar.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF1F0")));
            holder.btnEliminar.setOnClickListener(view -> listener.onStatusChange(v, false));
        } else {
            holder.txtEstado.setText("INACTIVO");
            holder.txtEstado.setTextColor(ContextCompat.getColor(context, R.color.rojo_error));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_rojo);
            
            holder.btnEliminar.setText("Activar");
            holder.btnEliminar.setIconResource(android.R.drawable.ic_menu_revert);
            holder.btnEliminar.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.verde_primario)));
            holder.btnEliminar.setTextColor(ContextCompat.getColor(context, R.color.verde_primario));
            holder.btnEliminar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.verde_superficial)));
            holder.btnEliminar.setOnClickListener(view -> listener.onStatusChange(v, true));
        }

        if (v.getNombre() != null && !v.getNombre().isEmpty()) {
            holder.txtAvatar.setText(String.valueOf(v.getNombre().charAt(0)).toUpperCase());
        }

        if (v.getFotoPerfil() != null && !v.getFotoPerfil().isEmpty()) {
            Glide.with(context)
                    .load(v.getFotoPerfil())
                    .transform(new CircleCrop())
                    .into(holder.imgFoto);
            holder.imgFoto.setVisibility(View.VISIBLE);
            holder.txtAvatar.setVisibility(View.GONE);
        } else {
            holder.imgFoto.setVisibility(View.GONE);
            holder.txtAvatar.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(view -> listener.onVendedorClick(v));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void updateList(List<Usuario> newList) {
        this.lista = newList;
        notifyDataSetChanged();
    }
}
