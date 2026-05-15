package com.example.lucene.support;

import com.example.lucene.util.Book;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * In-memory book index used by most of the integration tests.
 * <p>
 * Builds one Document per {@link Book} with every field a test could want:
 * StringField id/category for exact match, TextField title/author/description for full-text search,
 * IntPoint/DoublePoint for range queries, NumericDocValuesField/DoubleDocValuesField for sorts,
 * SortedDocValuesField for string sorts, StoredField for retrieval. Tests pick whichever
 * combination they need.
 */
public final class BookIndex implements Closeable {

    private final Directory directory;
    private final Analyzer analyzer;
    private final DirectoryReader reader;
    private final IndexSearcher searcher;

    private BookIndex(Directory directory, Analyzer analyzer, DirectoryReader reader) {
        this.directory = directory;
        this.analyzer = analyzer;
        this.reader = reader;
        this.searcher = new IndexSearcher(reader);
    }

    public static BookIndex withSampleBooks() throws IOException {
        return withBooks(SampleData.books());
    }

    public static BookIndex withBooks(List<Book> books) throws IOException {
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            for (Book book : books) {
                writer.addDocument(toDocument(book));
            }
            writer.commit();
        }
        return new BookIndex(directory, analyzer, DirectoryReader.open(directory));
    }

    public IndexSearcher searcher() { return searcher; }
    public DirectoryReader reader() { return reader; }
    public Analyzer analyzer()      { return analyzer; }
    public Directory directory()    { return directory; }

    /** Convenience: retrieve a stored field for a hit's docId. */
    public String stored(int docId, String field) throws IOException {
        return searcher.storedFields().document(docId).get(field);
    }

    @Override
    public void close() throws IOException {
        try { reader.close();   } catch (Exception ignored) {}
        try { analyzer.close(); } catch (Exception ignored) {}
        try { directory.close();} catch (Exception ignored) {}
    }

    private static Document toDocument(Book book) {
        Document doc = new Document();
        doc.add(new StringField("id", book.id, Field.Store.YES));
        doc.add(new StringField("category", book.category, Field.Store.YES));
        doc.add(new SortedDocValuesField("category_sort", new BytesRef(book.category)));

        doc.add(new TextField("title", book.title, Field.Store.YES));
        doc.add(new TextField("author", book.author, Field.Store.YES));
        doc.add(new TextField("description", book.description, Field.Store.YES));

        doc.add(new IntPoint("year", book.year));
        doc.add(new NumericDocValuesField("year", book.year));
        doc.add(new StoredField("year_stored", book.year));

        doc.add(new DoublePoint("rating", book.rating));
        doc.add(new DoubleDocValuesField("rating", book.rating));
        doc.add(new StoredField("rating_stored", book.rating));
        return doc;
    }
}
