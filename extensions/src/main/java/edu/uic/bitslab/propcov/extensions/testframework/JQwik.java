package edu.uic.bitslab.propcov.extensions.testframework;

import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import org.opalj.br.Method;

/**
 * JQwik is an implementation of the AbstractTestFramework, specifically designed to integrate
 * with the JQwik property-based testing framework. It uses annotations provided by JQwik
 * to identify and validate property-based test methods.
 */
public class JQwik extends AbstractTestFramework {
    /**
     * Initializes a JQwik instance using the provided YAMLConfig.Extension configuration.
     * This constructor configures the JQwik test framework by setting up its environment
     * and properties according to the given extension configuration.
     *
     * @param extension the YAMLConfig.Extension instance containing configuration details
     *                  for initializing the test framework, including properties specific
     *                  to JQwik and its runtime environment.
     */
    public JQwik(YAMLConfig.Extension extension) {
        super(extension, "PropCov.TestFramework.JQwik.");
    }

    public Boolean isTestProperty(Method method) {
        if (method == null) return false;
        return method.annotations().exists(
            a -> a.annotationType().asClassType().fqn().equals("net/jqwik/api/Property")
        );
    }
}
