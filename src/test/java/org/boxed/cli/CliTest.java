package org.boxed.cli;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.util.List;
import java.util.stream.Collectors;

import static org.boxed.cli.General.listT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CliTest extends BaseTest {
    static String appInput;
    static Integer appNumber;
    static Boolean appOption;

    public static class CliApp extends Cli.CliBasic {
        @Option(name = "-i", aliases = "--input", usage = "input string")
        private String input = "inputStr";
        @Option(name = "-n", aliases = "--number", usage = "input number")
        private Integer number = 10;
        @Option(name = "-o", aliases = "--option", usage = "input boolean option")
        private Boolean option = false;

        @Override
        public void run() {
            appInput = input;
            appNumber = number;
            appOption = option;
            LOG.info("Params are: input " + input + " number " + number + " option" + option);
        }

        public static void main(String[] args) {
            CliTest.LOG.info("Args: " + Lists.newArrayList(args).stream().collect(Collectors.joining(", ")));
            Cli.mainRunInternal(args, new CliApp());
        }
    }

    void AssertRun(String args, String input, Integer number, Boolean option) {
        CliApp.main(args.isEmpty() ? new String[0] : args.split(" "));
        Assert.assertEquals(appInput, input);
        Assert.assertEquals(appNumber, number);
        Assert.assertEquals(appOption, option);
    }
    @Test
    public void cliBasicTest() {
        AssertRun("", "inputStr", 10, false);
        AssertRun("-h", "inputStr", 10, false);         // testing build in - help
        LOG.error("=======    Testing bad params =================");
        AssertRun("-d", "inputStr", 10, false);         // testing build in - debug level
        LOG.error("=======    DONE Testing bad params  ============");
        AssertRun("-d DEBUG", "inputStr", 10, false);         // testing build in - debug level
        AssertRun("-i hello -n 5 -o", "hello", 5, true);//testing defined options
    }




    public static class CliMulti extends Cli.MultiTaskBasic {


        @Override
        public List<Pair<String, Class<?>>> getSubcommands() {
            return listT(
                    Pair.of("test-cmd", CliApp.class)
            );
        }
        @Override
        public Cli.CliBasic getCurrentCli() {
            return current;
        }
        @Argument(required = true, handler = SubCommandHandler.class, usage = "set run mode")
        @SubCommands({
                @SubCommand(name = "test-cmd", impl = CliApp.class)
        })
        Cli.CliBasic current = null;




        public static boolean main(String[] args) {
            CliTest.LOG.info("Args: " + Lists.newArrayList(args).stream().collect(Collectors.joining(", ")));
            return Cli.mainRunInternal(args, new Cli.MultiTaskRunner(new CliMulti()));
        }
    }
    @Test
    public void multiRun() {
        String []args = {"test-cmd", "-i", "III", "-d", "TRACE"};
        assertTrue("Good long option have to work", CliMulti.main(args));
        String []arg1 = {"-h"};
        assertTrue("Help request is ok on multi level", CliMulti.main(arg1));
        String []arg2 = {"-r"};
        assertFalse("Have to fail for bad param", CliMulti.main(arg2));
        String []arg3 = {""};
        assertFalse("Have to fail for empty param", CliMulti.main(arg3));
    }

}
