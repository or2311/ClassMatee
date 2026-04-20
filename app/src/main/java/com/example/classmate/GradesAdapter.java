package com.example.classmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מתאם הציונים (GradesAdapter).
 * תפקידו לקחת את רשימת הציונים של התלמיד ולהציג אותם בתוך רשימה נגוללת (RecyclerView).
 * הוא מחבר בין נתוני הציונים לבין העיצוב הגרפי של כל שורה.
 */
public class GradesAdapter extends RecyclerView.Adapter<GradesAdapter.GradeViewHolder> {

    private List<Grade> gradeList; // רשימת הציונים שנציג
    // פורמט להצגת תאריך בצורה נוחה (יום/חודש/שנה)
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // בנאי המקבל את רשימת הציונים ההתחלתית
    public GradesAdapter(List<Grade> gradeList) {
        this.gradeList = gradeList;
    }

    @NonNull
    @Override
    public GradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יוצר את העיצוב של שורה בודדת מתוך קובץ ה-XML (item_grade)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grade, parent, false);
        return new GradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GradeViewHolder holder, int position) {
        // לוקח ציון אחד מהרשימה ומציג את פרטיו בשורה המתאימה
        Grade grade = gradeList.get(position);
        holder.subjectText.setText(grade.getSubject()); // שם המקצוע
        holder.scoreText.setText(String.valueOf(grade.getScore())); // הציון עצמו
        holder.dateText.setText(dateFormat.format(new Date(grade.getTimestamp()))); // תאריך המבחן
    }

    @Override
    public int getItemCount() {
        // מחזיר את מספר הציונים שיש ברשימה
        return gradeList.size();
    }

    /**
     * פונקציה לעדכון הרשימה בנתונים חדשים (למשל לאחר טעינה מהאינטרנט).
     */
    public void updateGrades(List<Grade> newGrades) {
        this.gradeList = newGrades;
        notifyDataSetChanged(); // הוראה לרענן את התצוגה על המסך
    }

    /**
     * מחלקה פנימית המחזיקה את רכיבי התצוגה של שורה אחת ברשימת הציונים.
     */
    static class GradeViewHolder extends RecyclerView.ViewHolder {
        TextView subjectText, scoreText, dateText;

        public GradeViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור המשתנים לרכיבים הגרפיים שב-XML
            subjectText = itemView.findViewById(R.id.grade_subject_text);
            scoreText = itemView.findViewById(R.id.grade_score_text);
            dateText = itemView.findViewById(R.id.grade_date_text);
        }
    }
}
