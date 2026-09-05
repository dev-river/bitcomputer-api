package com.bitcomputer.portal.bgc;

import java.util.Set;

public class NameSplitter {

    private static final Set<String> COMPOUND_SURNAMES = Set.of(
        "남궁", "황보", "제갈", "선우", "독고", "사공", "서문", "동방"
    );

    public record SplitName(String lastName, String firstName) {}

    public static SplitName split(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        String trimmed = fullName.trim();
        for (String surname : COMPOUND_SURNAMES) {
            if (trimmed.startsWith(surname) && trimmed.length() > surname.length()) {
                return new SplitName(surname, trimmed.substring(surname.length()));
            }
        }
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("fullName too short to split: " + fullName);
        }
        return new SplitName(trimmed.substring(0, 1), trimmed.substring(1));
    }
}
