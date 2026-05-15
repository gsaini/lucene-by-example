package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollectorManager;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 6: FacetsCollectorManager yields the expected category counts")
class Module06_FacetingIT {

    private Directory indexDir;
    private Directory taxoDir;
    private StandardAnalyzer analyzer;
    private FacetsConfig facetsConfig;
    private DirectoryReader reader;
    private TaxonomyReader taxoReader;
    private IndexSearcher searcher;

    @BeforeEach
    void setUp() throws Exception {
        indexDir = new ByteBuffersDirectory();
        taxoDir  = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        facetsConfig = new FacetsConfig();
        facetsConfig.setHierarchical("Category", false);
        facetsConfig.setHierarchical("Decade", false);

        try (IndexWriter writer = new IndexWriter(indexDir, new IndexWriterConfig(analyzer));
             TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir)) {
            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));
                doc.add(new FacetField("Category", book.category));
                doc.add(new FacetField("Decade", decadeOf(book.year)));
                writer.addDocument(facetsConfig.build(taxoWriter, doc));
            }
            writer.commit();
        }
        reader = DirectoryReader.open(indexDir);
        taxoReader = new DirectoryTaxonomyReader(taxoDir);
        searcher = new IndexSearcher(reader);
    }

    @AfterEach
    void tearDown() throws Exception {
        reader.close();
        taxoReader.close();
        analyzer.close();
        indexDir.close();
        taxoDir.close();
    }

    @Test
    @DisplayName("Category facet counts match the sample data distribution")
    void category_facet_counts() throws Exception {
        FacetResult result = collectFacets("Category");

        Map<String, Integer> counts = Arrays.stream(result.labelValues)
            .collect(Collectors.toMap(lv -> lv.label, lv -> lv.value.intValue()));

        assertThat(counts).containsEntry("Programming", 5)
                          .containsEntry("Search", 3)
                          .containsEntry("Architecture", 2);
    }

    @Test
    @DisplayName("Decade facet counts buckets correctly")
    void decade_facet_counts() throws Exception {
        FacetResult result = collectFacets("Decade");

        Map<String, Integer> counts = Arrays.stream(result.labelValues)
            .collect(Collectors.toMap(lv -> lv.label, lv -> lv.value.intValue()));

        // Years in sample: 2010,2018,2006,2008,2017,1999,2010,2008,2016,2003
        assertThat(counts).containsEntry("1990s", 1)
                          .containsEntry("2000s", 4)
                          .containsEntry("2010s", 5);
    }

    @Test
    @DisplayName("Top-N limits how many label/value pairs come back")
    void top_n_limits_returned_labels() throws Exception {
        FacetResult result = collectFacets("Category", 2);
        assertThat(result.labelValues).hasSizeLessThanOrEqualTo(2);

        // The two largest categories must be present.
        assertThat(Arrays.stream(result.labelValues).map(lv -> lv.label).toList())
            .containsExactlyInAnyOrder("Programming", "Search");
    }

    private FacetResult collectFacets(String dim) throws Exception {
        return collectFacets(dim, 10);
    }

    private FacetResult collectFacets(String dim, int topN) throws Exception {
        FacetsCollectorManager fcm = new FacetsCollectorManager();
        FacetsCollectorManager.FacetsResult res =
            FacetsCollectorManager.search(searcher, MatchAllDocsQuery.INSTANCE, 10, fcm);
        FastTaxonomyFacetCounts facets =
            new FastTaxonomyFacetCounts(taxoReader, facetsConfig, res.facetsCollector());
        return facets.getTopChildren(topN, dim);
    }

    private static String decadeOf(int year) {
        return ((year / 10) * 10) + "s";
    }
}
