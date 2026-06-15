package com.th3curiosity.studycards.utils;

import org.apache.commons.lang3.RandomStringUtils;

public class TestDataHelper {

    private TestDataHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateRandomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
