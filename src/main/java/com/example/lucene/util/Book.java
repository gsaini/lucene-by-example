package com.example.lucene.util;

/**
 * Simple POJO used as the source domain object for every learning module.
 * Lucene itself has no concept of a "Book" — it only knows {@link org.apache.lucene.document.Document}s
 * made of {@link org.apache.lucene.document.Field}s. Each module shows a different way of mapping
 * this POJO into a Lucene Document.
 */
public class Book {

    public final String id;
    public final String title;
    public final String author;
    public final String category;
    public final String description;
    public final int year;
    public final double rating;

    public Book(String id, String title, String author, String category,
                String description, int year, double rating) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.description = description;
        this.year = year;
        this.rating = rating;
    }
}
