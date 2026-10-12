package com.winlator.optimization;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline validation of upstream GitHub release metadata. No Android dependency. */
public final class ReleaseAssetPolicy {
    private ReleaseAssetPolicy() {}
    public static final long MAX_DOWNLOAD_BYTES = 384L * 1024 * 1024;
    public static final String API = "https://api.github.com/repos/Kron4ek/Wine-Builds/releases?per_page=24";
    public static final String GITHUB = "https://github.com/Kron4ek/Wine-Builds/releases/download/";
    private static final Pattern SHA = Pattern.compile("(?m)^\\s*([a-fA-F0-9]{64})\\s+([^\\s]+)\\s*$");
    private static final Pattern PROTON = Pattern.compile("^wine-proton-([0-9][a-zA-Z0-9.\\-]*)-amd64-wow64\\.tar\\.xz$");
    private static final Pattern WINE = Pattern.compile("^wine-([0-9][a-zA-Z0-9.\\-]*)-staging-amd64-wow64\\.tar\\.xz$");

    public static boolean allowedTag(String tag) {
        return tag != null && tag.matches("[a-zA-Z0-9._-]{2,64}");
    }

    public static boolean allowedAsset(String name) {
        return name != null && (PROTON.matcher(name).matches() || WINE.matcher(name).matches());
    }

    public static boolean matchesRelease(String tag, String filename) {
        if (!allowedTag(tag) || !allowedAsset(filename)) return false;
        boolean proton = tag.startsWith("proton-");
        boolean stable = proton && !tag.contains("exp") && !tag.contains("beta")
            && !tag.contains("rc");
        return (stable && PROTON.matcher(filename).matches()
                && filename.startsWith("wine-" + tag + "-"))
            || (!proton && WINE.matcher(filename).matches()
                && filename.startsWith("wine-" + tag + "-"));
    }

    public static String sha256(String body, String filename) {
        if (body == null || filename == null || !allowedAsset(filename)) return null;
        Matcher matcher = SHA.matcher(body);
        while (matcher.find()) {
            if (filename.equals(matcher.group(2)))
                return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /** Signed-in GitHub links and other domains are never trusted as download roots. */
    public static boolean trustedAssetUrl(String tag, String name, String url) {
        if (!matchesRelease(tag, name) || url == null) return false;
        return url.equals(GITHUB + tag + "/" + name);
    }

    /** GitHub release asset redirects require HTTPS on official asset CDNs. */
    public static boolean trustedRedirect(String url) {
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || uri.getPort() != -1) return false;
            String host = uri.getHost();
            return host != null && (host.equals("github.com")
                || host.equals("release-assets.githubusercontent.com")
                || host.equals("objects.githubusercontent.com"));
        }
        catch (Exception ex) { return false; }
    }

    public static boolean expectedSize(long count) {
        return count > 1024 && count <= MAX_DOWNLOAD_BYTES;
    }
}
