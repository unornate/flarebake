package dev.flarebake;

import picocli.CommandLine.Command;
import picocli.CommandLine;

/** Root command. With no subcommand it prints the usage help. */
@Command(
        name = "flarebake",
        mixinStandardHelpOptions = true,
        version = "flarebake 0.1.0",
        description = "Prepare a JBake site for deployment to Cloudflare Workers.",
        subcommands = {InitCommand.class}
)
public final class FlarebakeCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}
