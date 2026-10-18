package com.nedaqalam.school;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/** فقط مدیر: ساخت کاربر برای معلم/شاگرد/کارمند/ولی و... + دسترسی نقش‌ها */
public class UsersActivity extends Activity {

    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        if (!App.isManager()) {
            Toast.makeText(this, R.string.only_admin, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.btnAddUser).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addUser(); }
        });
        findViewById(R.id.btnPerms).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickRoleForPerms(); }
        });

        ListView list = findViewById(R.id.list);
        View empty = findViewById(R.id.emptyBox);
        list.setEmptyView(empty);
        adapter = new UsersAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                userOptions(adapter.data.get(pos));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.reload();
    }

    private void addUser() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        box.setPadding(p, dp(8), p, 0);

        final EditText etName = new EditText(this);
        etName.setHint(R.string.full_name);
        final EditText etUser = new EditText(this);
        etUser.setHint(R.string.username);
        final EditText etPass = new EditText(this);
        etPass.setHint(R.string.password);
        final TextView tvRole = new TextView(this);
        tvRole.setText(R.string.role);
        tvRole.setPadding(0, dp(14), 0, dp(4));
        final android.widget.Spinner sp = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> sa = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Store.ROLES);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(sa);

        box.addView(etName);
        box.addView(etUser);
        box.addView(etPass);
        box.addView(tvRole);
        box.addView(sp);

        new AlertDialog.Builder(this)
            .setTitle(R.string.add_user)
            .setView(box)
            .setPositiveButton(R.string.save, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    String name = etName.getText().toString().trim();
                    String user = etUser.getText().toString().trim();
                    String pass = etPass.getText().toString().trim();
                    if (name.isEmpty() || user.isEmpty() || pass.length() < 4) {
                        Toast.makeText(UsersActivity.this, R.string.err_pass, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (Store.createUser(UsersActivity.this, name, user, pass,
                            (String) sp.getSelectedItem())) {
                        adapter.reload();
                        Toast.makeText(UsersActivity.this, R.string.user_added, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UsersActivity.this, R.string.err_user_exists, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void pickRoleForPerms() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.role_perms)
            .setItems(Store.ROLES, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int which) {
                    editPerms(Store.ROLES[which]);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void editPerms(final String role) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        TextView hint = new TextView(this);
        hint.setText(String.format(getString(R.string.perm_hint), role));
        hint.setPadding(0, 0, 0, dp(8));
        box.addView(hint);

        final ArrayList<CheckBox> boxes = new ArrayList<>();
        final ArrayList<String> current = Store.permsOf(role);
        for (final Sec s : Store.sections) {
            CheckBox cb = new CheckBox(this);
            cb.setText(s.title);
            cb.setChecked(current.contains(s.id));
            box.addView(cb);
            boxes.add(cb);
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.role_perms)
            .setView(box)
            .setPositiveButton(R.string.save, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < boxes.size(); i++) {
                        if (boxes.get(i).isChecked()) ids.add(Store.sections.get(i).id);
                    }
                    Store.setPerms(UsersActivity.this, role, ids);
                    adapter.reload();
                    Toast.makeText(UsersActivity.this, R.string.perms_saved, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void userOptions(final User u) {
        ArrayList<String> opts = new ArrayList<>();
        opts.add(getString(R.string.reset_pass));
        opts.add(u.active ? getString(R.string.deactivate) : getString(R.string.activate));
        opts.add(getString(R.string.delete));
        new AlertDialog.Builder(this)
            .setTitle(u.fullName + " (" + u.username + ")")
            .setItems(opts.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int which) {
                    if (which == 0) changePass(u);
                    else if (which == 1) toggleActive(u);
                    else deleteUser(u);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void changePass(final User u) {
        final EditText input = new EditText(this);
        input.setHint(R.string.new_pass_hint);
        input.setPadding(dp(20), dp(14), dp(20), dp(14));
        new AlertDialog.Builder(this)
            .setTitle(R.string.reset_pass)
            .setView(input)
            .setPositiveButton(R.string.save, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    String pass = input.getText().toString().trim();
                    if (pass.length() < 4) {
                        Toast.makeText(UsersActivity.this, R.string.err_pass, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Store.setUserPass(UsersActivity.this, u, pass);
                    adapter.reload();
                    Toast.makeText(UsersActivity.this, R.string.pass_changed, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void toggleActive(User u) {
        if (u.username.equals(App.me.username)
                && (User.ROLE_ADMIN.equals(u.role) && Store.managerCount() <= 1)) {
            Toast.makeText(this, R.string.last_admin, Toast.LENGTH_SHORT).show();
            return;
        }
        u.active = !u.active;
        Store.save(this);
        adapter.reload();
    }

    private void deleteUser(User u) {
        if (u.username.equals(App.me.username)) {
            Toast.makeText(this, R.string.cannot_delete_self, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setMessage(R.string.del_user_q)
            .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    Store.removeUser(UsersActivity.this, u);
                    adapter.reload();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private class UsersAdapter extends BaseAdapter {
        ArrayList<User> data = new ArrayList<>();

        void reload() {
            data = new ArrayList<>(Store.users);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(UsersActivity.this).inflate(R.layout.item_user, parent, false);
            }
            User u = data.get(position);
            TextView name = v.findViewById(R.id.iuName);
            TextView info = v.findViewById(R.id.iuInfo);
            TextView state = v.findViewById(R.id.iuState);
            name.setText(u.fullName);
            info.setText(u.username + " \u2022 " + u.role);
            state.setText(u.active ? R.string.user_active : R.string.user_inactive);
            state.setTextColor(u.active ? 0xFF2E7D32 : 0xFF9E9E9E);
            return v;
        }
    }
}
