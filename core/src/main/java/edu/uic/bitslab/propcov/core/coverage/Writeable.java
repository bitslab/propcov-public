package edu.uic.bitslab.propcov.core.coverage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a specific field is writeable.
 * This annotation is applied at the field level and
 * serves as a marker for fields that can be modified.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Writeable {
}
