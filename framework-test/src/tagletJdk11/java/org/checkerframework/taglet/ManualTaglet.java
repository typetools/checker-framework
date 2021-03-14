// Keep this file in sync with
// ../../../../../taglet/java/org/checkerframework/taglet/ManualTaglet.java .

package org.checkerframework.taglet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Taglet;

/**
 * A taglet for processing the {@code @checker_framework.manual} javadoc block tag, which inserts
 * references to the Checker Framework manual into javadoc.
 *
 * <p>The {@code @checker_framework.manual} tag is used as follows:
 *
 * <ul>
 *   <li>{@code @checker_framework.manual #} expands to a top-level link to the Checker Framework
 *       manual
 *   <li>{@code @checker_framework.manual #anchor text} expands to a link with some text to a
 *       particular part of the manual
 * </ul>
 */
public class ManualTaglet implements Taglet {

  private static final String NAME = "checker_framework.manual";

  @Override
  public String getName() {
    return NAME;
  }

  private final EnumSet<Location> allowedSet = EnumSet.allOf(Location.class);

  @Override
  public Set<Taglet.Location> getAllowedLocations() {
    return allowedSet;
  }

  @Override
  public boolean isInlineTag() {
    return false;
  }

  /**
   * Formats a link, given an array of tokens.
   *
   * @param parts the array of tokens
   * @return a link to the manual top-level if the array size is one, or a link to a part of the
   *     manual if it's larger than one
   */
  private String formatLink(String[] parts) {
    String anchor, text;
    if (parts.length < 2) {
      anchor = "";
      text = "Checker Framework";
    } else {
      anchor = parts[0];
      text = parts[1];
    }
    return String.format("<A HREF=\"https://checkerframework.org/manual/%s\">%s</A>", anchor, text);
  }

  /**
   * Formats the {@code @checker_framework.manual} tag, prepending the tag header to the tag
   * content.
   *
   * @param text the tag content
   * @return the formatted tag
   */
  private String formatHeader(String text) {
    return String.format("<DT><B>See the Checker Framework Manual:</B><DD>%s<BR>", text);
  }

  @Override
  public String toString(List<? extends DocTree> tags, Element element) {
    if (tags.isEmpty()) {
      return "";
    }
    StringJoiner sb = new StringJoiner(", ");
    for (DocTree t : tags) {
      String text = getText(t);
      String[] split = text.split(" ", 2);
      sb.add(formatLink(split));
    }
    return formatHeader(sb.toString());
  }

  static String getText(DocTree dt) {
    return new SimpleDocTreeVisitor<String, Void>() {
      @Override
      public String visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
        for (DocTree dt : node.getContent()) {
          return dt.accept(this, null);
        }
        return "";
      }

      @Override
      public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
        for (DocTree dt : node.getContent()) {
          return dt.accept(this, null);
        }
        return "";
      }

      @Override
      public String visitText(TextTree node, Void p) {
        return node.getBody();
      }

      @Override
      protected String defaultAction(DocTree node, Void p) {
        return "";
      }
    }.visit(dt, null);
  }
}
