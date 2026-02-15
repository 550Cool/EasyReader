package org.twodays.easyreader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // 硬编码版本号、作者和GitHub链接（请根据实际情况修改）
        String versionName = "1.0";
        String author = "2Days";
        String githubUrl = "https://github.com/550Cool/EasyReader";

        TextView tvVersion = findViewById(R.id.tv_version);
        TextView tvAuthor = findViewById(R.id.tv_author);
        Button btnGithub = findViewById(R.id.btn_github);

        tvVersion.setText("版本: " + versionName);
        tvAuthor.setText("作者: " + author);

        btnGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
            startActivity(intent);
        });
    }
}