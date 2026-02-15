package org.twodays.easyreader;

public class Book {
    private int id;
    private String name;
    private String uri;
    private float scrollPercent;      // 保留，但不再使用（兼容旧数据）
    private int lastPage;              // 上次阅读的页码（从0开始）
    private int scrollOffset;          // 上次阅读的滚动Y偏移
    private String encoding;           // 文件编码

    public Book(int id, String name, String uri, float scrollPercent, int lastPage, int scrollOffset, String encoding) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.scrollPercent = scrollPercent;
        this.lastPage = lastPage;
        this.scrollOffset = scrollOffset;
        this.encoding = encoding;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUri() { return uri; }
    public float getScrollPercent() { return scrollPercent; }
    public int getLastPage() { return lastPage; }
    public int getScrollOffset() { return scrollOffset; }
    public String getEncoding() { return encoding; }

    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
    public void setScrollOffset(int scrollOffset) { this.scrollOffset = scrollOffset; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
}