package edu.mayo.kmdp.language.translators.oas.v_2;

import static java.util.Objects.requireNonNull;

import edu.mayo.kmdp.util.FileUtil;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class SwaggerToMessageBundleTranscreator {

  private SwaggerToMessageBundleTranscreator() {
    // function only
  }

  public static String translate(byte[] source, String baseCode) {
    var sb = new StringBuilder();
    Swagger swag = new SwaggerParser().parse(new String(source));

    sb.append("# ").append("Init").append("\n");
    sb.append(baseCode).append("-").append("000.A=").append("\n\n\n");

    Map<String, Path> paths = swag.getPaths();
    List<String> idx = new ArrayList<>(paths.keySet());
    paths.forEach((pathStr, path) -> {
      addOperation(path.getHead(), idx, baseCode, HttpMethod.HEAD, pathStr, sb);
      addOperation(path.getGet(), idx, baseCode, HttpMethod.GET, pathStr, sb);
      addOperation(path.getPut(), idx, baseCode, HttpMethod.PUT, pathStr, sb);
      addOperation(path.getPost(), idx, baseCode, HttpMethod.POST, pathStr, sb);
      addOperation(path.getDelete(), idx, baseCode, HttpMethod.DELETE, pathStr, sb);
      addOperation(path.getPatch(), idx, baseCode, HttpMethod.PATCH, pathStr, sb);
      addOperation(path.getOptions(), idx, baseCode, HttpMethod.OPTIONS, pathStr, sb);
    });

    sb.append("# ").append("Shutdown").append("\n");
    sb.append(baseCode).append("-").append("900.A=").append("\n");

    return sb.toString();
  }

  private static void addOperation(Operation op, List<String> idx,
      String baseCode, HttpMethod method, String pathStr, StringBuilder sb) {
    if (op == null) {
      return;
    }
    sb.append("# ").append(method).append(" ")
        .append(op.getOperationId()).append(" / ");
    IntStream.range(0, op.getParameters().size())
        .forEach(j -> sb.append(op.getParameters().get(j).getName()).append(" | "));
    sb.append("\n");
    sb.append(baseCode).append("-")
        .append(String.format("%02d%d", idx.indexOf(pathStr) + 1, map(method))).append(".A");
    sb.append("=");
    sb.append("API Call ").append(op.getOperationId()).append(" /");
    IntStream.range(0, op.getParameters().size())
        .forEach(j -> sb.append(" ")
            .append(op.getParameters().get(j).getName())
            .append(" : ").append("{").append(j).append("} |"));
    sb.append("\n\n");
  }

  private static int map(HttpMethod get) {
    switch (get) {
      case HEAD:
        return 1;
      case GET:
        return 2;
      case PUT:
        return 3;
      case POST:
        return 4;
      case DELETE:
        return 5;
      case PATCH:
        return 6;
      case OPTIONS:
        return 7;
      default:
        return 0;
    }
  }

  /**
   * Run manually to generate templates for API log message bundles
   * @param args
   * @throws URISyntaxException
   */
  public static void main(String[] args) throws URISyntaxException {
    var base = "/openapi/v3/org/omg/spec/api4kp/5.0.0/";
    var tgtURI = requireNonNull(SwaggerToMessageBundleTranscreator.class.getResource("/")).toURI();
    var tgt = new File(tgtURI).getParentFile().getAbsolutePath();

    Map.of(
        "knowledgeArtifactRepository", "KART",
        "knowledgeAssetRepository", "KARS"
    ).forEach((api, code) ->
        Optional.of(api)
            .map(x -> base + x + ".swagger.yaml")
            .map(SwaggerToMessageBundleTranscreator.class::getResource)
            .flatMap(FileUtil::readBytes)
            .map(bytes -> translate(bytes, code))
            .map(s -> new ByteArrayInputStream(s.getBytes()))
            .ifPresent(is -> FileUtil.copyTo(
                is,
                new File(tgt + File.separator + api + "Messages.properties.default"))));
  }
}