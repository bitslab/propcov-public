package edu.uic.bitslab.propcov.extensions.testframework;

import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import org.opalj.br.Method;

/**
 * The JunitQuickCheck class is an implementation of the AbstractTestFramework designed to
 * integrate with the JUnit-QuickCheck property-based testing framework.
 */
public class JunitQuickCheck extends AbstractTestFramework {
    /**
     * Constructs a JunitQuickCheck instance using the provided YAMLConfig.Extension configuration.
     * This constructor initializes the JunitQuickCheck test framework by configuring its properties
     * and environment based on the given extension settings.
     *
     * @param extension the YAMLConfig.Extension instance containing configuration details required
     *                  for initializing the JunitQuickCheck framework, including properties specific
     *                  to the testing environment.
     */
    public JunitQuickCheck(YAMLConfig.Extension extension) {
        super(extension, "PropCov.TestFramework.JunitQuickCheck.");
    }

    public Boolean isTestProperty(Method method) {
        return method.annotations().exists(
            a -> a.annotationType().asClassType().fqn().equals("com/pholser/junit/quickcheck/Property")
        );
    }
}
