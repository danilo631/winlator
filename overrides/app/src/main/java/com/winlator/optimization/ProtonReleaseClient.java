package com.winlator.optimization;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

/**
 * Catalog and verified downloads, only Kron4ek glibc x86_64 Wine builds.
 * Never executes a downloaded binary, and never installs an Android/Bionic WCP.
 */
public final class ProtonReleaseClient {
    private ProtonReleaseClient() {}
    public static final class Asset {
        public final String title, tag, filename, url, sha256;
        public final long bytes;
        public final boolean proton;
        Asset(String title, String tag, String filename, String url,
              String sha256, long bytes, boolean proton) {
            this.title = title; this.tag = tag; this.filename = filename;
            this.url = url; this.sha256 = sha256; this.bytes = bytes;
            this.proton = proton;
        }
        @Override public String toString() {
            return title + "  ·  " + (bytes / (1024 * 1024)) + " MB";
        }
    }

    public interface Progress {
        void update(int percent);
    }

    public static ArrayList<Asset> catalog() throws Exception {
        // GitHub REST public API, fixed owner/repository; no user-controlled URL.
        HttpsURLConnection connection = open(ReleaseAssetPolicy.API);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException("GitHub API HTTP " + connection.getResponseCode()
                    + ". Se atingiu o limite de consultas, tente novamente mais tarde.");
            String json = readLimited(connection.getInputStream(), 2 * 1024 * 1024);
            JSONArray releases = new JSONArray(json);
            ArrayList<Asset> results = new ArrayList<>();
            int protonCount = 0, wineCount = 0;
            for (int i = 0; i < releases.length() && results.size() < 8; i++) {
                JSONObject rel = releases.getJSONObject(i);
                if (rel.optBoolean("draft", false) || rel.optBoolean("prerelease", false)) continue;
                String tag = rel.optString("tag_name");
                if (!ReleaseAssetPolicy.allowedTag(tag)) continue;
                String body = rel.optString("body", "");
                boolean proton = tag.startsWith("proton-");
                if (proton && protonCount >= 5) continue;
                if (!proton && wineCount >= 3) continue;

                JSONArray assets = rel.optJSONArray("assets");
                if (assets == null) continue;
                for (int j = 0; j < assets.length(); j++) {
                    JSONObject file = assets.getJSONObject(j);
                    String name = file.optString("name");
                    String url = file.optString("browser_download_url");
                    long bytes = file.optLong("size", -1);
                    if (!ReleaseAssetPolicy.trustedAssetUrl(tag, name, url)
                            || !ReleaseAssetPolicy.expectedSize(bytes)) continue;
                    String digest = ReleaseAssetPolicy.sha256(body, name);
                    // No digest means not eligible for unattended download.
                    if (digest == null) continue;
                    results.add(new Asset(proton ? "Proton " + tag.substring(7)
                            : "Wine Staging " + tag, tag, name, url, digest, bytes, proton));
                    if (proton) protonCount++; else wineCount++;
                    break;  // one tested WoW64 package per release
                }
            }
            if (results.isEmpty())
                throw new IOException("Nenhum pacote WoW64 Linux/glibc com SHA-256 publicado foi encontrado.");
            return results;
        }
        finally { connection.disconnect(); }
    }

    public static File download(Context context, Asset asset, AtomicBoolean cancelled,
                                Progress progress) throws Exception {
        if (!ReleaseAssetPolicy.trustedAssetUrl(asset.tag, asset.filename, asset.url)
                || !ReleaseAssetPolicy.expectedSize(asset.bytes)
                || asset.sha256 == null || !asset.sha256.matches("[a-f0-9]{64}"))
            throw new IOException("Metadados do pacote inválidos.");
        File directory = new File(context.getCacheDir(), "wine-releases");
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IOException("Não foi possível preparar o cache de Wine.");
        long free = directory.getUsableSpace();
        if (free < asset.bytes * 2 + 128L * 1024 * 1024)
            throw new IOException("Armazenamento insuficiente para baixar e extrair Wine.");
        File target = new File(directory, asset.filename);
        File partial = new File(directory, asset.filename + ".part");
        if (partial.exists() && !partial.delete())
            throw new IOException("Não foi possível limpar download antigo.");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long readTotal = 0;
        try {
            HttpsURLConnection connection = open(asset.url);
            try {
                // Redirects must stay on GitHub's official release asset CDN.
                for (int redirects = 0; redirects < 5
                    && connection.getResponseCode() / 100 == 3; redirects++) {
                    String redirect = connection.getHeaderField("Location");
                    if (!ReleaseAssetPolicy.trustedRedirect(redirect))
                        throw new IOException("Redirecionamento de download não confiável.");
                    connection.disconnect();
                    connection = open(redirect);
                }
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    throw new IOException("Download HTTP " + connection.getResponseCode());
                long advertised = connection.getContentLengthLong();
                if (advertised > ReleaseAssetPolicy.MAX_DOWNLOAD_BYTES ||
                        (advertised >= 0 && advertised != asset.bytes))
                    throw new IOException("Tamanho informado pelo servidor não confere.");
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(partial)) {
                    byte[] buffer = new byte[65536];
                    int n;
                    while ((n = input.read(buffer)) != -1) {
                        if (cancelled.get()) throw new IOException("Download cancelado.");
                        readTotal += n;
                        if (readTotal > asset.bytes || readTotal > ReleaseAssetPolicy.MAX_DOWNLOAD_BYTES)
                            throw new IOException("Arquivo ultrapassou o tamanho esperado.");
                        sha.update(buffer, 0, n);
                        output.write(buffer, 0, n);
                        progress.update((int)((100L * readTotal) / asset.bytes));
                    }
                    output.getFD().sync();
                }
            }
            finally { connection.disconnect(); }
            if (readTotal != asset.bytes)
                throw new IOException("Download incompleto: " + readTotal + "/" + asset.bytes + " bytes.");
            StringBuilder hex = new StringBuilder(64);
            for (byte octet : sha.digest())
                hex.append(String.format(java.util.Locale.ROOT, "%02x", octet & 255));
            if (!asset.sha256.equals(hex.toString()))
                throw new IOException("Falha na verificação SHA-256. Nenhum arquivo será instalado.");
            if (target.exists() && !target.delete())
                throw new IOException("Não foi possível atualizar o pacote antigo.");
            if (!partial.renameTo(target))
                throw new IOException("Não foi possível finalizar o download.");
            return target;
        }
        catch (Exception e) {
            partial.delete();
            throw e;
        }
    }

    private static HttpsURLConnection open(String url) throws Exception {
        URL parsed = new URL(url);
        if (!"https".equals(parsed.getProtocol()))
            throw new IOException("Apenas HTTPS é permitido.");
        HttpsURLConnection connection = (HttpsURLConnection)parsed.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "Winlator-Enhanced/11.2");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private static String readLimited(InputStream input, int maxBytes) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > maxBytes) throw new IOException("Resposta do catálogo muito grande.");
                out.write(buffer, 0, n);
            }
            return out.toString("UTF-8");
        }
    }
}
