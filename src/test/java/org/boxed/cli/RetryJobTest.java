package org.boxed.cli;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.boxed.cli.RetryJob.retryJob;
import static org.boxed.cli.RetryJob.timeLimitedJob;
import static org.boxed.cli.Timer.IterationTimer.measure;

public class RetryJobTest extends BaseTest {
    Callable<Boolean> build(String mes, int runTimeMs, boolean fail) {
        return () -> {
            JTry.of(() -> Thread.sleep(runTimeMs)).getOrThrow();
            if (fail) throw new RuntimeException("After " + runTimeMs + " failing job:" + mes);
            return true;
        };
    }
    @Test
    public void testRetryJob() {
        Callable<Boolean> testOkFunc = build("OK func", 10, false);
        Callable<Boolean> testFailFunc = build("Fail func", 11, true);
        Boolean res = measure(() -> JTry.of(() -> retryJob(testFailFunc, Duration.ofMillis(100))).getOrElse(null), time -> {
            long t = time.run_nanosecond_time / 1000 / 1000;
            LOG.info("Retry 100 got " + t);
            assert (t >= 50);
            assert (t < 150);
        }).getOrElse(null);
        assert(res == null);
        assert(retryJob(testOkFunc, Duration.ofMillis(4)).getOrElse(null));
        assert(timeLimitedJob(testOkFunc, Duration.ofMillis(100)).getOrElse(null));
        assert(timeLimitedJob(testOkFunc, Duration.ofMillis(4)).getOrElse(null) == null);
        assert(timeLimitedJob(testFailFunc, Duration.ofMillis(100)).getOrElse(null) == null);

    }


}