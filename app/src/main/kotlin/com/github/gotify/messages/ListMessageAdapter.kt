package com.github.gotify.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.ImageLoader
import coil.load
import com.github.gotify.MarkwonFactory
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.MessageItemBinding
import com.github.gotify.databinding.MessageItemCompactBinding
import com.github.gotify.messages.provider.MessageWithImage
import io.noties.markwon.Markwon
import java.text.DateFormat
import java.util.Date
import org.threeten.bp.OffsetDateTime

internal class ListMessageAdapter(
    private val context: Context,
    private val settings: Settings,
    private val imageLoader: ImageLoader,
    private val delete: Delete
) : ListAdapter<MessageWithImage, ListMessageAdapter.ViewHolder>(DiffCallback) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val markwon: Markwon = MarkwonFactory.createForMessage(context, imageLoader)

    private val timeFormatRelative =
        context.resources.getString(R.string.time_format_value_relative)
    private val timeFormatPrefsKey = context.resources.getString(R.string.setting_key_time_format)

    private var messageLayout = 0

    init {
        val messageLayoutPrefsKey = context.resources.getString(R.string.setting_key_message_layout)
        val messageLayoutNormal = context.resources.getString(R.string.message_layout_value_normal)
        val messageLayoutSetting = prefs.getString(messageLayoutPrefsKey, messageLayoutNormal)

        messageLayout = if (messageLayoutSetting == messageLayoutNormal) {
            R.layout.message_item
        } else {
            R.layout.message_item_compact
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (messageLayout == R.layout.message_item) {
            val binding = MessageItemBinding.inflate(layoutInflater, parent, false)
            ViewHolder(binding)
        } else {
            val binding = MessageItemCompactBinding.inflate(layoutInflater, parent, false)
            ViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = currentList[position]
        if (Extras.useMarkdown(message.message)) {
            holder.message.autoLinkMask = 0
            markwon.setMarkdown(holder.message, message.message.message)
        } else {
            holder.message.autoLinkMask = Linkify.WEB_URLS
            holder.message.text = message.message.message
        }
        holder.title.text = message.message.title
        if (message.image != null) {
            val url = Utils.resolveAbsoluteUrl("${settings.url}/", message.image)
            holder.image.load(url, imageLoader) {
                error(R.drawable.ic_alarm)
                placeholder(R.drawable.ic_placeholder)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val timeFormat = prefs.getString(timeFormatPrefsKey, timeFormatRelative)
        holder.setDateTime(message.message.date, timeFormat == timeFormatRelative)
        holder.date.setOnClickListener { holder.switchTimeFormat() }

        holder.delete.setOnClickListener {
            delete.delete(message.message)
        }
    }

    override fun getItemId(position: Int): Long {
        val currentItem = currentList[position]
        return currentItem.message.id
    }

    // Fix for message not being selectable (https://issuetracker.google.com/issues/37095917)
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.message.isEnabled = false
        holder.message.isEnabled = true
    }

    class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var image: ImageView
        lateinit var message: TextView
        lateinit var title: TextView
        lateinit var date: TextView
        lateinit var delete: ImageButton

        private var relativeTimeFormat = true
        private lateinit var dateTime: OffsetDateTime

        init {
            enableCopyToClipboard()
            if (binding is MessageItemBinding) {
                image = binding.messageImage
                message = binding.messageText
                title = binding.messageTitle
                date = binding.messageDate
                delete = binding.messageDelete
            } else if (binding is MessageItemCompactBinding) {
                image = binding.messageImage
                message = binding.messageText
                title = binding.messageTitle
                date = binding.messageDate
                delete = binding.messageDelete
            }
        }

        fun switchTimeFormat() {
            relativeTimeFormat = !relativeTimeFormat
            updateDate()
        }

        fun setDateTime(dateTime: OffsetDateTime, relativeTimeFormatPreference: Boolean) {
            this.dateTime = dateTime
            relativeTimeFormat = relativeTimeFormatPreference
            updateDate()
        }

        private fun updateDate() {
            val text = if (relativeTimeFormat) {
                // Relative time format
                Utils.dateToRelative(dateTime)
            } else {
                // Absolute time format
                val time = dateTime.toInstant().toEpochMilli()
                val date = Date(time)
                if (DateUtils.isToday(time)) {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
                } else {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
                }
            }

            date.text = text
        }

        private fun enableCopyToClipboard() {
            super.itemView.setOnLongClickListener { view: View ->
                val clipboard = view.context
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip = ClipData.newPlainText("GotifyMessageContent", message.text.toString())
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        view.context,
                        view.context.getString(R.string.message_copied_to_clipboard),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MessageWithImage>() {
        override fun areItemsTheSame(
            oldItem: MessageWithImage,
            newItem: MessageWithImage
        ): Boolean {
            return oldItem.message.id == newItem.message.id
        }

        override fun areContentsTheSame(
            oldItem: MessageWithImage,
            newItem: MessageWithImage
        ): Boolean {
            return oldItem == newItem
        }
    }

    fun interface Delete {
        fun delete(message: Message)
    }
}
