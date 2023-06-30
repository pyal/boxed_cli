package org.boxed.cli.run;

import org.apache.commons.lang3.tuple.Pair;
import org.boxed.cli.Cli;
import org.boxed.cli.json.JsonTools;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import static org.boxed.cli.General.listT;

/**
 * <pre>
 * Class to be executed from command line, defines <b>multy mode</b> command
 * reading / parsing user input, do the job
 * To run the command use
 * cd $BOXDIR
 * mvn install
 * java -cp target/boxed_cli-1.0.1-jar-with-dependencies.jar org/boxed/cli/run/MultiSample test-cmd -h -d TRACE ...
 * </pre>
 */
public class MultiSample {

    static public class TestCmd extends Cli.CliBasic implements Serializable {
        @Option(name = "-i", aliases = "--input", usage = "input string")
        private String input = "inputStr";
        @Option(name = "-n", aliases = "--number", usage = "input number")
        private Integer number = 10;
        @Option(name = "-o", aliases = "--option", usage = "input boolean option")
        private Boolean option = false;

        @Override
        public void run() {
            LOG.info("Main class\n" + JsonTools.obj2StrCustom(this, false, option));
            LOG.debug(() -> "Testing debug");
        }

    }

    static public class Base extends Cli.MultiTaskBasic {
        @Override
        public List<Pair<String, Class<?>>> getSubcommands() {
            return listT(
                    Pair.of("test-cmd", TestCmd.class)
            );
        }

        @Override
        public Cli.CliBasic getCurrentCli() {
            return current;
        }

        @Argument(required = true, handler = SubCommandHandler.class, usage = "set run mode")
        @SubCommands({
                @SubCommand(name = "test-cmd", impl = TestCmd.class)
        })
        Cli.CliBasic current = null;
    }
    public static void main(String[] args)  throws IOException {
        Cli.mainRun(args, new Cli.MultiTaskRunner(new MultiSample.Base()));
    }

}

