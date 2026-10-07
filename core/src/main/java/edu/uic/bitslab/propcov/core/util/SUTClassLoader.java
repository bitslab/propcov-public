package edu.uic.bitslab.propcov.core.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The SUTClassLoader class is a utility designed to manage a custom {@link URLClassLoader} for dynamically
 * loading classes and resources from specified paths. It provides methods to add paths to the class loader
 * and retrieve the configured class loader instance.
 * <p>
 * This class is particularly useful in scenarios where test environments or runtime execution environments
 * require the loading of classes or resources from dynamically set directories or JAR paths. The added
 * paths are used to construct the class loader, and later requests will return the same loader instance
 * unless explicitly modified.
 */
public class SUTClassLoader {
    private static URLClassLoader sutClassLoader;
    private static final List<URL> urls = new ArrayList<>();

    private SUTClassLoader() {}

    /**
     * Adds an array of paths to the internal list of URLs and resets the custom class loader.
     *
     * @param paths an array of {@code Path} objects to be added to the class loader's URL list.
     *              Each path is expected to point to a file or directory. The paths will be
     *              converted to JAR file URLs and normalized before being added.
     *              If a {@code MalformedURLException} occurs during the URL generation,
     *              a {@code RuntimeException} will be thrown.
     */
    public static void addPaths(Path[] paths) {
        urls.addAll(Arrays.stream(paths)
            .map( path -> {
                try {
                    return new URL("jar:file:" + path.toAbsolutePath().normalize() + "!/");
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList())
        );
        sutClassLoader = null;
    }

    /**
     * Retrieves the shared {@link URLClassLoader} instance managed by this class.
     * If the class loader has not been initialized yet, it creates a new instance
     * using the URLs added to the internal list.
     *
     * @return the shared {@link URLClassLoader} instance for dynamically loading classes and resources
     */
    public static URLClassLoader get() {
        if (sutClassLoader == null) {
            sutClassLoader = new URLClassLoader(urls.toArray(URL[]::new));
        }

        return sutClassLoader;
    }
}