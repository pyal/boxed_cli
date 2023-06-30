package org.boxed.cli;

import org.boxed.cli.ExceptionHandler.WrapRunnable;
import org.boxed.cli.ExceptionHandler.WrapSupplier;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.boxed.cli.ExceptionHandler.exceptionToString;
import static org.boxed.cli.ExceptionHandler.rethrow;

/**
 * <pre>
 * Class similar to Scala.Try - used to encapsulate in one structure
 * { result (of type T) ,  exception (Throwable) }
 * Exception handling can be done down the stack.
 * Result can be transformed many times using JTry.map - leaving original exception
 * </pre>
 * @param <T> stored object type
 */
public class JTry<T> {
    public Optional<T> var = Optional.empty();
    public Optional<Throwable> throwable = Optional.empty();

    public static <T> JTry<T> of(WrapSupplier<T, Throwable> builder) {
        return new JTry<T>(builder);
    }

    public static <T> JTry<T> of(WrapRunnable<Throwable> builder) {
        return new JTry<T>(builder);
    }

    public JTry(WrapSupplier<T, Throwable> builder) {
        try {
            var = Optional.ofNullable(builder.get());
        } catch (Throwable e) {
            throwable = Optional.of(e);
        }
    }

    public JTry(WrapRunnable<Throwable> builder) {
        try {
            builder.run();
        } catch (Throwable e) {
            throwable = Optional.of(e);
        }
    }

    public JTry(Throwable e) {
        throwable = Optional.of(e);
    }

    //rethrow RuntimeException, throw RuntimeException(exception) for the rest
    public T getOrThrow() {
        if (throwable.isPresent()) {
            try {
                RuntimeException e = (RuntimeException) throwable.get();
                if (e != null) throw e;
            } catch (Throwable bad) {
            }
            rethrow(throwable.get());
        }
        return var.orElseGet(() -> null);
    }

    public T getOrElse(T def) {
        if (throwable.isPresent()) return def;
        return var.orElseGet(() -> null);
    }

    public T getOrSet(WrapSupplier<T, Throwable> supplier) {
        if (throwable.isPresent()) return rethrow(supplier);
        return var.orElseGet(null);
    }

    //Build result for generated exception (if present)
    public JTry<T> recover(Function<Throwable, T> handle) {
        if (throwable.isPresent()) return JTry.of(() -> handle.apply(throwable.get()));
        return JTry.of(() -> this.getOrThrow());
    }

    public JTry<T> processException(Consumer<Throwable> handle) {
        if (throwable.isPresent()) handle.accept(throwable.get());
        return this;
    }

    //generate null result for handled exception
    public JTry<T> recover(Consumer<Throwable> handle) {
        if (throwable.isPresent()) return JTry.of(() -> handle.accept(throwable.get()));
        return JTry.of(() -> this.getOrThrow());
    }

    public <T1> JTry<T1> map(Function<T, T1> handle) {
        if (throwable.isPresent()) return new JTry<T1>(throwable.get());
        return JTry.of(() -> handle.apply(var.orElseGet(() -> null)));
    }

    public Boolean isOk() {
        return !throwable.isPresent();
    }

    @Override
    public String toString() {
        return map(x -> "value: " + x.toString())
                .recover((Throwable e) -> "exception: " + exceptionToString(e)).getOrThrow();
    }

    @Override
    public boolean equals(Object o) {
        @SuppressWarnings("unchecked")
        JTry<T> other = (JTry<T>) o;
        if (other == null) return false;
        Boolean ret = map(x -> other.map(y -> x.equals(y)).getOrElse(false))
                .recover((Throwable e) -> other.map(x -> false).recover(e1 -> true))
                .getOrThrow();
        return ret;
    }

    @Override
    public int hashCode() {
        int ret = map(x -> x == null ? 0 : x.hashCode()).getOrElse(1);
        return ret;
    }
}

