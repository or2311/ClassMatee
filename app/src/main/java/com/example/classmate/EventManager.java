package com.example.classmate;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * מנהל האירועים (EventManager).
 * מחלקה זו מרכזת את כל הפעולות מול בסיס הנתונים (Firebase) שקשורות לאירועים,
 * כגון טעינת מבחנים, שמירת אירוע חדש ומחיקת אירועים.
 */
public class EventManager {

    /**
     * ממשק (Interface) המשמש להחזרת רשימת האירועים למסך שביקש אותם.
     */
    public interface OnEventsLoadedListener {
        void onEventsLoaded(List<Event> events); // רץ כשהטעינה הצליחה
        void onError(String error);             // רץ כשיש תקלה
    }

    /**
     * טוען את כל האירועים השייכים לכיתה מסוימת מהענן.
     * @param className שם הכיתה
     * @param listener המאזין שיקבל את התוצאות
     */
    public static void loadEvents(String className, OnEventsLoadedListener listener) {
        if (className == null || className.isEmpty()) {
            listener.onEventsLoaded(new ArrayList<>());
            return;
        }
        
        // פנייה ל-Firebase כדי לקבל את כל האירועים של הכיתה
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Event> events = new ArrayList<>();
                        // מעבר על כל מסמך שהתקבל והפיכתו לאובייקט מסוג Event
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long timestamp = doc.getLong("timestamp");
                            Event event = new Event(
                                    doc.getId(),
                                    doc.getString("subject"),
                                    doc.getString("type"),
                                    timestamp != null ? timestamp : 0L,
                                    doc.getString("material"),
                                    doc.getString("notes"),
                                    doc.getString("className"),
                                    doc.getString("createdBy"),
                                    Boolean.TRUE.equals(doc.getBoolean("isGlobal"))
                            );
                            events.add(event);
                        }
                        listener.onEventsLoaded(events); // החזרת הרשימה המלאה
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "שגיאה בטעינה";
                        listener.onError(err);
                    }
                });
    }

    /**
     * שומר אירוע חדש (כמו מבחן או תזכורת) בבסיס הנתונים בענן.
     */
    public static void saveEvent(Event event, OnEventSavedListener listener) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("subject", event.getSubject() != null ? event.getSubject() : "");
        data.put("type", event.getType() != null ? event.getType() : "מבחן");
        data.put("timestamp", event.getTimestamp());
        data.put("material", event.getMaterial() != null ? event.getMaterial() : "");
        data.put("notes", event.getNotes() != null ? event.getNotes() : "");
        data.put("className", event.getClassName() != null ? event.getClassName() : "");
        data.put("createdBy", event.getCreatedBy() != null ? event.getCreatedBy() : "");
        data.put("isGlobal", event.isGlobal());

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

    /**
     * מוחק אירוע קיים מהענן לפי המזהה שלו.
     */
    public static void deleteEvent(Context context, String eventId) {
        if (eventId == null || eventId.isEmpty()) return;
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "האירוע נמחק", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
    }

    public interface OnEventSavedListener {
        void onSaved();
        void onError(String error);
    }
}
