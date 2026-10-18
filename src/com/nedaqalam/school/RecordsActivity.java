package com.nedaqalam.school;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/** فهرست رکوردهای یک بخش + جست‌وجو + خروجی TXT / Word / PDF */
public class RecordsActivity extends Activity {

    private static final int REQ_TXT = 70;
    private static final int REQ_DOC = 71;
    private String sectionId;
    private String query = "";
    private RecAdapter adapter;
    private EditText etSearch;
    private TextView tvEmptyTitle, tvEmptySub;
    private WebView printView;
    private String pendingExport = null;
    private boolean printFired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        sectionId = getIntent().getStringExtra("sectionId");
        if (sectionId == null) sectionId = Store.SEC_DOCS;

        TextView tvSection = findViewById(R.id.tvSection);
        tvSection.setText(String.format(getString(R.string.records_of_fmt),
                ExportUtil.secTitle(sectionId)));

        etSearch = findViewById(R.id.etSearch);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySub = findViewById(R.id.tvEmptySub);
        printView = findViewById(R.id.printView);

        ListView list = findViewById(R.id.list);
        View emptyBox = findViewById(R.id.emptyBox);
        list.setEmptyView(emptyBox);
        adapter = new RecAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(RecordsActivity.this, RecordEditorActivity.class);
                i.putExtra("recId", adapter.data.get(pos).id);
                startActivity(i);
            }
        });
        list.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> p, View v, final int pos, long id) {
                if (!App.isManager()) return false;
                new AlertDialog.Builder(RecordsActivity.this)
                    .setMessage(R.string.del_record_q)
                    .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) {
                            Store.deleteRec(RecordsActivity.this, adapter.data.get(pos));
                            refresh();
                            Toast.makeText(RecordsActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
                return true;
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { query = s.toString().trim(); refresh(); }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        View exportRow = findViewById(R.id.exportRow);
        View fab = findViewById(R.id.fab);
        boolean admin = App.isManager();
        exportRow.setVisibility(admin ? View.VISIBLE : View.GONE);
        fab.setVisibility(admin ? View.VISIBLE : View.GONE);
        if (admin) {
            findViewById(R.id.btnExpTxt).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { exportTxt(); }
            });
            findViewById(R.id.btnExpDoc).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { exportDoc(); }
            });
            findViewById(R.id.btnExpPdf).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { exportPdf(); }
            });
            fab.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openEditor(null); }
            });
        }
    }

    private void openEditor(String recId) {
        Intent i = new Intent(this, RecordEditorActivity.class);
        i.putExtra("recId", recId == null ? "" : recId);
        i.putExtra("sectionId", sectionId);
        startActivity(i);
    }

    private void refresh() {
        ArrayList<Rec> visible = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Rec r : Store.recsOf(sectionId)) {
            if (!q.isEmpty()) {
                boolean hit = r.title.toLowerCase(Locale.ROOT).contains(q)
                        || r.note.toLowerCase(Locale.ROOT).contains(q)
                        || r.code.toLowerCase(Locale.ROOT).contains(q)
                        || r.phone.toLowerCase(Locale.ROOT).contains(q);
                if (!hit) continue;
            }
            visible.add(r);
        }
        adapter.data = visible;
        adapter.notifyDataSetChanged();
        boolean searching = !query.isEmpty();
        if (searching) {
            tvEmptyTitle.setText(R.string.empty_search);
            tvEmptySub.setText("");
        } else {
            tvEmptyTitle.setText(R.string.empty_records);
            tvEmptySub.setText(R.string.empty_records_sub);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    // ---------- exports ----------

    private String fileName(String ext) {
        return "\u0646\u062F\u0627\u0627\u0644\u0642\u0644\u0645-" + ExportUtil.secTitle(sectionId) + "." + ext;
    }

    private void exportTxt() {
        pendingExport = ExportUtil.buildText(sectionId);
        launchCreate(REQ_TXT, "text/plain", fileName("txt"));
    }

    private void exportDoc() {
        pendingExport = ExportUtil.buildHtml(sectionId);
        launchCreate(REQ_DOC, "application/msword", fileName("doc"));
    }

    private void exportPdf() {
        printFired = false;
        final String html = ExportUtil.buildHtml(sectionId);
        final String job = "\u0646\u062F\u0627\u0627\u0644\u0642\u0644\u0645-" + ExportUtil.secTitle(sectionId);
        printView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (printFired) return;
                printFired = true;
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(job);
                pm.print(job, adapter, new PrintAttributes.Builder().build());
            }
        });
        printView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        Toast.makeText(this, R.string.pdf_hint, Toast.LENGTH_LONG).show();
    }

    private void launchCreate(int req, String mime, String name) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(i, req);
        } catch (Exception e) {
            Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) { pendingExport = null; return; }
        Uri uri = data.getData();
        if (uri != null && pendingExport != null) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    Toast.makeText(this, R.string.exp_done, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.exp_fail, Toast.LENGTH_SHORT).show();
            }
        }
        pendingExport = null;
    }

    // ---------- adapter ----------

    private class RecAdapter extends BaseAdapter {
        ArrayList<Rec> data = new ArrayList<>();

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            VH h;
            if (v == null) {
                v = LayoutInflater.from(RecordsActivity.this)
                        .inflate(R.layout.item_record, parent, false);
                h = new VH();
                h.title = v.findViewById(R.id.irTitle);
                h.sub = v.findViewById(R.id.irSub);
                h.note = v.findViewById(R.id.irNote);
                h.badge = v.findViewById(R.id.irBadge);
                h.date = v.findViewById(R.id.irDate);
                v.setTag(h);
            } else {
                h = (VH) v.getTag();
            }
            Rec r = data.get(position);
            h.title.setText(r.title.trim().isEmpty() ? "\u2014" : r.title);

            StringBuilder sb = new StringBuilder();
            if (!r.code.isEmpty()) sb.append("\u06A9\u062F: ").append(r.code).append("  ");
            if (!r.phone.isEmpty()) sb.append("\uD83D\uDCDE ").append(r.phone);
            h.sub.setVisibility(sb.length() == 0 ? View.GONE : View.VISIBLE);
            h.sub.setText(sb.toString());

            String note = r.note.replace("\n", " ").trim();
            if (note.length() > 90) note = note.substring(0, 90) + "\u2026";
            h.note.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
            h.note.setText(note);

            if (r.fileName != null && !r.fileName.isEmpty()) {
                h.badge.setVisibility(View.VISIBLE);
                h.badge.setText("\uD83D\uDCCE " + r.typeLabel());
            } else {
                h.badge.setVisibility(View.GONE);
            }
            h.date.setText(Fa.date(r.updated));
            return v;
        }
    }

    private static class VH {
        TextView title, sub, note, badge, date;
    }
}
