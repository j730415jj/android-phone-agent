package com.j730415jj.phoneagent;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_DOCUMENT = 101;
    private static final int MAX_DOCUMENT_BYTES = 1_500_000;

    private final int pageColor = Color.rgb(245, 246, 242);
    private final int inkColor = Color.rgb(23, 33, 29);
    private final int mutedColor = Color.rgb(102, 115, 109);
    private final int brandColor = Color.rgb(22, 107, 77);
    private final int userBubbleColor = Color.rgb(220, 239, 230);

    private DocumentStore store;
    private AgentEngine agent;
    private LinearLayout messages;
    private ScrollView messageScroll;
    private EditText input;
    private TextView libraryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(13, 81, 58));
        window.setNavigationBarColor(Color.WHITE);

        store = new DocumentStore(this);
        agent = new AgentEngine(store);
        setContentView(buildScreen());
        addMessage(false, "안녕하세요. 허용한 문서와 공유받은 내용을 휴대폰 안에서 찾아드릴게요.\n\n'검색 차량번호', '요약 계약내용', '자료 목록'처럼 말해 보세요.");
        handleSharedText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedText(intent);
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(pageColor);
        root.setFitsSystemWindows(true);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(buildActions(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        messageScroll = new ScrollView(this);
        messageScroll.setFillViewport(true);
        messageScroll.setClipToPadding(false);
        messageScroll.setPadding(dp(14), dp(8), dp(14), dp(12));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setGravity(Gravity.BOTTOM);
        messageScroll.addView(messages, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(messageScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildComposer(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(14));
        header.setBackgroundColor(brandColor);

        TextView mark = new TextView(this);
        mark.setText("AI");
        mark.setTextColor(Color.WHITE);
        mark.setTextSize(15);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundRect(Color.rgb(13, 81, 58), 8, 0, 0));
        header.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = text("폰 에이전트", 20, Color.WHITE, true);
        TextView subtitle = text("내 자료를 기기 안에서 검색하는 로컬 모드", 12, Color.rgb(211, 235, 225), false);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        libraryCount = text("", 12, Color.WHITE, true);
        libraryCount.setGravity(Gravity.CENTER);
        libraryCount.setPadding(dp(10), dp(7), dp(10), dp(7));
        libraryCount.setBackground(roundRect(Color.rgb(13, 81, 58), 8, 0, 0));
        updateLibraryCount();
        header.addView(libraryCount);
        return header;
    }

    private View buildActions() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        Button add = actionButton("+ 문서 추가");
        add.setOnClickListener(v -> openDocumentPicker());
        row.addView(add);

        Button list = actionButton("자료 목록");
        list.setOnClickListener(v -> runMessage("자료 목록"));
        row.addView(list);

        Button help = actionButton("사용 예시");
        help.setOnClickListener(v -> addMessage(false, "예시\n- 검색 홍길동\n- 확인 납품일\n- 요약 계약 조건\n- 자료 목록\n\n카카오톡이나 메모 앱의 공유 메뉴에서도 폰 에이전트로 내용을 보낼 수 있습니다."));
        row.addView(help);

        scroll.addView(row);
        return scroll;
    }

    private View buildComposer() {
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM);
        composer.setPadding(dp(12), dp(10), dp(12), dp(12));
        composer.setBackgroundColor(Color.WHITE);

        input = new EditText(this);
        input.setHint("자료에서 무엇을 찾을까요?");
        input.setTextColor(inkColor);
        input.setHintTextColor(Color.rgb(132, 143, 138));
        input.setTextSize(16);
        input.setMinHeight(dp(48));
        input.setMaxLines(4);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setBackground(roundRect(Color.rgb(247, 248, 245), 8, 1, Color.rgb(210, 216, 212)));
        composer.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button send = new Button(this);
        send.setText("전송");
        send.setTextColor(Color.WHITE);
        send.setTextSize(14);
        send.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        send.setAllCaps(false);
        send.setMinWidth(0);
        send.setPadding(dp(14), 0, dp(14), 0);
        send.setBackground(roundRect(brandColor, 8, 0, 0));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(68), dp(48));
        sendParams.setMargins(dp(8), 0, 0, 0);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> {
            String message = input.getText().toString();
            input.setText("");
            runMessage(message);
        });
        return composer;
    }

    private void runMessage(String message) {
        if (message.trim().isEmpty()) return;
        addMessage(true, message.trim());
        String response = agent.respond(message);
        addMessage(false, response);
    }

    private void addMessage(boolean fromUser, String body) {
        TextView bubble = text(body, 15, inkColor, false);
        bubble.setTextIsSelectable(true);
        bubble.setLineSpacing(0, 1.12f);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setBackground(roundRect(fromUser ? userBubbleColor : Color.WHITE, 8, 1,
                fromUser ? Color.TRANSPARENT : Color.rgb(225, 229, 226)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(fromUser ? dp(42) : 0, dp(6), fromUser ? 0 : dp(24), dp(6));
        messages.addView(bubble, params);
        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/plain", "text/csv", "text/html", "application/json", "application/xml"
        });
        startActivityForResult(intent, REQUEST_DOCUMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_DOCUMENT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            String content = readText(uri);
            if (content.isBlank()) throw new IllegalArgumentException("빈 문서");
            String name = queryName(uri);
            store.add(name, content);
            updateLibraryCount();
            addMessage(false, "'" + name + "' 문서를 저장했습니다. 이제 이 문서의 내용을 검색하거나 요약할 수 있습니다.");
        } catch (Exception error) {
            Toast.makeText(this, "문서를 읽지 못했습니다. 텍스트 문서인지 확인해 주세요.", Toast.LENGTH_LONG).show();
        }
    }

    private String readText(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (inputStream == null) throw new IllegalArgumentException("열 수 없는 문서");
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOCUMENT_BYTES) throw new IllegalArgumentException("문서가 너무 큼");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String queryName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        }
        return "가져온 문서";
    }

    private void handleSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (shared == null || shared.toString().trim().isEmpty()) return;
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (title == null || title.isBlank()) title = "공유받은 내용";
        store.add(title, shared.toString());
        updateLibraryCount();
        addMessage(false, "다른 앱에서 공유한 내용을 저장했습니다.\n\n" + preview(shared.toString()));
        intent.removeExtra(Intent.EXTRA_TEXT);
    }

    private String preview(String text) {
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() <= 220 ? clean : clean.substring(0, 220) + "...";
    }

    private void updateLibraryCount() {
        if (libraryCount != null) libraryCount.setText("자료 " + store.all().size() + "개");
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(inkColor);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(13), 0, dp(13), 0);
        button.setBackground(roundRect(Color.rgb(247, 248, 245), 8, 1, Color.rgb(218, 223, 219)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0 && strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
