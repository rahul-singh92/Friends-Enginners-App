package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatItem> chatItems;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    public ChatAdapter(List<ChatMessage> messages) {
        this.chatItems = processMessagesWithDateHeaders(messages);
    }

    private List<ChatItem> processMessagesWithDateHeaders(List<ChatMessage> messages) {
        List<ChatItem> items = new ArrayList<>();
        String lastDate = null;

        for (ChatMessage message : messages) {
            if (message == null || message.getText() == null) {
                continue; // Skip null messages
            }

            String messageDate = getDateString(message.getTimestamp());

            // Add date header if date changed
            if (!messageDate.equals(lastDate)) {
                items.add(new ChatItem(messageDate));
                lastDate = messageDate;
            }

            // Add message
            items.add(new ChatItem(message));
        }

        return items;
    }

    private String getDateString(long timestamp) {
        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        // Check if today
        if (isSameDay(messageCalendar, today)) {
            return "Today";
        }
        // Check if yesterday
        else if (isSameDay(messageCalendar, yesterday)) {
            return "Yesterday";
        }
        // Otherwise, show full date
        else {
            return dateFormat.format(new Date(timestamp));
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemViewType(int position) {
        // Return the type from ChatItem
        return chatItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatItem.TYPE_DATE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_received, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = chatItems.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).dateText.setText(item.getDateHeader());
        } else if (holder instanceof MessageViewHolder) {
            ChatMessage message = item.getMessage();
            if (message != null) {
                String text = message.getText() != null ? message.getText() : "";
                ((MessageViewHolder) holder).messageText.setText(text);

                String time = timeFormat.format(new Date(message.getTimestamp()));
                ((MessageViewHolder) holder).timestampText.setText(time);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    // Method to update messages and refresh date headers
    public void updateMessages(List<ChatMessage> messages) {
        this.chatItems = processMessagesWithDateHeaders(messages);
        notifyDataSetChanged();
    }

    // Date Header ViewHolder
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.tv_date_header);
        }
    }

    // Message ViewHolder
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_message);
            timestampText = itemView.findViewById(R.id.tv_timestamp);
        }
    }
}
