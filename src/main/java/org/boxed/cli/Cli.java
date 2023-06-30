package org.boxed.cli;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.boxed.cli.ExceptionHandler.exceptionToString;
import static org.boxed.cli.ExceptionHandler.rethrow;
import static org.boxed.cli.General.listT;
import static org.boxed.cli.PrettyNumberFormater.prettyTime;
import static org.boxed.cli.SupLog.setDebugLevel;
import static org.boxed.cli.json.JsonTools.obj2StrCustom;



/**
 * @Cli tools to simplify Cli usage @see <a href="https://args4j.kohsuke.org/">args4j</a>
 * @CliBasic class to provide common CLI parsing, help generation, debug level setting
 * consumed by mainRun - runner
 * @MultiTaskBasic class defining multiple cli modes under one program (similar to git / gcloud - multiple tasks)
 * @MultiTaskRunner CliBasic adpter for multi task job, taking MultiTaskBasic definition of tasks,
 * executed using the same mainRun
 *
 * <pre>
Cli program sample:
public class CliApp extends CliTools.CliBasic {
    @Option(name="-i", aliases = "--input", usage="input string")
    private String input = "inputStr";

    @Override public void run() {
        LOG.info("Params are: input " + input + " number " + number + " option" + option);
    }
    public static void main(String[] args) throws IOException {
        CliTools.mainRun(args, new CliApp());
    }
Usage of this classes can be found in run/Sample run/MultiSample
 * </pre>
 */

public class Cli {
    public final static Logger LOG = LogManager.getLogger(Cli.class);


    /**
     * Main entry point for CliBasic jobs  - if we define
     * class CliApp extends Cli.CliBasicthey
     * to use as application we have to define method
     * class Sample {
     * public static void main(String[] args) {mainRun(args, new CliApp());}
     * }
     * to be executed using
     * java -cp $BOXDIR/target/boxed_cli-1.0.1-jar-with-dependencies.jar org/boxed/cli/run/Sample -h -d TRACE ...
     *
     * @param args - input arguments provided to the programm
     * @param exec - CliBasic class defining input parameters using  @Option annotaion
     * @see <a href="https://args4j.kohsuke.org/">Description of base cli library - args4j</a>
     */
    //
    public static void mainRun(String[] args, CliBasic exec) {
        if (!mainRunInternal(args, exec)) System.exit(1);
    }

    /**
     * To test Cli functionality (no exit)
     *
     * @param args - see @mainRun
     * @param exec - see @mainRun
     * @return - return program result status (errors..)
     */
    @VisibleForTesting
    static Boolean mainRunInternal(String[] args, CliBasic exec) {
        TimerData.IterationTimer timer = new TimerData.IterationTimer(1.);
        JTry<Boolean> toDo = exec.parse(args, null);
        toDo.recover(e -> {
            LOG.error("Failed parsing command line: " + listT(args));
            LOG.error("Got exception: ", e);
            LOG.error("Usage: " + exec.usage());
            rethrow(e);
        });
        if (toDo.getOrElse(false)) {
            exec.run();
            timer.printLast(count -> LOG.info(exec.getClass().getCanonicalName() + ": done in " + prettyTime(count.run_nanosecond_time, 2)));
        }
        return toDo.isOk();
    }

    enum DebugLevel {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

    /**
     * Basic class for all programs parsing command line
     * Parameters defined using annotation @Option - will be used by all children cli classes
     */
    public static class CliBasic {

        /**
         * Potential conflict with scala spark - local varable will make class none serializable
         */
        public static final Logger LOG = LogManager.getLogger(CliBasic.class);
        public String debugLevel = DebugLevel.INFO.toString();
        public String[] cliArgs = null;

        /**
         * @param x debug level one of [INFO DEBUG ...] - command line arguments will be parsed by args4j library
         *          and if -d option found - this method will be called
         */
        @Option(name = "-d", aliases = "--debug", usage = "provide debug level for index: ALL, ALL|org.boxed.cli ", metaVar = "OFF, ALL, WARN, DEBUG")
        public void debugLevelSetter(String x) {
            Set<String> debugLevelSet = Lists.newArrayList(DebugLevel.class.getEnumConstants()).stream().
                    map(y -> y.toString()).collect(Collectors.toSet());
            ArrayList<String> levelArr = Lists.newArrayList(x.split("\\|"));
            debugLevel = levelArr.get(0);
            Preconditions.checkArgument(debugLevelSet.contains(debugLevel),
                    "Bad input debugLevel: " + debugLevel + " for input " + x + " possible values: " + debugLevelSet.stream().collect(Collectors.joining(" ")));
            String logName = (levelArr.size() < 2) ? "" : levelArr.get(1);
            setDebugLevel(debugLevel, logName);
        }

        /**
         *  command line arguments will be parsed by args4j library
         *          and if -h option found - printHelp will be settled to !defaultValue
         */
        @Option(name = "-h", aliases = "--help", help = true, usage = "print help")
        public Boolean printHelp = false;

        /**
         * @return Detailed description of program usage
         */
        public String usage() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                CmdLineParser parser = new CmdLineParser(this);
                parser.getProperties().withUsageWidth(120);
                parser.printUsage(out);
                out.write("\nExample: \ncli ".getBytes());
                parser.printSingleLineUsage(out);
                if (cliArgs != null) out.write(("\nArguments provided:\n[" +
                        Lists.newArrayList(cliArgs).stream().collect(Collectors.joining(" ")) + "]").getBytes());
            } catch (Throwable e) {
                LOG.error("Failed printing message:\n", e);
            }
            return "Command options:\n" + out.toString();
        }

        /**
         * To be defined in children as main entrance point after argument parsing
         */
        public void run() {
            LOG.debug("Not implemmented for: " + getClass().getCanonicalName());
        }

        /**
         * @param args        input command agruments
         * @param usageString provided usage string
         * @return AdvResult[Boolean]: Return error generated during argument parsing,
         * If no error - return boolen - if main exec to be executed (we can show only help, no error, no program to run)
         */
        public JTry<Boolean> parse(String[] args, Supplier<String> usageString) {
            return JTry.of(() -> {
                CmdLineParser parser = new CmdLineParser(this);
                cliArgs = Arrays.copyOf(args, args.length);
                parser.parseArgument(args);
                if (printHelp) {
                    String useStr = (usageString == null) ? usage() : usageString.get();
                    LOG.info("\n" + useStr);
                    return false;
                }
                return true;
            });
        }

        @Override
        public String toString() {
            return obj2StrCustom(this);
        }

    }


    /**
     * <pre>
     * For multi task Cli apps (aka git/gcloud)
     * &#64;MultiTaskBasic  defines collection of single CliBasic tasks (program modes)
     * usage (scala):
     * class MultiTaskCli extends MultiTaskBasic {
     *     override def getCurrentCli() = command
     *     override def getSubcommands = Lists.newArrayList(CliTools.Pair("simple", classOf[SingleJob]),...)
     *     &#64;Argument(required = true, handler = classOf[SubCommandHandler], usage = "set run mode")
     *     &#64;SubCommands(Array(new SubCommand(name = "simple", impl = classOf[SingleJob]),...)
     *     var command: CliBasic = null
     * }
     * usage (java):
     * static public class Base extends Cli.MultiTaskBasic {
     *   &#64;Override public List<Pair<String, Class<?>>> getSubcommands() {
     *        return listT(Pair.of("test-cmd", TestCmd.class)...);}
     *   &#64;Override public Cli.CliBasic getCurrentCli() { return current;}
     *   &#64;Argument(required = true, handler = SubCommandHandler.class, usage = "set run mode")
     *   &#64;SubCommands({&#64;SubCommand(name = "test-cmd", impl = TestCmd.class)...})
     *   Cli.CliBasic current = null;
     * }
     *
     * </pre>
     */
    public static abstract class MultiTaskBasic extends CliBasic {
        public abstract List<Pair<String, Class<?>>> getSubcommands();

        public abstract CliBasic getCurrentCli();
    }

    /**
     * CliBasic adapter for multiClass job
     * <P>Given MultiTaskBasic with list of tasks
     * <P>Builds virtual single task job to be used in @mainRun
     */
    public static class MultiTaskRunner extends CliBasic {
        MultiTaskBasic multiTaskBasic;
        Map<Class<?>, String> class2option;

        public MultiTaskRunner(MultiTaskBasic multiTaskBasic) {
            this.multiTaskBasic = multiTaskBasic;
            class2option = multiTaskBasic.getSubcommands().stream().
                    collect(Collectors.toMap(x -> x.getValue(), x -> x.getKey()));
        }

        @Override
        public String usage() {
            String str = "Top level params:\n" + multiTaskBasic.usage();
            str += "\nDetailed all modes help:\n" + multiTaskBasic.getSubcommands().stream().map(x -> {
                        return JTry.of(() -> "  Mode:   ======== " + x.getKey() + " ======\n" +
                                ((CliBasic) x.getValue().getDeclaredConstructor().newInstance()).usage() + "\n"
                        ).recover(e -> {
                            LOG.error("Parsing description exception caught: ", e);
                            return "ERROR Parsing description exception caught: " + exceptionToString(e);
                        }).getOrThrow();
                    }
            ).collect(Collectors.joining("\n"));
            return str;
        }

        @Override
        public void run() {
            LOG.debug("Option chosen: " + class2option.get(multiTaskBasic.getCurrentCli().getClass()) + " object: " +
                    multiTaskBasic.getCurrentCli().getClass().getCanonicalName());
            multiTaskBasic.getCurrentCli().run();
        }

        @Override
        public JTry<Boolean> parse(String[] args, Supplier<String> usageString) {
            JTry<Boolean> toDo = multiTaskBasic.parse(args, () -> usage());
            if (!toDo.isOk()) return toDo;
            return JTry.of(() -> {
                CliBasic cli = multiTaskBasic.getCurrentCli();
                if (cli.printHelp) {
                    String descr = "\nMode chosen: [" + class2option.get(cli.getClass()) + "] object: " +
                            cli.getClass().getCanonicalName() + "\nDetailed mode description:\n" + cli.usage();
                    LOG.info(descr);
                    return false;
                }
                return true;
            }).recover((e) -> false);
        }
    }
}