package com.example.lucene;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entry point for the learning project.
 * <p>
 * Run every module in order:
 * <pre>
 *   mvn -q compile exec:java
 * </pre>
 * <p>
 * Or pick a single module by number:
 * <pre>
 *   mvn -q compile exec:java -Dexec.args=3
 * </pre>
 */
public class Main {

    /** Each module is a tiny self-contained lesson. Kept in insertion order. */
    private static final Map<Integer, Runner> MODULES = new LinkedHashMap<>() {{
        put(1,  Module01_HelloLucene::run);
        put(2,  Module02_FieldsAndAnalyzers::run);
        put(3,  Module03_QueryTypes::run);
        put(4,  Module04_QueryParser::run);
        put(5,  Module05_Highlighting::run);
        put(6,  Module06_Faceting::run);
        put(7,  Module07_SortingAndScoring::run);
        put(8,  Module08_UpdatesAndDeletes::run);
        put(9,  Module09_CustomAnalyzer::run);
        put(10, Module10_Suggester::run);
    }};

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // No argument → run every module in order.
            for (Map.Entry<Integer, Runner> e : MODULES.entrySet()) {
                e.getValue().run();
            }
            return;
        }
        for (String arg : args) {
            int n = Integer.parseInt(arg.trim());
            Runner runner = MODULES.get(n);
            if (runner == null) {
                System.err.println("Unknown module: " + n + ". Valid: 1.." + MODULES.size());
                continue;
            }
            runner.run();
        }
    }

    @FunctionalInterface
    private interface Runner {
        void run() throws Exception;
    }
}
