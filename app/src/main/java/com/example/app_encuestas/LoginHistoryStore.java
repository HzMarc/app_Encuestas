package com.example.app_encuestas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Stores recent successful app logins locally and syncs authenticated accounts to Firestore. */
public final class LoginHistoryStore {
    private static final String PREFS = "login_history_local";
    private static final String USERS_KEY = "users";
    private static final String EVENTS_KEY = "events";

    private LoginHistoryStore() { }

    public static final class UserRecord {
        public final String uid;
        public final String name;
        public final String email;
        public final long lastLogin;
        public final long loginCount;
        public final boolean active;

        UserRecord(String uid, String name, String email, long lastLogin, long loginCount,
                   boolean active) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.lastLogin = lastLogin;
            this.loginCount = loginCount;
            this.active = active;
        }

        public String formattedDate() {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(lastLogin));
        }
    }

    public static void recordLogin(Context context, String uid, String name, String email) {
        long now = System.currentTimeMillis();
        saveLocally(context, uid, name, email, now);
        if (uid == null || uid.trim().isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("lastLogin", now);
        user.put("loginCount", FieldValue.increment(1));
        user.put("active", true);

        Map<String, Object> event = new HashMap<>();
        event.put("uid", uid);
        event.put("name", name);
        event.put("email", email);
        event.put("timestamp", now);

        db.batch()
                .set(db.collection("users").document(uid), user, SetOptions.merge())
                .set(db.collection("login_history").document(), event)
                .commit();
    }

    public static void loadUsers(Activity activity, Consumer<List<UserRecord>> callback) {
        List<UserRecord> local = readUsers(activity);
        callback.accept(local);
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) return;
        FirebaseFirestore.getInstance().collection("users")
                .orderBy("lastLogin", Query.Direction.DESCENDING).limit(200).get()
                .addOnSuccessListener(activity, snapshot -> {
                    List<UserRecord> records = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        records.add(fromDocument(document));
                    }
                    callback.accept(records);
                })
                .addOnFailureListener(activity, ignored -> { });
    }

    public static void updateUserActive(Activity activity, UserRecord user, boolean active,
                                        Consumer<Boolean> callback) {
        updateLocalActive(activity, user.uid, user.email, active);
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (user.uid == null || user.uid.trim().isEmpty() || current == null) {
            callback.accept(true);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("uid", user.uid);
        updates.put("name", user.name);
        updates.put("email", user.email);
        updates.put("active", active);
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(activity, ignored -> callback.accept(true))
                .addOnFailureListener(activity, ignored -> callback.accept(false));
    }

    public static void checkAccountActive(Activity activity, String uid, String email,
                                          Consumer<Boolean> callback) {
        if (uid == null || uid.trim().isEmpty()
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.accept(isLocallyActive(activity, email));
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(activity, document -> {
                    boolean active = !document.exists() || !Boolean.FALSE.equals(document.getBoolean("active"));
                    updateLocalActive(activity, uid, email, active);
                    callback.accept(active);
                })
                .addOnFailureListener(activity, ignored -> callback.accept(false));
    }

    public static boolean isLocallyActive(Context context, String email) {
        if (email == null) return true;
        JSONArray users = readArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(USERS_KEY, "[]"));
        for (int i = 0; i < users.length(); i++) {
            JSONObject record = users.optJSONObject(i);
            if (record != null && email.equalsIgnoreCase(record.optString("email", ""))) {
                return record.optBoolean("active", true);
            }
        }
        return true;
    }

    public static boolean hasLocallyVerifiedActiveAccount(Context context, String email) {
        if (email == null || email.trim().isEmpty()) return false;
        JSONArray users = readArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(USERS_KEY, "[]"));
        for (int i = 0; i < users.length(); i++) {
            JSONObject record = users.optJSONObject(i);
            if (record != null && email.equalsIgnoreCase(record.optString("email", ""))) {
                return record.optBoolean("active", false);
            }
        }
        return false;
    }

    private static void updateLocalActive(Context context, String uid, String email, boolean active) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray users = readArray(prefs.getString(USERS_KEY, "[]"));
        JSONArray updated = new JSONArray();
        for (int i = 0; i < users.length(); i++) {
            JSONObject old = users.optJSONObject(i);
            if (old == null) continue;
            String oldEmail = old.optString("email", "");
            String oldUid = old.optString("uid", "");
            boolean match = (email != null && email.equalsIgnoreCase(oldEmail))
                    || (uid != null && !uid.isEmpty() && uid.equals(oldUid));
            if (match) {
                try { old.put("active", active); } catch (Exception ignored) { }
            }
            updated.put(old);
        }
        prefs.edit().putString(USERS_KEY, updated.toString()).apply();
    }

    public static void loadRecent(Activity activity, Consumer<List<UserRecord>> callback) {
        List<UserRecord> local = readEvents(activity, 8);
        callback.accept(local);
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) return;
        FirebaseFirestore.getInstance().collection("login_history")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(8).get()
                .addOnSuccessListener(activity, snapshot -> {
                    List<UserRecord> records = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        records.add(fromEvent(document));
                    }
                    callback.accept(records);
                })
                .addOnFailureListener(activity, ignored -> { });
    }

    public static void loadTodayCount(Activity activity, Consumer<Integer> callback) {
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        long startOfDay = day.getTimeInMillis();
        int localCount = 0;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray events = readArray(prefs.getString(EVENTS_KEY, "[]"));
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event != null && event.optLong("lastLogin", 0) >= startOfDay) localCount++;
        }
        callback.accept(localCount);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("login_history")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay).get()
                .addOnSuccessListener(activity, snapshot -> callback.accept(snapshot.size()))
                .addOnFailureListener(activity, ignored -> { });
    }

    private static void saveLocally(Context context, String uid, String name,
                                    String email, long now) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray users = readArray(prefs.getString(USERS_KEY, "[]"));
        JSONArray updatedUsers = new JSONArray();
        boolean found = false;
        boolean active = true;
        long count = 1;
        for (int i = 0; i < users.length(); i++) {
            JSONObject old = users.optJSONObject(i);
            if (old == null) continue;
            if (email.equalsIgnoreCase(old.optString("email", ""))) {
                count = old.optLong("loginCount", 0) + 1;
                active = old.optBoolean("active", true);
                found = true;
                continue;
            }
            updatedUsers.put(old);
        }
        JSONObject record = new JSONObject();
        try {
            record.put("uid", uid == null ? "" : uid);
            record.put("name", name);
            record.put("email", email);
            record.put("lastLogin", now);
            record.put("loginCount", found ? count : 1);
            record.put("active", active);
            updatedUsers.put(record);

            JSONArray events = readArray(prefs.getString(EVENTS_KEY, "[]"));
            JSONArray updatedEvents = new JSONArray();
            updatedEvents.put(record);
            for (int i = 0; i < events.length() && i < 49; i++) {
                JSONObject event = events.optJSONObject(i);
                if (event != null) updatedEvents.put(event);
            }
            prefs.edit().putString(USERS_KEY, updatedUsers.toString())
                    .putString(EVENTS_KEY, updatedEvents.toString()).apply();
        } catch (Exception ignored) { }
    }

    private static List<UserRecord> readUsers(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return decodeArray(readArray(prefs.getString(USERS_KEY, "[]")), false, 200);
    }

    private static List<UserRecord> readEvents(Context context, int limit) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return decodeArray(readArray(prefs.getString(EVENTS_KEY, "[]")), true, limit);
    }

    private static List<UserRecord> decodeArray(JSONArray array, boolean events, int limit) {
        List<UserRecord> records = new ArrayList<>();
        for (int i = 0; i < array.length() && records.size() < limit; i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            records.add(new UserRecord(item.optString("uid", ""),
                    item.optString("name", "Usuario"), item.optString("email", ""),
                    item.optLong("lastLogin", 0), events ? 1 : item.optLong("loginCount", 1),
                    events || item.optBoolean("active", true)));
        }
        records.sort((left, right) -> Long.compare(right.lastLogin, left.lastLogin));
        return records;
    }

    private static JSONArray readArray(String raw) {
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static UserRecord fromDocument(DocumentSnapshot document) {
        Long time = document.getLong("lastLogin");
        Long count = document.getLong("loginCount");
        return new UserRecord(document.getString("uid"), document.getString("name"),
                document.getString("email"), time == null ? 0 : time,
                count == null ? 1 : count,
                !Boolean.FALSE.equals(document.getBoolean("active")));
    }

    private static UserRecord fromEvent(DocumentSnapshot document) {
        Long time = document.getLong("timestamp");
        return new UserRecord(document.getString("uid"), document.getString("name"),
                document.getString("email"), time == null ? 0 : time, 1, true);
    }
}
