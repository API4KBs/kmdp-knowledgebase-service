package edu.mayo.kmdp.language.translators.surrogate.v2;

import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static edu.mayo.kmdp.util.Util.isUUID;

import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class SurrogateV2ToHTML {

  public Document transform(KnowledgeAsset knowledgeAsset) {
    return transform(knowledgeAsset, null);
  }

  public Document transform(KnowledgeAsset knowledgeAsset, Properties config) {
    String html = MakeHTML.makeHTML(knowledgeAsset, config);
    return Jsoup.parse(html);
  }

  public static class MakeHTML {

    protected MakeHTML() {
      // nothing to do
    }

    private static final class HTMLStyle extends ToStringStyle {

      final Map<String, String> redirects = new HashMap<>();

      public HTMLStyle(Properties props) {
        if (props != null) {
          props.forEach((s, t) -> redirects.put(s.toString(), t.toString()));
        }

        setContentStart("<table>" + System.lineSeparator() +
            "<tbody><tr><td>");

        setFieldSeparator("</td></tr>" + System.lineSeparator() + "<tr><td>");
        setFieldNameValueSeparator("</td><td>");

        setContentEnd("</td></tr>" + System.lineSeparator() + "</tbody></table>");

        setArrayContentDetail(true);
        setUseShortClassName(true);
        setUseClassName(false);
        setUseIdentityHashCode(false);
      }

      @Override
      public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
        if (value == null) {
          return;
        }
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
          return;
        }
        super.append(buffer, fieldName, value, fullDetail);
      }

      @Override
      public void appendDetail(StringBuffer buffer, String fieldName, Object value) {

        if (value instanceof Term) {
          appendTerm((Term) value, buffer);
          return;
        }

        if (value instanceof UUID) {
          buffer.append(value);
          return;
        }

        if (value instanceof URI) {
          buffer.append(a(value.toString(), shorten((URI) value)));
          return;
        }

        if (value instanceof SemanticIdentifier) {
          appendIdentifier((SemanticIdentifier) value, buffer);
          return;
        }

        if (value instanceof Date) {
          buffer.append(DateTimeUtil.serializeAsDateTime((Date) value));
        }

        if (value.getClass().getName().startsWith("java.lang")) {
          super.appendDetail(buffer, fieldName, value);
          return;
        }

        buffer.append(ReflectionToStringBuilder.toString(value, this));
      }

      private String shorten(URI uri) {
        try {
          var shortUri = new URI(uri.getScheme(),
              uri.getAuthority(),
              uri.getPath(),
              shorten(uri.getQuery()),
              shorten(uri.getFragment()));
          return shortUri.toString();
        } catch (URISyntaxException e) {
          return uri.toString();
        }
      }

      private String shorten(String str) {
        if (str == null) {
          return null;
        }
        if (str.length() > 50) {
          return str.substring(0, 50) + "...";
        }
        return str;
      }

      private void appendIdentifier(SemanticIdentifier id, StringBuffer buffer) {
        String idRef = id.getVersionId() != null
            ? id.getVersionId().toString()
            : id.getResourceId().toString();
        if (idRef.startsWith("http")) {
          idRef = a(idRef, isUUID(id.getTag()) ? id.asKey().toString() : idRef);
        }
        URI type = null;
        if (id instanceof Pointer) {
          type = ((Pointer) id).getType();
        }
        buffer.append(labeled(idRef, id.getName(), type));
      }

      private String labeled(String ref, String name, URI type) {
        var sb = new StringBuilder(ref);
        if (isNotEmpty(name) || type != null) {
          sb.append(" | ");
          sb.append(name != null ? name : "");
          sb.append(" (");
          sb.append(type != null ? NameUtils.getTrailingPart(type.toString()) : "");
          sb.append(") ");
          sb.append(" | ");
        }
        return sb.toString();
      }

      private String a(String url, String text) {
        var effectiveUrl = url;
        for (var redirect : redirects.entrySet()) {
          if (url.startsWith(redirect.getKey())) {
            effectiveUrl = effectiveUrl.replace(redirect.getKey(), redirect.getValue());
          }
        }
        return "<a href=\"" + effectiveUrl + "\">" + text + "</a>";
      }

      private void appendTerm(Term trm, StringBuffer buffer) {
        String t = trm.getLabel();
        if (trm.getReferentId() != null && trm.getReferentId().toString().startsWith("http")) {
          t = a(trm.getReferentId().toString(), t);
        } else if (trm.getConceptId() != null) {
          t = a(trm.getConceptId().toString().replace('#','/'), t);
        }
        buffer.append(t);
      }

      @Override
      protected void appendDetail(StringBuffer buffer, String fieldName, Collection<?> coll) {
        coll.forEach(item -> {
          if (item instanceof Term) {
            appendTerm((Term) item, buffer);
            buffer.append(" | ");
            return;
          }

          buffer.append(ReflectionToStringBuilder.toString(item, this));
        });
      }
    }

    public static String makeHTML(Object object) {
      return makeHTML(object, null);
    }

    public static String makeHTML(Object object, Properties config) {
      return ReflectionToStringBuilder.toString(object, new HTMLStyle(config));
    }
  }
}
