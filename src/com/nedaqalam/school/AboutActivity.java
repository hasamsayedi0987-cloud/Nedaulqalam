package com.nedaqalam.school;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/** بخش اینفو: معرفی اپ + اطلاعات سازنده */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText(String.format(getString(R.string.version_fmt), "۱٫۰"));

        findViewById(R.id.rowPhone).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0791680632"));
                try { startActivity(i); } catch (Exception ignored) {}
            }
        });
        findViewById(R.id.rowEmail).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hasamsayedi0987@gmail.com"));
                try { startActivity(i); } catch (Exception ignored) {}
            }
        });
    }
}
