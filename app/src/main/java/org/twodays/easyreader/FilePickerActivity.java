package org.twodays.easyreader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvPath;
    private File currentDir;
    private List<File> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        listView = findViewById(R.id.list_view);
        tvPath = findViewById(R.id.tv_path);

        // 使用应用的外部私有目录，无需权限
        currentDir = getExternalFilesDir(null);
        if (currentDir == null) {
            // 如果外部私有目录不可用，回退到内部私有目录
            currentDir = getFilesDir();
        }

        if (currentDir == null || !currentDir.canRead()) {
            Toast.makeText(this, "无法访问应用私有目录", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadFileList(currentDir);
    }

    private void loadFileList(File dir) {
        tvPath.setText(dir.getAbsolutePath());

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // 只显示文件夹和 .txt 文件
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".txt");
            }
        });

        fileList.clear();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
            // 排序：文件夹在前，文件在后，按名称排序
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && !o2.isDirectory()) return -1;
                    if (!o1.isDirectory() && o2.isDirectory()) return 1;
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }

        // 由于只能访问应用私有目录，不提供返回上级（简化）
        List<String> names = new ArrayList<>();
        for (File file : fileList) {
            if (file.isDirectory()) {
                names.add("📁 " + file.getName());
            } else {
                names.add("📄 " + file.getName());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selected = fileList.get(position);
                if (selected.isDirectory()) {
                    // 进入子文件夹
                    loadFileList(selected);
                } else {
                    // 返回选中的文件
                    returnFile(selected);
                }
            }
        });
    }

    private void returnFile(File file) {
        // 使用 FileProvider 生成 content URI
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);
        // 授予临时读取权限
        grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent result = new Intent();
        result.setData(uri);
        setResult(RESULT_OK, result);
        finish();
    }
}