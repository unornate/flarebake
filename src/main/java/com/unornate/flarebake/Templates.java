package com.unornate.flarebake;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class Templates {

    private Templates() {
    }

    static String render(String name, Map<String, String> values) {
        String template = read(name);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private static String read(String name) {
        try (InputStream in = Templates.class.getResourceAsStream("/templates/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled template: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read template: " + name, e);
        }
    }
}
