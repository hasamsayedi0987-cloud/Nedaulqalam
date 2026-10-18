package com.nedaqalam.school;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** ثبت/ویرایش یک رکورد + پیوست فایل (txt / pdf / word / هر فایل) */
public class RecordEditorActivity extends Activity {

    private static final int REQ_PICK = 60;
    private static final int REQ_SAVE_OUT = 61;
    private static final int MAX_ATTACH = 15 * 1024 * 1024;

    private String sectionId;
    private Rec rec;
    private boolean isNew = false;
    private boolean readOnly = false;

    private EditText etTitle, etCode, etPhone, etNote;
    private TextView tvAttach, btnAttach, btnOpen, btnRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_editor);

        readOnly = !App.isManager();

        String recId = getIntent().getStringExtra("recId");
        sectionId = getIntent().getStringExtra("sectionId");
        if (sectionId == null) sectionId = Store.SEC_DOCS;
        if (recId != null && !recId.isEmpty()) {
            Rec found = Store.recById(recId);
            if (found != null) rec = found;
        }
        if (rec == null) {
            rec = new Rec();
            rec.id = Store.newId();
            rec.sectionId = sectionId;
            isNew = true;
        }

        TextView tvTop = findViewById(R.id.tvEdTitle);
        tvTop.setText(isNew ? R.string.add_record : R.string.edit_record);

        etTitle = findViewById(R.id.etTitle);
        etCode = findViewById(R.id.etCode);
        etPhone = findViewById(R.id.etPhone);
        etNote = findViewById(R.id.etNote);
        tvAttach = findViewById(R.id.tvAttach);
        btnAttach = findViewById(R.id.btnAttach);
        btnOpen = findViewById(R.id.btnOpen);
        btnRemove = findViewById(R.id.btnRemove);

        etTitle.setText(rec.title);
        etCode.setText(rec.code);
        etPhone.setText(rec.phone);
        etNote.setText(rec.note);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickFile(); }
        });
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openOrSaveOut(); }
        });
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { removeAttach(); }
        });

        View btnSave = findViewById(R.id.btnSave);
        View btnDelete = findViewById(R.id.btnDelete);
        if (readOnly) {
            btnSave.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnAttach.setVisibility(View.GONE);
            btnRemove.setVisibility(View.GONE);
            etTitle.setEnabled(false);
            etCode.setEnabled(false);
            etPhone.setEnabled(false);
            etNote.setEnabled(false);
        } else {
            btnDelete = findViewById(R.id.btnDelete);
            if (isNew) {
                btnDelete.setVisibility(View.GONE);
            } else {
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        new AlertDialog.Builder(RecordEditorActivity.this)
                            .setMessage(R.string.del_record_q)
                            .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface d, int w) {
                                    Store.deleteRec(RecordEditorActivity.this, rec);
                                    Toast.makeText(RecordEditorActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    }
                });
            }
        }
        styleAttach();
    }

    private void styleAttach() {
        boolean has = rec.fileName != null && !rec.fileName.isEmpty();
        tvAttach.setVisibility(has ? View.VISIBLE : View.GONE);
        btnOpen.setVisibility(has ? View.VISIBLE : View.GONE);
        btnRemove.setVisibility(has && !readOnly ? View.VISIBLE : View.GONE);
        if (has) {
            String label = String.format(getString(R.string.attached_fmt),
                    rec.fileName, rec.sizeLabel());
            tvAttach.setText(label);
        }
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_PICK);
        } catch (Exception e) {
            Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void openOrSaveOut() {
        if (rec.fileName == null || rec.fileName.isEmpty()) return;
        File f = Store.attachmentFile(this, rec.id, rec.fileName);
        if (!f.exists()) {
            Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        String lower = rec.fileName.toLowerCase();
        if (lower.endsWith(".txt")) {
            try {
                String text = Store.readStream(new FileInputStream(f));
                new AlertDialog.Builder(this)
                    .setTitle(rec.fileName)
                    .setMessage(text.length() > 4000 ? text.substring(0, 4000) + "\u2026" : text)
                    .setPositiveButton(R.string.save_out, new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { saveOut(); }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            } catch (Exception e) {
                saveOut();
            }
        } else {
            saveOut();
        }
    }

    private void saveOut() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_TITLE, rec.fileName);
        try {
            startActivityForResult(i, REQ_SAVE_OUT);
        } catch (Exception e) {
            Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void removeAttach() {
        try {
            File f = Store.attachmentFile(this, rec.id, rec.fileName);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
        rec.fileName = "";
        rec.fileSize = 0;
        styleAttach();
        Toast.makeText(this, R.string.attach_removed, Toast.LENGTH_SHORT).show();
    }

    private void save() {
        rec.title = etTitle.getText().toString().trim();
        rec.code = etCode.getText().toString().trim();
        rec.phone = etPhone.getText().toString().trim();
        rec.note = etNote.getText().toString().trim();
        if (rec.title.isEmpty() && rec.note.isEmpty()) {
            Toast.makeText(this, R.string.empty_not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }
        Store.upsertRec(this, rec);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();

        if (requestCode == REQ_PICK && uri != null) {
            try {
                String name = queryName(uri);
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) return;
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r, total = 0;
                while ((r = in.read(buf)) > 0) {
                    total += r;
                    if (total > MAX_ATTACH) {
                        in.close();
                        Toast.makeText(this, R.string.attach_too_big, Toast.LENGTH_LONG).show();
                        return;
                    }
                    bo.write(buf, 0, r);
                }
                in.close();
                String fname = name == null || name.trim().isEmpty() ? "file_" + System.currentTimeMillis() : name;
                if (rec.fileName != null && !rec.fileName.isEmpty()) {
                    File old = Store.attachmentFile(this, rec.id, rec.fileName);
                    if (old.exists()) old.delete();
                }
                File out = Store.attachmentFile(this, rec.id, fname);
                OutputStream os = new java.io.FileOutputStream(out);
                os.write(bo.toByteArray());
                os.close();
                rec.fileName = fname;
                rec.fileSize = out.length();
                styleAttach();
                Toast.makeText(this, R.string.attach_ok, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_SAVE_OUT && uri != null) {
            try {
                File f = Store.attachmentFile(this, rec.id, rec.fileName);
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(Store.readStream(new FileInputStream(f)).getBytes(StandardCharsets.UTF_8));
                    os.close();
                    Toast.makeText(this, R.string.exp_done, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String queryName(Uri uri) {
        android.database.Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }
}
