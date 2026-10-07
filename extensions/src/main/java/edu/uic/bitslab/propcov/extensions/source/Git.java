package edu.uic.bitslab.propcov.extensions.source;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Implementation fo the Git source method
 */
public class Git extends AbstractSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Git.class);

    /**
     * URL of the Git repository associated with this source. This variable holds
     * the remote URL used for cloning or fetching the repository. The URL may be
     * replaced by a cached directory location if caching is enabled and properly
     * configured.
     */
    public final String url;

    /**
     * Represents the specific commit or branch ID to be checked out from a Git repository.
     * This variable is used to specify the desired state of the repository during operations
     * such as cloning or fetching.
     */
    public final String checkoutID;

    /**
     * Represents the directory where cached repositories for the Git source are stored.
     * This directory is used to speed up future fetch operations by storing previously cloned repositories.
     * If set, it attempts to use the cache instead of cloning directly from the URL and populates the cache
     * if it's not yet populated.
     */
    public final Path cacheDirectory;

    /**
     * Constructs a new Git object for a given SUT and its associated configuration.
     *
     * @param extension the YAML extension configuration that includes the properties for the Git source (e.g., URL, CheckoutID, CacheDirectory)
     * @param projectName the name of the project associated with this Git source
     */
    public Git(YAMLConfig.Extension extension, String projectName) {
        super(extension, projectName, "PropCov.Source.Git.");

        this.url = extension.properties.get("URL");
        this.checkoutID = extension.properties.get("checkoutID");
        this.cacheDirectory = Path.of(extension.properties.get("CacheDirectory"));
    }

    @Override
    public void get() throws SourceException {
        try {
            cloneRepo();
        } catch (GitAPIException | JGitInternalException e) {
            throw new SourceException(e.getMessage(), e);
        }
    }

    private void fetch(Path destination, String sourceURL) throws GitAPIException {
        try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.cloneRepository()
                .setDirectory(destination.toFile())
                .setURI(sourceURL)
                .call()
        ) {
            // check out specified commit id
            git.checkout()
                    .setName(checkoutID)
                    .call();
        }
    }

    private String fullAbsolute(Path path) {
        return path.toAbsolutePath().normalize().toUri().toString();
    }

    /**
     * If cacheDirectory is set, then use the cache url instead and populate the cache
     * if it hasn't been downloaded yet. Returns URL that should be used.
     *
     */
    private String getCacheURL() {
        if (cacheDirectory == null) return url;

        String cacheHash = Util.sha1(url.getBytes(StandardCharsets.UTF_8));
        Path cacheProjectDir = cacheDirectory.resolve(cacheHash);

        if (cacheProjectDir.toFile().exists()) {
            return fullAbsolute(cacheProjectDir);
        }

        try {
            fetch(cacheProjectDir, url);
            return fullAbsolute(cacheProjectDir);

        } catch (GitAPIException | JGitInternalException e) {
            try {
                Files.delete(cacheProjectDir);
            } catch (IOException ioException) {
                LOGGER.warn("Unable to delete.", ioException);
            }

            // error building cache... so use the given url
            LOGGER.warn("Error cloning to cache, so using direct URL instead.", e);
            return url;
        }
    }

    private void cloneRepo() throws GitAPIException, JGitInternalException {
        fetch(localDirectory, getCacheURL());
    }
}
