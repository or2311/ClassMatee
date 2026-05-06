package com.example.classmate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * עוזר ההתראות (NotificationHelper).
 * מחלקה זו אחראית על כל סוגי התקשורת עם המשתמש מחוץ למסכי האפליקציה הרגילים.
 * היא מטפלת בשלושה סוגים של התראות:
 * 1. התראות מקומיות בטלפון (Notifications בראש המסך).
 * 2. שליחת מיילים לתלמידים.
 * 3. שמירת התראות בתוך מסד הנתונים (Firebase) לצפייה עתידית.
 */
public class NotificationHelper {

    // מזהה ייחודי לערוץ ההתראות (נדרש בגרסאות אנדרואיד חדשות)
    private static final String CHANNEL_ID = "grades_notifications";
    private static final String CHANNEL_NAME = "ציונים חדשים";

    /**
     * פונקציה היוצרת התראה מקומית שמופיעה בשורת המשימות בראש הטלפון.
     * @param context הסביבה שבה האפליקציה רצה (Activity).
     * @param title כותרת ההתראה (למשל: "ציון חדש!").
     * @param message תוכן ההתראה (למשל: "קיבלת 95 במתמטיקה").
     */
    public static void sendLocalNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // מאנדרואיד 8 (Oreo) ומעלה, חייבים ליצור "ערוץ התראות" כדי שהן יוצגו
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // בניית ההתראה (אייקון, כותרת, טקסט וכו')
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_badge) // האייקון הקטן שיופיע למעלה
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // קביעת רמת חשיבות רגילה
                .setAutoCancel(true); // ההתראה תיעלם כשהמשתמש ילחץ עליה

        // שליחת ההתראה בפועל למערכת ההפעלה
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * פונקציה שפותחת את אפליקציית המייל (כמו Gmail) בטלפון עם טקסט מוכן מראש.
     * זוהי הדרך הפשוטה והאמינה ביותר לשלוח מייל מהמכשיר ללא צורך בשרת מורכב.
     */
    public static void sendEmailViaIntent(Context context, String studentEmail, String subject, int score) {
        if (studentEmail == null || studentEmail.isEmpty()) {
            Toast.makeText(context, "לא נמצאה כתובת מייל לתלמיד", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנת הנושא ותוכן המייל
        String mailSubject = "ציון חדש במערכת ClassMate";
        String mailBody = "שלום רב,\n\nהוזן לך ציון חדש ב: " + subject + ".\nהציון הוא: " + score + ".\n\nבהצלחה,\nצוות ClassMate.";

        // יצירת "כוונה" (Intent) לפתיחת אפליקציית מייל
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // מסנן כך שרק אפליקציות מייל יוצגו למשתמש
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{studentEmail}); // כתובת הנמען
        intent.putExtra(Intent.EXTRA_SUBJECT, mailSubject);              // נושא המייל
        intent.putExtra(Intent.EXTRA_TEXT, mailBody);                    // גוף המייל

        try {
            // פתיחת חלונית בחירת אפליקציית מייל
            context.startActivity(Intent.createChooser(intent, "שלח מייל לתלמיד..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "לא מותקנת אפליקציית מייל במכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * שומרת את ההתראה בתוך בסיס הנתונים (Firestore).
     * זה מאפשר לנו להציג לתלמיד רשימת התראות בתוך האפליקציה עצמה כשהוא נכנס.
     */
    public static void notifyStudentOfNewGrade(String studentId, String subject, int score) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notification = new HashMap<>();
        notification.put("studentId", studentId);
        notification.put("title", "ציון חדש!");
        notification.put("message", "הוזן לך ציון חדש ב: " + subject + " (ציון: " + score + ")");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false); // האם התלמיד כבר ראה את ההתראה (ברירת מחדל: לא)

        // הוספת ההתראה לאוסף ה-notifications בענן
        db.collection("notifications").add(notification);
    }

    /**
     * פונקציה לשליחת מייל אוטומטי "מאחורי הקלעים" דרך Firebase.
     * הערה: שיטה זו דורשת הגדרה של תוסף מיוחד בענן (Trigger Email Extension).
     */
    public static void sendEmailToStudentBackground(String studentEmail, String subject, int score) {
        if (studentEmail == null || studentEmail.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // יצירת מבנה הנתונים שהתוסף של Firebase מצפה לקבל
        Map<String, Object> mail = new HashMap<>();
        mail.put("to", studentEmail);
        
        Map<String, String> message = new HashMap<>();
        message.put("subject", "הוזן לך ציון חדש ב: " + subject);
        message.put("text", "הוזן לך ציון חדש ב: " + subject + ". הציון הוא: " + score);
        
        mail.put("message", message);
        
        // שמירה באוסף ה-"mail" מפעילה אוטומטית את שליחת המייל מהענן
        db.collection("mail").add(mail);
    }
}
