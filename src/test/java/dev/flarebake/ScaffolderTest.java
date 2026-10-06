package dev.flarebake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaffolderTest {

    private static Scaffolder.Options options(Path root, boolean force) {
        return new Scaffolder.Options(root, "my-site", "2026-10-06", "2.7.0", "21", force);
    }

    @Test
    void scaffoldsAProject(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("jbake.properties"), "site.host=http://example.com\n");

        Scaffolder.Result result = new Scaffolder(options(dir, false)).run();

        assertTrue(result.jbakeProject());
        assertTrue(result.created().containsAll(List.of("wrangler.toml", "build.sh", ".gitignore")));

        String wrangler = Files.readString(dir.resolve("wrangler.toml"));
        assertTrue(wrangler.contains("name = \"my-site\""));
        assertTrue(wrangler.contains("compatibility_date = \"2026-10-06\""));
        assertTrue(wrangler.contains("[build]"));
        assertTrue(wrangler.contains("command = \"bash build.sh\""));
        assertTrue(wrangler.contains("directory = \"./output\""));

        String buildScript = Files.readString(dir.resolve("build.sh"));
        assertTrue(buildScript.contains("JBAKE_VERSION=\"${JBAKE_VERSION:-2.7.0}\""));
        assertTrue(buildScript.contains("JAVA_VERSION=\"${JAVA_VERSION:-21}\""));

        List<String> ignore = Files.readAllLines(dir.resolve(".gitignore"));
        assertTrue(ignore.contains("output"));
        assertTrue(ignore.contains(".tools/"));
        assertTrue(ignore.contains(".wrangler/"));
    }

    @Test
    void marksBuildScriptExecutable(@TempDir Path dir) throws IOException {
        new Scaffolder(options(dir, false)).run();

        if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
            assertTrue(Files.isExecutable(dir.resolve("build.sh")));
        }
    }

    @Test
    void doesNotOverwriteWithoutForce(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("wrangler.toml"), "keep me\n");
        Files.writeString(dir.resolve("build.sh"), "keep me\n");

        Scaffolder.Result result = new Scaffolder(options(dir, false)).run();

        assertEquals("keep me\n", Files.readString(dir.resolve("wrangler.toml")));
        assertEquals("keep me\n", Files.readString(dir.resolve("build.sh")));
        assertTrue(result.skipped().contains("wrangler.toml"));
        assertTrue(result.skipped().contains("build.sh"));
    }

    @Test
    void overwritesWithForce(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("wrangler.toml"), "old\n");

        Scaffolder.Result result = new Scaffolder(options(dir, true)).run();

        assertTrue(result.updated().contains("wrangler.toml"));
        assertTrue(Files.readString(dir.resolve("wrangler.toml")).contains("name = \"my-site\""));
    }

    @Test
    void mergesGitignoreAndKeepsExistingContent(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), ".idea\noutput\n");

        new Scaffolder(options(dir, false)).run();

        List<String> lines = Files.readAllLines(dir.resolve(".gitignore"));
        assertEquals(".idea", lines.get(0));
        assertEquals("output", lines.get(1));
        assertTrue(lines.contains(".tools/"));
        assertTrue(lines.contains(".wrangler/"));
        assertEquals(1, lines.stream().filter("output"::equals).count(), "output must not be duplicated");
    }

    @Test
    void isIdempotent(@TempDir Path dir) throws IOException {
        new Scaffolder(options(dir, false)).run();

        Scaffolder.Result second = new Scaffolder(options(dir, false)).run();

        assertTrue(second.created().isEmpty());
        assertTrue(second.updated().isEmpty());
        assertTrue(second.skipped().containsAll(List.of("wrangler.toml", "build.sh", ".gitignore")));
    }

    @Test
    void reportsMissingJbakeProject(@TempDir Path dir) throws IOException {
        Scaffolder.Result result = new Scaffolder(options(dir, false)).run();

        assertFalse(result.jbakeProject());
    }

    @Test
    void sanitizesWorkerNames() {
        assertEquals("my-site", Scaffolder.sanitizeName("My Site!"));
        assertEquals("my-site", Scaffolder.sanitizeName("  My---Site  "));
        assertEquals("jbake-site", Scaffolder.sanitizeName("!!!"));
        assertEquals("a", Scaffolder.sanitizeName("A"));
    }
}
