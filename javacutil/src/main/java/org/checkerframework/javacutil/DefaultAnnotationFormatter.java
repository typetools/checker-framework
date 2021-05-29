package org.checkerframework.framework.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.javacutil.BugInCF;

/** A utility for converting AnnotationMirrors to Strings. It omits full package names. */
public class DefaultAnnotationFormatter implements AnnotationFormatter {

  /**
   * Returns true if, by default, anno should not be printed.
   *
   * @see org.checkerframework.framework.qual.InvisibleQualifier
   * @return true if anno's declaration was qualified by InvisibleQualifier
   */
  public static boolean isInvisibleQualified(AnnotationMirror anno) {
    TypeElement annoElement = (TypeElement) anno.getAnnotationType().asElement();
    return annoElement.getAnnotation(InvisibleQualifier.class) != null;
  }

  /**
   * Creates a String of each annotation in annos separated by a single space character and
   * terminated by a space character, obeying the printInvisible parameter.
   *
   * @param annos a collection of annotations to print
   * @param printInvisible whether or not to print "invisible" annotation mirrors
   * @return the list of annotations converted to a String
   */
  @Override
  @SideEffectFree
  public String formatAnnotationString(
      Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
    StringBuilder sb = new StringBuilder();
    for (AnnotationMirror obj : annos) {
      if (obj == null) {
        throw new BugInCF(
            "AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror");
      }
      if (isInvisibleQualified(obj) && !printInvisible) {
        continue;
      }
      formatAnnotationMirror(obj, sb);
      sb.append(" ");
    }
    return sb.toString();
  }

  /**
   * Returns the string representation of a single AnnotationMirror, without showing full package
   * names.
   *
   * @param anno the annotation mirror to convert
   * @return the string representation of a single AnnotationMirror, without showing full package
   *     names
   */
  @Override
  @SideEffectFree
  public String formatAnnotationMirror(AnnotationMirror anno) {
    StringBuilder sb = new StringBuilder();
    formatAnnotationMirror(anno, sb);
    return sb.toString();
  }

  /** A helper method to output a single AnnotationMirror, without showing full package names. */
  protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
    sb.append("@");
    sb.append(am.getAnnotationType().asElement().getSimpleName());
    Map<ExecutableElement, AnnotationValue> args = removeDefaultValues(am.getElementValues());
    if (!args.isEmpty()) {
      sb.append("(");
      boolean oneValue = false;
      if (args.size() == 1) {
        Map.Entry<ExecutableElement, AnnotationValue> first = args.entrySet().iterator().next();
        if (first.getKey().getSimpleName().contentEquals("value")) {
          formatAnnotationMirrorArg(first.getValue(), sb);
          oneValue = true;
        }
      }
      if (!oneValue) {
        boolean notfirst = false;
        for (Map.Entry<ExecutableElement, AnnotationValue> arg : args.entrySet()) {
          if (!"{}".equals(arg.getValue().toString())) {
            if (notfirst) {
              sb.append(", ");
            }
            notfirst = true;
            sb.append(arg.getKey().getSimpleName() + "=");
            formatAnnotationMirrorArg(arg.getValue(), sb);
          }
        }
      }
      sb.append(")");
    }
  }

  /**
   * Returns a new map that only has the values in {@code elementValues} that are not the same as
   * the default value.
   *
   * @param elementValues a mapping of annotation element to annotation value
   * @return a new map with only the not default default values of {@code elementValues}
   */
  private Map<ExecutableElement, AnnotationValue> removeDefaultValues(
      Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
    Map<ExecutableElement, AnnotationValue> nonDefaults = new LinkedHashMap<>();
    elementValues.forEach(
        (element, value) -> {
          if (element.getDefaultValue() == null
              || !Objects.equals(value.getValue(), element.getDefaultValue().getValue())) {
            nonDefaults.put(element, value);
          }
        });
    return nonDefaults;
  }

  // A helper method to print AnnotationValues (annotation arguments), without showing full
  // package names.
  @SuppressWarnings("unchecked")
  protected void formatAnnotationMirrorArg(AnnotationValue av, StringBuilder sb) {
    Object val = av.getValue();
    if (List.class.isAssignableFrom(val.getClass())) {
      List<AnnotationValue> vallist = (List<AnnotationValue>) val;
      if (vallist.size() == 1) {
        formatAnnotationMirrorArg(vallist.get(0), sb);
      } else {
        sb.append('{');
        boolean notfirst = false;
        for (AnnotationValue nav : vallist) {
          if (notfirst) {
            sb.append(", ");
          }
          notfirst = true;
          formatAnnotationMirrorArg(nav, sb);
        }
        sb.append('}');
      }
    } else if (VariableElement.class.isAssignableFrom(val.getClass())) {
      VariableElement ve = (VariableElement) val;
      sb.append(ve.getEnclosingElement().getSimpleName() + "." + ve.getSimpleName());
    } else {
      sb.append(av.toString());
    }
  }
}
