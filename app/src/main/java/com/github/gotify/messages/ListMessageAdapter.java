package com.github.gotify.messages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.github.gotify.MarkwonFactory;
import com.github.gotify.R;
import com.github.gotify.Settings;
import com.github.gotify.Utils;
import com.github.gotify.client.model.Message;
import com.github.gotify.databinding.MessageItemBinding;
import com.github.gotify.databinding.MessageItemCompactBinding;
import com.github.gotify.messages.provider.MessageWithImage;
import com.squareup.picasso.Picasso;
import io.noties.markwon.Markwon;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import org.threeten.bp.OffsetDateTime;

public class ListMessageAdapter extends RecyclerView.Adapter<ListMessageAdapter.ViewHolder> {

    private Context context;
    private SharedPreferences prefs;
    private Picasso picasso;
    private List<MessageWithImage> items;
    private Delete delete;
    private Settings settings;
    private Markwon markwon;
    private int messageLayout;

    private final String TIME_FORMAT_RELATIVE;
    private final String TIME_FORMAT_PREFS_KEY;

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

        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.markwon = MarkwonFactory.createForMessage(context, picasso);

        TIME_FORMAT_RELATIVE =
                context.getResources().getString(R.string.time_format_value_relative);
        TIME_FORMAT_PREFS_KEY = context.getResources().getString(R.string.setting_key_time_format);

        String message_layout_prefs_key =
                context.getResources().getString(R.string.setting_key_message_layout);
        String messageLayoutNormal =
                context.getResources().getString(R.string.message_layout_value_normal);
        String messageLayoutSetting =
                prefs.getString(message_layout_prefs_key, messageLayoutNormal);

        if (messageLayoutSetting.equals(messageLayoutNormal)) {
            messageLayout = R.layout.message_item;
        } else {
            messageLayout = R.layout.message_item_compact;
        }
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
        ViewHolder holder;
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (messageLayout == R.layout.message_item) {
            MessageItemBinding binding = MessageItemBinding.inflate(layoutInflater, parent, false);
            holder = new ViewHolder(binding);
        } else {
            MessageItemCompactBinding binding =
                    MessageItemCompactBinding.inflate(layoutInflater, parent, false);
            holder = new ViewHolder(binding);
        }

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
        String timeFormat = prefs.getString(TIME_FORMAT_PREFS_KEY, TIME_FORMAT_RELATIVE);
        holder.setDateTime(message.message.getDate(), timeFormat.equals(TIME_FORMAT_RELATIVE));
        holder.date.setOnClickListener((ignored) -> holder.switchTimeFormat());

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
        ImageView image;
        TextView message;
        TextView title;
        TextView date;
        ImageButton delete;

        private boolean relativeTimeFormat;
        private OffsetDateTime dateTime;

        ViewHolder(final ViewBinding binding) {
            super(binding.getRoot());
            relativeTimeFormat = true;
            dateTime = null;
            enableCopyToClipboard();

            if (binding instanceof MessageItemBinding) {
                MessageItemBinding localBinding = (MessageItemBinding) binding;
                image = localBinding.messageImage;
                message = localBinding.messageText;
                title = localBinding.messageTitle;
                date = localBinding.messageDate;
                delete = localBinding.messageDelete;
            } else if (binding instanceof MessageItemCompactBinding) {
                MessageItemCompactBinding localBinding = (MessageItemCompactBinding) binding;
                image = localBinding.messageImage;
                message = localBinding.messageText;
                title = localBinding.messageTitle;
                date = localBinding.messageDate;
                delete = localBinding.messageDelete;
            }
        }

        void switchTimeFormat() {
            relativeTimeFormat = !relativeTimeFormat;
            updateDate();
        }

        void setDateTime(OffsetDateTime dateTime, boolean relativeTimeFormatPreference) {
            this.dateTime = dateTime;
            relativeTimeFormat = relativeTimeFormatPreference;
            updateDate();
        }

        void updateDate() {
            String text = "?";
            if (dateTime != null) {
                if (relativeTimeFormat) {
                    // Relative time format
                    text = Utils.dateToRelative(dateTime);
                } else {
                    // Absolute time format
                    long time = dateTime.toInstant().toEpochMilli();
                    Date date = new Date(time);
                    if (DateUtils.isToday(time)) {
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                    } else {
                        text =
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(date);
                    }
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
