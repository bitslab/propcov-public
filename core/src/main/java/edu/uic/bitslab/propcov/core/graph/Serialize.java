package edu.uic.bitslab.propcov.core.graph;

import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import edu.uic.bitslab.propcov.core.util.stream.ObjectInputStreamCustomLoader;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import java.io.*;


/**
 * The Serialize class provides utility methods for serializing and deserializing
 * objects of type AbstractBaseGraph with nodes of type ColorNode and edges of type DefaultEdge.
 * These methods allow converting a graph object into a byte array and vice versa.
 */
public class Serialize {
    /**
     * Serializes a graph object into a byte array.
     * The graph must be of a type that extends AbstractBaseGraph with nodes of type ColorNode
     * and edges of type DefaultEdge.
     *
     * @param <T> Type that extends AbstractBaseGraph
     * @param graph the graph object to serialize
     * @return a byte array representing the serialized graph
     * @throws IOException if an I/O error occurs during serialization
     */
    public static <T extends AbstractBaseGraph<? extends ColorNode,? extends DefaultEdge>> byte[] Write(T graph) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(graph);
            return bos.toByteArray();
        }
    }

    /**
     * Deserializes a byte array into an object of type AbstractBaseGraph with nodes of type ColorNode
     * and edges of type DefaultEdge. The method reads the serialized object data from the provided byte array.
     *
     * @param <T> Type that extends AbstractBaseGraph
     * @param bytes the byte array containing the serialized graph object
     * @return an object of type T that represents the deserialized graph
     * @throws IOException if an I/O error occurs during the deserialization process
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    public static <T extends AbstractBaseGraph<? extends ColorNode,? extends DefaultEdge>> T Read(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStreamCustomLoader(bis, SUTClassLoader.get())) {

            //noinspection unchecked
            return (T) in.readObject();
        }
    }
}
