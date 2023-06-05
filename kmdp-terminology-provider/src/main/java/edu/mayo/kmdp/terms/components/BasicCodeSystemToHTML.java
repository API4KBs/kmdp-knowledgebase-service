package edu.mayo.kmdp.terms.components;


import static edu.mayo.kmdp.util.Util.isEmpty;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import java.nio.charset.Charset;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

/**
 * Simple translator FHIR[CodeSystem] -> HTML compatible with the concept URI strategy
 * <p>
 * As the requirements grow, consider using a templated approach, using either KMDP's Mustache, or
 * HAPI's Thymeleaf
 */
public class BasicCodeSystemToHTML implements _applyTransrepresent {

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier src,
      String xAccept, String xParams) {
    return Answer.ofTry(src.as(CodeSystem.class).map(cs -> translate(cs, xParams)))
        .map(html -> of(html)
            .withRepresentation(rep(HTML, TXT, Charset.defaultCharset()))
            .withAssetId(src.getAssetId())
            .withLabel(src.getLabel()));
  }

  private String translate(CodeSystem cs, String baseUrl) {
    StringBuilder sb = new StringBuilder().append("<html><head/><body>");
    sb.append("<h1>").append(cs.getTitle()).append("</h1>");
    sb.append("<table border='0'>");
    for (var concept : cs.getConcept()) {
      sb.append("<tr>");
      sb.append(render(concept, baseUrl));
      sb.append("</tr>");
    }
    sb.append("</table>");
    sb.append("</body></html>");
    return sb.toString();
  }

  private static final String CONCEPT_PATTERN = ""
      + "<td>%s</td> <td><b>%s</b></td> <td><a href='%s'>%s</a></td>";

  private String render(
      ConceptDefinitionComponent concept, String baseUrl) {
    String cd = concept.getCode();
    String href = isEmpty(baseUrl)
        ? concept.getDefinition()
        : baseUrl + "/terminologies/terms/" + cd;
    return String.format(CONCEPT_PATTERN,
        concept.getCode(),
        concept.getDisplay(),
        href,
        concept.getDefinition());
  }
}