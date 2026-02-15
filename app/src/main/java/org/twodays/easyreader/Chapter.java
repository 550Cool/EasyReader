package org.twodays.easyreader;

/**
 * 章节信息类
 */
public class Chapter {
    private String title;
    private int startIndex;      // 在全文中的起始字符索引
    private String anchorId;     // HTML 锚点 ID

    public Chapter(String title, int startIndex, String anchorId) {
        this.title = title;
        this.startIndex = startIndex;
        this.anchorId = anchorId;
    }

    public String getTitle() {
        return title;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public String getAnchorId() {
        return anchorId;
    }
}