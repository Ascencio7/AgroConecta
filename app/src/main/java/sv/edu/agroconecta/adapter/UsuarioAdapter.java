package sv.edu.agroconecta.adapter;

import android.app.AlertDialog;
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
        holder.txtCorreo.setText(u.getCorreo());
        holder.txtTelefono.setText(u.getTelefono());
        if (holder.txtRol != null) holder.txtRol.setText(u.getRol() != null ? u.getRol() : "Usuario");

        // Estado visual
        if (u.getEstado() != null && u.getEstado()) {
            holder.txtEstado.setText("ACTIVO");
            holder.txtEstado.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.verde_primario));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_verde);
        } else {
            holder.txtEstado.setText("INACTIVO");
            holder.txtEstado.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_error));
            holder.txtEstado.setBackgroundResource(R.drawable.bg_badge_rojo);
        }

        // Editar
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditarUsuarioActivity.class);
            intent.putExtra("usuario_id", u.getUsuarioId());
            intent.putExtra("nombre", u.getNombre());
            intent.putExtra("correo", u.getCorreo());
            intent.putExtra("telefono", u.getTelefono());
            intent.putExtra("estado", u.getEstado());
            context.startActivity(intent);
        });

        // Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Usuario usuario = lista.get(pos);

            new AlertDialog.Builder(context)
                    .setTitle("Eliminar usuario")
                    .setMessage("¿Seguro que deseas eliminar este usuario?")
                    .setPositiveButton("Sí", (dialog, which) -> {

                        repository.eliminarUsuario(usuario.getUsuarioId())
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {

                                        if (response.isSuccessful()) {
                                            if (pos < lista.size()) {
                                                lista.remove(pos);
                                                notifyItemRemoved(pos);
                                            }
                                            Toast.makeText(context,
                                                    "Usuario eliminado",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context,
                                                    "Error al eliminar",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Toast.makeText(context,
                                                "Error de conexión",
                                                Toast.LENGTH_SHORT).show();
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