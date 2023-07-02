package org.boxed.cli;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.boxed.cli.PrettyNumberFormater.prettyNumber;
import static org.boxed.cli.PrettyNumberFormater.prettyTime;

public class TimerDataTest extends BaseTest {

    @Test
    public void timerTest() {
        Double printTime = 0.1;
        Integer printCount = 10;
        TimerData.IterationTimer timer = new TimerData.IterationTimer(printTime);
        AtomicInteger printNum = new AtomicInteger(0);
        int runNumber = 0;
        while (printNum.get() < printCount) {
            runNumber += 1;
            timer.printIter(1, count->{
                LOG.info(count.iterCountTime() + " : " + count.iterPace() + " " + count.runCount());
                printNum.incrementAndGet();
            });
        }
        Integer n = runNumber;
        timer.printLast(count->{
            LOG.info("Done " + count.runCountTime());
            LOG.info("Done: " + prettyTime(count.run_nanosecond_time, 2) + " expect " + prettyTime(((Double)(1e9 * printCount * printTime)).longValue(), 2));
            Assert.assertTrue("Ten iterations have to be done in 0.01 sec, precision is"
                    + " 0.002 sec",Math.abs(count.run_nanosecond_time - 1e9 * printCount * printTime) < 2e9 * printTime);
            Assert.assertEquals("Counted correct number of iterations", count.run_count, (long)n);
            Assert.assertEquals("Counted correct number of iterations", printNum.get(), (long)printCount);
        });

    }


}