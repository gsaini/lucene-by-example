package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 1 — Hello, Lucene!
 * <p>
 * Goal: build the smallest possible end-to-end pipeline so the moving parts are obvious.
 * <p>
 * The five fundamental Lucene concepts shown here:
 * <ol>
 *   <li><b>Directory</b> — where the index lives (RAM, on disk, network ...).</li>
 *   <li><b>Analyzer</b> — how text is split into tokens (words) before indexing/searching.</li>
 *   <li><b>IndexWriter</b> — adds Documents to the index.</li>
 *   <li><b>IndexReader / IndexSearcher</b> — opens a snapshot of the index and runs queries.</li>
 *   <li><b>Query</b> — what you actually want to find. {@link TermQuery} is the simplest.</li>
 * </ol>
 */
public class Module01_HelloLucene {

    public static void run() throws Exception {
        Console.header("Module 1 — Hello, Lucene!");

        // 1. A Directory holds the inverted index. ByteBuffersDirectory is an in-memory
        //    implementation — perfect for learning. In production you'd use FSDirectory.open(path).
        try (Directory directory = new ByteBuffersDirectory();
             // 2. An Analyzer tokenizes and normalizes text. StandardAnalyzer lowercases tokens
             //    and removes a small set of stop words.
             Analyzer analyzer = new StandardAnalyzer()) {

            // --- INDEXING PHASE ---------------------------------------------------------
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (Book book : SampleData.books()) {
                    writer.addDocument(toDocument(book));
                }
                writer.commit(); // make the additions visible to new readers
            }

            // --- SEARCHING PHASE --------------------------------------------------------
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // TermQuery looks up an exact token in a single field. Note the lower-case
                // "java" — StandardAnalyzer lower-cased terms while indexing, so the search
                // term must also be lower case. This is a *very* common source of confusion.
                Query query = new TermQuery(new Term("title", "java"));

                Console.section("Top 10 hits for TermQuery(title:java)");
                TopDocs hits = searcher.search(query, 10);
                System.out.println("totalHits = " + hits.totalHits);

                for (ScoreDoc sd : hits.scoreDocs) {
                    Document doc = searcher.storedFields().document(sd.doc);
                    System.out.printf("  score=%.3f  id=%s  title=%s%n",
                        sd.score, doc.get("id"), doc.get("title"));
                }
            }
        }
    }

    /**
     * Map a Book into a Lucene Document. The choice of Field type is the most important
     * decision you make as a Lucene user — it controls how that field can be searched.
     */
    private static Document toDocument(Book book) {
        Document doc = new Document();
        // StringField: indexed *verbatim* (no tokenization). Use for IDs, codes, enums.
        doc.add(new StringField("id", book.id, Field.Store.YES));
        // TextField: tokenized through the Analyzer. Use for natural-language text.
        doc.add(new TextField("title", book.title, Field.Store.YES));
        doc.add(new TextField("author", book.author, Field.Store.YES));
        doc.add(new TextField("description", book.description, Field.Store.YES));
        return doc;
    }
}
