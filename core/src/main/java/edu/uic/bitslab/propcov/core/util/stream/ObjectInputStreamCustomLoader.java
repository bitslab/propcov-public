package edu.uic.bitslab.propcov.core.util.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * A specialized implementation of {@link ObjectInputStream} that allows the use of a custom
 * {@link ClassLoader} during the deserialization process. This is useful in scenarios where
 * classes need to be loaded from a specific custom class loader rather than the default system
 * class loader.
 */
public class ObjectInputStreamCustomLoader extends ObjectInputStream {
    private final ClassLoader cl;

    /**
     * Constructs a new ObjectInputStreamCustomLoader with the specified input stream and custom class loader.
     * This allows deserialization to use the provided class loader for resolving classes instead of the default.
     *
     * @param in the input stream to read serialized objects from
     * @param cl the custom class loader to use for resolving classes during deserialization
     * @throws IOException if an I/O error occurs while creating the ObjectInputStream
     */
    public ObjectInputStreamCustomLoader(InputStream in, ClassLoader cl) throws IOException {
        super(in);
        this.cl = cl;
    }

    @Override
    public Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException, IOException {
        try {
            // custom cl first...
            return Class.forName(desc.getName(), true, cl);
        } catch (ClassNotFoundException ignored) {
            // then try normal way
            return super.resolveClass(desc);
        }
    }
}
