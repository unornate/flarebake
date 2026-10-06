package com.unornate.flarebake;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "init",
        mixinStandardHelpOptions = true,
        description = "Add Cloudflare Workers deployment files to a JBake project."
)
public final class InitCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "DIRECTORY",
            description = "JBake project directory (default: current directory).")
    private Path directory;

    @Option(names = {"-n", "--name"}, paramLabel = "NAME",
            description = "Cloudflare Worker name (default: derived from the directory name).")
    private String workerName;

    @Option(names = {"-f", "--force"},
            description = "Overwrite existing wrangler.toml and build.sh.")
    private boolean force;

    @Option(names = "--jbake-version", paramLabel = "VERSION", defaultValue = "2.7.0",
            description = "JBake version used by build.sh (default: ${DEFAULT-VALUE}).")
    private String jbakeVersion;

    @Option(names = "--java-version", paramLabel = "VERSION", defaultValue = "21",
            description = "Temurin JRE major version used by build.sh (default: ${DEFAULT-VALUE}).")
    private String javaVersion;

    @Option(names = "--compatibility-date", paramLabel = "DATE",
            description = "Cloudflare compatibility_date (default: today).")
    private String compatibilityDate;

    @Override
    public Integer call() throws Exception {
        Path root = (directory == null ? Path.of("") : directory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.err.println("error: not a directory: " + root);
            return 1;
        }

        String name = (workerName != null && !workerName.isBlank())
                ? workerName
                : Scaffolder.sanitizeName(String.valueOf(root.getFileName()));
        String date = (compatibilityDate != null && !compatibilityDate.isBlank())
                ? compatibilityDate
                : LocalDate.now().toString();

        Scaffolder.Options options = new Scaffolder.Options(root, name, date, jbakeVersion, javaVersion, force);
        Scaffolder.Result result = new Scaffolder(options).run();

        PrintWriter out = new PrintWriter(System.out, true);
        out.println("flarebake - Cloudflare Workers setup for JBake");
        out.println();
        out.printf("Project: %s%n", result.root());
        out.printf("Worker:  %s%n", name);
        if (!result.jbakeProject()) {
            out.println("Warning: jbake.properties not found - is this a JBake project?");
        }
        out.println();
        print(out, "Created", result.created());
        print(out, "Updated", result.updated());
        print(out, "Skipped (already present, use --force to overwrite)", result.skipped());
        out.println();
        out.println("Next steps:");
        out.println("  1. Review wrangler.toml and adjust the Worker name if needed.");
        out.println("  2. Commit the new files and push.");
        out.println("  3. Cloudflare dashboard: Workers & Pages -> Create -> Workers -> Connect to Git.");
        out.println("     Deploy command: npx wrangler deploy (leave the build command empty).");
        return 0;
    }

    private static void print(PrintWriter out, String label, List<String> files) {
        if (files.isEmpty()) {
            return;
        }
        out.printf("%s:%n", label);
        files.forEach(file -> out.printf("  - %s%n", file));
    }
}
