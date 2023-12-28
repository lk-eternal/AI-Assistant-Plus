package lk.eternal.ai.util;

import lk.eternal.ai.exception.ApiValidationException;
import org.springframework.util.StringUtils;

public class Assert {

    public static void hasText(String str, String errMsg) {
        if (!StringUtils.hasText(str)) {
            throw new ApiValidationException(errMsg);
        }
    }

    public static void hasText(String str, RuntimeException exception) {
        if (!StringUtils.hasText(str)) {
            throw exception;
        }
    }

    public static void notNull(Object o, String errMsg) {
        if (o == null) {
            throw new ApiValidationException(errMsg);
        }
    }

    public static void notNull(Object o, RuntimeException exception) {
        if (o == null) {
            throw exception;
        }
    }

    public static void isTrue(boolean flag, String errMsg) {
        if (!flag) {
            throw new ApiValidationException(errMsg);
        }
    }

    public static void isTrue(boolean flag, RuntimeException exception) {
        if (!flag) {
            throw exception;
        }
    }

}
