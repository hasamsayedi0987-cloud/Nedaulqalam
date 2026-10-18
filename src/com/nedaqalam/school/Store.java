package com.nedaqalam.school;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/** هسته ذخیره‌سازی: کاربران، نقش‌ها، بخش‌ها، رکوردها (JSON آفلاین) */
public class Store {
    public static final String SEC_T = "t";    // معلمین
    public static final String SEC_S = "s";    // شاگردان
    public static final String SEC_HT = "ht";  // سرمعلمین
    public static final String SEC_E = "e";    // کارمندان
    public static final String SEC_P = "p";    // اولیا
    public static final String SEC_LIB = "lib";// کتاب‌خانه
    public static final String SEC_DOCS = "docs"; // اسناد

    public static String[] ROLES;
    public static ArrayList<User> users = new ArrayList<>();
    public static ArrayList<Sec> sections = new ArrayList<>();
    public static ArrayList<Rec> records = new ArrayList<>();
    public static LinkedHashMap<String, ArrayList<String>> rolePerms = new LinkedHashMap<>();

    private static long idCounter = 0;

    public static String newId() {
        idCounter++;
        return Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(idCounter);
    }

    public static void init(Context ctx) {
        ROLES = new String[]{
            ctx.getString(R.string.role_admin),
            ctx.getString(R.string.role_teacher),
            ctx.getString(R.string.role_head),
            ctx.getString(R.string.role_student),
            ctx.getString(R.string.role_staff),
            ctx.getString(R.string.role_parent),
            ctx.getString(R.string.role_other)
        };

        users.clear();
        sections.clear();
        records.clear();
        rolePerms.clear();

        try {
            JSONObject all = loadJson(ctx, "school.json");
            JSONArray ua = all.optJSONArray("users");
            if (ua != null) for (int i = 0; i < ua.length(); i++) {
                JSONObject o = ua.optJSONObject(i);
                if (o != null) users.add(User.fromJson(o));
            }
            JSONArray sa = all.optJSONArray("sections");
            if (sa != null) for (int i = 0; i < sa.length(); i++) {
                JSONObject o = sa.optJSONObject(i);
                if (o != null) sections.add(Sec.fromJson(o));
            }
            JSONArray ra = all.optJSONArray("records");
            if (ra != null) for (int i = 0; i < ra.length(); i++) {
                JSONObject o = ra.optJSONObject(i);
                if (o != null) records.add(Rec.fromJson(o));
            }
            JSONObject pa = all.optJSONObject("rolePerms");
            if (pa != null) {
                java.util.Iterator<String> it = pa.keys();
                while (it.hasNext()) {
                    String role = it.next();
                    ArrayList<String> list = new ArrayList<>();
                    JSONArray arr = pa.optJSONArray(role);
                    if (arr != null) for (int i = 0; i < arr.length(); i++) list.add(arr.optString(i));
                    rolePerms.put(role, list);
                }
            }
        } catch (Exception ignored) {}

        // بخش‌های اصلی — همیشه موجود
        ensureSection(SEC_T, ctx.getString(R.string.sec_t), false);
        ensureSection(SEC_S, ctx.getString(R.string.sec_s), false);
        ensureSection(SEC_HT, ctx.getString(R.string.sec_ht), false);
        ensureSection(SEC_E, ctx.getString(R.string.sec_e), false);
        ensureSection(SEC_P, ctx.getString(R.string.sec_p), false);
        ensureSection(SEC_LIB, ctx.getString(R.string.sec_lib), false);
        ensureSection(SEC_DOCS, ctx.getString(R.string.sec_docs), false);

        // دسترسی پیش‌فرض هر نقش
        ensurePerms(ctx.getString(R.string.role_teacher), new String[]{SEC_LIB, SEC_DOCS});
        ensurePerms(ctx.getString(R.string.role_head), new String[]{SEC_LIB, SEC_DOCS, SEC_T});
        ensurePerms(ctx.getString(R.string.role_student), new String[]{SEC_LIB});
        ensurePerms(ctx.getString(R.string.role_staff), new String[]{SEC_DOCS});
        ensurePerms(ctx.getString(R.string.role_parent), new String[]{SEC_DOCS});
        ensurePerms(ctx.getString(R.string.role_other), new String[]{SEC_DOCS});

        save(ctx);
    }

    private static void ensureSection(String id, String title, boolean custom) {
        for (Sec s : sections) if (s.id.equals(id)) { s.title = title; s.custom = custom; return; }
        Sec s = new Sec();
        s.id = id;
        s.title = title;
        s.custom = custom;
        s.created = System.currentTimeMillis();
        sections.add(s);
    }

    private static void ensurePerms(String role, String[] secIds) {
        if (rolePerms.containsKey(role)) return;
        ArrayList<String> list = new ArrayList<>();
        for (String s : secIds) list.add(s);
        rolePerms.put(role, list);
    }

    // ---------- persistence ----------

    private static JSONObject loadJson(Context ctx, String name) throws Exception {
        File f = new File(ctx.getFilesDir(), name);
        if (!f.exists()) return new JSONObject();
        InputStream in = new FileInputStream(f);
        String txt = readStream(in);
        return new JSONObject(txt);
    }

    public static String readStream(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
        in.close();
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void save(Context ctx) {
        try {
            JSONObject all = new JSONObject();
            JSONArray ua = new JSONArray();
            for (User u : users) ua.put(u.toJson());
            all.put("users", ua);
            JSONArray sa = new JSONArray();
            for (Sec s : sections) sa.put(s.toJson());
            all.put("sections", sa);
            JSONArray ra = new JSONArray();
            for (Rec r : records) ra.put(r.toJson());
            all.put("records", ra);
            JSONObject pa = new JSONObject();
            for (java.util.Map.Entry<String, ArrayList<String>> e : rolePerms.entrySet()) {
                JSONArray arr = new JSONArray();
                for (String s : e.getValue()) arr.put(s);
                pa.put(e.getKey(), arr);
            }
            all.put("rolePerms", pa);
            File f = new File(ctx.getFilesDir(), "school.json");
            OutputStream os = new FileOutputStream(f);
            os.write(all.toString(2).getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception ignored) {}
    }

    // ---------- users & auth ----------

    public static String hash(String salt, String pass) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((salt + ":" + pass).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String newSalt() {
        SecureRandom r = new SecureRandom();
        byte[] salt = new byte[16];
        r.nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static User findUser(String username) {
        for (User u : users) if (u.username.equalsIgnoreCase(username)) return u;
        return null;
    }

    public static boolean login(String username, String pass) {
        User u = findUser(username);
        return u != null && u.active && u.hash.equals(hash(u.salt, pass));
    }

    public static boolean createUser(Context ctx, String fullName, String username,
                                     String pass, String role) {
        username = username.trim();
        if (username.isEmpty() || findUser(username) != null) return false;
        User u = new User();
        u.id = System.currentTimeMillis();
        u.fullName = fullName.trim();
        u.username = username;
        u.salt = newSalt();
        u.hash = hash(u.salt, pass);
        u.role = role;
        u.active = true;
        u.created = System.currentTimeMillis();
        users.add(u);
        save(ctx);
        return true;
    }

    public static void setUserPass(Context ctx, User u, String newPass) {
        u.salt = newSalt();
        u.hash = hash(u.salt, newPass);
        save(ctx);
    }

    public static void removeUser(Context ctx, User u) {
        users.remove(u);
        save(ctx);
    }

    public static int managerCount() {
        int n = 0;
        for (User u : users) {
            if (User.ROLE_ADMIN.equals(u.role) && u.active) n++;
        }
        return n;
    }

    // ---------- sections ----------

    public static Sec secById(String id) {
        for (Sec s : sections) if (s.id.equals(id)) return s;
        return null;
    }

    public static void addSection(Context ctx, String title) {
        Sec s = new Sec();
        s.id = newId();
        s.title = title.trim();
        s.custom = true;
        s.created = System.currentTimeMillis();
        sections.add(s);
        save(ctx);
    }

    public static void renameSection(Context ctx, Sec s, String title) {
        s.title = title.trim();
        save(ctx);
    }

    public static void deleteSection(Context ctx, Sec s) {
        sections.remove(s);
        ArrayList<Rec> keep = new ArrayList<>();
        for (Rec r : records) {
            if (!r.sectionId.equals(s.id)) keep.add(r);
        }
        records = keep;
        save(ctx);
    }

    public static boolean canSee(String role, String sectionId) {
        if (User.ROLE_ADMIN.equals(role)) return true;
        ArrayList<String> list = rolePerms.get(role);
        if (list == null) return false;
        return list.contains(sectionId);
    }

    // ---------- records ----------

    public static Rec recById(String id) {
        for (Rec r : records) if (r.id.equals(id)) return r;
        return null;
    }

    public static ArrayList<Rec> recsOf(String sectionId) {
        ArrayList<Rec> out = new ArrayList<>();
        for (Rec r : records) if (r.sectionId.equals(sectionId)) out.add(r);
        return out;
    }

    public static void upsertRec(Context ctx, Rec r) {
        r.updated = System.currentTimeMillis();
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id.equals(r.id)) {
                records.set(i, r);
                save(ctx);
                return;
            }
        }
        r.created = r.updated;
        records.add(r);
        save(ctx);
    }

    public static void deleteRec(Context ctx, Rec r) {
        if (r.fileName != null && !r.fileName.isEmpty()) {
            try {
                File f = attachmentFile(ctx, r.id, r.fileName);
                if (f != null && f.exists()) f.delete();
            } catch (Exception ignored) {}
        }
        records.remove(r);
        save(ctx);
    }

    public static File attachDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "attach");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File attachmentFile(Context ctx, String recId, String fileName) {
        String safe = fileName.replaceAll("[^\\w.\\-\\u0600-\\u06FF]", "_");
        return new File(attachDir(ctx), recId + "_" + safe);
    }

    public static String fullDump() {
        try {
            JSONObject all = new JSONObject();
            JSONArray ua = new JSONArray();
            for (User u : users) ua.put(u.toJson());
            all.put("users", ua);
            JSONArray sa = new JSONArray();
            for (Sec s : sections) sa.put(s.toJson());
            all.put("sections", sa);
            JSONArray ra = new JSONArray();
            for (Rec r : records) ra.put(r.toJson());
            all.put("records", ra);
            JSONObject pa = new JSONObject();
            for (java.util.Map.Entry<String, ArrayList<String>> e : rolePerms.entrySet()) {
                JSONArray arr = new JSONArray();
                for (String s : e.getValue()) arr.put(s);
                pa.put(e.getKey(), arr);
            }
            all.put("rolePerms", pa);
            all.put("app", "Neda-al-Qalam School Manager v1.0");
            return all.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Set<String> permRoles() {
        return rolePerms.keySet();
    }

    public static ArrayList<String> permsOf(String role) {
        ArrayList<String> l = rolePerms.get(role);
        return l == null ? new ArrayList<String>() : l;
    }

    public static void setPerms(Context ctx, String role, ArrayList<String> secIds) {
        rolePerms.put(role, secIds);
        save(ctx);
    }
}
