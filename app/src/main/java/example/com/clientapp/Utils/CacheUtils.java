package example.com.clientapp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheUtils {
        public static boolean getBoolean(Context context, String key) {
            SharedPreferences sp = context.getSharedPreferences("Bleapp", Context.MODE_PRIVATE);
            return sp.getBoolean(key,false);
        }


        public static void putBoolean(Context context, String key, boolean value) {
            SharedPreferences sp = context.getSharedPreferences("Bleapp", Context.MODE_PRIVATE);
            sp.edit().putBoolean(key,value).commit();
        }


        public static void putString(Context context, String key, String value) {
            SharedPreferences sp = context.getSharedPreferences("Bleapp", Context.MODE_PRIVATE);
            sp.edit().putString(key,value).commit();

        }


        public static String getString(Context context, String key) {
            SharedPreferences sp = context.getSharedPreferences("Bleapp", Context.MODE_PRIVATE);
            return sp.getString(key,"");
        }

}