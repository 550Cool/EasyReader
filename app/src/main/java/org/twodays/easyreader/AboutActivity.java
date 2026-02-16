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

        String versionName = "1.0.2";
        String author = "2Days";
        String githubUrl = "https://github.com/550Cool/EasyReader";

        TextView tvVersion = findViewById(R.id.tv_version);
        TextView tvAuthor = findViewById(R.id.tv_author);
        TextView tvDbVersion = findViewById(R.id.tv_db_version);
        Button btnGithub = findViewById(R.id.btn_github);

        tvVersion.setText("版本: " + versionName);
        tvAuthor.setText("作者: " + author);

        // 从数据库助手获取当前数据库版本
        int dbVersion = BooksDatabaseHelper.DATABASE_VERSION;
        tvDbVersion.setText("数据库版本: " + dbVersion);

        btnGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
            startActivity(intent);
        });
    }
}