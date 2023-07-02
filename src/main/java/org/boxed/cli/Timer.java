package org.boxed.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.boxed.cli.PrettyNumberFormater.prettyNumber;
import static org.boxed.cli.PrettyNumberFormater.prettyTime;



/**
 * <pre>
 * Helper class to measure long program progress
 * Sample : want to measure, print progress speed every 10 sec
 * TimerData.IterationTimer timer = new TimerData.IterationTimer(10.);
 *
 * while (true) {
 *     //It will skip printing for fast running steps, print every 10 seconds
 *     timer.printIter(1, count->LOG.info(count.iterPace() + " " + count.runCount()));
 * }
 * //Print overall, general statistics
 * timer.printLast(count->LOG.info("Done " + count.runCountTime()));
 *
 * </pre>
 */
public class TimerData {
    static Logger LOG = LogManager.getLogger(Cli.class);

    public long iteration_nanosecond_time;
    public long iteration_count;
    public long run_nanosecond_time;
    public long run_count;

    public TimerData(long iteration_nanosecond_time, long iteration_count, long run_nanosecond_time, long run_count) {
        this.iteration_nanosecond_time = iteration_nanosecond_time;
        this.iteration_count = iteration_count;
        this.run_nanosecond_time = run_nanosecond_time;
        this.run_count = run_count;
    }

    public String runCountTime() {
        return "RunCount " + prettyNumber(run_count, 2) + " in " + prettyTime(run_nanosecond_time, 2);
    }

    public String runTime() {
        return "RunTime " + prettyTime(run_nanosecond_time, 2);
    }

    public String runCount() {
        return "RunCount " + prettyNumber(run_count, 2);
    }

    public String runPace() {
        double iPerSec = 1e9 * Math.max(run_count, 1) / run_nanosecond_time;
        return "Average(I/s) " + String.format("%3.2e", iPerSec) + " s/I " + prettyTime(((Double) (1e9 / iPerSec)).longValue(), 2);
    }

    public String iterCountTime() {
        return "ItCount " + prettyNumber(iteration_count, 2) + " in " + prettyTime(iteration_nanosecond_time, 2);
    }

    public String iterPace() {
        double iPerSec = 1e9 * Math.max(iteration_count, 1) / iteration_nanosecond_time;
        return "I/s " + String.format("%3.2e", iPerSec) + " s/I " + prettyTime(((Double) (1e9 / iPerSec)).longValue(), 2);
    }

    public static class IterationPrinter implements Consumer<TimerData> {
        public IterationPrinter(Logger logger, String message) {
            this.logger = logger;
            this.message = message;
        }
        Logger logger;
        String message;
        @Override
        public void accept(TimerData timerData) {
            logger.info(message + " " + timerData.iterPace() + " " + timerData.runCount());
        }
        public IterationPrinter setMessage(String message) {
            this.message = message;
            return this;
        }
    }
    public static class IterationTimer implements Serializable {
        Long startTime, publishTime, publishIter, currentIter, publishTimeStep;
        public IterationTimer(double publishTimeStepInSec) {
            this.publishTimeStep = ((Double)(publishTimeStepInSec * 1e9)).longValue();
            reset();
        }
        public void  printIter(int iterationDone, String message) {
            printIter(iterationDone, x->LOG.info(message + " " + x.iterPace() + " " + x.runCount()));
        }
        public void  printIter(int iterationDone, Consumer<TimerData> publisher) {
            Long curTime = System.nanoTime();
            currentIter += iterationDone;
            if(curTime - publishTime >= publishTimeStep) {
                publisher.accept(getTimerData(curTime));
                publishIter = currentIter;
                publishTime = curTime;
            }
        }
        public void  printLast(Consumer<TimerData> publisher) {
            publisher.accept(getTimerData());
            reset();
        }
        public IterationTimer addIterations(long add) {
            currentIter += add;
            return this;
        }
        public TimerData getTimerData() {
            return getTimerData(System.nanoTime());
        }
        public TimerData getTimerData(long curTime) {
            return new TimerData(curTime - publishTime, currentIter - publishIter, curTime - startTime, currentIter);
        }
        public void reset() {
            startTime = System.nanoTime();
            publishTime = startTime;
            publishIter = currentIter = 0L;
        }
        static public <T> T measure(Supplier<T> measureFunc, Consumer<TimerData> publisher) {
            IterationTimer timer = new IterationTimer(100);
            T res = measureFunc.get();
            publisher.accept(timer.getTimerData());
            return res;
        }
        static public void measure(Runnable measureFunc,
                                   Consumer<TimerData> publisher) {
            IterationTimer timer = new IterationTimer(100);
            measureFunc.run();
            publisher.accept(timer.getTimerData());
        }
    }

}
