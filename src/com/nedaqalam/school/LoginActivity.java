package com.nedaqalam.school;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/** ورود؛ بار اول: ساخت حساب مدیر */
public class LoginActivity extends Activity {

    private EditText etUser, etPass, etPass2, etName;
    private View nameBox, pass2Box;
    private TextView tvTitle, tvHint, btnGo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        etPass2 = findViewById(R.id.etPass2);
        etName = findViewById(R.id.etName);
        nameBox = findViewById(R.id.nameBox);
        pass2Box = findViewById(R.id.etPass2);
        tvTitle = findViewById(R.id.tvLoginTitle);
        tvHint = findViewById(R.id.tvLoginHint);
        btnGo = findViewById(R.id.btnGo);

        boolean setup = Store.users.isEmpty();
        if (setup) {
            tvTitle.setText(R.string.create_admin_title);
            tvHint.setText(R.string.create_admin_hint);
            nameBox.setVisibility(View.VISIBLE);
            pass2Box.setVisibility(View.VISIBLE);
            btnGo.setText(R.string.btn_create);
        }

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { go(setup); }
        });
    }

    private void go(boolean setup) {
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        if (setup) {
            String name = etName.getText().toString().trim();
            String pass2 = etPass2.getText().toString().trim();
            if (name.isEmpty() || user.isEmpty() || pass.length() < 4 || !pass.equals(pass2)) {
                Toast.makeText(this, R.string.err_pass, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Store.createUser(this, name, user, pass, User.ROLE_ADMIN)) {
                Toast.makeText(this, R.string.err_user_exists, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, R.string.err_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Store.login(user, pass)) {
                Toast.makeText(this, R.string.err_login, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        App.me = Store.findUser(user);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
