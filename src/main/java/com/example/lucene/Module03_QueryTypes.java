package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 3 — A tour of Lucene's most common Query classes.
 * <p>
 * Each demo below is intentionally tiny: one Query, one search, a short explanation.
 * The same in-memory index is reused for every example so you can compare results easily.
 */
public class Module03_QueryTypes {

    public static void run() throws Exception {
        Console.header("Module 3 — Query Types");

        try (Directory dir = new ByteBuffersDirectory();
             StandardAnalyzer analyzer = new StandardAnalyzer()) {

            buildIndex(dir, analyzer);

            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // TermQuery — exact match on a single tokenized term. Note "java" is lower case
                // because StandardAnalyzer lower-cased the indexed text.
                runQuery(searcher, "TermQuery: title contains 'java'",
                    new TermQuery(new Term("title", "java")));

                // PhraseQuery — terms in order, optionally with a "slop" allowing other tokens
                // between them. slop=0 means strictly adjacent.
                PhraseQuery phrase = new PhraseQuery.Builder()
                    .add(new Term("description", "data"))
                    .add(new Term("description", "systems"))
                    .setSlop(3)
                    .build();
                runQuery(searcher, "PhraseQuery: 'data' near 'systems' (slop=3)", phrase);

                // BooleanQuery — combine sub-queries with MUST / SHOULD / MUST_NOT / FILTER.
                // MUST + MUST_NOT affect both matching *and* scoring; FILTER affects matching
                // only (cheaper, no scoring) — prefer FILTER when you don't need a score boost.
                BooleanQuery bool = new BooleanQuery.Builder()
                    .add(new TermQuery(new Term("description", "search")), BooleanClause.Occur.MUST)
                    .add(new TermQuery(new Term("category", "Search")),    BooleanClause.Occur.FILTER)
                    .add(new TermQuery(new Term("description", "elasticsearch")), BooleanClause.Occur.SHOULD)
                    .build();
                runQuery(searcher, "BooleanQuery: search MUST in description AND category=Search", bool);

                // WildcardQuery — '?' matches one character, '*' matches zero or more. Avoid
                // leading wildcards on huge indexes: they force a full term-dictionary scan.
                runQuery(searcher, "WildcardQuery: author contains 'mart*'",
                    new WildcardQuery(new Term("author", "mart*")));

                // PrefixQuery — convenient shorthand for "term*".
                runQuery(searcher, "PrefixQuery: description starts with 'multi'",
                    new PrefixQuery(new Term("description", "multi")));

                // FuzzyQuery — Damerau-Levenshtein edit distance, max 2 edits by default.
                // Great for typo tolerance ("lucen" still finds "lucene").
                runQuery(searcher, "FuzzyQuery: title ~ 'lucen' (1 edit away from 'lucene')",
                    new FuzzyQuery(new Term("title", "lucen"), 1));

                // RegexpQuery — full regular expression on terms. Powerful but expensive.
                runQuery(searcher, "RegexpQuery: author matches 'er[a-z]+'",
                    new RegexpQuery(new Term("author", "er[a-z]+")));

                // Numeric range query on a Point field. IntPoint.newRangeQuery / DoublePoint.newRangeQuery
                // use a BKD tree internally — very fast even on millions of docs.
                runQuery(searcher, "IntPoint range: year in [2005, 2015]",
                    IntPoint.newRangeQuery("year", 2005, 2015));

                runQuery(searcher, "DoublePoint range: rating >= 4.6",
                    DoublePoint.newRangeQuery("rating", 4.6, Double.POSITIVE_INFINITY));
            }
        }
    }

    private static void buildIndex(Directory dir, StandardAnalyzer analyzer) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(dir, cfg)) {
            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));
                doc.add(new TextField("author", book.author, Field.Store.YES));
                doc.add(new StringField("category", book.category, Field.Store.YES));
                doc.add(new TextField("description", book.description, Field.Store.YES));

                // Numeric fields: a *Point for range queries + a StoredField so we can show
                // the value back to the user. Storing it on the Point alone is not enough.
                doc.add(new IntPoint("year", book.year));
                doc.add(new StoredField("year_stored", book.year));
                doc.add(new DoublePoint("rating", book.rating));
                doc.add(new StoredField("rating_stored", book.rating));

                writer.addDocument(doc);
            }
            writer.commit();
        }
    }

    private static void runQuery(IndexSearcher searcher, String label, Query query) throws Exception {
        System.out.println();
        System.out.println("> " + label);
        System.out.println("  Query class : " + query.getClass().getSimpleName());
        System.out.println("  toString()  : " + query.toString());

        TopDocs hits = searcher.search(query, 10);
        System.out.println("  totalHits   : " + hits.totalHits);
        for (ScoreDoc sd : hits.scoreDocs) {
            Document doc = searcher.storedFields().document(sd.doc);
            System.out.printf("    score=%.3f  id=%s  title=%s%n",
                sd.score, doc.get("id"), doc.get("title"));
        }
    }
}
