package org.boxed.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import static org.boxed.cli.SupLog.setDebugTest;
import static org.junit.Assert.assertTrue;

public class BaseTest {
    public static Logger LOG = LogManager.getLogger(BaseTest.class);
    @Before
    public void before() {
        setDebugTest("DEBUG");
    }

    public void testMethodFailure(Runnable run, String message) {
        Boolean isOk = JTry.of(() -> {
            run.run();
            return false;
        }).recover((e) -> true).getOrThrow();
        assertTrue(message, isOk);
    }

}
