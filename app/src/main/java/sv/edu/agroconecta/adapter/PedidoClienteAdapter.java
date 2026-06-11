package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Pedido;
import sv.edu.agroconecta.ui.CalificacionActivity;
import sv.edu.agroconecta.ui.SeguimientoPedidoActivity;

public class PedidoClienteAdapter extends RecyclerView.Adapter<PedidoClienteAdapter.VH> {
    private List<Pedido> lista;
    private Context context;

    public PedidoClienteAdapter(List<Pedido> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    public void updateList(List<Pedido> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Pedido p = lista.get(pos);

        // Datos básicos
        h.tvId.setText("Pedido #" + p.getPedidoId());
        h.tvTotal.setText(String.format("$%.2f", p.getTotal()));
        h.tvFecha.setText("📅 " + (p.getFecha() != null ? p.getFecha().substring(0, 10) : "Hoy"));
        
        String estadoYPago = p.getEstadoTexto();
        if (p.getMetodoPago() != null && !p.getMetodoPago().isEmpty()) {
            estadoYPago += " • " + p.getMetodoPago();
        }
        h.tvEstado.setText(estadoYPago);

        // Items del pedido
        if (h.tvItems != null) {
            if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < p.getDetalles().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(p.getDetalles().get(i).getNombre());
                }
                h.tvItems.setText(sb.toString());
            } else {
                h.tvItems.setText("Productos del pedido");
            }
        }

        // Vendedor - reset estado
        h.ivFotoV.setVisibility(View.GONE);
        h.tvAvatarV.setVisibility(View.VISIBLE);

        if (p.getNombreVendedor() != null && !p.getNombreVendedor().isEmpty()) {
            h.tvNombreV.setText(p.getNombreVendedor());
            h.tvAvatarV.setText(String.valueOf(p.getNombreVendedor().charAt(0)).toUpperCase());
        } else {
            h.tvNombreV.setText("Vendedor");
            h.tvAvatarV.setText("V");
        }

        if (p.getFotoVendedor() != null && !p.getFotoVendedor().isEmpty()) {
            Glide.with(context).load(p.getFotoVendedor()).transform(new CircleCrop()).into(h.ivFotoV);
            h.ivFotoV.setVisibility(View.VISIBLE);
            h.tvAvatarV.setVisibility(View.GONE);
        }

        // WhatsApp
        if (p.getTelefonoVendedor2() != null && !p.getTelefonoVendedor2().isEmpty()) {
            h.btnWA.setVisibility(View.VISIBLE);
            h.btnWA.setOnClickListener(v -> {
                String num = p.getTelefonoVendedor2().replaceAll("[^0-9]", "");
                String msg = Uri.encode("Hola! Tengo una consulta sobre mi pedido #" + p.getPedidoId());
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/503" + num + "?text=" + msg));
                context.startActivity(i);
            });
        } else {
            h.btnWA.setVisibility(View.GONE);
        }

        // Botón Seguimiento
        h.btnSeg.setOnClickListener(v -> {
            Intent i = new Intent(context, SeguimientoPedidoActivity.class);
            i.putExtra("pedido_id", p.getPedidoId());
            i.putExtra("estado_id", p.getEstadoId());
            context.startActivity(i);
        });

        // Botón Calificar
        if (h.btnCal != null) {
            h.btnCal.setOnClickListener(v -> {
                // El backend exige producto_id — tomamos el del primer detalle del pedido
                int productoId = 0;
                if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                    productoId = p.getDetalles().get(0).getProductoId();
                }
                Intent i = new Intent(context, CalificacionActivity.class);
                i.putExtra("pedido_id",       p.getPedidoId());
                i.putExtra("producto_id",     productoId);
                i.putExtra("vendedor_id",     p.getVendedorId());
                i.putExtra("vendedor_nombre", p.getNombreVendedor());
                context.startActivity(i);
            });
        }
    }

    @Override
    public int getItemCount() { return lista != null ? lista.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvId, tvTotal, tvFecha, tvEstado, tvNombreV, tvAvatarV, tvItems;
        ImageView ivFotoV;
        MaterialButton btnWA, btnSeg, btnCal;

        VH(View v) {
            super(v);
            tvId      = v.findViewById(R.id.tvPedidoId);
            tvTotal   = v.findViewById(R.id.tvPedidoTotal);
            tvFecha   = v.findViewById(R.id.tvPedidoFecha);
            tvEstado  = v.findViewById(R.id.tvPedidoEstado);
            tvNombreV = v.findViewById(R.id.tvNombreVendedor);
            tvAvatarV = v.findViewById(R.id.tvAvatarVendedor);
            tvItems   = v.findViewById(R.id.tvPedidoItems);
            ivFotoV   = v.findViewById(R.id.ivFotoVendedor);
            btnWA     = v.findViewById(R.id.btnWhatsAppVendedor);
            btnSeg    = v.findViewById(R.id.btnSeguimiento);
            btnCal    = v.findViewById(R.id.btnCalificar);
        }
    }
}
