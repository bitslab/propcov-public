package edu.uic.bitslab.propcov.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * The YamlConfigRepresenter class extends the OrderedRepresenter class to customize
 * YAML representation for specific configuration classes, namely YAMLConfig and
 * AnalysisConfig. It ensures that instances of these classes are represented as
 * YAML maps without including their class names in the output.
 * This class is typically used in scenarios where structured YAML configuration
 * needs to be output, adhering to a specific order for properties as defined in
 * the parent class's sorting mechanism.
 */
public class YamlConfigRepresenter extends OrderedRepresenter {
    /**
     * Constructs an instance of YamlConfigRepresenter, which extends the OrderedRepresenter
     * class. This constructor customizes the YAML representation for specific configuration
     * classes, such as YAMLConfig and AnalysisConfig. It ensures that instances of these
     * classes are represented as YAML maps, omitting their class names in the output.
     *
     * @param options an instance of DumperOptions that configures the behavior of the YAML
     *                output, including formatting and encoding options.
     */
    public YamlConfigRepresenter(DumperOptions options) {
        super(options);

        // ensure we don't print the object names at the top
        this.addClassTag(YAMLConfig.class, Tag.MAP);
        this.addClassTag(AnalysisConfig.class, Tag.MAP);
    }
}
