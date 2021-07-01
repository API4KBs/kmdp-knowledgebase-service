package edu.mayo.kmdp.knowledgebase.extractors.dita.v1_3;


import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Stream.concat;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Extraction_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DITA_1_3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedExtractDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Component
@KPComponent
@KPSupport(DITA_1_3)
@KPOperation(Extraction_Task)
public class DITAConceptExtractor
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedExtractDirect {

  public static final UUID id = UUID.fromString("b0f268f0-719f-41c2-8fe2-b652b017b556");
  public static final String version = "1.0.0";

  public DITAConceptExtractor() {
    super(SemanticIdentifier.newId(id, version));
  }

  public DITAConceptExtractor(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DITA_1_3;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedExtractDirect(
      UUID operatorId,
      KnowledgeCarrier artifact,
      UUID rootFragmentId,
      String xParams) {
    // FUTURE: use rootFragmentId to restrict the extraction to a sub-tree of the document
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }
    return parse(artifact)
        .flatOpt(kc -> kc.as(Document.class))
        .map(this::doSelect)
        .map(wb -> AbstractCarrier.ofAst(wb)
            .withRepresentation(rep((KnowledgeRepresentationLanguage) null))
            .withAssetId(artifact.getAssetId()));
  }

  private Workbook doSelect(Document dox) {
    List<Entry> entries = collectEntries(dox);

    var workbook = initEmptyWorkbook();
    var sheet = workbook.getSheetAt(0);

    for (var j = 1; j <= entries.size(); j++) {
      var entry = entries.get(j - 1);
      var row = sheet.createRow(j);
      row.createCell(0).setCellValue(entry.getTerm());
      row.createCell(1).setCellValue(entry.getMapping());
      row.createCell(2).setCellValue(entry.getTerm());
      row.createCell(3).setCellValue(entry.isKey() ? "Key" : "Prop");
      row.createCell(4).setCellValue(entry.getValues());
    }

    return workbook;
  }

  private Workbook initEmptyWorkbook() {
    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("DEK Parameters");
    sheet.setColumnWidth(0, 6000);
    sheet.setColumnWidth(1, 6000);
    sheet.setColumnWidth(2, 6000);
    sheet.setColumnWidth(3, 6000);
    sheet.setColumnWidth(4, 6000);

    var header = sheet.createRow(0);
    header.createCell(0).setCellValue("Param (Value)");
    header.createCell(1).setCellValue("B2X");
    header.createCell(2).setCellValue("Label");
    header.createCell(3).setCellValue("Type");
    header.createCell(4).setCellValue("Options (Documentation)");
    return workbook;
  }

  private List<Entry> collectEntries(Document dox) {
    Map<String, List<Entry>> entryMap =
        extractEntriesFromDox(dox)
            .collect(Collectors.groupingBy(Entry::getGroupingId));
    return entryMap.values().stream()
        .map(list -> list.stream().reduce(Entry::merge))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());
  }

  private Stream<Entry> extractEntriesFromDox(Document dox) {
    NodeList props = new XPathUtil().xList(dox, "//@props");
    NodeList otherprops = new XPathUtil().xList(dox, "//@otherprops");
    NodeList keys = new XPathUtil().xList(dox, "//@keyref");
    return concat(
        concat(parseProps(props), parseProps(otherprops)),
        parseKeys(keys));
  }

  private Stream<Entry> parseKeys(NodeList keys) {
    return XMLUtil.asAttributeStream(keys)
        .map(Attr::getValue)
        .map(v -> new Entry(v, true));
  }

  private Stream<Entry> parseProps(NodeList props) {
    return XMLUtil.asAttributeStream(props)
        .map(Attr::getValue)
        .flatMap(this::parseProp);
  }

  private Stream<Entry> parseProp(String prop) {
    var p = Pattern.compile("\\w+(\\([\\w|\\d ]*\\))?");
    return p.matcher(prop).results()
        .map(res -> res.group(0))
        .flatMap(this::parseSingleProp);
  }

  private Stream<Entry> parseSingleProp(String s) {
    String tmp = s.trim();
    if (Util.isEmpty(s)) {
      return Stream.empty();
    }
    boolean hasValues = tmp.contains("(");
    String term = hasValues
        ? tmp.substring(0, s.indexOf('('))
        : tmp;
    String[] values =
        tmp.substring(tmp.indexOf('(') + 1, tmp.lastIndexOf(')'))
            .trim()
            .split("\\s");
    return Stream.of(new Entry(term, values, false));
  }

  public _applyLift parser() {
    return (kc, level, into, cfg) -> parse(kc);
  }

  public _applyLower serializer() {
    return (kc, level, into, cfg) -> serialize(kc);
  }

  protected Answer<KnowledgeCarrier> parse(KnowledgeCarrier wbCarrier) {
    switch (ParsingLevelSeries.asEnum(wbCarrier.getLevel())) {
      case Encoded_Knowledge_Expression:
      case Serialized_Knowledge_Expression:
        return Answer.of(wbCarrier.asString()
            .map(str -> str.replaceAll("<!DOCTYPE.*>", ""))
            .flatMap(XMLUtil::loadXMLDocument)
            .map(dox -> AbstractCarrier.ofTree(dox)
                .withRepresentation(rep(DITA_1_3, XML_1_1)).withAssetId(wbCarrier.getAssetId())));
      case Concrete_Knowledge_Expression:
        return Answer.of(wbCarrier);
      case Abstract_Knowledge_Expression:
      default:
        return Answer.unsupported();
    }
  }

  protected Answer<KnowledgeCarrier> serialize(KnowledgeCarrier wbCarrier) {
    return Answer.of(wbCarrier.as(Workbook.class)
        .map(wb -> {
          var baos = new ByteArrayOutputStream();
          try {
            wb.write(baos);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return baos.toByteArray();
        }).map(b -> AbstractCarrier.of(b)
            .withRepresentation(rep(null, XML_1_1, defaultCharset(), Encodings.DEFAULT))
            .withAssetId(wbCarrier.getAssetId())));
  }


  private static class Entry {

    private final String term;
    private final List<String> vals;
    private final boolean key;

    public Entry(String term, boolean isKey) {
      this(term, new String[0], isKey);
    }

    public Entry(String term, String[] values, boolean isKey) {
      this(term, Arrays.asList(values), isKey);
    }

    public Entry(String term, List<String> values, boolean isKey) {
      this.term = term;
      this.vals = values;
      this.key = isKey;
    }

    public String getTerm() {
      return term;
    }

    public String getMapping() {
      return "return \"" + getTerm() + "\";";
    }

    public String getValues() {
      return String.join(" ", vals);
    }

    public boolean isKey() {
      return key;
    }

    public static Entry merge(Entry e1, Entry e2) {
      Set<String> set = new HashSet<>();
      set.addAll(e1.vals);
      set.addAll(e2.vals);
      List<String> l = new ArrayList<>(set);
      Collections.sort(l);
      return new Entry(e1.getTerm(), l, e1.key);
    }

    public String getGroupingId() {
      return getTerm() + (key ? "-K" : "-P");
    }

  }


}