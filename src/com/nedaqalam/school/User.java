package com.nedaqalam.school;

import org.json.JSONObject;

/** کاربر سیستم: مدیر، معلم، سرمعلم، شاگرد، کارمند، ولی، سایر */
public class User {
    public static final String ROLE_ADMIN = "\u0645\u062F\u06CC\u0631"; // مدیر

    public long id;
    public String fullName = "";
    public String username = "";
    public String salt = "";
    public String hash = "";
    public String role = "";
    public boolean active = true;
    public long created;

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("fullName", fullName);
            o.put("username", username);
            o.put("salt", salt);
            o.put("hash", hash);
            o.put("role", role);
            o.put("active", active);
            o.put("created", created);
        } catch (Exception ignored) {}
        return o;
    }

    public static User fromJson(JSONObject o) {
        User u = new User();
        u.id = o.optLong("id", 0);
        u.fullName = o.optString("fullName", "");
        u.username = o.optString("username", "");
        u.salt = o.optString("salt", "");
        u.hash = o.optString("hash", "");
        u.role = o.optString("role", "");
        u.active = o.optBoolean("active", true);
        u.created = o.optLong("created", 0);
        return u;
    }
}
