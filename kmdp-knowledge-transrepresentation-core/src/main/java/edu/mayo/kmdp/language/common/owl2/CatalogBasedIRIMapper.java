package edu.mayo.kmdp.language.common.owl2;

import edu.mayo.kmdp.util.CatalogBasedURIResolver;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.xml.sax.InputSource;

public class CatalogBasedIRIMapper implements OWLOntologyIRIMapper {

  protected transient CatalogBasedURIResolver resolver;

  protected URI catalogURI;

  public CatalogBasedIRIMapper(URI catalogURI) {
    this.catalogURI = catalogURI;
  }

  public CatalogBasedIRIMapper(String catalogURI) {
    this(URI.create(catalogURI));
  }

  @Nullable
  @Override
  public IRI getDocumentIRI(IRI ontologyIRI) {
    String iriStr = ontologyIRI.getIRIString();
    InputSource resolved = getResolver().resolveEntity(iriStr, null);
    return Optional.ofNullable(resolved)
        .map(InputSource::getSystemId)
        .map(IRI::create)
        .orElse(ontologyIRI);
  }

  private CatalogBasedURIResolver getResolver() {
    if (resolver == null) {
      resolver = new CatalogBasedURIResolver(catalogURI);
    }
    return resolver;
  }
}