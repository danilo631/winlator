import com.winlator.optimization.ReleaseAssetPolicy;

public final class ReleaseAssetPolicyTest {
    private static void ok(boolean pass, String reason) {
        if (!pass) throw new AssertionError(reason);
    }
    public static void main(String[] argv) {
        final String proton = "wine-proton-11.0-2-amd64-wow64.tar.xz";
        final String wine = "wine-11.19-staging-amd64-wow64.tar.xz";
        final String hash = "f166f7daa1a37b3c8e0ade213a6e7915e582091804071a58a4a32c6c672e1595";
        ok(ReleaseAssetPolicy.matchesRelease("proton-11.0-2", proton), "Proton Linux WoW64");
        ok(ReleaseAssetPolicy.matchesRelease("11.19", wine), "Staging Linux");
        ok(!ReleaseAssetPolicy.matchesRelease("proton-exp-11.0", proton), "not experimental");
        ok(!ReleaseAssetPolicy.matchesRelease("proton-11.0-2", wine), "tag mismatch");
        ok(!ReleaseAssetPolicy.allowedAsset("proton-11.0-2-arm64ec.wcp"), "Android ABI excluded");
        ok(!ReleaseAssetPolicy.allowedAsset("proton-wine-11.0-2-x86_64.wcp.xz"), "Bionic excluded");
        ok(!ReleaseAssetPolicy.allowedAsset("../wine-proton-11.0-2-amd64-wow64.tar.xz"), "path traversal");
        ok(!ReleaseAssetPolicy.allowedTag("../unsafe"), "bad tag");
        ok(ReleaseAssetPolicy.sha256("not-a-hash  " + proton + "\n"
            + hash + "  " + proton + "\n", proton).equals(hash), "sha256 metadata");
        ok(ReleaseAssetPolicy.sha256(hash + "  another.tar.xz", proton) == null, "missing checksum");
        ok(ReleaseAssetPolicy.trustedAssetUrl("proton-11.0-2", proton,
            "https://github.com/Kron4ek/Wine-Builds/releases/download/proton-11.0-2/"
                + proton), "canonical URL");
        ok(!ReleaseAssetPolicy.trustedAssetUrl("proton-11.0-2", proton,
            "https://evil.test/releases/download/proton-11.0-2/" + proton), "evil URL");
        ok(ReleaseAssetPolicy.trustedRedirect("https://release-assets.githubusercontent.com/ab?x=1"),
            "official asset host");
        ok(!ReleaseAssetPolicy.trustedRedirect("http://release-assets.githubusercontent.com/a"),
            "TLS only");
        ok(!ReleaseAssetPolicy.trustedRedirect("https://github.com.evil.invalid/a"), "suffix domain");
        ok(!ReleaseAssetPolicy.trustedRedirect("https://foo@github.com/a"), "no credentials");
        ok(ReleaseAssetPolicy.expectedSize(80017220), "normal archive");
        ok(!ReleaseAssetPolicy.expectedSize(0), "empty archive");
        ok(!ReleaseAssetPolicy.expectedSize(ReleaseAssetPolicy.MAX_DOWNLOAD_BYTES + 1),
            "size cap");
        System.out.println("ReleaseAssetPolicyTest: PASS");
    }
}
