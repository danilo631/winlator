package com.winlator.optimization;

import java.text.Normalizer;
import java.util.Locale;

/** Pure deterministic library filtering; accents and letter case are ignored. */
public final class LibraryFilter {
    private LibraryFilter() {}

    public static boolean matches(String query, String... searchable) {
        String needle = normalize(query);
        if (needle.length() == 0) return true;
        if (searchable == null) return false;
        for (String value : searchable) {
            if (normalize(value).contains(needle)) return true;
        }
        return false;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
