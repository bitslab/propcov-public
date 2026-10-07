package edu.uic.bitslab.propcov.core.graph;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.net.URLClassLoader;

/**
 * The ColorNode class represents a node in a graph structure that includes information
 * about color, coverage, and other metadata. This class provides serialization support
 * and overrides equals and toString for specific behavior.
 * ColorNode is used for graph-based coverage analysis, storing detailed coverage data
 * (e.g., lines and branches covered/missed) and additional metadata about the node's
 * role within the graph (e.g., entry points, coverage type).
 */
public class ColorNode extends AbstractNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 202410140826L;

    /**
     * Represents the label of the node that typically has the method name.
     */
    public final String label;
    /**
     * Represents the color attribute of a node in the graph.
     */
    public final String color;
    /**
     * Indicates whether this particular instance of the node is excluded from
     * for the analysis.
     */
    public final boolean excluded;
    /**
     * Represents whether the current node has been covered or reached during a coverage analysis.
     * This field is intended to indicate whether the node is effectively included in the
     * coverage computation, such as for code coverage or graph traversal purposes.
     */
    public final boolean covered;
    /**
     * Indicates whether the node serves as an entry point in the graph.
     */
    public final boolean isEntryPoint;
    /**
     * Indicates an unknown coverage state specific to other coverage analysis for this node.
     */
    public final boolean unknownOther;
    /**
     * Represents the type associated with this ColorNode object.
     */
    public final String type;

    /**
     * Represents the coverage details associated with a specific node in the graph, encapsulating
     * other coverage-related data such as lines of code, branches, and methods covered or missed.
     * This field is immutable and helps determine code coverage metrics for the associated graph node.
     */
    public final CoverageDetail otherCoverageDetail;
    /**
     * Represents detailed coverage information for the associated node in the graph.
     * This object stores data such as lines, branches, and methods covered or missed,
     * and includes maps for tracking specific line information per source file.
     */
    public final CoverageDetail propCovCoverageDetail;

    /**
     * Represents the number of lines of code that are covered during testing or analysis.
     * This field is typically used to track the amount of code that has been executed
     * as part of a code coverage metric, providing insight into the effectiveness of test cases.
     */
    public final long linesCovered;
    /**
     * The `linesMissed` field represents the number of lines of code that have not been covered during testing.
     * This value is used in code coverage analysis to identify untested lines in the source code.
     */
    public final long linesMissed;
    /**
     * Represents the number of branches that have been covered in a graph or
     * code coverage measurement context. This field is typically used to track
     * and report the execution paths within the graph or code that have been
     * traversed or tested successfully.
     */
    public final long branchesCovered;
    /**
     * The branchesMissed field represents the number of branches missed during a code coverage analysis.
     * This value indicates the branches of the code that were not executed during testing.
     */
    public final long branchesMissed;
    /**
     * A reference to the runtime {@code Class} representation for the associated object.
     * This field holds metadata about the class type of the respective {@code ColorNode} instance.
     * It is typically used for reflection or to define specialized behavior based on the class type.
     */
    public final Class<?> clazz;
    /**
     * Represents the associated executable method for the {@code ColorNode}.
     * This variable holds a reference to an underlying {@link Executable},
     * which can represent a method or constructor within a class.
     */
    public final Executable method;

    @Serial
    private void writeObject(java.io.ObjectOutputStream stream) throws Exception {
        stream.writeObject(label);
        stream.writeObject(color);
        stream.writeBoolean(excluded);
        stream.writeBoolean(covered);
        stream.writeBoolean(isEntryPoint);
        stream.writeObject(type);
        stream.writeLong(linesCovered);
        stream.writeLong(linesMissed);
        stream.writeLong(branchesCovered);
        stream.writeLong(branchesMissed);

        stream.writeObject(clazz == null ? "" : clazz.getName());
        stream.writeObject(method == null ? "" : method.getName());

        // parameter types
        stream.writeObject(method == null ? new Class<?>[]{} : method.getParameterTypes());

        stream.writeBoolean(unknownOther);

        stream.writeObject(otherCoverageDetail);
        stream.writeObject(propCovCoverageDetail);
    }

    private void setFinal(String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field id = this.getClass().getDeclaredField(name);
        id.setAccessible(true);
        id.set(this, value);
        id.setAccessible(false);
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        setFinal("label", stream.readObject());
        setFinal("color", stream.readObject());
        setFinal("excluded", stream.readBoolean());
        setFinal("covered", stream.readBoolean());
        setFinal("isEntryPoint", stream.readBoolean());
        setFinal("type", stream.readObject());
        setFinal("linesCovered", stream.readLong());
        setFinal("linesMissed", stream.readLong());
        setFinal("branchesCovered", stream.readLong());
        setFinal("branchesMissed", stream.readLong());

        // get sutClassLoader
        URLClassLoader sutClassLoader = SUTClassLoader.get();

        // set clazz
        String clazzName = (String) stream.readObject();
        setFinal("clazz", clazzName.isEmpty() ? null : sutClassLoader.loadClass(clazzName));

        // set method
        String methodName = (String) stream.readObject();
        
        // method parameter class load
        if (methodName.isEmpty()) {
            stream.readObject();
        } else {
            // get method params
            Class<?>[] methodParams = (Class<?>[]) stream.readObject();

            // set method
            setFinal("method", Util.resolveMethod(clazz, methodName, methodParams));
        }

        setFinal("unknownOther", stream.readBoolean());

        setFinal("otherCoverageDetail", stream.readObject());
        setFinal("propCovCoverageDetail", stream.readObject());
    }

    private ColorNode(Builder builder) {
        this.label = builder.label;
        this.color = builder.color;
        this.excluded = builder.excluded;
        this.covered = builder.covered;
        this.isEntryPoint = builder.isEntryPoint;
        this.type = builder.type;
        this.linesCovered = builder.linesCovered;
        this.linesMissed = builder.linesMissed;
        this.branchesCovered = builder.branchesCovered;
        this.branchesMissed = builder.branchesMissed;
        this.clazz = builder.clazz;
        this.method = builder.method;
        this.unknownOther = builder.unknownOther;
        this.otherCoverageDetail = builder.otherCoverageDetail;
        this.propCovCoverageDetail = builder.propCovCoverageDetail;
    }

    /**
     * Determines if the node has any coverage by checking its type against specific coverage types.
     *
     * @return true if the node's type matches any of the defined coverage types, false otherwise
     */
    public boolean hasSomeCoverage() {
        return (
            type.equals(Config.NodeType.COVERAGE1.name()) ||
            type.equals(Config.NodeType.COVERAGE2.name()) ||
            type.equals(Config.NodeType.COVERAGE3.name()) ||
            type.equals(Config.NodeType.COVERAGE4.name())
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ColorNode)) return false;
        ColorNode node = (ColorNode) obj;
        return node.label.equals(this.label);
    }

    public String toString() {
        return label;
    }


    /**
     * The Builder class provides a mechanism to construct instances of ColorNode
     * with various configurations. It uses the builder design pattern to allow
     * for flexible and readable object creation by chaining method calls.
     */
    static public class Builder {
        private final String label;
        private Class<?> clazz;
        private Executable method;
        private String color = "white";
        private boolean excluded = false;
        private boolean covered = false;
        private boolean isEntryPoint = false;
        private boolean unknownOther = false;
        private String type;
        private long linesCovered = 0;
        private long linesMissed = 0;
        private long branchesCovered = 0;
        private long branchesMissed = 0;
        private CoverageDetail otherCoverageDetail;
        private CoverageDetail propCovCoverageDetail;

        /**
         * Constructs a new Builder instance with the specified label.
         *
         * @param label the label to be associated with the Builder instance
         */
        public Builder(String label) {
            this.label = label;
        }

        /**
         * Constructs a new Builder instance with the specified parameters.
         *
         * @param label the label to be associated with the Builder instance
         * @param clazz the Class object representing the type of the associated entity
         * @param method the Executable object representing the method to be associated with the Builder
         */
        public Builder(String label, Class<?> clazz, Executable method) {
            this.label = label;
            this.clazz = clazz;
            this.method = method;
        }

        /**
         * Constructs a new Builder object with the specified parameters.
         *
         * @param label The label associated with this builder.
         * @param clazz The class object representing the type associated with this builder.
         * @param method The executable method associated with this builder.
         * @param other The coverage details as reported by the other coverage system, or null if unavailable.
         * @param propCov The property-specific coverage details. Cannot be null unless the node is an entry point, a test method, or an unknown class.
         * @param isEntryPoint Indicates whether this builder represents an entry point.
         * @param isEntryPointOrTest Indicates whether this builder represents either an entry point or a test method.
         * @throws IllegalArgumentException If the propCov parameter is null and the node is not an entry point, test method, or unknown class.
         */
        public Builder(String label, Class<?> clazz, Executable method, CoverageDetail other, CoverageDetail propCov, boolean isEntryPoint, boolean isEntryPointOrTest) {
            this(label, clazz, method);

            if (propCov == null) {
                if (isEntryPointOrTest || label.contains(AbstractAnalysisFramework.PARAM_NOT_FOUND) || label.startsWith("ClassNotFound(")) {
                    covered(false);
                    linesCovered(0);
                    linesMissed(0);
                    branchesCovered(0);
                    branchesMissed(0);
                    this.unknownOther = false;
                } else {
                    throw new IllegalArgumentException("The property 'propCovCoverageDetail' cannot be null when node is not an entry point, test method, or unknown class.");
                }
            } else {
                covered(propCov.covered);
                linesCovered(propCov.linesCovered);
                linesMissed(propCov.linesMissed);
                branchesCovered(propCov.branchesCovered);
                branchesMissed(propCov.branchesMissed);
                this.unknownOther = propCov.otherUnknownMethod;
            }

            this.otherCoverageDetail = other;
            this.propCovCoverageDetail = propCov;
            isEntryPoint(isEntryPoint);
        }

        /**
         * Constructs a new Builder instance by copying the attributes from the specified ColorNode object.
         *
         * @param colorNode the ColorNode instance from which to copy attributes
         */
        public Builder(ColorNode colorNode) {
            this.label = colorNode.label;
            this.color = colorNode.color;
            this.excluded = colorNode.excluded;
            this.covered = colorNode.covered;
            this.isEntryPoint = colorNode.isEntryPoint;
            this.type = colorNode.type;
            this.linesCovered = colorNode.linesCovered;
            this.linesMissed = colorNode.linesMissed;
            this.branchesCovered = colorNode.branchesCovered;
            this.branchesMissed = colorNode.branchesMissed;
            this.clazz = colorNode.clazz;
            this.method = colorNode.method;
            this.unknownOther = colorNode.unknownOther;
        }

        /**
         * Sets the color attribute for the builder and returns the builder instance.
         *
         * @param color the color value to be set
         * @return the current instance of Builder for method chaining
         */
        public Builder color(String color) {
            this.color = color;
            return this;
        }

        /**
         * Sets the type attribute for the builder and returns the builder instance.
         *
         * @param type the type value to be set
         * @return the current instance of Builder for method chaining
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the excluded state for the builder.
         *
         * @param excluded a boolean indicating whether the corresponding element should be excluded
         * @return the builder instance with the updated excluded state
         */
        public Builder excluded(boolean excluded) {
            this.excluded = excluded;
            return this;
        }

        /**
         * Sets the covered state for the builder and returns the updated builder instance.
         *
         * @param covered a boolean indicating whether the associated element is covered
         * @return the current instance of Builder with the updated covered state
         */
        public Builder covered(boolean covered) {
            this.covered = covered;
            return this;
        }

        /**
         * Sets whether this object is an entry point in the graph and returns the builder instance.
         *
         * @param isEntryPoint a boolean indicating if this object should be marked as an entry point
         * @return the current instance of the Builder for method chaining
         */
        public Builder isEntryPoint(boolean isEntryPoint) {
            this.isEntryPoint = isEntryPoint;
            return this;
        }

        /**
         * Sets the number of lines covered by a specific entity and updates the builder state.
         *
         * @param linesCovered the number of lines that have been covered
         * @return the updated Builder instance
         */
        public Builder linesCovered(long linesCovered) {
            this.linesCovered = linesCovered;
            return this;
        }

        /**
         * Sets the number of lines missed during coverage analysis.
         *
         * @param linesMissed the count of missed lines
         * @return the Builder instance for method chaining
         */
        public Builder linesMissed(long linesMissed) {
            this.linesMissed = linesMissed;
            return this;
        }

        /**
         * Sets the number of branches covered during coverage analysis and updates the builder state.
         *
         * @param branchesCovered the count of branches that have been covered
         * @return the updated Builder instance for method chaining
         */
        public Builder branchesCovered(long branchesCovered) {
            this.branchesCovered = branchesCovered;
            return this;
        }

        /**
         * Sets the number of branches missed during coverage analysis and updates the builder state.
         *
         * @param branchesMissed the count of missed branches
         * @return the updated Builder instance for method chaining
         */
        public Builder branchesMissed(long branchesMissed) {
            this.branchesMissed = branchesMissed;
            return this;
        }

        /**
         * Builds and returns a new instance of {@code ColorNode} based on the current state of the {@code Builder}.
         *
         * @return a new {@code ColorNode} instance constructed using the state defined in this {@code Builder}
         */
        public ColorNode Build() {
            return new ColorNode(this);
        }
    }
}
