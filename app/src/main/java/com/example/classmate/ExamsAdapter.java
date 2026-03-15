package com.example.classmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExamsAdapter extends RecyclerView.Adapter<ExamsAdapter.ExamViewHolder> {

    public interface OnExamActionListener {
        void onViewDetails(Event event);
        void onDeleteExam(Event event);
    }

    private List<Event> events;
    private final OnExamActionListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd\nMMM", Locale.forLanguageTag("he"));

    public ExamsAdapter(List<Event> events, OnExamActionListener listener) {
        this.events = events;
        this.listener = listener;
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exam, parent, false);
        return new ExamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        Event event = events.get(position);
        holder.dateTextView.setText(dateFormat.format(new Date(event.getTimestamp())));
        holder.subjectTextView.setText(event.getSubject());

        StringBuilder details = new StringBuilder(event.getType());
        if (event.getMaterial() != null && !event.getMaterial().isEmpty()) {
            details.append(" | ").append(event.getMaterial());
        }
        holder.detailsTextView.setText(details.toString());

        holder.optionsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 0, 0, "פרטים");
            popup.getMenu().add(0, 1, 1, "מחיקה");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 0) {
                    listener.onViewDetails(event);
                } else {
                    listener.onDeleteExam(event);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class ExamViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView subjectTextView;
        TextView detailsTextView;
        ImageButton optionsButton;

        ExamViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.exam_date);
            subjectTextView = itemView.findViewById(R.id.exam_subject);
            detailsTextView = itemView.findViewById(R.id.exam_details);
            optionsButton = itemView.findViewById(R.id.btn_exam_options);
        }
    }
}
