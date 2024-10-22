package com.menene.notebox;

import io.realm.RealmObject;

public class NoteModel extends RealmObject {
    private String title;
    private String content;
    private long milliseconds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public void setSeconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }
}
