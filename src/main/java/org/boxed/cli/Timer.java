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
 * Timer.IterationTimer timer = new Timer.IterationTimer(10.);
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
public class Timer {
    static Logger LOG = LogManager.getLogger(Timer.class);
    public static class IterationTimer implements Serializable {
        Long startTime, publishTime, publishIter, currentIter, publishTimeStep;
        public IterationTimer(double publishTimeStepInSec) {
            this.publishTimeStep = ((Double)(publishTimeStepInSec * 1e9)).longValue();
            reset();
        }
        public void  printIter(int iterationDone, String message) {
            printIter(iterationDone, x->LOG.info(message + " " + x.iterSpeed() + " " + x.totalCount()));
        }
        public void  printIter(int iterationDone, Consumer<IterationData> publisher) {
            long curTime = System.nanoTime();
            currentIter += iterationDone;
            if(curTime - publishTime >= publishTimeStep) {
                publisher.accept(getIterationData(curTime));
                publishIter = currentIter;
                publishTime = curTime;
            }
        }
        public void  printLast(Consumer<IterationData> publisher) {
            publisher.accept(getIterationData());
            reset();
        }
        public IterationTimer addIterations(long add) {
            currentIter += add;
            return this;
        }
        public IterationData getIterationData() {
            return getIterationData(System.nanoTime());
        }
        public IterationData getIterationData(long curTime) {
            return new IterationData(curTime - publishTime, currentIter - publishIter, curTime - startTime, currentIter);
        }
        public void reset() {
            startTime = System.nanoTime();
            publishTime = startTime;
            publishIter = currentIter = 0L;
        }
        static public <T> T measure(Supplier<T> measureFunc, Consumer<IterationData> publisher) {
            IterationTimer timer = new IterationTimer(100);
            T res = measureFunc.get();
            publisher.accept(timer.getIterationData());
            return res;
        }
        static public void measure(Runnable measureFunc, Consumer<IterationData> publisher) {
            IterationTimer timer = new IterationTimer(100);
            measureFunc.run();
            publisher.accept(timer.getIterationData());
        }
    }

    public static class IterationData {
        public long iteration_nanosecond_time, iteration_count,
                run_nanosecond_time, run_count;

        public IterationData(long iteration_nanosecond_time, long iteration_count, long run_nanosecond_time, long run_count) {
            this.iteration_nanosecond_time = iteration_nanosecond_time;
            this.iteration_count = iteration_count;
            this.run_nanosecond_time = run_nanosecond_time;
            this.run_count = run_count;
        }

        public String totalCountTime() {
            return "RunCount " + prettyNumber(run_count, 2) + " in " + prettyTime(run_nanosecond_time, 2);
        }

        public String totalTime() {
            return "RunTime " + prettyTime(run_nanosecond_time, 2);
        }

        public String totalCount() {
            return "RunCount " + prettyNumber(run_count, 2);
        }

        public String totalSpeed() {
            double iPerSec = 1e9 * Math.max(run_count, 1) / run_nanosecond_time;
            return "Average(I/s) " + String.format("%3.2e", iPerSec) + " s/I " + prettyTime(((Double) (1e9 / iPerSec)).longValue(), 2);
        }

        public String iterCountTime() {
            return "ItCount " + prettyNumber(iteration_count, 2) + " in " + prettyTime(iteration_nanosecond_time, 2);
        }

        public String iterSpeed() {
            double iPerSec = 1e9 * Math.max(iteration_count, 1) / iteration_nanosecond_time;
            return "I/s " + String.format("%3.2e", iPerSec) + " s/I " + prettyTime(((Double) (1e9 / iPerSec)).longValue(), 2);
        }

    }

}
