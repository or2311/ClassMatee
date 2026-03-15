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

public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {

    public interface OnStudentActionListener {
        void onEditStudent(Student student);
        void onDeleteStudent(Student student);
    }

    private List<Student> students;
    private final boolean isAdmin;
    private final OnStudentActionListener listener;

    public StudentsAdapter(List<Student> students, boolean isAdmin, OnStudentActionListener listener) {
        this.students = students;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void updateStudents(List<Student> newStudents) {
        this.students = newStudents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.nameTextView.setText(student.getFullName());
        holder.usernameTextView.setText(student.getEmail());

        if (isAdmin) {
            holder.adminActionsLayout.setVisibility(View.VISIBLE);
            holder.editButton.setOnClickListener(v -> listener.onEditStudent(student));
            holder.deleteButton.setOnClickListener(v -> listener.onDeleteStudent(student));
        } else {
            holder.adminActionsLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return students != null ? students.size() : 0;
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView usernameTextView;
        LinearLayout adminActionsLayout;
        ImageButton editButton;
        ImageButton deleteButton;

        StudentViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.student_name);
            usernameTextView = itemView.findViewById(R.id.student_username);
            adminActionsLayout = itemView.findViewById(R.id.admin_actions_layout);
            editButton = itemView.findViewById(R.id.btn_edit_student);
            deleteButton = itemView.findViewById(R.id.btn_delete_student);
        }
    }
}
