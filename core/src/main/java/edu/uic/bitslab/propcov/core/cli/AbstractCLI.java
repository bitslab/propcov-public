package edu.uic.bitslab.propcov.core.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uic.bitslab.propcov.core.analysisframework.AnalysisFrameworkException;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.coverage.CoverageException;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * The AbstractCLI class serves as a base class for creating command-line interface applications
 * that implement specific validation and processing logic. It defines an abstract structure for
 * validating arguments and processing operations, allowing for customization in subclasses.
 */
abstract public class AbstractCLI {
    abstract protected void validate(String[] args);
    abstract protected void process(String[] args) throws TimerException, BuildSystemException, IOException, ExternalProcessException, SourceException, InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, AnalysisFrameworkException, CoverageException, ConfigException;

    /**
     * Parses a JSON string into a map of string keys and string values.
     * This method ensures that all keys and values in the JSON are of type {@code String}.
     *
     * @param json the JSON string to be parsed
     * @return a {@code Map<String, String>} containing the parsed key-value pairs
     * @throws JsonProcessingException if the JSON string cannot be parsed
     * @throws UnsupportedOperationException if the JSON contains keys or values that are not strings
     */
    public static Map<String, String> parseArgs(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> args = mapper.readValue(json, Map.class);

        // Note: JSON requires string for the key portion of an object. Mapper ensures we have string values as keys

        if (!args.values().stream().allMatch(v -> v instanceof String)) throw new UnsupportedOperationException("Json values must all be string type.");

        //noinspection unchecked,CastCanBeRemovedNarrowingVariableType
        return (Map<String, String>) args;
    }
}
