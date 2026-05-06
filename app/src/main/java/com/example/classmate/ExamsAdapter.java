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

/**
 * מתאם המבחנים (ExamsAdapter).
 * תפקידו לקחת את רשימת המבחנים/אירועים ולהציג אותם בתוך רשימה נגוללת (RecyclerView).
 * הוא אחראי על העיצוב של כל שורה ברשימה ועל הקישור בין הנתונים למסך.
 */
public class ExamsAdapter extends RecyclerView.Adapter<ExamsAdapter.ExamViewHolder> {

    /**
     * ממשק המגדיר אילו פעולות ניתן לבצע על מבחן ברשימה.
     */
    public interface OnExamActionListener {
        void onViewDetails(Event event);    // צפייה בפרטים
        void onDeleteExam(Event event);     // מחיקה
        void onAddToCalendar(Event event);  // הוספה ליומן האישי בטלפון
    }

    private List<Event> events; // רשימת האירועים שנציג
    private final OnExamActionListener listener; // מי שמאזין לפעולות המשתמש
    
    // פורמט להצגת התאריך (למשל: 15 יוני)
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd\nMMM", Locale.forLanguageTag("he"));

    // בנאי המקבל את רשימת האירועים והמאזין
    public ExamsAdapter(List<Event> events, OnExamActionListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * מעדכן את הרשימה בנתונים חדשים ומרענן את המסך.
     */
    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יוצר את העיצוב של שורה בודדת מתוך קובץ ה-XML (item_exam)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exam, parent, false);
        return new ExamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        // מחבר את הנתונים של אירוע ספציפי לשורה המתאימה ברשימה
        Event event = events.get(position);
        
        // הצגת התאריך והמקצוע
        holder.dateTextView.setText(dateFormat.format(new Date(event.getTimestamp())));
        holder.subjectTextView.setText(event.getSubject());

        // בניית מחרוזת פרטים (סוג האירוע והחומר למבחן)
        StringBuilder details = new StringBuilder(event.getType());
        if (event.getMaterial() != null && !event.getMaterial().isEmpty()) {
            details.append(" | ").append(event.getMaterial());
        }
        holder.detailsTextView.setText(details.toString());

        // הגדרת תפריט אפשרויות (שלוש נקודות) לכל מבחן
        holder.optionsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 0, 0, "פרטים");
            popup.getMenu().add(0, 1, 1, "הוסף ליומן הטלפון");
            popup.getMenu().add(0, 2, 2, "מחיקה");
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 0) {
                    listener.onViewDetails(event);
                } else if (id == 1) {
                    listener.onAddToCalendar(event);
                } else if (id == 2) {
                    listener.onDeleteExam(event);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        // מחזיר את מספר האירועים שיש ברשימה
        return events != null ? events.size() : 0;
    }

    /**
     * מחלקה פנימית המחזיקה את רכיבי התצוגה של שורה אחת.
     */
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
