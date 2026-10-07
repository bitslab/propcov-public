package edu.uic.bitslab.propcov.extensions.source;

import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Local source implementation
 */
public class Local extends AbstractSource {
    /**
     * Represents the directory used as a local source for the project.
     * This variable holds the path to the directory specified in the source configuration
     * and is validated during initialization to ensure it exists.
     */
    public final Path directory;

    /**
     * Initializes a Local source with the provided extension configuration and project name.
     * This constructor validates the presence of the directory specified in the extension configuration
     * and initializes the directory field for the local source.
     *
     * @param extension the extension configuration object containing properties and environment settings
     * @param projectName the name of the project associated with this local source
     * @throws SourceException if the directory specified in the extension configuration is not found or invalid
     */
    public Local(YAMLConfig.Extension extension, String projectName) throws SourceException {
        super(extension, projectName, "PropCov.Source.Local.");

        Path directory = Path.of(extension.properties.get("Directory"));

        try {
            validateDirectory(directory);
            this.directory = directory;
        } catch (NoSuchFileException e) {
            throw new SourceException("Could not find directory: " + directory, e);
        }
    }

    @Override
    public void get() {
    }

    private static void validateDirectory(Path directory) throws NoSuchFileException, IllegalArgumentException {
        if (Files.exists(directory)) {
            return;
        }

        throw new NoSuchFileException(directory.toString());
    }
}
