package sv.edu.agroconecta.adapter;

import android.app.AlertDialog;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.model.Usuario;
import sv.edu.agroconecta.repository.UsuarioRepository;
import sv.edu.agroconecta.ui.EditarUsuarioActivity;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.ViewHolder> {
    private List<Usuario> lista;
    private Context context;
    private UsuarioRepository repository;

    public UsuarioAdapter(List<Usuario> lista, Context context) {
        this.lista = lista;
        this.context = context;
        this.repository = new UsuarioRepository();
    }

    public String obtenerIniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) return "?";

        String[] partes = nombreCompleto.trim().split(" ");
        String iniciales = "";

        for (int i = 0; i < partes.length; i++) {
            if (!partes[i].isEmpty()) {
                iniciales += partes[i].charAt(0);
            }
        }
        return iniciales.toUpperCase();
    }

    // Titular de la vista
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtCorreo, txtTelefono, txtEstado, txtAvatar, txtRol;
        View btnEditar, btnEliminar;
        ImageView ivFotoUsuario;

        public ViewHolder(View view) {
            super(view);
            txtNombre = view.findViewById(R.id.txtNombre);
            txtCorreo = view.findViewById(R.id.txtCorreo);
            txtTelefono = view.findViewById(R.id.txtTelefono);
            txtEstado = view.findViewById(R.id.txtEstado);
            txtAvatar = view.findViewById(R.id.txtAvatar);
            txtRol = view.findViewById(R.id.txtRol);
            btnEditar = view.findViewById(R.id.btnEditar);
            btnEliminar = view.findViewById(R.id.btnEliminar);
            ivFotoUsuario = view.findViewById(R.id.ivFotoUsuario);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario u = lista.get(position);
        holder.txtNombre.setText(u.getNombre());
        holder.txtAvatar.setText(obtenerIniciales(u.getNombre()));

        // Foto de perfil
        holder.ivFotoUsuario.setVisibility(android.view.View.GONE);
        holder.txtAvatar.setVisibility(android.view.View.VISIBLE);
        String foto = u.getFotoPerfil();
        if (foto != null && !foto.isEmpty() && holder.ivFotoUsuario != null) {
            String fullUrl = foto.startsWith("http") ? foto : "https://ac-backend-4iax.onrender.com/" + foto;
            Glide.with(context)
                .load(fullUrl)
                .transform(new CircleCrop())
                .into(holder.ivFotoUsuario);
            holder.ivFotoUsuario.setVisibility(android.view.View.VISIBLE);
            holder.txtAvatar.setVisibility(android.view.View.GONE);
        }
        holder.txtCorreo.setText(u.getCorreo());
        holder.txtTelefono.setText(u.getTelefono());
        
        // Rol
        if (holder.txtRol != null) {
            String rol = u.getRol();
            
            // Fallback si el nombre del rol no viene del backend
            if (rol == null || rol.isEmpty()) {
                int rolId = u.getRolId();
                if (rolId == 1) rol = "Admin";
                else if (rolId == 2) rol = "Vendedor";
                else if (rolId == 3) rol = "Cliente";
                else rol = "Usuario";
            }
            
            holder.txtRol.setText(rol.toUpperCase());
            
            // Colores por rol
            if (rol.equalsIgnoreCase("Admin")) {
                holder.txtRol.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
            } else if (rol.equalsIgnoreCase("Vendedor")) {
                holder.txtRol.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
            } else {
                holder.txtRol.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_tenue));
            }
        }

        // Estado visual
        if (u.getEstado() != null && u.getEstado()) {
            holder.txtEstado.setText("ACTIVO");
            holder.txtEstado.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_verde);

            // Estilo botón Desactivar
            if (holder.btnEliminar instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) holder.btnEliminar;
                btn.setText("Desactivar");
                btn.setIconResource(android.R.drawable.ic_lock_power_off);
                btn.setIconTint(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error)));
                btn.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF1F0")));
            }
        } else {
            holder.txtEstado.setText("INACTIVO");
            holder.txtEstado.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_rojo);

            // Estilo botón Activar
            if (holder.btnEliminar instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) holder.btnEliminar;
                btn.setText("Activar");
                btn.setIconResource(android.R.drawable.ic_menu_revert);
                btn.setIconTint(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario)));
                btn.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.verde_superficial)));
            }
        }

        // Editar
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditarUsuarioActivity.class);
            intent.putExtra("usuario_id", u.getUsuarioId());
            intent.putExtra("nombre", u.getNombre());
            intent.putExtra("correo", u.getCorreo());
            intent.putExtra("telefono", u.getTelefono());
            intent.putExtra("estado", u.getEstado());
            intent.putExtra("rol_id", u.getRolId());
            context.startActivity(intent);
        });

        // Desactivar / Activar
        holder.btnEliminar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Usuario usuario = lista.get(pos);
            boolean activar = !usuario.isActivo();

            String titulo = activar ? "Activar usuario" : "Desactivar usuario";
            String mensaje = activar ? "¿Seguro que deseas activar este usuario?" : "¿Seguro que deseas desactivar este usuario?";

            new AlertDialog.Builder(context)
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Sí", (dialog, which) -> {
                        usuario.setEstado(activar);
                        repository.actualizar(usuario.getUsuarioId(), usuario)
                                .enqueue(new Callback<Usuario>() {
                                    @Override
                                    public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                                        if (response.isSuccessful()) {
                                            notifyItemChanged(pos);
                                            Toast.makeText(context,
                                                    activar ? "Usuario activado" : "Usuario desactivado",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            usuario.setEstado(!activar); // Revertir localmente si falla
                                            Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Usuario> call, Throwable t) {
                                        usuario.setEstado(!activar);
                                        Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    // Actualizar la lista
    public void updateList(List<Usuario> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}