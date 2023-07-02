package org.boxed.cli;

import com.google.api.core.ApiClock;
import com.google.api.core.NanoClock;
import com.google.api.gax.retrying.BasicResultRetryAlgorithm;
import com.google.api.gax.retrying.ResultRetryAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.RetryHelper;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.boxed.cli.General.forceClc;

/**
 * Couple of functions to run time limited job, or time limited job retries
 * Added helper function withResource - to generate resource, use it for data generation, close resource
 */
public class RetryJob {


  /**
   * Reruns provided function several times, but tries hard to fit within timeout.
   * Gives the time left as a parameter to a function to help code under rerun
   * to stay within a total timeout. Does not use a separate thread, so there
   * is no simple way to cancel execution.
   *
   * @see #defaultRetrySttings(Duration)
   */
  public static <V> JTry<V> retryJob(Callable<V> func, Duration totalTimeout) {
    if (totalTimeout.isNegative())
      return JTry.of(func::call);
    return JTry.of(() -> retryJob(func,
        // Pass when and how to retry
        defaultRetrySttings(totalTimeout),
        // Retry all exceptions, but don't retry any results
        new BasicResultRetryAlgorithm<>()));
  }

  /**
   * To do operation apply using generated resource, close resource after usage
   * @param getResource get closable resource of type A
   * @param apply       use resource to generate result of type B
   * @param ignoreCloseFailure ignore failures when clsosing resource
   * @param <A>  type of resource
   * @param <B>  type of geerated result
   * @return     JTry pair of result B or exception
   */
  public static <A, B> JTry<B> withResource(Supplier<A> getResource, Function<A,B> apply, boolean ignoreCloseFailure) {
    JTry<A> tryResource = JTry.of(getResource::get);
    LOG.info("Resource: " + tryResource);
    AutoCloseable c = (AutoCloseable) tryResource.getOrElse(null);
    JTry<B> tryClc = tryResource.map(apply::apply);
    LOG.info("Clc: " + tryClc);
    forceClc(tryClc.var.isPresent());
    JTry<Object> tryClose = JTry.of(c::close);
    LOG.info("Close: " + tryClose);
    if (ignoreCloseFailure) return tryClc;
    return tryClose.map(x -> tryClc.getOrThrow());
  }

  /**
   * Run time limited job
   * @param generator        object generator
   * @param timeOut   abort job after ms
   * @param <T>         generator type
   * @return            generated object
   */
  public static <T> JTry<T> timeLimitedJob(Callable<T> generator, Duration timeOut) {
     return withResource(TLimitedJob::new, limiter -> limiter.call(generator, timeOut.toMillis()).getOrThrow(),
             true);
  }
  private static RetrySettings defaultRetrySttings(
      Duration totalTimeout) {
    return defaultRetrySttings(totalTimeout, 3);
  }
  /**
   * Creates a default retry policy based on a duration passed in, which
   * uses some kind of exponential back-off. If you want more control, use
   * {@link com.google.cloud.RetryHelper#runWithRetries(Callable, RetrySettings, ResultRetryAlgorithm, ApiClock)}
   * instead. Implementation is subject to change, so don't depend on specific number of
   * retries and delays.
   *
   * @param totalTimeout Request would not take longer than that even with retries
   */
  private static RetrySettings defaultRetrySttings(Duration totalTimeout, Integer maxAttempts) {
    org.threeten.bp.Duration internalDuration =
        org.threeten.bp.Duration.ofNanos(totalTimeout.toNanos());
    RetrySettings.Builder builder = RetrySettings.newBuilder();
    if (maxAttempts > 0) builder.setMaxAttempts(maxAttempts);
    return builder
        // make sure that total timeout is always enforced
        .setTotalTimeout(internalDuration)
        .setMaxRetryDelay(internalDuration.dividedBy(3))
        .setMaxRpcTimeout(internalDuration)
        // make sure that if the first request is just under a total timeout
        .setInitialRpcTimeout(internalDuration)
        //Cannot make it less without violation retry delay time generation logic
        .setInitialRetryDelay(org.threeten.bp.Duration.ofMillis(1))
        .setRetryDelayMultiplier(2)
        .build();
  }

  private static <V> V retryJob(Callable<V> callable, RetrySettings retrySettings, ResultRetryAlgorithm<?> retryAlgorithm) {
      return RetryHelper.runWithRetries(callable, retrySettings, retryAlgorithm, NanoClock.getDefaultClock());
  }


  /**
   * <pre>
   * Simple time limiter for jobs, usage:
   * Using Try block to call limiter close
   * try(TLimitedJob limiter = new TLimitedJob()) {
   *   limiter.call(func, timeOutMS)
   * }
   * Using withResource
   * withResource(() -> new TLimitedJob(), limiter -> limiter.call(func, timeout));
   * </pre>
   */
  public static class TLimitedJob implements AutoCloseable {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    SimpleTimeLimiter timeLimiter = SimpleTimeLimiter.create(executor);
    public <T> JTry<T> call(Callable<T> callable, Long timeOutMS) {
      if (timeOutMS != null && timeOutMS > 0)
        return JTry.of(() -> timeLimiter.callWithTimeout(callable, timeOutMS, TimeUnit.MILLISECONDS));
      return JTry.of(callable::call);
    }

    @Override public void close() throws Exception {
      executor.shutdownNow();
    }
  }

  private static final Logger LOG = LogManager.getLogger(RetryJob.class);
}
