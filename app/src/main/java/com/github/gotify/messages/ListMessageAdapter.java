package com.github.gotify.messages;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.messages.provider.MessageWithImage;
import com.squareup.picasso.Picasso;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.picasso.PicassoImagesPlugin;
import io.noties.markwon.movement.MovementMethodPlugin;
import java.util.List;

public class ListMessageAdapter extends RecyclerView.Adapter<ListMessageAdapter.ViewHolder> {

    private Context content;
    private Picasso picasso;
    private List<MessageWithImage> items;
    private Settings settings;
    private Markwon markwon;

    ListMessageAdapter(
            Context context, Settings settings, Picasso picasso, List<MessageWithImage> items) {
        super();
        this.content = context;
        this.settings = settings;
        this.picasso = picasso;
        this.items = items;

        this.markwon =
                Markwon.builder(context)
                        .usePlugin(CorePlugin.create())
                        .usePlugin(MovementMethodPlugin.create())
                        .usePlugin(PicassoImagesPlugin.create(picasso))
                        .usePlugin(TablePlugin.create(context))
                        .build();
    }

    public List<MessageWithImage> getItems() {
        return items;
    }

    public void setItems(List<MessageWithImage> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(content).inflate(R.layout.message_item, parent, false);

        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MessageWithImage message = items.get(position);
        if (Extras.useMarkdown(message.message)) {
            holder.message.setAutoLinkMask(0);
            markwon.setMarkdown(holder.message, message.message.getMessage());
        } else {
            holder.message.setAutoLinkMask(Linkify.WEB_URLS);
            holder.message.setText(message.message.getMessage());
        }
        holder.title.setText(message.message.getTitle());
        picasso.load(Utils.resolveAbsoluteUrl(settings.url() + "/", message.image))
                .error(R.drawable.ic_alarm)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.image);
        holder.date.setText(
                message.message.getDate() != null
                        ? Utils.dateToRelative(message.message.getDate())
                        : "?");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        MessageWithImage currentItem = items.get(position);
        return currentItem.message.getId();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_image)
        ImageView image;

        @BindView(R.id.message_text)
        TextView message;

        @BindView(R.id.message_title)
        TextView title;

        @BindView(R.id.message_date)
        TextView date;

        ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
