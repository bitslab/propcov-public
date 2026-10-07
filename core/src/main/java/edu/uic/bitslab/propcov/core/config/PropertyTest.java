package edu.uic.bitslab.propcov.core.config;

import java.nio.file.Path;

import static edu.uic.bitslab.propcov.core.config.Config.Builder.validateNotEmpty;

/**
 * This class represents a testable property configuration, which includes
 * fields annotated for serialization in YAML output. It exposes methods for
 * retrieving and transforming property values.
 */
public class PropertyTest {
    /**
     * Represents the name of a property to be serialized in YAML format.
     */
    @YamlOutput(order=1)
    public String name;

    /**
     * Represents the entry point for a property configuration.
     * This field is serialized into YAML output with a specified order.
     */
    @YamlOutput(order=2)
    public String entryPoint;

    /**
     * Optional - Represents the Maven subproject where this test lives.
     */
    @YamlOutput(order=3)
    public String subProject;

    /**
     * Default constructor for the PropertyTest class. This constructor is primarily
     * used dynamically by frameworks like SnakeYAML for object creation and should not
     * be removed despite its lack of direct usage within the codebase.
     * <p>
     * The `@SuppressWarnings("unused")` annotation is applied to prevent accidental
     * removal by signaling its intentional presence in the code.
     */
    @SuppressWarnings("unused")
    public PropertyTest() {
        // snakeyaml uses this dynamically
        // marked to suppress unused warning,
        //   so that it doesn't get accidentally removed.
    }

    /**
     * Default constructor for the PropertyTest class.
     * @param name
     * @param entryPoint
     */
    public PropertyTest(String name, String entryPoint) {
        this(name, entryPoint, null);
    }

    /**
     * Constructs a new PropertyTest instance with the specified name and entryPoint.
     *
     * @param name the name of the property being tested. This can be null or empty
     *             as there is no explicit validation on this parameter.
     * @param entryPoint the entry point for the property configuration. This must
     *                   not be null or empty, and will be validated using
     *                   {@code validateNotEmpty} to ensure this constraint is met.
     * @param subProject the maven subproject where this test lives.
     * @throws IllegalArgumentException if the {@code entryPoint} is null or empty.
     */
    public PropertyTest(String name, String entryPoint, String subProject) {
        validateNotEmpty("entryPoint", entryPoint);

        this.name = name;
        this.entryPoint = entryPoint;
        this.subProject = subProject;
    }

    /**
     * Converts the `entryPoint` string into a {@link Path} object by replacing all
     * forward slashes ('/') with dots ('.').
     *
     * @return a {@link Path} representing the transformed `entryPoint` string.
     */
    public Path entryPointAsPath() {
        return Path.of(entryPoint.replace("/", "."));
    }
}
