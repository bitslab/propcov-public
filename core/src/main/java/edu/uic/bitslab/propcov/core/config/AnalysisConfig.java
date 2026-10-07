package edu.uic.bitslab.propcov.core.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Represents the configuration to be used for analysis. This class is immutable
 * and provides a flexible way of setting up different configurations using its
 * nested {@code Builder} class.
 */
public class AnalysisConfig implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public AnalysisConfig clone() throws CloneNotSupportedException {
        return (AnalysisConfig) super.clone();
    }

    /**
     * Represents a set of flags that can be used within the context of the analysis configuration.
     * Each flag signifies a specific type of operation or condition relevant to the analysis process.
     */
    public enum AnalysisFlag {
        /**
         * Turns on EXCEPTION processing
         */
        EXCEPTION,
        /**
         * Turns on ITERATOR processing
         */
        ITERATOR,

        /**
         *
         */
        CFA,

        /**
         *
         */
        NO_LAMBDA
    }

    /**
     * The AnalysisType enum defines various types of program analysis strategies
     * commonly used in static analysis and optimization processes. Each constant
     * represents a specific analysis method with distinct characteristics and use cases.
     */
    public enum AnalysisType {
        /**
         * CHA (Class Hierarchy Analysis) is a type of analysis that operates
         * by examining the class hierarchy to determine the potential methods
         * or types that can be invoked or used at specific points in the code.
         * It primarily uses information available statically in the class structure
         * without requiring additional runtime details.
         */
        CHA,
        /**
         * Represents Rapid Type Analysis (RTA), a specific analysis strategy used
         * to determine the possible types of objects at runtime. It is designed
         * to be simpler and faster compared to more detailed analyses, with a focus
         * on scalability and speed by leveraging a static, class-hierarchy-based
         * approach.
         */
        RTA,
        /**
         * CTA stands for Context-sensitive Type Analysis.
         * It is an advanced analysis strategy that considers the context
         * of method invocations to determine more accurate type information.
         * This approach can provide precise results but may require
         * additional computational resources compared to simpler strategies.
         */
        CTA,
        /**
         * Represents the Field-based Type Analysis (FTA).
         * This analysis strategy focuses on determining the types of fields in a program
         * and propagating type information based on field assignments and usages.
         */
        FTA,
        /**
         * Represents the Modula Type Analysis (MTA) strategy.
         * This analysis type focuses on identifying modular type information
         * within a program to enhance type checking and optimization processes.
         */
        MTA,
        /**
         * Represents the XTA (Extended Type Analysis) strategy.
         * This type of analysis extends basic type analysis techniques by incorporating
         * additional context or structural information to provide more precise results.
         * It is typically used to analyze programs more comprehensively, capturing
         * patterns and dependencies that simpler analyses might overlook.
         */
        XTA,
        /**
         * Represents the CFA0 (Control Flow Analysis Level 0) analysis type.
         * This constant corresponds to a specific strategy within the control flow analysis framework.
         * It is used to analyze program control flow at the most basic level,
         * providing foundational insights for further, more detailed analysis strategies.
         */
        CFA0,
        /**
         * Represents the CFA1 (Control Flow Analysis Level 1) analysis type.
         * This constant corresponds to a specific strategy within the control flow analysis framework
         * used to determine information about the flow of control in a program.
         */
        CFA1,

        /**
         *
         */
        CFA10,

        /**
         *
         */
        CFA11
    }

    /**
     * A set of {@link AnalysisFlag} options that determine specific behaviors or features for the analysis.
     * These flags configure additional constraints or functionality applied to the analysis process.
     */
    public final Set<AnalysisFlag> analysisFlags;
    /**
     * Specifies the type of analysis to be performed.
     */
    public final AnalysisType analysisType;

    /**
     * Constructs an {@code AnalysisConfig} instance using the specified {@code Builder}.
     * This constructor is protected to enforce the use of the {@code Builder} for creating
     * instances of {@code AnalysisConfig}.
     *
     * @param builder the {@code Builder} instance containing the configuration details
     *                for the {@code AnalysisConfig}; must not be null
     */
    protected AnalysisConfig(Builder builder) {
        this.analysisFlags = builder.analysisFlags;
        this.analysisType = builder.analysisType;
    }

    /**
     * Builder class for constructing instances of {@code AnalysisConfig}.
     * This class uses the builder design pattern to allow step-by-step configuration
     * of an {@code AnalysisConfig} object. The builder provides methods to
     * configure various properties, including analysis types and flags,
     * for the resulting {@code AnalysisConfig} instance.
     */
    public static class Builder {
        private final Set<AnalysisFlag> analysisFlags = new HashSet<>();
        private AnalysisType analysisType = AnalysisType.RTA;

        /**
         * Constructs a new {@code Builder} instance for configuring and creating
         * {@code AnalysisConfig} objects. The builder allows for step-by-step
         * setting of configuration parameters such as analysis types and flags.
         */
        public Builder() {}

        /**
         * Sets the flags to be used for the analysis configuration.
         * This method replaces any previously set flags with the provided collection.
         * If the input collection is null, the existing flags will remain unchanged.
         *
         * @param analysisFlags the collection of {@code AnalysisFlag} instances to be used in the configuration.
         * @return the current {@code Builder} instance for method chaining.
         */
        public Builder analysisFlags(Collection<AnalysisFlag> analysisFlags) {
            if (analysisFlags == null) return this;
            this.analysisFlags.clear();
            this.analysisFlags.addAll(analysisFlags);
            return this;
        }

        /**
         * Sets the {@code AnalysisType} for the {@code AnalysisConfig} being built.
         * This method updates the analysis type if the provided value is not null.
         * If the input is null, the method does nothing and returns the current {@code Builder} instance.
         *
         * @param analysisType the {@code AnalysisType} to be set for the configuration.
         *                     This parameter represents the type of analysis strategy to be used.
         * @return the current {@code Builder} instance, allowing method chaining.
         */
        public Builder analysisType(AnalysisType analysisType) {
            if (analysisType == null) return this;
            this.analysisType = analysisType;
            return this;
        }

        /**
         * Builds and returns a new {@code AnalysisConfig} instance based on the current
         * state of the {@code Builder}. The returned {@code AnalysisConfig} object is
         * immutable and reflects all configurations made through the builder methods.
         *
         * @return a new {@code AnalysisConfig} instance initialized with the current builder settings
         */
        public AnalysisConfig build() {
            return new AnalysisConfig(this);
        }
    }
}