package org.boxed.cli;

import junit.framework.TestCase;
import org.junit.Test;

import static org.boxed.cli.ExceptionHandler.exceptionToString;

public class ExceptionHandlerTest extends BaseTest {

    @Test
    public void testExceptionToString() {
        LOG.info(exceptionToString(new RuntimeException("Test"), 100));
        LOG.info(exceptionToString(new RuntimeException("Test")));


    }

}