package com.j730415jj.phoneagent;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class DocumentStore {
    private static final String FILE_NAME = "agent_library.json";
    private final File file;
    private final List<LibraryItem> items = new ArrayList<>();

    DocumentStore(Context context) {
        file = new File(context.getFilesDir(), FILE_NAME);
        load();
    }

    synchronized LibraryItem add(String title, String content) {
        LibraryItem item = new LibraryItem(
                UUID.randomUUID().toString(),
                title,
                content.trim(),
                System.currentTimeMillis()
        );
        items.add(0, item);
        save();
        return item;
    }

    synchronized List<LibraryItem> all() {
        return new ArrayList<>(items);
    }

    synchronized List<LibraryItem> search(String query) {
        String normalized = query.trim().toLowerCase(Locale.KOREAN);
        List<LibraryItem> matches = new ArrayList<>();
        if (normalized.isEmpty()) {
            return matches;
        }
        String[] terms = normalized.split("\\s+");
        for (LibraryItem item : items) {
            String haystack = (item.title + "\n" + item.content).toLowerCase(Locale.KOREAN);
            int score = 0;
            for (String term : terms) {
                if (term.length() > 1 && haystack.contains(term)) {
                    score++;
                }
            }
            if (score > 0) {
                matches.add(item);
            }
        }
        matches.sort(Comparator.comparingInt((LibraryItem item) -> score(item, terms)).reversed());
        return matches;
    }

    private int score(LibraryItem item, String[] terms) {
        String title = item.title.toLowerCase(Locale.KOREAN);
        String content = item.content.toLowerCase(Locale.KOREAN);
        int score = 0;
        for (String term : terms) {
            if (term.length() <= 1) continue;
            if (title.contains(term)) score += 3;
            if (content.contains(term)) score += 1;
        }
        return score;
    }

    private void load() {
        if (!file.exists()) return;
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) break;
                offset += read;
            }
            String json = new String(bytes, 0, offset, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                items.add(LibraryItem.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception ignored) {
            items.clear();
        }
    }

    private void save() {
        JSONArray array = new JSONArray();
        try {
            for (LibraryItem item : items) {
                array.put(item.toJson());
            }
            byte[] bytes = array.toString().getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(bytes);
            }
        } catch (Exception ignored) {
            // A failed save leaves the current in-memory library available for this session.
        }
    }
}
