package lk.eternal.ai.util;

import java.util.function.Function;

public class TryUtils {

    public static <T, R> Function<T, R> all(TryFunction<T, R> function) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                return null;
            }
        };
    }

    public static <T, R> Function<T, R> assign(Function<T, R> function, Class<? extends Exception> type) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                if (e.getClass().isAssignableFrom(type)) {
                    return null;
                }
                throw e;
            }
        };
    }

    @FunctionalInterface
    public interface TryFunction<T, R> {

        R apply(T t) throws Exception;

    }
}