package org.boxed.cli.run;

import org.apache.logging.log4j.LogManager;
import org.boxed.cli.Cli;
import org.boxed.cli.json.JsonTools;
import org.kohsuke.args4j.Option;

import java.io.IOException;

/**
 * <pre>
 * Class to be executed from command line, defines <b>single mode</b> command
 * reading / parsing user input, do the job
 * To run the command use
 * cd $BOXDIR
 * mvn install
 * java -cp target/boxed_cli-1.0.1-jar-with-dependencies.jar org/boxed/cli/run/Sample -h -d TRACE ...
 * </pre>
 */
public class Sample {
    public static class CliApp extends Cli.CliBasic {
        @Option(name = "-i", aliases = "--input", usage = "input string")
        private String input = "inputStr";
        @Option(name = "-n", aliases = "--number", usage = "input number")
        private Integer number = 10;
        @Option(name = "-o", aliases = "--option", usage = "input boolean option")
        private Boolean option = false;

        @Override
        public void run() {
            LOG.info(() -> "Params are: input:" + input + " number:" + number + " option:" + option);
            LOG.info("Main class\n" + JsonTools.obj2StrCustom(this, false, option));
            LOG.debug(() -> "Testing debug");
            LogManager.getLogger(Sample.class).debug("Local debug test");
        }

    }
    public static void main(String[] args)  throws IOException {
        Cli.mainRun(args, new CliApp());
    }
}
