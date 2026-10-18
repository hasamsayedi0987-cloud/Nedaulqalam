package com.nedaqalam.school;

import org.json.JSONObject;

/** یک بخش از مکتب: معلمین، شاگردان، کتاب‌خانه، اسناد یا بخش دلخواه */
public class Sec {
    public String id = "";
    public String title = "";
    public boolean custom = false;
    public long created;

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title);
            o.put("custom", custom);
            o.put("created", created);
        } catch (Exception ignored) {}
        return o;
    }

    public static Sec fromJson(JSONObject o) {
        Sec s = new Sec();
        s.id = o.optString("id", "");
        s.title = o.optString("title", "");
        s.custom = o.optBoolean("custom", false);
        s.created = o.optLong("created", 0);
        return s;
    }
}
