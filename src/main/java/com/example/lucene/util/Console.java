package com.example.lucene.util;

/** Tiny formatting helpers so module output stays readable. */
public final class Console {

    private Console() {}

    public static void header(String title) {
        String bar = "=".repeat(Math.max(20, title.length() + 4));
        System.out.println();
        System.out.println(bar);
        System.out.println("  " + title);
        System.out.println(bar);
    }

    public static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
