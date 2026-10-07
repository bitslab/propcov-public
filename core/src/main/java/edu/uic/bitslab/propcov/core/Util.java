package edu.uic.bitslab.propcov.core;

import edu.uic.bitslab.propcov.Run;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uic.bitslab.propcov.core.Props.resolveStringList;
import static edu.uic.bitslab.propcov.core.config.Config.WorkflowItem;
import static edu.uic.bitslab.propcov.core.config.Config.WorkflowShortName;

/**
 * Utility class containing various helper methods and constants for operations such as logging, file manipulations,
 * regular expressions, and process handling.
 */
public class Util {

    /**
     * The package name of the main class used by the application.
     * This is determined by retrieving the package name of the {@code Run} class.
     * It is declared as a constant for global use across the application.
     */
    public static final String mainPackageName = Run.class.getPackageName();
    private static final Logger LOGGER = getLogger(Util.class);

    /**
     * A compiled regular expression pattern used to parse and extract components of a
     * fully qualified method signature string. The expected string format includes
     * the class name, an optional inner class, the method name, and the method
     * descriptor (parameters and return type).
     * <p>
     * The named capturing groups within the pattern are:
     * - "clazz": Represents the class name, excluding inner classes and method details.
     * - "inner": Represents the inner class name of the containing class, if any.
     * - "method": Represents the method name being referenced.
     * - "descriptor": Represents the method signature, including parameter types and
     *   return type.
     * <p>
     * This pattern is useful for extracting or validating information about methods
     * in Java bytecode or similar contexts where method signatures follow a structured
     * format.
     */
    public static final Pattern labelToParts = Pattern.compile("^(?<clazz>[^()$]+)(\\$(?<inner>[^()]+))?\\.(?<method>[^()]+)(?<descriptor>\\([^()]*\\).*)$");

    /**
     * Retrieves a logger instance associated with the given class.
     *
     * @param clazz the class for which the logger is to be retrieved
     * @return the logger instance for the specified class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(truncateLogClass(clazz));
    }

    /**
     * Retrieves a logger instance for the specified name.
     *
     * @param name the name associated with the logger instance
     * @return the logger instance for the specified name
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Constructs the fully qualified class name, including inner class identifiers if present.
     *
     * @param m a Matcher instance where the named groups "clazz" and optionally "inner" are expected.
     *          The group "clazz" represents the outer class name,
     *          and "inner" represents the inner class name if it exists.
     * @return the full class name, including the inner class, formed by concatenating the "clazz" group
     *         with the "inner" group using a "$" as a delimiter, or just the "clazz" if "inner" is null.
     */
    public static String fullClass(Matcher m) {
        String inner = m.group("inner");
        if (inner == null) return m.group("clazz");
        return m.group("clazz") + "$" + m.group("inner");
    }

    /**
     * Recursively removes a directory and all of its contents. Files and subdirectories
     * within the specified directory are deleted in reverse order to ensure proper removal.
     * Any encountered exceptions during deletion are logged.
     *
     * @param localDirectory the root path of the directory to remove. If the directory
     *                        does not exist, the method exits without performing any action.
     * @throws IOException if an I/O error occurs while accessing the file system.
     */
    public static void recursiveRemove(Path localDirectory) throws IOException {
        // doesn't exist, so nothing to remove
        if (!Files.exists(localDirectory)) return;

        try (Stream<Path> paths = Files.walk(localDirectory)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.warn("IOException {} when deleting {}", e.getMessage(), path);
                        }
                    });
        }

        if (Files.exists(localDirectory)) LOGGER.warn("Unable to remove directory.");
    }


    private static String tunnelInnerClassHelper(Class<?> clazz, ArrayList<String> accumulate) {
        if (clazz.isMemberClass() ) {
            accumulate.add(clazz.getSimpleName());
            return tunnelInnerClassHelper(clazz.getEnclosingClass(), accumulate);
        }
        accumulate.add(clazz.getSimpleName());
        Collections.reverse(accumulate);
        return String.join("$", accumulate).concat(".class");
    }

    /**
     * Constructs the name of an inner class or anonymous class as a `.class` file representation.
     * If the provided class is an anonymous class, the method returns its class file name.
     * Otherwise, it constructs and returns the class file name for member or inner classes
     * by concatenating their outer class names and inner class names using `$` as a delimiter.
     *
     * @param clazz the class object representing the inner class, member class, or anonymous class
     * @return the `.class` file name of the specified class
     */
    public static String tunnelInnerClass(Class<?> clazz) {
        if (clazz.isAnonymousClass()) {
            //String encClazz = clazz.getEnclosingClass().getSimpleName();
            String fullName = clazz.getName();
            int dot = fullName.lastIndexOf('.');
            return fullName.substring(dot+1) + ".class";
        } else {
            return tunnelInnerClassHelper(clazz, new ArrayList<>());
        }

    }

    /**
     * Recursively copies files and directories from the source path to the destination path.
     * If basePath is specified, the destination path is constructed relative to it.
     * This method creates any necessary directories in the destination path and overwrites existing files.
     * If the source does not exist, the method exits without performing any action.
     * Any encountered exceptions during copying are logged.
     *
     * @param source the root path of the source directory or file to copy. Must not be null.
     * @param destination the target path where the source should be copied. Must not be null.
     */
    public static void recursiveCopy(Path source, Path destination) {
        // doesn't exist, so nothing to copy
        if (!Files.exists(source)) return;

        // is source a file?
        if (Files.isRegularFile(source)) {
            try {
                Files.createDirectories(destination);
                Files.copy(source, destination.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.warn("{} {} when copying {} to {}", e.getClass().getName(), e.getMessage(), source, destination);
            }
            return;
        }

        try (Stream<Path> relativeSourcePaths = Files.walk(source)) {
            relativeSourcePaths
                .forEach(fullSourcePath -> {
                    Path relativeSourcePath = source.relativize(fullSourcePath);
                    Path relativeDestinationPath = destination.resolve(relativeSourcePath);

                    try {
                        if (fullSourcePath.toFile().isDirectory()) {
                            Files.createDirectories(relativeDestinationPath);

                        } else {
                            Files.createDirectories(relativeDestinationPath.getParent());
                            Files.copy(fullSourcePath, relativeDestinationPath, StandardCopyOption.REPLACE_EXISTING);

                        }
                    } catch (IOException e) {
                        LOGGER.warn("{} {} when copying {} to {}", e.getClass().getName(), e.getMessage(), source, destination);
                    }
                });
        } catch (IOException e) {
            LOGGER.warn("{} {} when copying {} to {}", e.getClass().getName(), e.getMessage(), source, destination);
        }
    }

    /**
     * Converts a DOT file to the specified format using an external DOT process.
     *
     * @param type the target type to which the DOT file needs to be converted
     * @param dotFile the DOT file to be converted
     * @param dotTimeoutInSec the maximum time in seconds to allow the conversion process to run
     * @return the content of the converted file as a byte array
     * @throws IOException if an I/O error occurs during file processing
     * @throws TimerException if a timeout occurs during the conversion process
     * @throws ExternalProcessException if the external DOT process execution fails
     * @throws InterruptedException if the thread executing the process is interrupted
     */
    public static byte[] dotTo(DotConvertType type, File dotFile, long dotTimeoutInSec) throws IOException, TimerException, ExternalProcessException, InterruptedException {
        Path tempFile = Files.createTempFile("dot_", type.name() + ".tmp");

        try {
            String dotConvert = String.format("dot -T%s \"%s\" -o \"%s\"", type.cmd, dotFile.getAbsolutePath(), tempFile.toAbsolutePath());
            ExternalProcess.Result result = (new ExternalProcess(dotTimeoutInSec)).command(dotConvert).run();
            if (result.exitCode > 0) {
                LOGGER.error("DOT Convert Command Failed.  Exit Code {}", result.exitCode);
            }

            return Files.readAllBytes(tempFile);
        } finally {
            if (tempFile.toFile().delete()) {
                LOGGER.info("Deleted temp file {}", tempFile.toAbsolutePath());
            } else {
                LOGGER.warn("Unable to delete temp file {}", tempFile.toAbsolutePath());
            }
        }
    }

    /**
     * Retrieves the entry point node from the given graph. An entry point node is
     * identified by its `isEntryPoint` attribute being set to true. This method
     * ensures there is exactly one entry point. If no entry point or multiple
     * entry points are found, a {@link NoSuchElementException} is thrown.
     *
     * @param g the graph from which the entry point node is to be retrieved
     * @return the entry point node if exactly one such node is found
     * @throws NoSuchElementException if no entry point is found or multiple entry points exist
     */
    public static ColorNode getEntryPoint(Graph<ColorNode, DefaultEdge> g) throws NoSuchElementException {
        ColorNode[] possibleEntryPoints = g.vertexSet().stream()
                .filter(v -> v.isEntryPoint)
                .toArray(ColorNode[]::new);

        if (possibleEntryPoints.length == 1) return possibleEntryPoints[0];

        if (possibleEntryPoints.length == 0) throw new NoSuchElementException("No entry point found.");

        throw new NoSuchElementException("Multiple entry points found.");
    }

    /**
     * The {@code DotConvertType} enum represents different file or output formats supported
     * by dot graph visualization tools. Each enum constant corresponds to a specific
     * format and has an associated command string.
     */
    public enum DotConvertType {
        /**
         * Defines an enum constant representing the PostScript file format.
         * The associated command string for this file type is "ps".
         */
        PostScript("ps"),
        /**
         * Defines an enum constant representing the PDF file format.
         * The associated command string for this file type is "pdf".
         */
        PDF("pdf"),
        /**
         * Defines an enum constant representing the SVG file format.
         * The associated command string for this file type is "svg".
         */
        SVG("svg"),
        /**
         * Defines an enum constant representing the PNG image file format.
         * The associated command string for this file type is "png".
         */
        PNG("png");

        /**
         * Represents the command string associated with the DotConvertType enum constants.
         * Each constant is mapped to a specific command line or file type representation,
         * such as "ps" for PostScript or "pdf" for PDF.
         */
        public final String cmd;

        DotConvertType(String cmd) {
            this.cmd = cmd;
        }
    }

    private static String truncateLogClass(Class<?> clazz) {
        String className = clazz.getName();
        String commonPart = getCommonClassName(className, mainPackageName);
        return className.substring(commonPart.length() + 1);
    }

    /**
     * Determines the longest common class name or package name shared between two fully qualified
     * class names. If the two names do not share any common prefix or if one of the parameters is null,
     * the method returns an empty string.
     *
     * @param a the first fully qualified class name; must not be null
     * @param b the second fully qualified class name; if null, the method returns the value of {@code a}
     * @return the longest common class name or package name shared between {@code a} and {@code b}.
     * If they do not share any common prefix, an empty string is returned.
     */
    public static String getCommonClassName(String a, String b) {
        Objects.requireNonNull(a, "parameter a cannot be null");
        if (b == null || a.equals(b)) return a;

        String[] aPart = a.split("\\.");
        String[] bPart = b.split("\\.");

        int maxIndex = Integer.min(aPart.length, bPart.length);
        StringBuilder similar = new StringBuilder();

        if (aPart[0].equals(bPart[0])) {
            similar.append(aPart[0]);

            for (@SuppressWarnings("ReassignedVariable") int i = 1; i < maxIndex; i++) {
                if (aPart[i].equals(bPart[i])) {
                    similar.append(".").append(aPart[i]);
                } else {
                    break;
                }
            }
        }

        return similar.toString();
    }

    /**
     * Retrieves the set of initial workflow steps to be executed. If no specific properties
     * are defined, all available workflow items from the {@link WorkflowItem} enum are returned.
     * Otherwise, it resolves the property string, maps it to the corresponding workflow items,
     * and returns a set of those items. This process includes handling shorthand workflow names
     * defined in the {@link WorkflowShortName} enum.
     *
     * @return a set of {@link WorkflowItem} representing the starting workflow steps.
     * If the workflow property is not defined, then return the default set containing all
     * workflow items. If the property is defined, then return the resolved set of workflow items.
     */
    public static Set<WorkflowItem> getStartingWorkflow() {
        Collection<String> workflow = resolveStringList("PropCov.Workflow", null, ",");
        if (workflow == null) return new HashSet<>(Set.of(WorkflowItem.values()));

        return workflow.stream()
                // map workflow using a short name/regular name
                .map((o) -> {
                    try {
                        return WorkflowShortName.valueOf(o).workflowItems;
                    } catch (IllegalArgumentException | NullPointerException exception) {
                        return new WorkflowItem[]{WorkflowItem.valueOf(o)};
                    }
                })

                // flatten the inner array to one array
                .flatMap(Stream::of)
                .collect(Collectors.toSet());
    }

    private static final Map<String, List<PropertyTest>> cacheGetEntryPoints = new HashMap<>();

    /**
     * Retrieves a list of entry points from a specified JAR file, using the specified test framework
     * and analysis framework to analyze the class files within the JAR. The entry points are represented
     * as instances of {@code PropertyTest}, containing the name and entry point properties.
     *
     * @param jarFileObj the JAR file to be analyzed; must not be null
     * @param testFramework the test framework used to determine test properties from methods; must not be null
     * @param analysisFramework the analysis framework used to evaluate properties from class files; must not be null
     * @return a list of {@code PropertyTest} instances representing the detected entry points
     * @throws IOException if an I/O error occurs while reading the JAR file
     */
    public static List<PropertyTest> getEntryPoints(File jarFileObj, AbstractTestFramework testFramework, AbstractAnalysisFramework analysisFramework) throws IOException {
        String jarFileName = jarFileObj.getAbsolutePath();

        if (cacheGetEntryPoints.containsKey(jarFileName)) return cacheGetEntryPoints.get(jarFileName);

        try (JarFile jarFile = new JarFile(jarFileObj)) {
            cacheGetEntryPoints.put(jarFileName,
                    jarFile.stream()
                            .filter(Objects::nonNull)
                            .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                            .map(jarEntry -> {
                                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                                    return analysisFramework.GetProperties(inputStream, testFramework)
                                            .stream()
                                            .map(m -> new PropertyTest(m._1, m._2))
                                            .collect(Collectors.toList());
                                } catch (IOException ioException) {
                                    throw new RuntimeException(ioException);
                                }
                            })
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
            );
        }

        return cacheGetEntryPoints.get(jarFileName);
    }

    private static Class<?> cl(Class<?> clazz, int dim) {
        if (dim == 0)
            return clazz;

        if (dim == 1)
            return Array.newInstance(clazz, dim).getClass();

        return Array.newInstance(cl(clazz, dim-1)).getClass();
    }

    /**
     * Converts a method descriptor string to an array of property types represented as {@code Class<?>} instances.
     * This method parses the descriptor string and determines the corresponding classes for each type
     * described in the method's signature.
     *
     * @param description the method descriptor string to be parsed. It should follow the standard format
     *                    for method descriptors used in the JVM. For example, "(Ljava/lang/String;I)V".
     * @return an array of {@code Class<?>} objects representing the parameter types described in the method descriptor.
     * @throws ClassNotFoundException if any of the classes described in the descriptor cannot be found.
     */
    public static Class<?>[] descriptionToPropertyTypes(String description) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();

        if (description.charAt(0) != '(') throw new Error("Invalid description provided.");
        int dim = 0;

        loop: for(int i = 1; i < description.length(); i++) {
            switch(description.charAt(i)) {
                case 'B': classes.add(cl(Byte.TYPE, dim)); dim = 0; break;
                case 'C': classes.add(cl(Character.TYPE, dim)); dim = 0; break;
                case 'D': classes.add(cl(Double.TYPE, dim)); dim = 0; break;
                case 'F': classes.add(cl(Float.TYPE, dim)); dim = 0; break;
                case 'I': classes.add(cl(Integer.TYPE, dim)); dim = 0; break;
                case 'J': classes.add(cl(Long.TYPE, dim)); dim = 0; break;
                case 'S': classes.add(cl(Short.TYPE, dim)); dim = 0; break;
                case 'Z': classes.add(cl(Boolean.TYPE, dim)); dim = 0; break;

                case 'L':
                    i++;

                    int semi =
                        (description.substring(i).startsWith(AbstractAnalysisFramework.PARAM_NOT_FOUND) && !description.substring(i).startsWith(AbstractAnalysisFramework.PARAM_NOT_FOUND + ";"))
                        ? (i + AbstractAnalysisFramework.PARAM_NOT_FOUND.length())
                        : description.indexOf(';', i);
                    String objectName = description.substring(i, semi);
                    classes.add(cl(Class.forName(objectName.replace('/', '.'), false, SUTClassLoader.get()), dim));
                    i = semi;
                    dim = 0;
                    break;

                case '[':
                    dim++;
                    break;

                case 'V':
                    throw new Error("Saw void when not in return");

                case ')':
                    break loop;

                default:
                    return null;
            }
        }

        return classes.toArray(Class[]::new);
    }


    /**
     * Resolves a method or constructor from the provided class based on its name and parameter types.
     * This method attempts to locate the method in the following order:
     * 1. Public methods inherited or declared in the class.
     * 2. Declared methods are explicitly defined in the class, excluding inherited ones.
     * 3. Public constructors are inherited or declared in the class.
     * 4. Declared constructors are explicitly defined in the class, excluding inherited ones.
     *
     * @param clazz the class to search for the method or constructor; must not be null
     * @param methodName the name of the method to resolve; if resolving a constructor, this should be null
     * @param methodParams a parameter type array of the method or constructor to resolve; must not be null
     * @return the resolved method or constructor as an {@code Executable} instance
     * @throws NoSuchMethodException if no matching method or constructor is found
     */
    public static Executable resolveMethod(Class<?> clazz, String methodName, Class<?>[] methodParams) throws NoSuchMethodException {
        // public methods including inheritance
        try { return clazz.getMethod(methodName, methodParams); } catch (NoSuchMethodException ignored) {}

        // declared methods (all methods without inheritance)
        try { return clazz.getDeclaredMethod(methodName, methodParams); } catch (NoSuchMethodException ignored) {}

        // public constructor including inheritance
        try { return clazz.getConstructor(methodParams); } catch (NoSuchMethodException ignored) {}

        // declared constructor (all without inheritance)
        return clazz.getDeclaredConstructor(methodParams);
    }

    /**
     * Updates the fields of the original object with values from the override object.
     * If a field in the override object is not null, the corresponding field in
     * the original object is updated with its value.
     *
     * @param <T> the type of objects being processed
     * @param original the object whose fields are to be updated; must not be null
     * @param override the object containing the overriding values; if null, no action is performed
     * @throws IllegalAccessException if the field in the original object cannot be accessed or updated
     */
    public static <T> void setOverrideConfig(T original, T override) throws IllegalAccessException {
        if (override == null) return;

        for (Field declaredField : original.getClass().getDeclaredFields()) {
            Object overValue = declaredField.get(override);
            if (overValue != null) declaredField.set(original, overValue);
        }
    }

    /**
     * Computes the SHA-1 hash of a given input byte array and returns its hexadecimal representation.
     *
     * @param in the input byte array to hash; must not be null
     * @return the hexadecimal representation of the SHA-1 hash as a String
     * @throws RuntimeException if the SHA-1 algorithm is not available
     */
    public static String sha1(byte[] in) {
        try {
            StringBuilder hex = new StringBuilder();

            for (byte b : MessageDigest.getInstance("SHA-1").digest(in)) {
                hex.append(String.format("%02X", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a method or constructor from the specified class based on its name and method descriptor.
     * This function attempts to find the method or constructor recursively from the class and its superclasses.
     * If the specified method name is {@literal <init>}, it will search for a constructor.
     *
     * @param clazz       The class from which the method or constructor is to be retrieved.
     * @param method      The name of the method or {@literal <init>} if searching for a constructor.
     * @param descriptor  A string descriptor specifying the parameter types of the method or constructor.
     * @return The matching {@link Executable} representing the method or constructor if found,
     *         otherwise null if the method or constructor could not be found or an error occurred.
     */
    public static Executable methodFrom(Class<?> clazz, String method, String descriptor) {

        if (clazz == null || method == null || descriptor == null) return null;

        try {
            if (method.equals("<init>"))
                return clazz.getDeclaredConstructor(Util.descriptionToPropertyTypes(descriptor));
            else
                return clazz.getDeclaredMethod(method, Util.descriptionToPropertyTypes(descriptor));
        } catch (ReflectiveOperationException exception) {
            return methodFrom(clazz.getSuperclass(), method, descriptor);
        } catch (NoClassDefFoundError | IllegalArgumentException e) {
            LOGGER.warn("Error getting method {} {} for {}", method, descriptor, clazz.getName());
            return null;
        }
    }
}