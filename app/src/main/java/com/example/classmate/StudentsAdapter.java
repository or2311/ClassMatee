package com.example.classmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * מתאם התלמידים (StudentsAdapter).
 * תפקידו של קובץ זה הוא לקשר בין רשימת התלמידים (הנתונים) לבין הדרך שבה הם מוצגים על המסך.
 * הוא אחראי ליצור כל שורה ברשימה ולמלא אותה בפרטים של תלמיד ספציפי (שם ואימייל).
 */
public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {

    /**
     * ממשק (Interface) המגדיר אילו פעולות ניתן לבצע על תלמיד ברשימה.
     * מאפשר למסך שמשתמש ברשימה להגיב ללחיצות על עריכה או מחיקה.
     */
    public interface OnStudentActionListener {
        void onEditStudent(Student student);   // פעולה שתתבצע בעת לחיצה על עריכה
        void onDeleteStudent(Student student); // פעולה שתתבצע בעת לחיצה על מחיקה
    }

    private List<Student> students;           // רשימת התלמידים שמוצגת כרגע
    private final boolean isAdmin;            // האם המשתמש הצופה הוא מנהל (כדי להציג כפתורי ניהול)
    private final OnStudentActionListener listener; // המאזין שמבצע את הפעולות (עריכה/מחיקה)

    /**
     * בנאי (Constructor) - יוצר את המתאם עם הנתונים הראשוניים.
     */
    public StudentsAdapter(List<Student> students, boolean isAdmin, OnStudentActionListener listener) {
        this.students = students;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    /**
     * עדכון הרשימה בנתונים חדשים (למשל לאחר חיפוש או הוספת תלמיד) ורענון המסך.
     */
    public void updateStudents(List<Student> newStudents) {
        this.students = newStudents;
        notifyDataSetChanged(); // פקודה המורה למסך להתרענן ולהציג את הנתונים החדשים
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "מנפח" (Inflate) את העיצוב של שורה בודדת מתוך קובץ ה-XML (item_student)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        // לוקח את התלמיד שנמצא במיקום הנוכחי ברשימה
        Student student = students.get(position);
        
        // הצגת השם והאימייל של התלמיד בתיבות הטקסט המתאימות
        holder.nameTextView.setText(student.getFullName());
        holder.usernameTextView.setText(student.getEmail());

        // אם המשתמש הוא מנהל, נציג לו את כפתורי העריכה והמחיקה
        if (isAdmin) {
            holder.adminActionsLayout.setVisibility(View.VISIBLE); // הצגת הכלים לניהול
            
            // הגדרת מה קורה כשלוחצים על כפתור העריכה
            holder.editButton.setOnClickListener(v -> listener.onEditStudent(student));
            
            // הגדרת מה קורה כשלוחצים על כפתור המחיקה
            holder.deleteButton.setOnClickListener(v -> listener.onDeleteStudent(student));
        } else {
            // אם המשתמש הוא לא מנהל, נסתיר את אפשרויות העריכה והמחיקה
            holder.adminActionsLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // מחזיר את כמות התלמידים ברשימה כדי שהמערכת תדע כמה שורות ליצור
        return students != null ? students.size() : 0;
    }

    /**
     * מחלקה פנימית (ViewHolder) המחזיקה את כל רכיבי ה-UI של שורה בודדת.
     * חוסכת למערכת זמן על ידי שמירת הקישורים לרכיבים הגרפיים.
     */
    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;           // תיבת הטקסט לשם התלמיד
        TextView usernameTextView;       // תיבת הטקסט לאימייל/שם המשתמש
        LinearLayout adminActionsLayout;  // האזור המכיל את כפתורי הניהול
        ImageButton editButton;          // כפתור העריכה (אייקון עיפרון)
        ImageButton deleteButton;        // כפתור המחיקה (אייקון פח)

        StudentViewHolder(View itemView) {
            super(itemView);
            // קישור בין המשתנים בקוד לרכיבים הגרפיים שבקובץ ה-XML
            nameTextView = itemView.findViewById(R.id.student_name);
            usernameTextView = itemView.findViewById(R.id.student_username);
            adminActionsLayout = itemView.findViewById(R.id.admin_actions_layout);
            editButton = itemView.findViewById(R.id.btn_edit_student);
            deleteButton = itemView.findViewById(R.id.btn_delete_student);
        }
    }
}
