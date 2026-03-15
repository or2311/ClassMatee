package com.example.classmate;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventManager {

    public interface OnEventsLoadedListener {
        void onEventsLoaded(List<Event> events);
        void onError(String error);
    }

    public static void loadEvents(String className, OnEventsLoadedListener listener) {
        if (className == null || className.isEmpty()) {
            listener.onEventsLoaded(new ArrayList<>());
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Event> events = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long timestamp = doc.getLong("timestamp");
                            Event event = new Event(
                                    doc.getId(),
                                    doc.getString("subject"),
                                    doc.getString("type"),
                                    timestamp != null ? timestamp : 0L,
                                    doc.getString("material"),
                                    doc.getString("notes"),
                                    doc.getString("className")
                            );
                            events.add(event);
                        }
                        listener.onEventsLoaded(events);
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";
                        listener.onError(err);
                    }
                });
    }

    public static void saveEvent(Event event, OnEventSavedListener listener) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("subject", event.getSubject());
        data.put("type", event.getType());
        data.put("timestamp", event.getTimestamp());
        data.put("material", event.getMaterial());
        data.put("notes", event.getNotes());
        data.put("className", event.getClassName());

        FirebaseFirestore.getInstance()
                .collection("events")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (listener != null) listener.onSaved();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public static void deleteEvent(Context context, Event event, String className) {
        if (event.getId() == null || event.getId().isEmpty()) return;
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(event.getId())
                .delete()
                .addOnFailureListener(e ->
                        Toast.makeText(context, "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public interface OnEventSavedListener {
        void onSaved();
        void onError(String error);
    }
}
