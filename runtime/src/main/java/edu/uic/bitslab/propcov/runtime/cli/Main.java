package edu.uic.bitslab.propcov.runtime.cli;

import edu.uic.bitslab.propcov.core.AbstractSetup;
import edu.uic.bitslab.propcov.core.Props;

import java.util.Arrays;
import java.util.Map;

import static edu.uic.bitslab.propcov.runtime.cli.Main.ACTION.*;

/**
 * The Main class serves as the entry point for executing various actions
 * related to application functionality, including configuration, manual run,
 * generating configuration files, and running reports. The class also handles
 * loading and initialization of extension setups.
 */
public class Main {

    static {
        AbstractSetup.load();
    }

    enum ACTION {
        runconfig,
        runmanual,
        genconfig
    }

    private static final Map<ACTION, String> HELP_MESSAGE = Map.of(
        runconfig,
            """
                     Run using YAML Config:
                      java -jar PATH/TO/PROVCOV.jar runconfig PROJECT_NAME
                    
                         PROJECT_NAME matches artifact/config folder
                    
                    
                    """,

        runmanual,
            """
                     Run using manual:
                      java \\\s
                       -DPropCov.LocalDirectory=path/to/sut \\\s
                       -DPropCov.TargetPath=target \\\s
                       -DPropCov.MainJar=main.jar \\\s
                       -DPropCov.TestJar=test.jar \\\s
                       -DPropCov.MainJarsWithDependencies=main-with-dep.jar \\\s
                       -DPropCov.TestPropertyEndpoint="com.project.TestMe.canRoundTrip(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V" \\\s
                       -jar PATH/TO/PROVCOV.jar \\\s
                       runmanual\\\s
                    
                    
                       Optional Values:
                         PropCov.Source.ExtensionClass
                         PropCov.BuildSystem.ExtensionClass
                         PropCov.Coverage.ExtensionClass
                         PropCov.TestFramework.ExtensionClass
                         PropCov.AnalysisFramework.ExtensionClass
                    
                    
                    """,

        genconfig,
            """
                     Generate Config from JAR to standard output:
                       java -jar PATH/TO/PROVCOV.jar genconfig path/to/test.jar
                    
                    
                     Generate Config from JAR to file:
                       java -jar PATH/TO/PROVCOV.jar genconfig path/to/test.jar path/to/new.yaml
                    
                    
                    """
    );

    private static void printExtensions() {
        System.out.println("Loaded Extensions/Properties:");
        System.out.println("=".repeat(60));
        Props.getLoadedExtensions().forEach(System.out::println);
        System.out.println("\n");

        Props.getProperties()
            .forEach(p -> System.out.printf("- %s\n\t%s\n\tDefault: %s\n", p.name, p.description, p.defaultValue));
    }

    /**
     * The entry point of the application. This method interprets the first argument to determine the action to
     * execute and delegates the further processing to the respective classes based on the action type.
     * If no valid actions are provided, it will display usage instructions.
     * Supported actions:
     * <ul>
     * <li>runconfig: Executes configuration-based processing.</li>
     * <li>runmanual: Executes manual configuration processing.</li>
     * <li>genconfig: Generates configuration files.</li>
     * <li>runreport: Generates reports based on processed data.</li>
     * </ul>
     *
     * In case of invalid arguments or if the required parameters for a specific action are not provided,
     * it prints an error message and displays usage instructions.
     *
     * @param args The command-line arguments. The first argument specifies the action to execute; the following
     *             arguments, if any, are passed to the respective action processors.
     * @throws Exception If any error occurs during the processing of the specified action.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) help();

        // included to help with static analysis, since these normally are
        //  instantiated dynamically
        new edu.uic.bitslab.propcov.core.Setup().init();
        new edu.uic.bitslab.propcov.extensions.Setup().init();
        AbstractSetup.load();

        try {
            ACTION action = ACTION.valueOf(args[0]);

            try {
                switch (action) {
                    case runconfig:
                        (new RunConfig()).process(args);
                        break;

                    case runmanual:
                        (new RunManual()).process(args);
                        break;

                    case genconfig:
                        (new GenConfig()).process(Arrays.copyOfRange(args, 1, args.length));
                        break;

                    default:
                        help();
                }
            } catch (IllegalArgumentException illegalArgumentException) {
                System.err.println("\nInvalid argument: " + illegalArgumentException.getMessage() + "\n\n");
                help(action);
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            help();
        }
    }

    private static void help() {
        System.out.print("\n\n Usage\n====================\n\n");

        for (ACTION action : values()) {
            if (HELP_MESSAGE.containsKey(action)) {
                System.out.print(HELP_MESSAGE.get(action));
            } else {
                System.out.println(action + " not found in HELP_MESSAGE");
            }
        }

        printExtensions();

        System.exit(1);
    }

    static void help(ACTION action) {
        System.out.print("\n\n Usage " + action.name() + "\n====================\n\n");
        System.out.print(HELP_MESSAGE.get(action));
        System.exit(1);
    }
}
