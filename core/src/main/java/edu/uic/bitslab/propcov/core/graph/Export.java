package edu.uic.bitslab.propcov.core.graph;

import edu.uic.bitslab.propcov.core.Util;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.slf4j.Logger;

import java.io.*;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Export class is responsible for exporting graph structures to the DOT
 * graph description language format. It extends the functionality of the
 * DOTExporter class to support customization and transformation of graph
 * data. The class is parametrized with vertex and edge types.
 *
 * @param <V> the type of the vertices, which must extend {@link ColorNode}
 * @param <E> the type of the edges, which must extend {@link DefaultEdge}
 */
public class Export<V extends ColorNode, E extends DefaultEdge> extends DOTExporter<V, E> {
    private static final Logger LOGGER = Util.getLogger(Export.class);

    private final Graph<V, E> graph;

    private Export(Builder<V, E> builder) {
        this.graph = builder.graph;

        this.setGraphAttributeProvider(builder.graphAttributes);
        this.setVertexAttributeProvider(builder.vertexAttributes);
        this.setVertexIdProvider(builder.vertexIdProvider);
        this.setEdgeAttributeProvider(builder.edgeAttributeProvider);
    }

    /**
     * Formats the given vertex string by enclosing it in double quotes
     * to make it compatible with DOT format requirements.
     *
     * @param vertex the vertex name to be formatted
     * @return the vertex string enclosed in double quotes
     */
    public static String dotFormat(String vertex) {
        return "\"" + vertex + "\"";
    }

    /**
     * Converts the current graph into a byte array representation.
     * Uses a ByteArrayOutputStream to write the graph export and returns
     * the resulting byte array.
     *
     * @return a byte array representing the exported graph
     * @throws IOException if an error occurs during writing or exporting the graph
     */
    public byte[] asByteArray() throws IOException {
        ByteArrayOutputStream report = new ByteArrayOutputStream();

        try(Writer writer = new OutputStreamWriter(report)) {
            exportGraph(graph, writer);
            LOGGER.info("Graph generated");
        }

        return report.toByteArray();
    }

    /**
     * Converts a given graph into a byte array representation.
     *
     * @param graph the graph to be converted; it must contain nodes extending {@code ColorNode}
     *              and edges extending {@code DefaultEdge}
     * @return a byte array representing the serialized form of the graph
     * @throws IOException if an I/O error occurs during graph serialization
     */
    public static byte[] GraphToByte(Graph<? extends ColorNode, ? extends DefaultEdge> graph) throws IOException {
        return (new Export.Builder<>(graph)).Build().asByteArray();
    }

    /**
     * Converts a given graph with String vertices to a byte array representation.
     * The provided graph is transformed such that its vertices are mapped to {@code ColorNode} instances,
     * preserving the edges, and then exported as a byte array.
     *
     * @param graph the graph with String vertices to be converted
     * @return a byte array representation of the transformed graph
     * @throws IOException if an error occurs during the conversion process
     */
    public static byte[] StringGraphToByte(Graph<String, DefaultEdge> graph) throws IOException {
        Graph<ColorNode, DefaultEdge> newGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (DefaultEdge e : graph.edgeSet()) {
            ColorNode v1 = (new ColorNode.Builder( graph.getEdgeSource(e) )).Build();
            ColorNode v2 = (new ColorNode.Builder( graph.getEdgeTarget(e) )).Build();
            newGraph.addVertex(v1);
            newGraph.addVertex(v2);
            newGraph.addEdge(v1, v2);
        }

        return (new Export.Builder<>(newGraph)).Build().asByteArray();
    }


    /**
     * A builder class for customizing the configuration of exporting a graph structure.
     * It provides options to define graph-level attributes, vertex attributes, vertex ID provider,
     * and edge attributes.
     *
     * @param <V> the vertex type, extending {@code ColorNode}
     * @param <E> the edge type, extending {@code DefaultEdge}
     */
    @SuppressWarnings("unused")
    public static class Builder<V extends ColorNode, E extends DefaultEdge> {
        private final Graph<V, E> graph;

        private Supplier<Map<String, Attribute>> graphAttributes = () ->
                Map.of(
                        "ranksep", DefaultAttribute.createAttribute(1.5),
                        "rankdir", DefaultAttribute.createAttribute("LR")
                );

        private Function<V, String> vertexIdProvider = (v) -> dotFormat(v.label);

        private Function<V, Map<String, Attribute>> vertexAttributes = (v) ->
                Map.of(
                        "label", DefaultAttribute.createAttribute(vertexIdProvider.apply(v)),
                        "style", DefaultAttribute.createAttribute("filled"),
                        "fillcolor", DefaultAttribute.createAttribute(v.color)
                );

        private Function<E, Map<String, Attribute>> edgeAttributeProvider = (e) -> null;

        /**
         * Constructs a new builder for customizing the configuration of exporting a graph structure.
         *
         * @param graph the graph instance to be used in the builder
         */
        public Builder(Graph<V, E> graph) {
            this.graph = graph;
        }

        /**
         * Sets the supplier for graph-level attributes, allowing customization of specific properties
         * associated with the graph.
         *
         * @param graphAttributes a supplier function that provides a map of attributes, where the
         *                        key is the attribute name (as a string), and the value is an
         *                        {@code Attribute} object representing the corresponding attribute value
         * @return the {@code Builder} instance for method chaining
         */
        public Builder<V, E> graphAttributes(Supplier<Map<String, Attribute>> graphAttributes) {
            this.graphAttributes = graphAttributes;
            return this;
        }

        /**
         * Configures a function to provide custom attributes for each vertex in the graph.
         *
         * @param vertexAttributes a function that takes a vertex of type {@code V}
         *                         and returns a map of attributes, where the key
         *                         is the attribute name (as a string) and the value
         *                         is an {@code Attribute} object representing the
         *                         corresponding attribute value
         * @return the {@code Builder} instance for method chaining
         */
        public Builder<V, E> vertexAttributes(Function<V, Map<String, Attribute>> vertexAttributes) {
            this.vertexAttributes = vertexAttributes;
            return this;
        }

        /**
         * Configures a function to provide a custom identifier for each vertex in the graph.
         *
         * @param vertexIdProvider a function that takes a vertex of type {@code V} and returns a
         *                         {@code String} representing the unique identifier for that vertex
         * @return the {@code Builder} instance for method chaining
         */
        public Builder<V, E> vertexIdProvider(Function<V, String> vertexIdProvider) {
            this.vertexIdProvider = vertexIdProvider;
            return this;
        }

        /**
         * Configures a function to provide custom attributes for each edge in the graph.
         *
         * @param edgeAttributeProvider a function that takes an edge of type {@code E}
         *                              and returns a map of attributes, where the key
         *                              is the attribute name (as a string) and the value
         *                              is an {@code Attribute} object representing the
         *                              corresponding attribute value
         * @return the {@code Builder} instance for method chaining
         */
        public Builder<V, E> edgeAttributeProvider(Function<E, Map<String, Attribute>> edgeAttributeProvider) {
            this.edgeAttributeProvider = edgeAttributeProvider;
            return this;
        }

        /**
         * Builds and returns an instance of the {@code Export<V, E>} class based on the current
         * configuration of the {@code Builder}.
         *
         * @return an {@code Export<V, E>} instance initialized with the specified attributes
         * and customization options defined in the {@code Builder}
         */
        public Export<V, E> Build() {
            return new Export<>(this);
        }
    }
}
