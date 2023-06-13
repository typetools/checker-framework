package org.checkerframework.checker.units;

import java.util.Collection;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.javacutil.AnnotationFormatter;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.DefaultAnnotationFormatter;

/** Formats units-of-measure annotations. */
public class UnitsAnnotatedTypeFormatter extends DefaultAnnotatedTypeFormatter {
  /** The checker. */
  protected final BaseTypeChecker checker;

  /** Javac element utilities. */
  protected final Elements elements;

  /**
   * Create a UnitsAnnotatedTypeFormatter.
   *
   * @param checker the checker
   */
  public UnitsAnnotatedTypeFormatter(BaseTypeChecker checker) {
    // Utilize the Default Type Formatter, but force it to print out Invisible Qualifiers.
    // Keep super call in sync with implementation in DefaultAnnotatedTypeFormatter.
    // Keep checker options in sync with implementation in AnnotatedTypeFactory.
    super(
        new UnitsFormattingVisitor(
            checker,
            new UnitsAnnotationFormatter(checker),
            checker.hasOption("printVerboseGenerics"),
            true));

    this.checker = checker;
    this.elements = checker.getElementUtils();
  }

  protected static class UnitsFormattingVisitor
      extends DefaultAnnotatedTypeFormatter.FormattingVisitor {
    protected final BaseTypeChecker checker;
    protected final Elements elements;

    public UnitsFormattingVisitor(
        BaseTypeChecker checker,
        AnnotationFormatter annoFormatter,
        boolean printVerboseGenerics,
        boolean defaultInvisiblesSetting) {

      super(annoFormatter, printVerboseGenerics, defaultInvisiblesSetting);
      this.checker = checker;
      this.elements = checker.getElementUtils();
    }
  }

  /** Format the error printout of any units qualifier that uses Prefix.one. */
  protected static class UnitsAnnotationFormatter extends DefaultAnnotationFormatter {
    protected final BaseTypeChecker checker;
    protected final Elements elements;

    public UnitsAnnotationFormatter(BaseTypeChecker checker) {
      this.checker = checker;
      this.elements = checker.getElementUtils();
    }

    @Override
    public String formatAnnotationString(
        Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
      // create an empty annotation set
      AnnotationMirrorSet trimmedAnnoSet = new AnnotationMirrorSet();

      // loop through all the annotation mirrors to see if they use Prefix.one, remove
      // Prefix.one if it does
      for (AnnotationMirror anno : annos) {
        if (UnitsRelationsTools.getPrefix(anno) == Prefix.one) {
          anno = UnitsRelationsTools.removePrefix(elements, anno);
        }
        // add to set
        trimmedAnnoSet.add(anno);
      }

      return super.formatAnnotationString(
          Collections.unmodifiableSet(trimmedAnnoSet), printInvisible);
    }
  }
}
