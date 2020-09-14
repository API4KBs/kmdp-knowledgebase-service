package edu.mayo.kmdp.knowledgebase.extractors.rdf;

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.JenaUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedExtract;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

public class SimplePivotExtractor
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedExtract {

  public static final UUID id = UUID.fromString("13881270-6556-4cb1-99b9-5a3dacff96f6");
  public static final String version = "1.0.0";

  private KnowledgeBaseApiInternal kbManager;

  public SimplePivotExtractor() {
    super(SemanticIdentifier.newId(id,version));
  }

  public SimplePivotExtractor(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

  String select = "SELECT ?Y WHERE { ?X ?p* ?Y }";

  @Override
  public Answer<KnowledgeCarrier> applyNamedExtract(UUID operatorId, UUID kbaseId,
      String versionTag, UUID rootAssetid, String xParams) {
    if (!operatorId.equals(this.getOperatorId().getUuid())) {
      return Answer.unsupported();
    }
    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .map(kc -> {
          KnowledgeCarrier kcc = (KnowledgeCarrier) kc.clone();
          if (kcc instanceof CompositeKnowledgeCarrier) {
            kcc.components()
                .filter(comp -> rootAssetid.equals(comp.getAssetId().getUuid()))
                .findFirst()
                .ifPresent(root -> pivot((CompositeKnowledgeCarrier) kcc, root.getAssetId()));
          }
          return kcc;
        });
  }

  private void pivot(CompositeKnowledgeCarrier kcc,
      ResourceIdentifier root) {
    KnowledgeCarrier clonedStruct = (KnowledgeCarrier) kcc.getStruct().clone();

    Model m = clonedStruct.as(Model.class).orElseThrow();

    ParameterizedSparqlString psq = new ParameterizedSparqlString(select);
    psq.setIri("?X", root.getVersionId().toString());
    psq.setIri("?p", DependencyTypeSeries.Imports.getReferentId().toString());

    Set<Resource> relatedClosure
        = new HashSet<>(JenaUtil.askQuery(m, psq.asQuery()));

    Model m1 = ModelFactory.createDefaultModel();
    m.listStatements()
        .filterKeep(s -> relatedClosure.contains(s.getSubject()))
        .forEachRemaining(m1::add);

    clonedStruct.setExpression(m1);
    kcc.setStruct(clonedStruct);

    List<KnowledgeCarrier> filteredComponents = kcc.getComponent().stream()
        .filter(kc -> relatedClosure
            .contains(ResourceFactory.createResource(kc.getAssetId().getVersionId().toString())))
        .collect(Collectors.toList());
    kcc.getComponent().clear();
    kcc.getComponent().addAll(filteredComponents);

    kcc.setRootId(root);
  }
}
