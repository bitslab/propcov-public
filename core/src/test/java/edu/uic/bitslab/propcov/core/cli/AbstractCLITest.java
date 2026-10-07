package edu.uic.bitslab.propcov.core.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.uic.bitslab.propcov.core.analysisframework.AnalysisFrameworkException;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.coverage.CoverageException;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCLITest {

    @Test
    void parseArgs() throws JsonProcessingException {
        Map<String, String> m = AbstractCLI.parseArgs("{\"prop1\":\"value1\",\"prop2\":\"value2\"}");
        assertEquals(2, m.size());
        assertEquals("value1", m.get("prop1"));
        assertEquals("value2", m.get("prop2"));

        assertThrows(JsonProcessingException.class,
                () -> AbstractCLI.parseArgs("{1:\"value1\",2:\"value2\"}"));

        assertTrue(
                assertThrows(UnsupportedOperationException.class,
                        () -> AbstractCLI.parseArgs("{\"prop1\":1,\"prop2\":2}"))
                        .getMessage().contains("Json values must all be string type")
        );
    }

    @Test
    void testObject() throws ConfigException, IOException, ExternalProcessException, SourceException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, TimerException, AnalysisFrameworkException, BuildSystemException, CoverageException, ClassNotFoundException, InstantiationException {
        TestAbstractCli testAbstractCli = new TestAbstractCli();
        assertNotNull(testAbstractCli);

        String[] args = { "arg1", "arg2" };
        testAbstractCli.validate(args);
        assertEquals(args, testAbstractCli.args);

        String[] args2 = { "arg3", "arg4" };
        testAbstractCli.process(args2);
        assertEquals(args2, testAbstractCli.args);
    }

    static class TestAbstractCli extends AbstractCLI {
        public String[] args;

        @Override
        protected void validate(String[] args) {
            this.args = args;
        }

        @Override
        protected void process(String[] args) throws TimerException, BuildSystemException, IOException, ExternalProcessException, SourceException, InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, AnalysisFrameworkException, CoverageException, ConfigException {
            this.args = args;
        }
    }
}