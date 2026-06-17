package sv.edu.agroconecta.adapter;

import android.content.Context;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.List;
import sv.edu.agroconecta.R;
import sv.edu.agroconecta.modelo.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Product product);
    }

    public interface OnRatingChangeListener {
        void onRatingChange(Product product, float rating);
    }

    private Context context;
    private List<Product> products;
    private OnOrderClickListener orderListener;
    private OnRatingChangeListener ratingListener;

    public ProductAdapter(Context context, List<Product> products,
                          OnOrderClickListener orderListener,
                          OnRatingChangeListener ratingListener) {
        this.context        = context;
        this.products       = products;
        this.orderListener  = orderListener;
        this.ratingListener = ratingListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_in));
        Product product = products.get(position);

        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(String.format("$%.2f", product.getPrice()));
        holder.tvDescription.setText(product.getDescription());
        holder.ratingBar.setRating(product.getUserRating());

        // ── Cargar foto desde productos.imagen (URL de Supabase Storage) ─
        String imagen = product.getImagen();
        if (imagen != null && imagen.startsWith("https://")) {
            Glide.with(context)
                    .load(imagen)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(holder.ivProductoImagen);
        } else {
            holder.ivProductoImagen.setImageResource(R.drawable.ic_launcher_foreground);
        }
        // ─────────────────────────────────────────────────────────────────

        holder.ratingBar.setOnRatingBarChangeListener(null);
        holder.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser && ratingListener != null) {
                product.setUserRating(rating);
                ratingListener.onRatingChange(product, rating);
            }
        });

        holder.btnOrder.setOnClickListener(v -> {
            if (orderListener != null) orderListener.onOrderClick(product);
        });
    }

    @Override
    public int getItemCount() { return products.size(); }

    public void updateList(List<Product> newList) {
        this.products = newList;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductoImagen;
        TextView  tvName, tvPrice, tvDescription;
        RatingBar ratingBar;
        Button    btnOrder;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductoImagen = itemView.findViewById(R.id.ivProductoImagen);
            tvName           = itemView.findViewById(R.id.tvProductName);
            tvPrice          = itemView.findViewById(R.id.tvProductPrice);
            tvDescription    = itemView.findViewById(R.id.tvProductDescription);
            ratingBar        = itemView.findViewById(R.id.ratingBar);
            btnOrder         = itemView.findViewById(R.id.btnOrder);
        }
    }
}
