package com.j730415jj.phoneagent;

import org.json.JSONException;
import org.json.JSONObject;

final class LibraryItem {
    final String id;
    final String title;
    final String content;
    final long createdAt;

    LibraryItem(String id, String title, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("title", title);
        object.put("content", content);
        object.put("createdAt", createdAt);
        return object;
    }

    static LibraryItem fromJson(JSONObject object) {
        return new LibraryItem(
                object.optString("id"),
                object.optString("title", "이름 없는 자료"),
                object.optString("content"),
                object.optLong("createdAt", System.currentTimeMillis())
        );
    }
}
