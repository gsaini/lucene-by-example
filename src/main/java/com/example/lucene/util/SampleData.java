package com.example.lucene.util;

import java.util.List;

/**
 * A tiny in-memory book catalogue used as the corpus for every learning module.
 * Keeping the dataset small makes it easy to predict what each query should return.
 */
public final class SampleData {

    private SampleData() {}

    public static List<Book> books() {
        return List.of(
            new Book("1", "Lucene in Action",
                "Erik Hatcher", "Programming",
                "A practical guide to building search applications with Apache Lucene.",
                2010, 4.6),
            new Book("2", "Effective Java",
                "Joshua Bloch", "Programming",
                "Best practices for the Java programming language, third edition.",
                2018, 4.8),
            new Book("3", "Java Concurrency in Practice",
                "Brian Goetz", "Programming",
                "How to write correct multithreaded programs on the JVM.",
                2006, 4.7),
            new Book("4", "Clean Code",
                "Robert C. Martin", "Programming",
                "A handbook of agile software craftsmanship and readable code.",
                2008, 4.4),
            new Book("5", "Designing Data-Intensive Applications",
                "Martin Kleppmann", "Architecture",
                "The big ideas behind reliable, scalable, and maintainable data systems.",
                2017, 4.9),
            new Book("6", "The Pragmatic Programmer",
                "Andrew Hunt", "Programming",
                "From journeyman to master — pragmatic tips for software developers.",
                1999, 4.5),
            new Book("7", "Search Patterns",
                "Peter Morville", "Search",
                "Design patterns for building delightful search user experiences.",
                2010, 4.1),
            new Book("8", "Introduction to Information Retrieval",
                "Christopher Manning", "Search",
                "The textbook on text indexing, ranking, and evaluation of search systems.",
                2008, 4.6),
            new Book("9", "Relevant Search",
                "Doug Turnbull", "Search",
                "With applications for Solr and Elasticsearch built on Lucene.",
                2016, 4.3),
            new Book("10", "Domain-Driven Design",
                "Eric Evans", "Architecture",
                "Tackling complexity in the heart of software.",
                2003, 4.2)
        );
    }
}
