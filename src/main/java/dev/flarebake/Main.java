package dev.flarebake;

import picocli.CommandLine;

/** Entry point for the {@code flarebake} CLI. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FlarebakeCommand()).execute(args);
        System.exit(exitCode);
    }
}
