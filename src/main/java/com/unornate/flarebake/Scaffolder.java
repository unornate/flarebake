package com.unornate.flarebake;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Scaffolder {

    public static final String WRANGLER_FILE = "wrangler.toml";
    public static final String BUILD_SCRIPT = "build.sh";
    public static final String GITIGNORE = ".gitignore";

    private static final String GITIGNORE_HEADER = "# Cloudflare Workers (flarebake)";
    private static final List<String> GITIGNORE_ENTRIES = List.of("output", ".tools/", ".wrangler/");

    private final Options options;

    public Scaffolder(Options options) {
        this.options = options;
    }

    public Result run() throws IOException {
        Path root = options.root();
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        write(root.resolve(WRANGLER_FILE), renderWrangler(), created, updated, skipped);
        write(root.resolve(BUILD_SCRIPT), renderBuildScript(), created, updated, skipped);
        makeExecutable(root.resolve(BUILD_SCRIPT));
        updateGitignore(root.resolve(GITIGNORE), created, updated, skipped);

        boolean jbakeProject = Files.isRegularFile(root.resolve("jbake.properties"));
        return new Result(root, jbakeProject, List.copyOf(created), List.copyOf(updated), List.copyOf(skipped));
    }

    private String renderWrangler() {
        return Templates.render("wrangler.toml.tmpl", Map.of(
                "WORKER_NAME", options.workerName(),
                "COMPATIBILITY_DATE", options.compatibilityDate()));
    }

    private String renderBuildScript() {
        return Templates.render("build.sh.tmpl", Map.of(
                "JBAKE_VERSION", options.jbakeVersion(),
                "JAVA_VERSION", options.javaVersion()));
    }

    private void write(Path file, String content, List<String> created, List<String> updated, List<String> skipped)
            throws IOException {
        boolean exists = Files.exists(file);
        if (exists && !options.force()) {
            skipped.add(relative(file));
            return;
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
        (exists ? updated : created).add(relative(file));
    }

    private void updateGitignore(Path file, List<String> created, List<String> updated, List<String> skipped)
            throws IOException {
        boolean exists = Files.isRegularFile(file);
        String original = exists ? Files.readString(file, StandardCharsets.UTF_8) : "";

        Set<String> present = Arrays.stream(original.split("\\R"))
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));

        List<String> missing = GITIGNORE_ENTRIES.stream()
                .filter(entry -> !present.contains(entry))
                .toList();

        if (missing.isEmpty()) {
            skipped.add(relative(file));
            return;
        }

        StringBuilder out = new StringBuilder(original);
        if (!original.isEmpty()) {
            if (!original.endsWith("\n")) {
                out.append('\n');
            }
            out.append('\n');
        }
        out.append(GITIGNORE_HEADER).append('\n');
        missing.forEach(entry -> out.append(entry).append('\n'));

        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        (exists ? updated : created).add(relative(file));
    }

    private void makeExecutable(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = new HashSet<>(Files.getPosixFilePermissions(file));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(file, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }

    private String relative(Path file) {
        return options.root().relativize(file).toString();
    }

    public static String sanitizeName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (name.isEmpty()) {
            name = "jbake-site";
        }
        if (name.length() > 63) {
            name = name.substring(0, 63).replaceAll("-+$", "");
        }
        return name;
    }

    public record Options(Path root, String workerName, String compatibilityDate,
                          String jbakeVersion, String javaVersion, boolean force) {
    }

    public record Result(Path root, boolean jbakeProject,
                         List<String> created, List<String> updated, List<String> skipped) {
    }
}
