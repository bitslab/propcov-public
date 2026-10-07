package edu.uic.bitslab.propcov.core.util.stream;

import edu.uic.bitslab.propcov.core.util.Serialize;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ObjectInputStreamCustomLoaderTest {
    @Test
    public void testResolveClassWithCustomClassLoaderSuccess() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        try (InputStream byteArrayInputStream = new ByteArrayInputStream(Serialize.Write("1234"));
            ObjectInputStreamCustomLoader objectInputStream = new ObjectInputStreamCustomLoader(byteArrayInputStream, classLoader)) {

            Class<?> resolvedClass = objectInputStream.resolveClass(objectStreamClass);
            assertEquals(String.class, resolvedClass, "Class should be resolved using the custom class loader");
        }
    }

    @Test
    public void testResolveClassDefaultFallback() throws Exception {
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        ClassLoader mockClassLoader = spy(ClassLoader.class);
        doThrow(new ClassNotFoundException("Class not found")).when(mockClassLoader).loadClass(Mockito.anyString());

        try (InputStream byteArrayInputStream = new ByteArrayInputStream(Serialize.Write("1234"));
             ObjectInputStreamCustomLoader objectInputStream = new ObjectInputStreamCustomLoader(byteArrayInputStream, mockClassLoader)) {

            Class<?> resolvedClass = objectInputStream.resolveClass(objectStreamClass);
            assertEquals(String.class, resolvedClass, "Class should fall back to the superclass implementation");
        }
    }

    @Test
    public void testResolveClassThrowsIOException() {
        ObjectStreamClass objectStreamClass = mock(ObjectStreamClass.class);
        when(objectStreamClass.getName()).thenThrow(new RuntimeException("Unexpected runtime exception to simulate IO failure"));

        ClassLoader mockClassLoader = spy(ClassLoader.class);

        assertThrows(IOException.class, () -> {
            try (InputStream byteArrayInputStream = new ByteArrayInputStream(new byte[0]);
                 ObjectInputStreamCustomLoader objectInputStream = new ObjectInputStreamCustomLoader(byteArrayInputStream, mockClassLoader)) {

                objectInputStream.resolveClass(objectStreamClass);
            }
        }, "Should wrap runtime exceptions into IOExceptions");
    }
}