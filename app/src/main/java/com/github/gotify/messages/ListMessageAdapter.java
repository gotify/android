package com.github.gotify.messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.gotify.R;
import com.github.gotify.Utils;
import com.github.gotify.client.model.Message;
import com.github.gotify.messages.provider.MessageWithImage;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ListMessageAdapter extends BaseAdapter {

    private Context content;
    private List<MessageWithImage> items;
    private Delete delete;

    ListMessageAdapter(Context context, List<MessageWithImage> items, Delete delete) {
        super();
        this.content = context;
        this.items = items;
        this.delete = delete;
    }

    void items(List<MessageWithImage> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public MessageWithImage getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).message.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(content).inflate(R.layout.message_item, parent, false);
        } else {
            view = convertView;
        }
        ViewHolder holder = new ViewHolder(view);
        final MessageWithImage message = items.get(position);
        holder.message.setText(message.message.getMessage());
        holder.title.setText(message.message.getTitle());
        Picasso.get()
                .load(message.image)
                .error(R.drawable.ic_alarm)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.image);
        holder.date.setText(
                message.message.getDate() != null
                        ? Utils.dateToRelative(message.message.getDate())
                        : "?");
        holder.delete.setOnClickListener((ignored) -> delete.delete(message.message));

        return view;
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

        ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public interface Delete {
        void delete(Message message);
    }
}
