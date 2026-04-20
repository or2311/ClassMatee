package com.example.classmate;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * מחלקה זו עוזרת לנו לנהל את מצב התצוגה של האפליקציה (מצב בהיר או כהה).
 * היא משתמשת ב-"SharedPreferences" שזהו זיכרון קטן בטלפון ששומר הגדרות גם כשהאפליקציה נסגרת.
 */
public class ThemeHelper {

    // שם הקובץ הפנימי שבו נשמרות ההגדרות בתוך הטלפון
    private static final String PREF_NAME = "theme_prefs";
    
    // מילת מפתח שתעזור לנו למצוא את ההגדרה הספציפית של המצב הכהה בתוך הקובץ
    private static final String KEY_PREFIX = "is_dark_mode_";

    /**
     * פונקציה זו שומרת את הבחירה של המשתמש (בהיר או כהה) בזיכרון של הטלפון.
     * @param userId המזהה הייחודי של המשתמש (כדי שכל משתמש יוכל לבחור מצב אחר).
     * @param isDarkMode האם המשתמש בחר במצב כהה? (אמת או שקר).
     */
    public static void saveThemeMode(Context context, String userId, boolean isDarkMode) {
        if (userId == null) return; // אם אין משתמש, לא שומרים כלום
        
        // פתיחת ה-"מגירה" בזיכרון של הטלפון שבה נשמרות ההגדרות
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // שמירת הבחירה: אנחנו מצמידים את המצב הכהה ל-ID של המשתמש
        editor.putBoolean(KEY_PREFIX + userId, isDarkMode);
        editor.apply(); // אישור סופי של השמירה
    }

    /**
     * פונקציה זו בודקת מה המשתמש בחר ומשנה את צבעי האפליקציה בהתאם.
     * @param userId המזהה של המשתמש שאת ההגדרות שלו אנחנו רוצים להפעיל.
     */
    public static void applyTheme(Context context, String userId) {
        if (userId == null) return;
        
        // קריאת המצב השמור מהזיכרון. אם לא מצאנו כלום, נניח שהמשתמש רוצה מצב בהיר (false).
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_PREFIX + userId, false);
        
        // פקודה למערכת אנדרואיד לשנות את צבעי המסך
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // הפעלת מצב לילה
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // הפעלת מצב יום
        }
    }

    /**
     * פונקציה זו רק בודקת ומחזירה לנו: האם המשתמש הנוכחי נמצא כרגע במצב כהה?
     */
    public static boolean isDarkMode(Context context, String userId) {
        if (userId == null) return false;
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_PREFIX + userId, false);
    }
}
