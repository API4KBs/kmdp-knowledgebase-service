package edu.mayo.kmdp.terms;


import java.net.URI;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Spring component that determines the base URL of the Terms server
 * <p>
 * Used to construct relative API endpoint URLs
 */
@Component
public class TermsContextAwareHrefBuilder {

  private static final Logger logger = LoggerFactory.getLogger(TermsContextAwareHrefBuilder.class);

  /**
   * Determine the base URL used to construct full URLs
   *
   * @return the base URL the Terms application is deployed at
   */
  public String getHost() {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return null;
    }
  }

  public URI fromTerminologyPointer(Pointer p) {
    return URI.create(getHost()
        + "/terminologies/" + p.getTag() + "/versions/" + p.getVersionTag());
  }

  public URI fromTermPointer(Pointer p) {
    return URI.create(getHost()
        + "/terminologies/terms/" + p.getTag());
  }

  public URI fromTerm(String onto, String conceptId) {
    return URI.create(getHost() +
        "/terminologies/terms/" + onto + "/" + conceptId);
  }


  public String getCurrentURL() {
    return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
  }
}