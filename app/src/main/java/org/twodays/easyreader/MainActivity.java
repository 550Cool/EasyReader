package org.twodays.easyreader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final String PREFS_NAME = "EasyReaderPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private RecyclerView recyclerView;
    private TextView tvEmptyHint;
    private BookAdapter adapter;
    private BooksDatabaseHelper dbHelper;
    private List<Book> books;

    private ActionMode actionMode;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 读取深色模式设置
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        applyDarkMode(isDarkMode);

        dbHelper = new BooksDatabaseHelper(this);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> openFileChooser());

        loadBooks();
    }

    private void applyDarkMode(boolean dark) {
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void saveDarkMode(boolean dark) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        darkModeItem.setChecked(isDarkMode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_dark_mode) {
            isDarkMode = !isDarkMode;
            item.setChecked(isDarkMode);
            saveDarkMode(isDarkMode);
            applyDarkMode(isDarkMode);
            recreate(); // 重新创建活动以应用新主题
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBooks() {
        books = dbHelper.getAllBooks();
        if (books.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter = new BookAdapter(books,
                (book, position) -> {
                    Intent intent = new Intent(MainActivity.this, ReaderActivity.class);
                    intent.setData(Uri.parse(book.getUri()));
                    intent.putExtra("book_id", book.getId());
                    startActivity(intent);
                },
                (book, position) -> {
                    if (actionMode != null) {
                        return false;
                    }
                    actionMode = startSupportActionMode(actionModeCallback);
                    if (actionMode != null) {
                        adapter.setInActionMode(true);
                        adapter.toggleSelection(position);
                        updateActionModeTitle();
                    }
                    return true;
                });

        adapter.setOnSelectionChangedListener(count -> {
            if (actionMode != null) {
                actionMode.setTitle(count + " 项已选择");
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void openFileChooser() {
        // 检测是否为 Wear OS
        PackageManager pm = getPackageManager();
        boolean isWatch = pm.hasSystemFeature(PackageManager.FEATURE_WATCH);

        if (isWatch) {
            // 启动自定义文件选择器
            Intent intent = new Intent(this, FilePickerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
            return;
        }

        // 非手表设备：使用系统文件选择器
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), REQUEST_CODE_PICK_FILE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();

                // 尝试获取持久化权限（仅对系统文件选择器有效，自定义选择器会通过 FileProvider 授权）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        // 忽略，可能是 FileProvider 返回的 URI 不支持持久化
                    }
                }

                String rawName = uri.getLastPathSegment();
                String fileName = cleanFileName(rawName);

                dbHelper.addBook(fileName, uri.toString());
                loadBooks();
            } else {
                Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String cleanFileName(String raw) {
        if (raw == null) return getString(R.string.unknown_file);
        String cleaned = raw;
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex != -1) {
            cleaned = cleaned.substring(colonIndex + 1);
        }
        if (cleaned.toLowerCase().endsWith(".txt")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        if (cleaned.trim().isEmpty()) {
            cleaned = raw;
        }
        return cleaned;
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            int count = adapter.getSelectedItemCount();
            actionMode.setTitle(count + " 项已选择");
        }
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                SparseBooleanArray selected = adapter.getSelectedItems();
                for (int i = selected.size() - 1; i >= 0; i--) {
                    int position = selected.keyAt(i);
                    Book book = books.get(position);
                    dbHelper.deleteBook(book.getId());
                }
                loadBooks();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setInActionMode(false);
        }
    };
}