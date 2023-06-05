package edu.mayo.kmdp.terms.components;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newIdAsPointer;

import edu.mayo.kmdp.terms.TermsContextAwareHrefBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermsSearcher {

  private static final Logger logger = LoggerFactory.getLogger(TermsSearcher.class);

  public static final String SF_LABEL = "label";
  public static final String SF_TAG = "tag";

  protected final StandardAnalyzer analyzer = new StandardAnalyzer();
  protected Directory searchIndex;


  public Answer<List<Pointer>> searchTerms(String searchQuery, TermsContextAwareHrefBuilder hrefBuilder) {
    try {
      var parser = new MultiFieldQueryParser(
          new String[]{SF_TAG, SF_LABEL}, analyzer);
      parser.setDefaultOperator(QueryParser.Operator.OR);
      var q = parser.parse(searchQuery);

      IndexReader reader = DirectoryReader.open(searchIndex);
      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs docs = searcher.search(q, IndexSearcher.getMaxClauseCount() - 1);
      ScoreDoc[] hits = docs.scoreDocs;

      List<Pointer> results = new ArrayList<>();
      // 4. display results
      for (ScoreDoc hit : hits) {
        Document d = searcher.doc(hit.doc);
        var l = d.get(SF_LABEL);
        var id = d.get(SF_TAG);
        var ptr = newIdAsPointer(id)
            .withName(l);
        if (hrefBuilder != null) {
          ptr.withHref(hrefBuilder.fromTermPointer(ptr));
        }
        results.add(ptr);
      }
      return Answer.of(results);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  public void index(Map<UUID, ConceptDefinitionComponent> tempConceptIndex) {
    Directory index = new ByteBuffersDirectory();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter w;
    try {
      w = new IndexWriter(index, config);
      for (ConceptDefinitionComponent cd : tempConceptIndex.values()) {
        addDoc(w, cd.getDisplay(), cd.getCode());
      }
      w.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    this.searchIndex = index;
  }

  private void addDoc(IndexWriter w, String label, String tag) throws IOException {
    Document doc = new Document();
    doc.add(new TextField(SF_LABEL, label, Field.Store.YES));
    doc.add(new TextField(SF_TAG, tag, Field.Store.YES));
    w.addDocument(doc);
  }

}
