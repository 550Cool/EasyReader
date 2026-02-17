package org.twodays.easyreader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderActivity extends AppCompatActivity {

    private static final String TAG = "ReaderActivity";
    private static final int MAX_PAGE_CHARS = 20000; // 单页最大字符数（当章节过长时拆分）

    private NestedScrollView scrollView;
    private TextView tvContent;
    private SeekBar seekBarFont, progressSeekBar;
    private SwitchCompat switchDarkMode;
    private TextView tvPageInfo, tvBookTitle;
    private LinearLayout topControl, bottomControl;
    private ImageButton btnClose, btnPrev, btnNext;
    private Button btnToc, btnEncoding;

    private String fullText = "";
    private List<Page> pages = new ArrayList<>();          // 页面列表（每个页面对应一段文本）
    private List<Chapter> chapters = new ArrayList<>();    // 章节列表（按出现顺序）
    private int currentPageIndex = 0;
    private int totalPages = 0;

    private int currentFontSize = 16;                      // 当前字体大小（从数据库读取）
    private boolean isDarkMode = false;
    private boolean controlsVisible = false;

    private int bookId = -1;
    private String bookName = "";
    private String currentEncoding = "UTF-8";
    private Uri bookUri;

    // 上次阅读的页码和滚动偏移
    private int savedPage = 0;
    private int savedScrollOffset = 0;

    private GestureDetector gestureDetector;

    // 支持的编码列表
    private final String[] encodings = {"UTF-8", "GBK", "GB2312", "BIG5", "ISO-8859-1"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // 隐藏 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();

        // 读取全局深色模式设置，设置开关初始状态
        SharedPreferences prefs = getSharedPreferences("EasyReaderPrefs", MODE_PRIVATE);
        boolean globalDarkMode = prefs.getBoolean("dark_mode", false);
        isDarkMode = globalDarkMode;
        switchDarkMode.setChecked(isDarkMode);

        setupGestureDetector();

        bookUri = getIntent().getData();
        if (bookUri == null) {
            Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 获取书籍信息（包含字体大小）
        bookId = getIntent().getIntExtra("book_id", -1);
        if (bookId != -1) {
            BooksDatabaseHelper dbHelper = new BooksDatabaseHelper(this);
            Book book = dbHelper.getBook(bookId);
            if (book != null) {
                savedPage = book.getLastPage();
                savedScrollOffset = book.getScrollOffset();
                bookName = book.getName();
                currentEncoding = book.getEncoding();
                currentFontSize = book.getFontSize(); // 读取字号
                Log.d(TAG, "Loaded: page=" + savedPage + ", offset=" + savedScrollOffset + ", encoding=" + currentEncoding + ", fontSize=" + currentFontSize);
            }
            dbHelper.close();
        }
        if (bookName.isEmpty()) {
            bookName = bookUri.getLastPathSegment();
            if (bookName == null) bookName = getString(R.string.unknown_file);
        }
        tvBookTitle.setText(bookName);

        // 设置 SeekBar 初始值
        seekBarFont.setProgress(currentFontSize - 10); // 因为进度0对应10

        // 使用保存的编码加载文件
        loadFileWithEncoding(bookUri, currentEncoding);

        // 字体调节
        seekBarFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFontSize = progress + 10;
                applyTextStyle();
                // 保存字号到数据库
                if (bookId != -1) {
                    BooksDatabaseHelper dbHelper = new BooksDatabaseHelper(ReaderActivity.this);
                    dbHelper.updateFontSize(bookId, currentFontSize);
                    dbHelper.close();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 深色模式切换（仅影响当前阅读界面）
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDarkMode = isChecked;
            applyTextStyle();
        });

        btnClose.setOnClickListener(v -> finish());
        btnToc.setOnClickListener(v -> showChapterDialog());
        btnEncoding.setOnClickListener(v -> showEncodingDialog());

        btnPrev.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                displayCurrentPage();
                updateProgressBar();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++;
                displayCurrentPage();
                updateProgressBar();
            }
        });

        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = progress / 10;
                tvPageInfo.setText(percent + "%");
                if (fromUser && totalPages > 0) {
                    int targetPage = (int) ((float) progress / 1000 * totalPages);
                    if (targetPage >= totalPages) targetPage = totalPages - 1;
                    if (targetPage != currentPageIndex) {
                        currentPageIndex = targetPage;
                        displayCurrentPage();
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * 音量键处理：控制栏可见时调节音量，隐藏时滚动屏幕
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 如果控制栏可见，则让系统处理音量键（即调节音量）
        if (controlsVisible) {
            return super.onKeyDown(keyCode, event);
        }
        // 控制栏隐藏时，音量键用于滚动
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            scrollView.smoothScrollBy(0, -scrollView.getHeight() / 2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            scrollView.smoothScrollBy(0, scrollView.getHeight() / 2);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initViews() {
        scrollView = findViewById(R.id.scroll_view);
        tvContent = findViewById(R.id.tv_content);
        seekBarFont = findViewById(R.id.seekbar_font);
        progressSeekBar = findViewById(R.id.progress_seekbar);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        tvPageInfo = findViewById(R.id.tv_page_info);
        tvBookTitle = findViewById(R.id.tv_book_title);
        topControl = findViewById(R.id.top_control);
        bottomControl = findViewById(R.id.bottom_control);
        btnClose = findViewById(R.id.btn_close);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnToc = findViewById(R.id.btn_toc);
        btnEncoding = findViewById(R.id.btn_encoding);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleControls();
                return true;
            }
        });
        scrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // 让 NestedScrollView 继续处理滚动
        });
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        topControl.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
        bottomControl.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
        if (controlsVisible) {
            updateProgressBar();
        }
    }

    private void updateProgressBar() {
        if (totalPages == 0) return;
        int progress = (int) ((float) currentPageIndex / totalPages * 1000);
        progressSeekBar.setProgress(progress);
        tvPageInfo.setText((progress / 10) + "%");
    }

    /**
     * 使用指定编码加载文件，并构建页面
     */
    private void loadFileWithEncoding(Uri uri, String encoding) {
        try {
            fullText = readTextFromUri(uri, encoding);
            currentEncoding = encoding;
            parseChapters(fullText);
            buildPages(); // 根据章节构建页面
            totalPages = pages.size();

            // 恢复上次阅读位置
            if (savedPage >= 0 && savedPage < totalPages) {
                currentPageIndex = savedPage;
            } else {
                currentPageIndex = 0;
            }

            displayCurrentPage();
            // 恢复滚动偏移（在页面显示后执行）
            scrollView.post(() -> scrollView.scrollTo(0, savedScrollOffset));
        } catch (IOException e) {
            Log.e(TAG, "Read file failed with encoding " + encoding, e);
            Toast.makeText(this, "使用 " + encoding + " 读取失败，请尝试其他编码", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示编码选择对话框，并更新数据库记忆
     */
    private void showEncodingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择文件编码");
        builder.setItems(encodings, (dialog, which) -> {
            String selectedEncoding = encodings[which];
            if (!selectedEncoding.equals(currentEncoding)) {
                // 保存新编码到数据库
                if (bookId != -1) {
                    BooksDatabaseHelper dbHelper = new BooksDatabaseHelper(this);
                    dbHelper.updateEncoding(bookId, selectedEncoding);
                    dbHelper.close();
                }
                // 重新加载文件
                loadFileWithEncoding(bookUri, selectedEncoding);
            }
        });
        builder.show();
    }

    /**
     * 解析章节：匹配中文“第...章/节”以及连续三位以上数字
     */
    private void parseChapters(String text) {
        chapters.clear();

        Pattern chinesePattern = Pattern.compile(
                "^(\\s*第[一二三四五六七八九十百千万0-9]+[章节])",
                Pattern.MULTILINE);
        Pattern digitPattern = Pattern.compile(
                "^(\\s*[0-9]{2,})",
                Pattern.MULTILINE);

        List<Chapter> tempChapters = new ArrayList<>();

        // 中文章节
        Matcher matcher = chinesePattern.matcher(text);
        while (matcher.find()) {
            String title = matcher.group().trim();
            int start = matcher.start();
            tempChapters.add(new Chapter(title, start, ""));
        }

        // 数字章节（过滤年份）
        matcher = digitPattern.matcher(text);
        while (matcher.find()) {
            String title = matcher.group().trim();
            int start = matcher.start();
            // 过滤四位数字且值在1900-2099之间（年份）
            if (title.length() == 4) {
                try {
                    int num = Integer.parseInt(title);
                    if (num >= 1900 && num <= 2099) {
                        continue; // 跳过年份
                    }
                } catch (NumberFormatException ignored) {}
            }
            tempChapters.add(new Chapter(title, start, ""));
        }

        // 按起始位置排序
        Collections.sort(tempChapters, new Comparator<Chapter>() {
            @Override
            public int compare(Chapter a, Chapter b) {
                return Integer.compare(a.getStartIndex(), b.getStartIndex());
            }
        });

        // 添加“前言”章节
        List<Chapter> finalChapters = new ArrayList<>();
        if (!tempChapters.isEmpty() && tempChapters.get(0).getStartIndex() > 0) {
            // 创建前言章节，起始索引为0
            Chapter preface = new Chapter("前言", 0, "");
            finalChapters.add(preface);
        }
        finalChapters.addAll(tempChapters);

        // 重新生成 anchorId
        for (int i = 0; i < finalChapters.size(); i++) {
            Chapter ch = finalChapters.get(i);
            chapters.add(new Chapter(ch.getTitle(), ch.getStartIndex(), "chapter_" + i));
        }

        Log.d(TAG, "Parsed " + chapters.size() + " chapters (including preface)");
    }

    /**
     * 构建页面：优先按章节分页，每个章节一页；若章节内容过长则拆分为多页
     */
    private void buildPages() {
        pages.clear();
        if (chapters.isEmpty()) {
            // 无章节，按固定字符分页
            int len = fullText.length();
            int start = 0;
            while (start < len) {
                int end = Math.min(start + MAX_PAGE_CHARS, len);
                pages.add(new Page(start, end));
                start = end;
            }
        } else {
            // 有章节，以章节为单位分页
            int lastPos = 0;
            for (int i = 0; i < chapters.size(); i++) {
                Chapter ch = chapters.get(i);
                int chapterStart = ch.getStartIndex();
                int chapterEnd = (i < chapters.size() - 1) ? chapters.get(i + 1).getStartIndex() : fullText.length();

                // 计算本章长度
                int chapterLen = chapterEnd - chapterStart;
                if (chapterLen <= MAX_PAGE_CHARS) {
                    // 本章一页
                    pages.add(new Page(chapterStart, chapterEnd));
                } else {
                    // 本章拆分为多页
                    int subStart = chapterStart;
                    while (subStart < chapterEnd) {
                        int subEnd = Math.min(subStart + MAX_PAGE_CHARS, chapterEnd);
                        pages.add(new Page(subStart, subEnd));
                        subStart = subEnd;
                    }
                }
                lastPos = chapterEnd;
            }
        }
        Log.d(TAG, "Built " + pages.size() + " pages");
    }

    private void displayCurrentPage() {
        if (totalPages == 0) return;
        Page page = pages.get(currentPageIndex);
        String pageText = fullText.substring(page.start, page.end);

        SpannableString spannable = new SpannableString(pageText);

        // 1. 基础字体大小
        spannable.setSpan(new AbsoluteSizeSpan(currentFontSize, true), 0, pageText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 2. 基础文字颜色
        int bgColor = isDarkMode ? 0xFF1E1E1E : 0xFFFFFFFF;
        int textColor = isDarkMode ? 0xFFC0C0C0 : 0xFF000000;
        tvContent.setBackgroundColor(bgColor);
        spannable.setSpan(new ForegroundColorSpan(textColor), 0, pageText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 3. 标记章节标题（加粗+放大）
        for (Chapter chapter : chapters) {
            int chapterStart = chapter.getStartIndex();
            int chapterEnd = chapterStart + chapter.getTitle().length();
            if (chapterEnd > page.start && chapterStart < page.end) {
                int localStart = Math.max(chapterStart - page.start, 0);
                int localEnd = Math.min(chapterEnd - page.start, pageText.length());
                if (localStart < localEnd) {
                    // 加粗
                    spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), localStart, localEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // 放大（基础字体*1.2）
                    int biggerSize = (int) (currentFontSize * 1.2);
                    spannable.setSpan(new AbsoluteSizeSpan(biggerSize, true), localStart, localEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        tvContent.setText(spannable);

        // 恢复滚动偏移（注意：切换页面后需要重置滚动位置到保存的偏移，但通常新页面偏移应为0）
        if (currentPageIndex == savedPage) {
            scrollView.scrollTo(0, savedScrollOffset);
        } else {
            scrollView.scrollTo(0, 0);
        }
        updateProgressBar();
    }

    private void applyTextStyle() {
        if (totalPages == 0) return;
        displayCurrentPage(); // 重新生成Spannable，应用新字体/颜色
    }

    /**
     * 显示目录对话框
     */
    private void showChapterDialog() {
        if (chapters.isEmpty()) {
            Toast.makeText(this, "未检测到章节", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) {
            titles[i] = chapters.get(i).getTitle();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("目录");
        builder.setItems(titles, (dialog, which) -> {
            Chapter chapter = chapters.get(which);
            jumpToChapter(chapter);
            toggleControls();
        });
        builder.show();
    }

    /**
     * 跳转到指定章节（找到包含该章节的页面）
     */
    private void jumpToChapter(Chapter chapter) {
        int charIndex = chapter.getStartIndex();
        // 查找包含该字符的页面
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            if (charIndex >= p.start && charIndex < p.end) {
                if (i != currentPageIndex) {
                    currentPageIndex = i;
                    displayCurrentPage();
                } else {
                    // 同一页内，滚动到章节标题位置（估算）
                    int localOffset = charIndex - p.start;
                    tvContent.post(() -> scrollView.scrollTo(0, (int)(localOffset * currentFontSize * 0.5)));
                }
                break;
            }
        }
        updateProgressBar();
    }

    /**
     * 保存当前阅读进度（页码 + 滚动偏移）
     */
    private void saveProgress() {
        if (bookId == -1) return;
        int scrollY = scrollView.getScrollY();
        BooksDatabaseHelper dbHelper = new BooksDatabaseHelper(this);
        dbHelper.updateProgress(bookId, currentPageIndex, scrollY);
        dbHelper.close();
        Log.d(TAG, "Saved: page=" + currentPageIndex + ", offset=" + scrollY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveProgress();
    }

    private String readTextFromUri(Uri uri, String charsetName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charsetName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 页面内部类
     */
    private static class Page {
        int start;
        int end;
        Page(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}