package org.twodays.easyreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BooksDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "books.db";
    private static final int DATABASE_VERSION = 4; // 升级到4

    public static final String TABLE_BOOKS = "books";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_SCROLL_PERCENT = "scroll_percent"; // 保留，但不再使用
    public static final String COLUMN_LAST_PAGE = "last_page";           // 新增
    public static final String COLUMN_SCROLL_OFFSET = "scroll_offset";   // 新增
    public static final String COLUMN_ENCODING = "encoding";             // 新增

    private static final String CREATE_TABLE_BOOKS = "CREATE TABLE " + TABLE_BOOKS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_URI + " TEXT,"
            + COLUMN_SCROLL_PERCENT + " REAL DEFAULT 0,"
            + COLUMN_LAST_PAGE + " INTEGER DEFAULT 0,"
            + COLUMN_SCROLL_OFFSET + " INTEGER DEFAULT 0,"
            + COLUMN_ENCODING + " TEXT DEFAULT 'UTF-8')";

    public BooksDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BOOKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 逐级升级
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_SCROLL_PERCENT + " REAL DEFAULT 0");
        }
        if (oldVersion < 3) {
            // 版本3：未使用，跳过
        }
        if (oldVersion < 4) {
            // 添加新列
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_LAST_PAGE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_SCROLL_OFFSET + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_ENCODING + " TEXT DEFAULT 'UTF-8'");
        }
    }

    // 插入书籍（默认编码UTF-8）
    public long addBook(String name, String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_URI, uri);
        values.put(COLUMN_SCROLL_PERCENT, 0f);
        values.put(COLUMN_LAST_PAGE, 0);
        values.put(COLUMN_SCROLL_OFFSET, 0);
        values.put(COLUMN_ENCODING, "UTF-8");
        long id = db.insert(TABLE_BOOKS, null, values);
        db.close();
        return id;
    }

    // 获取所有书籍
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BOOKS + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int idIndex = cursor.getColumnIndex(COLUMN_ID);
        int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
        int uriIndex = cursor.getColumnIndex(COLUMN_URI);
        int percentIndex = cursor.getColumnIndex(COLUMN_SCROLL_PERCENT);
        int lastPageIndex = cursor.getColumnIndex(COLUMN_LAST_PAGE);
        int offsetIndex = cursor.getColumnIndex(COLUMN_SCROLL_OFFSET);
        int encodingIndex = cursor.getColumnIndex(COLUMN_ENCODING);

        while (cursor.moveToNext()) {
            float percent = (percentIndex != -1) ? cursor.getFloat(percentIndex) : 0f;
            int lastPage = (lastPageIndex != -1) ? cursor.getInt(lastPageIndex) : 0;
            int offset = (offsetIndex != -1) ? cursor.getInt(offsetIndex) : 0;
            String encoding = (encodingIndex != -1) ? cursor.getString(encodingIndex) : "UTF-8";
            Book book = new Book(
                    cursor.getInt(idIndex),
                    cursor.getString(nameIndex),
                    cursor.getString(uriIndex),
                    percent,
                    lastPage,
                    offset,
                    encoding
            );
            books.add(book);
        }
        cursor.close();
        db.close();
        return books;
    }

    // 根据ID获取单本书
    public Book getBook(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKS, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Book book = null;
        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(COLUMN_ID);
            int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
            int uriIndex = cursor.getColumnIndex(COLUMN_URI);
            int percentIndex = cursor.getColumnIndex(COLUMN_SCROLL_PERCENT);
            int lastPageIndex = cursor.getColumnIndex(COLUMN_LAST_PAGE);
            int offsetIndex = cursor.getColumnIndex(COLUMN_SCROLL_OFFSET);
            int encodingIndex = cursor.getColumnIndex(COLUMN_ENCODING);

            float percent = (percentIndex != -1) ? cursor.getFloat(percentIndex) : 0f;
            int lastPage = (lastPageIndex != -1) ? cursor.getInt(lastPageIndex) : 0;
            int offset = (offsetIndex != -1) ? cursor.getInt(offsetIndex) : 0;
            String encoding = (encodingIndex != -1) ? cursor.getString(encodingIndex) : "UTF-8";

            book = new Book(
                    cursor.getInt(idIndex),
                    cursor.getString(nameIndex),
                    cursor.getString(uriIndex),
                    percent,
                    lastPage,
                    offset,
                    encoding
            );
        }
        cursor.close();
        db.close();
        return book;
    }

    // 更新阅读进度（页码 + 滚动偏移）
    public void updateProgress(int bookId, int page, int scrollOffset) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_PAGE, page);
        values.put(COLUMN_SCROLL_OFFSET, scrollOffset);
        db.update(TABLE_BOOKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(bookId)});
        db.close();
    }

    // 更新编码
    public void updateEncoding(int bookId, String encoding) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENCODING, encoding);
        db.update(TABLE_BOOKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(bookId)});
        db.close();
    }

    // 删除书籍
    public void deleteBook(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BOOKS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}