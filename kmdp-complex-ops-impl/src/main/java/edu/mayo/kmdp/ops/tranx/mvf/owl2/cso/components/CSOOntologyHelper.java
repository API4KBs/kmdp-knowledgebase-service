package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components;

import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSO_NS_SNAPSHOT;

import java.net.URL;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class CSOOntologyHelper {


  public static OWLOntology initOntology(OWLOntologyManager om) throws OWLOntologyCreationException {
    var onto = om.createOntology(new OWLOntologyID(
        IRI.create("http://ontology.mayo.edu/ontologies/clinicalvernacular/"),
        IRI.create("http://ontology.mayo.edu/ontologies/SNAPSHOT/clinicalvernacular/")
    ));
    var importDecl = om.getOWLDataFactory()
        .getOWLImportsDeclaration(
            IRI.create(CSO_NS_SNAPSHOT));
    om.applyChange(new AddImport(onto, importDecl));
    return onto;
  }

  public static Optional<OWLOntology> loadCSO(String csoURL, OWLOntologyManager om) {
    if (csoURL.isEmpty()) {
      return Optional.empty();
    }
    try {
      URL loc = new URL(csoURL);
      return Optional.ofNullable(
          om.loadOntologyFromOntologyDocument(loc.openStream()));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static OWLOntologyManager newCSOOntologyManager() {
    var mgr = OWLManager.createOWLOntologyManager();

    OWLOntologyLoaderConfiguration loaderCfg = new OWLOntologyLoaderConfiguration() {
      @Override
      public boolean isIgnoredImport(@Nonnull IRI iri) {
        return true;
      }

      @Override
      public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return MissingImportHandlingStrategy.SILENT;
      }
    };
    mgr.setOntologyLoaderConfiguration(loaderCfg);

    return mgr;
  }


}
