package com.unornate.flarebake;

import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FlarebakeCommand()).execute(args);
        System.exit(exitCode);
    }
}
