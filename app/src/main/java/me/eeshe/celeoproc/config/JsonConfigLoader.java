package me.eeshe.celeoproc.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for JSON configuration files.
 *
 * <p>
 * Prefers an external file when it exists, otherwise falls back to a bundled
 * classpath resource. Subclasses describe where the file lives and how to apply
 * its parsed contents.
 */
public abstract class JsonConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConfigLoader.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String label;
    private final String directory;
    private final String fileName;
    private final String resourcePath;
    private final String pathEnv;

    protected JsonConfigLoader(
            final String label,
            final String directory,
            final String fileName,
            final String resourcePath,
            final String pathEnv) {
        this.label = label;
        this.directory = directory;
        this.fileName = fileName;
        this.resourcePath = resourcePath;
        this.pathEnv = pathEnv;
    }

    /**
     * Mounts the configuration, validates it and applies it.
     */
    public final void load() {
        final JsonNode root = mount();

        validate(root);
        apply(root);
    }

    /**
     * Reloads the configuration, keeping the current values if parsing fails.
     *
     * @return {@code true} if the configuration was reloaded successfully,
     *         {@code false} if it could not be parsed
     */
    public final boolean reload() {
        try {
            load();
            LOGGER.info("Reloaded {} from {}", label, externalPath().toAbsolutePath());
            return true;
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to reload {}, keeping current values", label, exception);
            return false;
        }
    }

    /**
     * Hook validating the mounted configuration before it is applied.
     *
     * @param root parsed root node
     */
    protected void validate(final JsonNode root) {
    }

    /**
     * Applies the mounted configuration.
     *
     * @param root parsed root node
     */
    protected abstract void apply(JsonNode root);

    /**
     * Resolves the external path, preferring a system property over an environment
     * variable over the default directory and file name.
     *
     * @return resolved external path
     */
    protected final Path externalPath() {
        final String environment = System.getenv(pathEnv);
        if (environment != null && !environment.isBlank()) {
            return Path.of(environment.trim());
        }
        return Path.of(directory, fileName);
    }

    private JsonNode mount() {
        final Path externalPath = externalPath();
        if (Files.isRegularFile(externalPath)) {
            return readFile(externalPath);
        }
        return readResource(externalPath);
    }

    private JsonNode readFile(final Path path) {
        try {
            final JsonNode root = OBJECT_MAPPER.readTree(Files.readAllBytes(path));
            if (root == null || root.isNull()) {
                throw new IllegalStateException(
                        "'%s' file '%s' is empty".formatted(label, path.toAbsolutePath()));
            }
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read '%s' file '%s'".formatted(label, path.toAbsolutePath()), exception);
        }
    }

    private JsonNode readResource(final Path externalPath) {
        try (InputStream stream = JsonConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "No '%s' found: '%s' is missing and bundled '%s' does not exist"
                                .formatted(label, externalPath.toAbsolutePath(), resourcePath));
            }
            final JsonNode root = OBJECT_MAPPER.readTree(stream);
            if (root == null || root.isNull()) {
                throw new IllegalStateException(
                        "Bundled '%s' resource '%s' is empty".formatted(label, resourcePath));
            }
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read bundled '%s' resource '%s'".formatted(label, resourcePath), exception);
        }
    }
}
