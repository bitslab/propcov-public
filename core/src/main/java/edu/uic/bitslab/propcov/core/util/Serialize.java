package edu.uic.bitslab.propcov.core.util;

import edu.uic.bitslab.propcov.core.util.stream.ObjectInputStreamCustomLoader;
import java.io.*;

/**
 * The Serialize class provides utility methods for serializing and deserializing Java objects to and from
 * byte arrays. It uses standard Java serialization mechanisms and supports custom class loading during
 * deserialization.
 */
public class Serialize {
    /**
     * Serializes a given object into a byte array using Java's standard serialization mechanism.
     * The object must implement the {@code Serializable} interface.
     *
     * @param <T> the type of the object to be serialized
     * @param obj the object to be serialized, which must not be null and must implement {@code Serializable}
     * @return a byte array representation of the serialized object
     * @throws IOException if an I/O error occurs during the serialization process
     */
    public static <T> byte[] Write(T obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    /**
     * Deserializes an object from a given byte array using Java's standard deserialization mechanism
     * with support for a custom class loader. The byte array must represent a serialized object.
     *
     * @param <T> the type of the object expected to be deserialized
     * @param bytes the byte array containing the serialized object data
     * @return the deserialized object of type {@code T}
     * @throws IOException if an I/O error occurs during the deserialization process
     * @throws ClassNotFoundException if the class of the deserialized object cannot be found
     */
    public static <T> T Read(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStreamCustomLoader(bis, SUTClassLoader.get())) {

            //noinspection unchecked
            return (T) in.readObject();
        }
    }
}