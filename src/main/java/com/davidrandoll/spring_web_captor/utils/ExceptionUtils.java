package com.davidrandoll.spring_web_captor.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class ExceptionUtils {
    /**
     * Safely executes a supplier and returns a fallback value in case of an exception.
     *
     * @param supplier the supplier to execute
     * @param fallback the fallback value to return in case of an exception
     * @param <T>      the type of the value returned by the supplier
     * @return the result of the supplier or the fallback value if an exception occurs
     */
    public static <T> T safe(@NonNull Supplier<T> supplier, @Nullable T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Error in creating HttpRequestEvent: {}", e.getMessage(), e);
            return fallback;
        }
    }

    //safe or else call another supplier
    public static <T> T safeOrElse(@NonNull Supplier<T> supplier, @NonNull Supplier<T> fallbackSupplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Error in creating HttpRequestEvent: {}", e.getMessage(), e);
            return fallbackSupplier.get();
        }
    }
}