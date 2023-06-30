package org.boxed.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.function.Consumer;

/**
 * <pre>
 * To make log string calculation lazy - only if we have proper debug level
 * Logging is done by consumer, to report correctly  file and line for the log message
 * </pre>
 */
public class SupLog {
    public static final Logger LOG = LogManager.getLogger(SupLog.class);

    public static void setDebugTest(String level) {
        Level l = Level.getLevel(level);
        Configurator.setAllLevels("org.boxed.cli", l);
        Configurator.setRootLevel(l);
        Configurator.setAllLevels("", l);
    }
    public static void setDebugLevel(String level, String logName) {
        Level l = Level.getLevel(level);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(SupLog.class.getClassLoader(), false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = (logName == null || logName.isEmpty()  ) ?
                config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME) :
                config.getLoggerConfig(logName);
        loggerConfig.setLevel(l);
        ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
        LogManager.getLogger(SupLog.class).debug("Setting log level: " + level + " -> " + logName);
    }

    public static void log(Level level, Consumer<Logger> consume) {
        if (LOG.isEnabled(level)) consume.accept(LOG);
    }
    public static void debug(Consumer<Logger> consume) {
        log(Level.DEBUG, consume);
    }
    public static void info(Consumer<Logger> consume) {
        log(Level.INFO, consume);
    }

}
