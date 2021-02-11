package com.github.gotify.messages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.client.model.Message;
import com.github.gotify.messages.provider.MessageWithImage;
import com.squareup.picasso.Picasso;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.picasso.PicassoImagesPlugin;
import io.noties.markwon.movement.MovementMethodPlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

public class ListMessageAdapter extends RecyclerView.Adapter<ListMessageAdapter.ViewHolder> {

    private Context context;
    private Picasso picasso;
    private List<MessageWithImage> items;
    private Delete delete;
    private Settings settings;
    private Markwon markwon;

    ListMessageAdapter(
            Context context,
            Settings settings,
            Picasso picasso,
            List<MessageWithImage> items,
            Delete delete) {
        super();
        this.context = context;
        this.settings = settings;
        this.picasso = picasso;
        this.items = items;
        this.delete = delete;

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
        View view = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false);

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean preciseDateDefault = prefs.getBoolean(context.getResources().getString(R.string.setting_key_precise_date), false);
        holder.setDateTime(message.message.getDate(), preciseDateDefault);
        holder.date.setOnClickListener((ignored) -> holder.switchPreciseDate());

        holder.delete.setOnClickListener(
                (ignored) -> delete.delete(holder.getAdapterPosition(), message.message, false));
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

        @BindView(R.id.message_delete)
        ImageButton delete;

        private boolean preciseDate;
        private OffsetDateTime dateTime;

        ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);
            preciseDate = false;
            dateTime = null;
            enableCopyToClipboard();
        }

        void switchPreciseDate() {
            preciseDate = !preciseDate;
            updateDate();
        }

        void setDateTime(OffsetDateTime dateTime, boolean preciseDateDefault) {
            this.dateTime = dateTime;
            preciseDate = preciseDateDefault;
            updateDate();
        }

        void updateDate() {
            String text = "?";
            if (dateTime != null) {
                if (preciseDate) {
                    long time = dateTime.toInstant().toEpochMilli();
                    Date date = new Date(time);
                    if (DateUtils.isToday(time)) {
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                    } else {
                        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date);
                    }
                } else {
                    text = Utils.dateToRelative(dateTime);
                }
            }
            date.setText(text);
        }

        private void enableCopyToClipboard() {
            super.itemView.setOnLongClickListener(
                    view -> {
                        ClipboardManager clipboard =
                                (ClipboardManager)
                                        view.getContext()
                                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip =
                                ClipData.newPlainText(
                                        "GotifyMessageContent", message.getText().toString());

                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast toast =
                                    Toast.makeText(
                                            view.getContext(),
                                            view.getContext()
                                                    .getString(
                                                            R.string.message_copied_to_clipboard),
                                            Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        return true;
                    });
        }
    }

    public interface Delete {
        void delete(int position, Message message, boolean listAnimation);
    }
}
