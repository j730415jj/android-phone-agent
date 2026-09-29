package com.j730415jj.phoneagent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AgentEngine {
    private final DocumentStore store;

    AgentEngine(DocumentStore store) {
        this.store = store;
    }

    String respond(String message) {
        String input = message.trim();
        if (input.isEmpty()) return "질문이나 검색어를 입력해 주세요.";

        if (input.equals("목록") || input.contains("자료 목록")) {
            return listLibrary();
        }

        if (input.startsWith("검색 ") || input.startsWith("찾아 ") || input.startsWith("확인 ")) {
            String query = input.substring(input.indexOf(' ') + 1).trim();
            return searchResponse(query, false);
        }

        if (input.startsWith("요약 ")) {
            String query = input.substring(3).trim();
            return searchResponse(query, true);
        }

        List<LibraryItem> matches = store.search(input);
        if (!matches.isEmpty()) {
            return formatMatches(input, matches, false);
        }

        if (store.all().isEmpty()) {
            return "아직 저장된 자료가 없습니다. 위의 '문서 추가'를 누르거나 다른 앱의 공유 메뉴에서 폰 에이전트를 선택해 주세요.";
        }
        return "저장된 자료에서 관련 내용을 찾지 못했습니다. '검색 계약기간'처럼 핵심 단어로 다시 입력해 보세요.";
    }

    private String searchResponse(String query, boolean summarize) {
        if (query.isEmpty()) return summarize ? "요약할 주제를 입력해 주세요." : "찾을 내용을 입력해 주세요.";
        List<LibraryItem> matches = store.search(query);
        if (matches.isEmpty()) return "'" + query + "'와 관련된 내용을 저장된 자료에서 찾지 못했습니다.";
        return formatMatches(query, matches, summarize);
    }

    private String formatMatches(String query, List<LibraryItem> matches, boolean summarize) {
        StringBuilder result = new StringBuilder();
        result.append("저장된 자료 ").append(matches.size()).append("개에서 관련 내용을 찾았습니다.\n\n");
        int limit = Math.min(matches.size(), 3);
        for (int i = 0; i < limit; i++) {
            LibraryItem item = matches.get(i);
            result.append("[").append(item.title).append("]\n");
            result.append(summarize ? summarize(item.content, query) : snippet(item.content, query));
            if (i < limit - 1) result.append("\n\n");
        }
        return result.toString();
    }

    private String snippet(String content, String query) {
        String lower = content.toLowerCase(Locale.KOREAN);
        int index = -1;
        for (String term : query.toLowerCase(Locale.KOREAN).split("\\s+")) {
            if (term.length() > 1) {
                index = lower.indexOf(term);
                if (index >= 0) break;
            }
        }
        if (index < 0) index = 0;
        int start = Math.max(0, index - 80);
        int end = Math.min(content.length(), index + 220);
        String text = content.substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "..." : "") + text + (end < content.length() ? "..." : "");
    }

    private String summarize(String content, String query) {
        String[] sentences = content.replace('\n', ' ').split("(?<=[.!?。])\\s+|(?<=다\\.)\\s*");
        Set<String> selected = new LinkedHashSet<>();
        String loweredQuery = query.toLowerCase(Locale.KOREAN);
        for (String sentence : sentences) {
            String clean = sentence.replaceAll("\\s+", " ").trim();
            if (clean.length() < 8) continue;
            for (String term : loweredQuery.split("\\s+")) {
                if (term.length() > 1 && clean.toLowerCase(Locale.KOREAN).contains(term)) {
                    selected.add(clean);
                    break;
                }
            }
            if (selected.size() == 3) break;
        }
        if (selected.isEmpty()) {
            for (String sentence : sentences) {
                String clean = sentence.replaceAll("\\s+", " ").trim();
                if (clean.length() >= 8) selected.add(clean);
                if (selected.size() == 3) break;
            }
        }
        String summary = String.join("\n- ", selected);
        return summary.isEmpty() ? snippet(content, query) : "- " + summary;
    }

    private String listLibrary() {
        List<LibraryItem> items = store.all();
        if (items.isEmpty()) return "저장된 자료가 없습니다.";
        StringBuilder result = new StringBuilder("저장된 자료 ").append(items.size()).append("개\n\n");
        int limit = Math.min(items.size(), 12);
        for (int i = 0; i < limit; i++) {
            result.append(i + 1).append(". ").append(items.get(i).title).append("\n");
        }
        return result.toString().trim();
    }
}
