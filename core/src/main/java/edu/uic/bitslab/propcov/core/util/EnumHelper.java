package edu.uic.bitslab.propcov.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to help with processing enums
 */
public class EnumHelper {
    private EnumHelper() {}

    /**
     * Convert a given comma-separated string to a list of enum elements typed
     * based on the given class.
     *
     * @param value String of values to process
     * @param enumClass Enum class used for each element
     * @param <T> Type that extends Enum
     * @return List of elements with each element of type T
     */
    public static <T extends Enum<T>> Collection<T> stringToCollection(String value, Class<T> enumClass) {
        return stringToCollection(value, enumClass, ",");
    }

    /**
     * Convert a given separated string to a list of enum elements typed
     * based on the given class.
     *
     * @param value String of values to process
     * @param enumClass Enum class used for each element
     * @param separator Separator value used to split the string.
     * @param <T> Type that extends Enum
     * @return List of elements with each element of type T
     */
    public static <T extends Enum<T>> Collection<T> stringToCollection(String value, Class<T> enumClass, String separator) {
        if (value == null || value.isEmpty()) return List.of();

        return Arrays.stream(value.split(separator))
                .map(m -> stringToObject(m, enumClass))
                .collect(Collectors.toList());
    }

    /**
     * @param value Value to locate the enum class element.
     * @param enumClass Class of an enum
     * @param <T> Enum value
     * @return T representing the enum value found.
     */
    public static <T extends Enum<T>> T stringToObject(String value, Class<T> enumClass) {
        return T.valueOf(enumClass, value);
    }

}
