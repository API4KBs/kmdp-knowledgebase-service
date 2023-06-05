package edu.mayo.kmdp.language.validators;

import static edu.mayo.kmdp.util.PropertiesUtil.pEnum;
import static edu.mayo.kmdp.util.PropertiesUtil.parseProperties;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Explainer.newOutcomeProblem;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;

import edu.mayo.kmdp.util.CharsetEncodingUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcome;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.zalando.problem.Problem;

/**
 * Applies validation rules to the Artifact wrapped in a {@link KnowledgeCarrier}
 * <p>
 * Requires the Carrier to have an AssetID, and an explicit {@link org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel},
 * or the Validation will fail
 * <p>
 * Applies to Carriers which are either Encoded (Binary) or Serialized (String). Succeeds trivially
 * if applied to artifacts that have already been parsed. Empty carriers (i.e. no content) are also
 * trivially validated.
 * <p>
 * Custom configuration: Supports two modalities, that look for one (default) vs all the
 * non-conformant characters.
 */
public class ASCIIValidator extends AbstractValidator {

  /**
   * xConfig modality. Use as "Mode=ANY" or "Mode=ALL"
   */
  public enum Mode {
    /**
     * Will check for ANY ONE invalid character, and fail fast
     */
    ANY,
    /**
     * Will process the entire String, checking for ALL invalid character(s)
     */
    ALL
  }

  /**
   * Unique "asset" identifier for the platform component implemented by this Java class
   */
  public static final ResourceIdentifier OPERATOR_ID =
      newId(UUID.fromString("e24040eb-9994-4cd4-929d-31a0b3d75d28"), "1.0.0");

  @Override
  public ResourceIdentifier getOperatorId() {
    return OPERATOR_ID;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return null;
  }

  @Override
  protected boolean isSupported(SyntacticRepresentation representation) {
    return true;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(null, null, StandardCharsets.US_ASCII, Encodings.DEFAULT),
        rep(null, null, StandardCharsets.UTF_8, Encodings.DEFAULT),
        rep(null, null, Charset.forName("windows-1252"), Encodings.DEFAULT)
    );
  }

  @Override
  public KnowledgeResourceOutcome getValidationType() {
    return KnowledgeResourceOutcomeSeries.Well_Formedness;
  }

  /**
   * Applies validation rules to the Artifact wrapped in a {@link KnowledgeCarrier}
   *
   * @param carrier the Carrier wrapping the Artifact to be validated
   * @param xConfig additional config parameters
   * @return the validation results
   */
  @Override
  protected Answer<Void> validateComponent(final KnowledgeCarrier carrier, String xConfig) {
    if (carrier.getLevel() == null || carrier.getAssetId() == null) {
      Answer<Void> ans = Answer.unsupported();
      ans.withExplanationMessage("KnowledgeCarrier does not have all the required metadata");
      return ans;
    }

    switch (ParsingLevelSeries.asEnum(carrier.getLevel())) {
      case Serialized_Knowledge_Expression:
      case Encoded_Knowledge_Expression:
        return carrier.asString()
            .map(content -> innerValidate(content, carrier, parseProperties(xConfig)))
            .map(p -> Answer.succeed().withExplanationDetail(p))
            .orElseGet(() -> Answer.succeed()
                .withExplanationDetail(
                    newOutcomeProblem(getValidationType(), Severity.INF)
                    .withDetail("No content to validate").build()));
      case Abstract_Knowledge_Expression:
      case Concrete_Knowledge_Expression:
      default:
        return Answer.succeed().withExplanationMessage(
            "Not Applicable for Level " + carrier.getLevel().getName());
    }
  }

  /**
   * Validates a String for the presence of non-ASCII characters
   *
   * @param str the serialized Knowledge Artifact to validate
   * @param kc  the String wrapper, providing context
   * @param cfg configuration properties, possibly emtpy
   * @return a {@link Problem} with validation result details
   */
  private Problem innerValidate(
      final String str, final KnowledgeCarrier kc, final Properties cfg) {
    Mode mode = pEnum(Mode.class.getSimpleName(), cfg, Mode::valueOf).orElse(Mode.ANY);

    boolean valid;
    String msg;

    switch (mode) {
      // scan the string to isolate problematic characters, reporting them
      case ALL:
        byte[] bytes = str.getBytes();
        List<Integer> index = new LinkedList<>();
        for (int j = 0; j < bytes.length; j++) {
          if (!CharsetEncodingUtil.isASCII(bytes[j])) {
            index.add(j);
          }
        }
        valid = index.isEmpty();
        msg = valid
            ? "All characters in ASCII core"
            : "Non-ASCII characters in " + kc.getLabel() + " at index " + index
                + ", hints: " + hint(str, index);
        break;

      // report a problem upon finding the first character
      case ANY:
      default:
        valid = str.chars().parallel().allMatch(CharsetEncodingUtil::isASCII);
        msg = valid
            ? "All characters in ASCII core"
            : "Non-ASCII characters detected in serialized artifact" + kc.getLabel();
        break;
    }

    return format(
        kc.getAssetId().getVersionId(),
        valid ? Severity.OK : Severity.FATAL,
        "ASCII Core Conformance",
        msg);

  }

  private String hint(String str, List<Integer> index) {
    return index.stream()
        .map(i -> str.substring(Math.max(0, i-10), Math.min(str.length(), i+10)))
        .collect(Collectors.joining("|"));
  }
}
