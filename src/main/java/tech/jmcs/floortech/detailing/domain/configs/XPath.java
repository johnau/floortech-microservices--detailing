package tech.jmcs.floortech.detailing.domain.configs;

import java.nio.file.Path;
import java.nio.file.Paths;

public record XPath(
    String path,
    Boolean isRelative
) {
    public static XPath relative(String path) {
        return new XPath(path, true);
    }
    public static XPath relative(Path path) {
        return new XPath(path.toString(), true);
    }
    public static XPath aboslute(String path) {
        return new XPath(path, false);
    }

    public Path toPath() {
        return Paths.get(path);
    }

    public String toFileName() {
        return Paths.get(path).getFileName().toString();
    }
}
