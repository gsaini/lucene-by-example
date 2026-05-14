package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 6 — Faceting (counts of results grouped by a field).
 * <p>
 * Faceting is the "5 results in Programming, 3 in Search, 2 in Architecture" sidebar you see
 * on every e-commerce site. Lucene supports it via a separate <em>taxonomy index</em> that
 * stores the category hierarchy, and a {@link FacetsCollector} that piggy-backs on a normal
 * search to count category memberships.
 * <p>
 * The flow:
 * <ol>
 *   <li>While indexing, add a {@link FacetField} for each facet value and let {@link FacetsConfig}
 *       rewrite the document so the {@link TaxonomyWriter} can record the category.</li>
 *   <li>While searching, run your normal {@link Query} through {@link FacetsCollector#search}.</li>
 *   <li>Ask {@link FastTaxonomyFacetCounts} for the top categories.</li>
 * </ol>
 */
public class Module06_Faceting {

    public static void run() throws Exception {
        Console.header("Module 6 — Faceting");

        try (Directory indexDir = new ByteBuffersDirectory();
             Directory taxoDir  = new ByteBuffersDirectory();
             StandardAnalyzer analyzer = new StandardAnalyzer()) {

            FacetsConfig facetsConfig = new FacetsConfig();
            // Two facet dimensions: a category and a decade. Both are flat hierarchies here,
            // but FacetsConfig supports nested paths too (e.g. "Programming/JVM/Java").
            facetsConfig.setHierarchical("Category", false);
            facetsConfig.setHierarchical("Decade", false);

            buildIndex(indexDir, taxoDir, analyzer, facetsConfig);

            try (DirectoryReader reader = DirectoryReader.open(indexDir);
                 TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir)) {

                IndexSearcher searcher = new IndexSearcher(reader);
                FacetsCollector fc = new FacetsCollector();

                // Run a normal search but route hits to the FacetsCollector. Use any Query here —
                // a MatchAllDocsQuery is the simplest, equivalent to "no filter applied".
                Query q = new MatchAllDocsQuery();
                FacetsCollector.search(searcher, q, /* topN hits */ 10, fc);

                FastTaxonomyFacetCounts facets =
                    new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

                Console.section("Top categories");
                printFacet(facets.getTopChildren(10, "Category"));

                Console.section("Top decades");
                printFacet(facets.getTopChildren(10, "Decade"));
            }
        }
    }

    private static void printFacet(FacetResult result) {
        if (result == null) {
            System.out.println("  (no values)");
            return;
        }
        System.out.println("  dim = " + result.dim + ", total docs = " + result.value);
        for (var lv : result.labelValues) {
            System.out.printf("    %-20s %s%n", lv.label, lv.value);
        }
    }

    private static void buildIndex(Directory indexDir, Directory taxoDir,
                                   StandardAnalyzer analyzer,
                                   FacetsConfig facetsConfig) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(indexDir, cfg);
             TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir)) {

            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));

                // Add facet fields. FacetsConfig.build() rewrites the document so the
                // facet machinery can index its bookkeeping fields alongside the user fields.
                doc.add(new FacetField("Category", book.category));
                doc.add(new FacetField("Decade", decadeOf(book.year)));

                writer.addDocument(facetsConfig.build(taxoWriter, doc));
            }
            writer.commit();
        }
    }

    private static String decadeOf(int year) {
        int decade = (year / 10) * 10;
        return decade + "s";
    }
}
