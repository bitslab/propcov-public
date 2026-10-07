package edu.uic.bitslab.propcov.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define metadata for serializing fields to YAML output.
 * This annotation enables control over the ordering of fields in the
 * serialized output and can be applied to fields that should be included
 * in the YAML representation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YamlOutput {
    /**
     * Represents the default ordering value for YAML output fields. This constant
     * is used when no specific order is defined, assigning the highest possible
     * order value by default.
     */
    int DEFAULT_ORDER = Integer.MAX_VALUE;
    /**
     * Specifies the order in which fields should be output.
     * By default, it uses the value of {@code DEFAULT_ORDER}.
     *
     * @return the order value indicating the position of the field
     */
    int order() default DEFAULT_ORDER;
}
