package org.boxed.cli;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.Math.min;

public class ExceptionHandler {
    /**
     * @param e exception
     * @return string containing exception stack
     */
    public static String exceptionToString(Throwable e) {
        return exceptionToString(e, -1);
    }
    /**
     * @param e exception
     * @param limit maximum size of exception stack string
     * @return  exception stack string
     */
    public static String exceptionToString(Throwable e, Integer limit) {
        String ret = "exceptionToString";
        try {
            if (e == null) return "null exception";
            ret = "Throwable: " + e.toString() +
                    "\nStack:\n" + Arrays.stream(e.getStackTrace()).map(x -> x.toString())
                    .collect(Collectors.joining("\n"));
        } catch (Throwable a) {
            ret = "Failed parsing exception: " + a.toString();
        }
        return ret.substring(0, min(limit, ret.length()));
    }

    /**
     * Rethrow  fakes java throw contract (no need to special treat error in all the stack)
     * @param builder job which can generate exception
     * @param <T>     job output type
     * @return        job output
     */
    public static <T> T rethrow(WrapSupplier<T, Throwable> builder) {
        try { return builder.get(); } catch (Throwable e) {rethrow(e);}
        assert false;
        return null;
    }

    /**
     * Rethrow  fakes java throw contract (no need to special treat error in all the stack)
     * @param builder job which can generate exception
     */
    public static void rethrow(WrapRunnable<Throwable> builder) {
        try { builder.run(); } catch (Throwable e) {rethrow(e);}
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrowInternal(Throwable throwable) throws T {
        throw (T) throwable;
    }
    /**
     * Rethrow  fakes java throw contract (no need to special treat error in all the stack)
     * @param throwable exception to rethrow
     */
    public static void rethrow(Throwable throwable) {
        rethrowInternal(throwable);
    }

    public interface WrapSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface WrapRunnable<E extends Throwable> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface WrapFunction<R, T, E extends Throwable> {
        T apply(R r) throws E;
    }
}
