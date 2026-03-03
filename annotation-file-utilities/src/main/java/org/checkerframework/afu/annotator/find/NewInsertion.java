package org.checkerframework.afu.annotator.find;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;

public class NewInsertion extends TypedInsertion {
  private static final Pattern qualifiers = Pattern.compile("(?:\\w++\\.)*+");

  /** If true, the type will be qualified with the name of the superclass. */
  protected boolean qualifyType;

  /**
   * Construct a NewInsertion.
   *
   * <p>If "new" already exists in the initializer, then pass a {@link DeclaredType} thats name is
   * the empty String. This will only insert an annotation on the existing type.
   *
   * <p>To insert the annotation along with "new" and the type (for example, {@code @Anno new Type[]
   * \{...\}}), set the name to the type to insert. This can be done either before calling this
   * constructor, or by modifying the return value of {@link #getType()}.
   *
   * @param type the type to use when inserting the receiver
   * @param criteria where to insert the text
   * @param innerTypeInsertions the inner types to go on this receiver
   */
  public NewInsertion(Type type, Criteria criteria, List<Insertion> innerTypeInsertions) {
    super(type, criteria, innerTypeInsertions);
    annotationsOnly = false;
    qualifyType = false;
  }

  @Override
  protected String getText(boolean abbreviate) {
    if (annotationsOnly || type.getKind() != Type.Kind.ARRAY) {
      StringBuilder b = new StringBuilder();
      List<String> annotations = type.getAnnotations();
      if (annotations.isEmpty()) {
        return "";
      }
      for (String a : annotations) {
        b.append(' ').append(a); // initial space removed below
      }
      AnnotationInsertion aIns =
          new AnnotationInsertion(b.substring(1), getCriteria(), isSeparateLine());
      String result = aIns.getText(abbreviate);
      // This is a hack.  There might be other side effects that are needed too.
      // We should avoid making temporary Insertions, due to the design of this program.
      packageNames.addAll(aIns.getPackageNames());
      return result;
    } else {
      DeclaredType baseType = getBaseType();
      String result = typeToString(type, abbreviate);
      if (!baseType.getName().isEmpty()) {
        // First, temporarily strip off any qualifiers.
        Matcher matcher = qualifiers.matcher(result);
        String prefix = "";
        if (matcher.find() && matcher.start() == 0) {
          prefix = result.substring(0, matcher.end());
          result = result.substring(matcher.end());
        }
        // If the variable name preceded the array brackets in the
        // source, extract it from the result.
        if (qualifyType) {
          for (DeclaredType t = baseType; t != null; t = t.getInnerType()) {
            result += t.getName() + ".";
          }
        }
        // Finally, prepend extracted qualifiers.
        result = prefix + result;
      }
      result = "new " + result;
      return result;
    }
  }

  /**
   * If {@code true}, qualify {@code type} with the name of the superclass. This will only happen if
   * a "new" is inserted.
   */
  public void setQualifyType(boolean qualifyType) {
    this.qualifyType = qualifyType;
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    if ((precedingChar == '.' || precedingChar == '(') && getBaseType().getName().isEmpty()) {
      // If only the annotation is being inserted then don't insert a
      // space if it's immediately after a '.' or '('
      return false;
    }
    return super.addLeadingSpace(gotSeparateLine, pos, precedingChar);
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.NEW;
  }
}
