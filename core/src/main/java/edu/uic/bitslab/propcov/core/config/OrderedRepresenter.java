package edu.uic.bitslab.propcov.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The OrderedRepresenter class extends the Representer class and provides a mechanism
 * for determining the order of properties when serializing an object to YAML. It enables
 * custom sorting of properties based on their annotations or natural order.
 */
public class OrderedRepresenter extends Representer {
    /**
     * Constructs an instance of OrderedRepresenter with the specified {@code DumperOptions}.
     * This constructor initializes the OrderedRepresenter with the provided options for YAML
     * serialization and formatting.
     *
     * @param options the DumperOptions instance that specifies YAML serialization settings such
     *                as indentation, line breaks, and other representation-related configurations.
     */
    public OrderedRepresenter(DumperOptions options) {
        super(options);
    }

    protected Set<Property> getProperties(Class<?> type) {
        TreeSet<Property> treeSet = new TreeSet<>((a, b) -> {
            YamlOutput annotationA = a.getAnnotation(YamlOutput.class);
            int orderA = annotationA == null ? YamlOutput.DEFAULT_ORDER : annotationA.order();

            YamlOutput annotationB = b.getAnnotation(YamlOutput.class);
            int orderB = annotationB == null ? YamlOutput.DEFAULT_ORDER : annotationB.order();

            if (orderA == orderB) {
                return a.getName().compareTo(b.getName());
            }

            return Integer.compare(orderA, orderB);
        });

        treeSet.addAll(
                typeDefinitions.containsKey(type)
                        ? typeDefinitions.get(type).getProperties()
                        : getPropertyUtils().getProperties(type)
        );

        return treeSet;
    }

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
        // Exclude null values
        if (propertyValue == null) {
            return null;
        }

        // Exclude empty collections
        if (propertyValue instanceof Collection && ((Collection<?>) propertyValue).isEmpty()) {
            return null;
        }
        if (propertyValue instanceof Map && ((Map<?, ?>) propertyValue).isEmpty()) {
            return null;
        }

        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }

}
