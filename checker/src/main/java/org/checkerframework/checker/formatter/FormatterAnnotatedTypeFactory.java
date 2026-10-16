package org.checkerframework.checker.formatter;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.checker.formatter.util.FormatUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceJavaParserStorage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.reflection.Signatures;

/**
 * Adds {@link Format} to the type of tree, if it is a {@code String} or {@code char} literal that
 * represents a satisfiable format. The annotation's value is set to be a list of appropriate {@link
 * ConversionCategory} values for every parameter of the format.
 *
 * @see ConversionCategory
 */
public class FormatterAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The @{@link UnknownFormat} annotation. */
  protected final AnnotationMirror UNKNOWNFORMAT =
      AnnotationBuilder.fromClass(elements, UnknownFormat.class);

  /** The @{@link FormatBottom} annotation. */
  protected final AnnotationMirror FORMATBOTTOM =
      AnnotationBuilder.fromClass(elements, FormatBottom.class);

  /** The @{@link FormatMethod} annotation. */
  protected final AnnotationMirror FORMATMETHOD =
      AnnotationBuilder.fromClass(elements, FormatMethod.class);

  /** The fully-qualified name of the {@link Format} qualifier. */
  protected static final @CanonicalName String FORMAT_NAME = Format.class.getCanonicalName();

  /** The fully-qualified name of the {@link InvalidFormat} qualifier. */
  protected static final @CanonicalName String INVALIDFORMAT_NAME =
      InvalidFormat.class.getCanonicalName();

  /** Syntax tree utilities. */
  protected final FormatterTreeUtil treeUtil = new FormatterTreeUtil(checker);

  /** Creates a FormatterAnnotatedTypeFactory. */
  @SuppressWarnings("this-escape")
  public FormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    addAliasedDeclAnnotation(
        com.google.errorprone.annotations.FormatMethod.class, FormatMethod.class, FORMATMETHOD);

    this.postInit();
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new FormatterQualifierHierarchy();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new FormatterTreeAnnotator(this));
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a method is annotated with {@code @FormatMethod}, remove any {@code @Format} annotation
   * from its format string parameter.
   */
  @Override
  public void wpiPrepareMethodForWriting(String className, AMethod method) {
    super.wpiPrepareMethodForWriting(className, method);
    if (hasFormatMethodAnno(method)) {
      AField param = formatStringParameter(method);
      if (param != null) {
        Set<Annotation> paramTypeAnnos = param.type.tlAnnotationsHere;
        paramTypeAnnos.removeIf(
            a -> a.def.name.equals("org.checkerframework.checker.formatter.qual.Format"));
      }
    }
  }

  /**
   * Returns the format string parameter of the given method: its first formal parameter whose
   * declared type is {@code String}, which is what {@code @FormatMethod} means by "format string".
   *
   * <p>Returns null if the method has no such parameter, or if nothing was inferred about it. A
   * parameter about which nothing was inferred has no {@code @Format} annotation to remove.
   *
   * @param method the AFU representation of a method that is annotated as {@code @FormatMethod}
   * @return the method's format string parameter, or null if there is none
   * @see FormatterVisitor#formatStringIndex
   */
  private static @Nullable AField formatStringParameter(AMethod method) {
    int index = formatStringIndex(method.methodSignature);
    if (index == -1) {
      return null;
    }
    // `method.parameters` might not have an entry for every formal parameter.  If it has no entry
    // for the format string parameter, then nothing was inferred about that parameter, so the
    // parameter has no `@Format` annotation to remove.
    return method.parameters.get(index);
  }

  /**
   * Returns the index of the format string parameter of the given method signature: its first
   * formal parameter whose declared type is {@code String}.
   *
   * <p>Returns -1 if {@code methodSignature} is not a well-formed method signature. That can happen
   * when the signature was read from a stale or hand-written annotation file rather than being
   * computed from a method declaration.
   *
   * @param methodSignature a method's simple name followed by its erased signature in JVML format,
   *     for example {@code bar(B[I[[Ljava/lang/String;)I}
   * @return the 0-based index of the method's format string parameter, or -1 if there is none
   */
  private static int formatStringIndex(String methodSignature) {
    int openParenIndex = methodSignature.indexOf('(');
    int closeParenIndex = methodSignature.lastIndexOf(')');
    if (openParenIndex == -1 || closeParenIndex < openParenIndex) {
      return -1;
    }
    String jvmArglist = methodSignature.substring(openParenIndex, closeParenIndex + 1);
    List<@FieldDescriptor String> paramDescriptors;
    try {
      paramDescriptors = Signatures.splitJvmArglist(jvmArglist);
    } catch (Error e) {
      // `Signatures.splitJvmArglist` throws `Error` if `jvmArglist` is malformed.  Removing an
      // inferred `@Format` annotation is optional cleanup, so don't abort the compilation.
      return -1;
    }
    for (int i = 0; i < paramDescriptors.size(); i++) {
      if (paramDescriptors.get(i).equals("Ljava/lang/String;")) {
        return i;
      }
    }
    return -1;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a method is annotated with {@code @FormatMethod}, remove any {@code @Format} annotation
   * from its format string parameter.
   */
  @Override
  public void wpiPrepareMethodForWriting(
      WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos,
      Collection<WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos> inSupertypes,
      Collection<WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos> inSubtypes) {
    super.wpiPrepareMethodForWriting(methodAnnos, inSupertypes, inSubtypes);
    if (hasFormatMethodAnno(methodAnnos)) {
      AnnotatedTypeMirror atm = formatStringParameterType(methodAnnos);
      if (atm != null) {
        atm.removePrimaryAnnotationByClass(Format.class);
      }
    }
  }

  /**
   * Returns the inferred type of the format string parameter of the given method: its first formal
   * parameter whose declared type is {@code String}, which is what {@code @FormatMethod} means by
   * "format string".
   *
   * <p>Returns null if the method has no such parameter, or if nothing was inferred about it. A
   * parameter about which nothing was inferred has no {@code @Format} annotation to remove.
   *
   * @param methodAnnos the annotations of a method that is annotated as {@code @FormatMethod}
   * @return the inferred type of the method's format string parameter, or null if there is none
   * @see FormatterVisitor#formatStringIndex
   */
  private static @Nullable AnnotatedTypeMirror formatStringParameterType(
      WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos) {
    List<Parameter> params = methodAnnos.declaration.getParameters();
    for (int i = 0; i < params.size(); i++) {
      // getParameterType's index is 0-based.  It returns null if nothing was inferred about the
      // parameter, in which case the parameter has no annotation to remove.
      AnnotatedTypeMirror atm = methodAnnos.getParameterType(i);
      // An inferred type is built from the parameter's TypeMirror, so when one exists, testing it
      // is exactly the test that `FormatterVisitor.formatStringIndex` performs.  Only when nothing
      // was inferred about the parameter is the less precise syntactic test necessary.
      boolean isString =
          atm != null
              ? TypesUtils.isString(atm.getUnderlyingType())
              : isStringParameter(params.get(i));
      if (isString) {
        return atm;
      }
    }
    return null;
  }

  /**
   * Returns true if the declared type of the given formal parameter is {@code String}.
   *
   * <p>The test is syntactic, because the JavaParser declaration has not been resolved: it assumes
   * that the simple name {@code String} refers to {@code java.lang.String}. Do not call this method
   * if the parameter's {@code TypeMirror} is available.
   *
   * @param param a formal parameter declaration
   * @return true if the parameter's declared type is {@code String}
   */
  private static boolean isStringParameter(Parameter param) {
    if (param.isVarArgs()) {
      // The declared type of a varargs parameter is an array type.
      return false;
    }
    if (!(param.getType() instanceof ClassOrInterfaceType classType)) {
      return false;
    }
    String name = classType.getNameWithScope();
    return name.equals("String") || name.equals("java.lang.String");
  }

  /**
   * Returns true if the method has a {@code @FormatMethod} annotation.
   *
   * @param methodAnnos method annotations
   * @return true if the method has a {@code @FormatMethod} annotation
   */
  private boolean hasFormatMethodAnno(AMethod methodAnnos) {
    for (Annotation anno : methodAnnos.tlAnnotationsHere) {
      String annoName = anno.def.name;
      if (annoName.equals("org.checkerframework.checker.formatter.qual.FormatMethod")
          || anno.def.name.equals("com.google.errorprone.annotations.FormatMethod")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the method has a {@code @FormatMethod} annotation.
   *
   * @param methodAnnos method annotations
   * @return true if the method has a {@code @FormatMethod} annotation
   */
  private boolean hasFormatMethodAnno(
      WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos) {
    AnnotationMirrorSet declarationAnnos = methodAnnos.getDeclarationAnnotations();
    return !declarationAnnos.isEmpty()
        && (containsSameByClass(declarationAnnos, FormatMethod.class)
            || AnnotationUtils.containsSameByName(
                declarationAnnos, "com.google.errorprone.annotations.FormatMethod"));
  }

  /** The tree annotator for the Format String Checker. */
  private class FormatterTreeAnnotator extends TreeAnnotator {
    /**
     * Create the tree annotator for the Format String Checker.
     *
     * @param atypeFactory the Format String Checker type factory
     */
    public FormatterTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
      if (!type.hasPrimaryAnnotationInHierarchy(UNKNOWNFORMAT)) {
        String format = null;
        if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
          format = (String) tree.getValue();
        }
        if (format != null) {
          AnnotationMirror anno;
          try {
            ConversionCategory[] cs = FormatUtil.formatParameterCategories(format);
            anno = FormatterAnnotatedTypeFactory.this.treeUtil.categoriesToFormatAnnotation(cs);
          } catch (IllegalFormatException e) {
            anno =
                FormatterAnnotatedTypeFactory.this.treeUtil.exceptionToInvalidFormatAnnotation(e);
          }
          type.addAnnotation(anno);
        }
      }
      return super.visitLiteral(tree, type);
    }
  }

  /** Qualifier hierarchy for the Formatter Checker. */
  class FormatterQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

    /** Qualifier kind for the @{@link Format} annotation. */
    private final QualifierKind FORMAT_KIND;

    /** Qualifier kind for the @{@link InvalidFormat} annotation. */
    private final QualifierKind INVALIDFORMAT_KIND;

    /** Creates a {@link FormatterQualifierHierarchy}. */
    public FormatterQualifierHierarchy() {
      super(
          FormatterAnnotatedTypeFactory.this.getSupportedTypeQualifiers(),
          elements,
          FormatterAnnotatedTypeFactory.this);
      FORMAT_KIND = getQualifierKind(FORMAT_NAME);
      INVALIDFORMAT_KIND = getQualifierKind(INVALIDFORMAT_NAME);
    }

    @Override
    protected boolean isSubtypeWithElements(
        AnnotationMirror subAnno,
        QualifierKind subKind,
        AnnotationMirror superAnno,
        QualifierKind superKind) {
      if (subKind == FORMAT_KIND && superKind == FORMAT_KIND) {
        ConversionCategory[] rhsArgTypes = treeUtil.formatAnnotationToCategories(subAnno);
        ConversionCategory[] lhsArgTypes = treeUtil.formatAnnotationToCategories(superAnno);

        if (rhsArgTypes.length > lhsArgTypes.length) {
          return false;
        }

        for (int i = 0; i < rhsArgTypes.length; ++i) {
          if (!ConversionCategory.isSubsetOf(lhsArgTypes[i], rhsArgTypes[i])) {
            return false;
          }
        }
        return true;
      } else if (subKind == INVALIDFORMAT_KIND && superKind == INVALIDFORMAT_KIND) {
        return true;
      }
      throw new TypeSystemError("Unexpected kinds: %s %s", subKind, superKind);
    }

    @Override
    protected AnnotationMirror leastUpperBoundWithElements(
        AnnotationMirror anno1,
        QualifierKind qualifierKind1,
        AnnotationMirror anno2,
        QualifierKind qualifierKind2,
        QualifierKind lubKind) {
      if (qualifierKind1.isBottom()) {
        return anno2;
      } else if (qualifierKind2.isBottom()) {
        return anno1;
      } else if (qualifierKind1 == FORMAT_KIND && qualifierKind2 == FORMAT_KIND) {
        ConversionCategory[] shorterArgTypesList = treeUtil.formatAnnotationToCategories(anno1);
        ConversionCategory[] longerArgTypesList = treeUtil.formatAnnotationToCategories(anno2);
        if (shorterArgTypesList.length > longerArgTypesList.length) {
          ConversionCategory[] temp = longerArgTypesList;
          longerArgTypesList = shorterArgTypesList;
          shorterArgTypesList = temp;
        }

        // From the manual:
        // It is legal to use a format string with fewer format specifiers
        // than required, but a warning is issued.

        ConversionCategory[] resultArgTypes = new ConversionCategory[longerArgTypesList.length];

        for (int i = 0; i < shorterArgTypesList.length; ++i) {
          resultArgTypes[i] =
              ConversionCategory.intersect(shorterArgTypesList[i], longerArgTypesList[i]);
        }
        for (int i = shorterArgTypesList.length; i < longerArgTypesList.length; ++i) {
          resultArgTypes[i] = longerArgTypesList[i];
        }
        return treeUtil.categoriesToFormatAnnotation(resultArgTypes);
      } else if (qualifierKind1 == INVALIDFORMAT_KIND && qualifierKind2 == INVALIDFORMAT_KIND) {

        assert !anno1.getElementValues().isEmpty();
        assert !anno2.getElementValues().isEmpty();

        if (AnnotationUtils.areSame(anno1, anno2)) {
          return anno1;
        }

        return treeUtil.stringToInvalidFormatAnnotation(
            "("
                + treeUtil.invalidFormatAnnotationToErrorMessage(anno1)
                + " or "
                + treeUtil.invalidFormatAnnotationToErrorMessage(anno2)
                + ")");
      }

      return UNKNOWNFORMAT;
    }

    @Override
    protected AnnotationMirror greatestLowerBoundWithElements(
        AnnotationMirror anno1,
        QualifierKind qualifierKind1,
        AnnotationMirror anno2,
        QualifierKind qualifierKind2,
        QualifierKind glbKind) {
      if (qualifierKind1.isTop()) {
        return anno2;
      } else if (qualifierKind2.isTop()) {
        return anno1;
      } else if (qualifierKind1 == FORMAT_KIND && qualifierKind2 == FORMAT_KIND) {
        ConversionCategory[] anno1ArgTypes = treeUtil.formatAnnotationToCategories(anno1);
        ConversionCategory[] anno2ArgTypes = treeUtil.formatAnnotationToCategories(anno2);

        // From the manual:
        // It is legal to use a format string with fewer format specifiers
        // than required, but a warning is issued.
        int length = anno1ArgTypes.length;
        if (anno2ArgTypes.length < length) {
          length = anno2ArgTypes.length;
        }

        ConversionCategory[] anno3ArgTypes = new ConversionCategory[length];

        for (int i = 0; i < length; ++i) {
          anno3ArgTypes[i] = ConversionCategory.union(anno1ArgTypes[i], anno2ArgTypes[i]);
        }
        return treeUtil.categoriesToFormatAnnotation(anno3ArgTypes);
      } else if (qualifierKind1 == INVALIDFORMAT_KIND && qualifierKind2 == INVALIDFORMAT_KIND) {

        assert !anno1.getElementValues().isEmpty();
        assert !anno2.getElementValues().isEmpty();

        if (AnnotationUtils.areSame(anno1, anno2)) {
          return anno1;
        }

        return treeUtil.stringToInvalidFormatAnnotation(
            "("
                + treeUtil.invalidFormatAnnotationToErrorMessage(anno1)
                + " and "
                + treeUtil.invalidFormatAnnotationToErrorMessage(anno2)
                + ")");
      }

      return FORMATBOTTOM;
    }
  }

  /**
   * Returns the annotation type mirror for the type of {@code expressionTree} with default
   * annotations applied.
   */
  @Override
  public @Nullable AnnotatedTypeMirror getDummyAssignedTo(ExpressionTree expressionTree) {
    TypeMirror type = TreeUtils.typeOf(expressionTree);
    if (type.getKind() != TypeKind.VOID) {
      AnnotatedTypeMirror atm = type(expressionTree);
      addDefaultAnnotations(atm);
      return atm;
    }
    return null;
  }
}
