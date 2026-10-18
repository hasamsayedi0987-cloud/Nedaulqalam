package com.nedaqalam.school;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/** فهرست بخش‌های مکتب (اصلی + دلخواه) */
public class MainActivity extends Activity {

    private SectionsAdapter adapter;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvSchool = findViewById(R.id.tvSchool);
        tvSchool.setText(R.string.school_full);
        tvWelcome = findViewById(R.id.tvWelcome);
        btn(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                App.me = null;
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finishAffinity();
            }
        });
        btn(R.id.btnAbout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        View btnUsers = btn(R.id.btnUsers);
        if (App.isManager()) {
            btnUsers.setVisibility(View.VISIBLE);
            btnUsers.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, UsersActivity.class));
                }
            });
            btn(R.id.btnAddSection).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { newSection(); }
            });
        } else {
            btnUsers.setVisibility(View.GONE);
            findViewById(R.id.btnAddSection).setVisibility(View.GONE);
        }

        ListView list = findViewById(R.id.list);
        View empty = findViewById(R.id.emptyBox);
        list.setEmptyView(empty);
        adapter = new SectionsAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Sec s = adapter.data.get(pos);
                Intent i = new Intent(MainActivity.this, RecordsActivity.class);
                i.putExtra("sectionId", s.id);
                startActivity(i);
            }
        });
        list.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Sec s = adapter.data.get(pos);
                if (!App.isManager()) return false;
                if (!s.custom) return true; // بخش‌های اصلی تغییر نمی‌کنند
                sectionOptions(s);
                return true;
            }
        });
    }

    private View btn(int id) { return findViewById(id); }

    private void newSection() {
        final EditText input = new EditText(this);
        input.setHint(R.string.new_section_hint);
        input.setPadding(dp(20), dp(14), dp(20), dp(14));
        new AlertDialog.Builder(this)
            .setTitle(R.string.new_section)
            .setView(input)
            .setPositiveButton(R.string.save, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    Store.addSection(MainActivity.this, name);
                    adapter.reload();
                    Toast.makeText(MainActivity.this, R.string.section_added, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void sectionOptions(final Sec s) {
        final EditText input = new EditText(this);
        input.setText(s.title);
        input.setPadding(dp(20), dp(14), dp(20), dp(14));
        new AlertDialog.Builder(this)
            .setTitle(s.title)
            .setView(input)
            .setPositiveButton(R.string.rename, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Store.renameSection(MainActivity.this, s, name);
                        adapter.reload();
                        Toast.makeText(MainActivity.this, R.string.section_renamed, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNeutralButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.del_section_q)
                        .setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface dd, int ww) {
                                Store.deleteSection(MainActivity.this, s);
                                adapter.reload();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String tpl = getString(R.string.welcome_fmt);
        tvWelcome.setText(String.format(tpl,
            App.me == null ? "" : App.me.fullName,
            App.me == null ? "" : App.me.role));
        adapter.reload();
    }

    private class SectionsAdapter extends BaseAdapter {
        ArrayList<Sec> data = new ArrayList<>();

        void reload() {
            data.clear();
            for (Sec s : Store.sections) {
                if (Store.canSee(App.me == null ? "" : App.me.role, s.id)) data.add(s);
            }
            notifyDataSetChanged();
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_section, parent, false);
            }
            Sec s = data.get(position);
            TextView icon = v.findViewById(R.id.itIcon);
            TextView title = v.findViewById(R.id.itTitle);
            TextView count = v.findViewById(R.id.itCount);
            icon.setText(iconFor(s.id));
            title.setText(s.title);
            count.setText(String.format(getString(R.string.count_fmt),
                    Fa.d(Store.recsOf(s.id).size())));
            return v;
        }
    }

    static String iconFor(String id) {
        if (Store.SEC_T.equals(id)) return "\uD83D\uDC68\u200D\uD83C\uDFEB";      // 👨‍🏫
        if (Store.SEC_S.equals(id)) return "\uD83C\uDF93";                        // 🎓
        if (Store.SEC_HT.equals(id)) return "\uD83E\uDDD1\u200D\uD83C\uDFEB";     // 🧑‍🏫
        if (Store.SEC_E.equals(id)) return "\uD83E\uDDD1\u200D\uD83D\uDCBC";      // 🧑‍💼
        if (Store.SEC_P.equals(id)) return "\uD83D\uDC6A";                        // 👪
        if (Store.SEC_LIB.equals(id)) return "\uD83D\uDCDA";                      // 📚
        if (Store.SEC_DOCS.equals(id)) return "\uD83D\uDDC2\uFE0F";               // 🗂️
        return "\uD83D\uDCC1";                                                    // 📁
    }
}
