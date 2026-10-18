package com.nedaqalam.school;

import org.json.JSONObject;

/** یک رکورد ثبت‌شده در هر بخش (معلم/شاگرد/کتاب/سند/...) */
public class Rec {
    public String id = "";
    public String sectionId = "";
    public String title = "";
    public String code = "";
    public String phone = "";
    public String note = "";
    public String fileName = "";   // پیوست
    public long fileSize;
    public long created;
    public long updated;

    public String typeLabel() {
        if (fileName == null || fileName.isEmpty()) return "";
        String f = fileName.toLowerCase();
        if (f.endsWith(".pdf")) return "PDF";
        if (f.endsWith(".doc") || f.endsWith(".docx")) return "Word";
        if (f.endsWith(".txt")) return "TXT";
        return "\uD83D\uDCCE";
    }

    public String sizeLabel() {
        if (fileSize <= 0) return "";
        if (fileSize < 1024) return Fa.d((int) fileSize) + " B";
        if (fileSize < 1024 * 1024) return Fa.d((int) (fileSize / 1024)) + " KB";
        return Fa.d((int) (fileSize / (1024 * 1024))) + " MB";
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("sectionId", sectionId);
            o.put("title", title);
            o.put("code", code);
            o.put("phone", phone);
            o.put("note", note);
            o.put("fileName", fileName);
            o.put("fileSize", fileSize);
            o.put("created", created);
            o.put("updated", updated);
        } catch (Exception ignored) {}
        return o;
    }

    public static Rec fromJson(JSONObject o) {
        Rec r = new Rec();
        r.id = o.optString("id", "");
        r.sectionId = o.optString("sectionId", "");
        r.title = o.optString("title", "");
        r.code = o.optString("code", "");
        r.phone = o.optString("phone", "");
        r.note = o.optString("note", "");
        r.fileName = o.optString("fileName", "");
        r.fileSize = o.optLong("fileSize", 0);
        r.created = o.optLong("created", 0);
        r.updated = o.optLong("updated", 0);
        return r;
    }
}
