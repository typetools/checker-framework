package org.checkerframework.checker.i18nformatter;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.SimpleTypeVisitor7;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.FormatterTreeUtil.InvocationType;
import org.checkerframework.checker.formatter.FormatterTreeUtil.Result;
import org.checkerframework.checker.i18nformatter.qual.I18nChecksFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nValidFormat;
import org.checkerframework.checker.i18nformatter.util.I18nFormatUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class provides a collection of utilities to ease working with syntax trees that have
 * something to do with I18nFormatters.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@SuppressWarnings("deprecation") // For *Visitor7
public class I18nFormatterTreeUtil {
  /** The checker. */
  public final BaseTypeChecker checker;
  /** The processing environment. */
  public final ProcessingEnvironment processingEnv;

  /** The value() element/field of an @I18nFormat annotation. */
  protected final ExecutableElement i18nFormatValueElement;
  /** The value() element/field of an @I18nFormatFor annotation. */
  protected final ExecutableElement i18nFormatForValueElement;
  /** The value() element/field of an @I18nInvalidFormat annotation. */
  protected final ExecutableElement i18nInvalidFormatValueElement;

  /**
   * Creates a new I18nFormatterTreeUtil.
   *
   * @param checker the checker
   */
  public I18nFormatterTreeUtil(BaseTypeChecker checker) {
    this.checker = checker;
    this.processingEnv = checker.getProcessingEnvironment();
    i18nFormatValueElement = TreeUtils.getMethod(I18nFormat.class, "value", 0, processingEnv);
    i18nFormatForValueElement = TreeUtils.getMethod(I18nFormatFor.class, "value", 0, processingEnv);
    i18nInvalidFormatValueElement =
        TreeUtils.getMethod(I18nInvalidFormat.class, "value", 0, processingEnv);
  }

  /** Describe the format annotation type. */
  public enum FormatType {
    I18NINVALID,
    I18NFORMAT,
    I18NFORMATFOR
  }

  /**
   * Takes an exception that describes an invalid formatter string and returns a syntax trees
   * element that represents a {@link I18nInvalidFormat} annotation with the exception's error
   * message as value.
   */
  public AnnotationMirror exceptionToInvalidFormatAnnotation(IllegalArgumentException ex) {
    return stringToInvalidFormatAnnotation(ex.getMessage());
  }

  /**
   * Creates an {@link I18nInvalidFormat} annotation with the given string as its value.
   *
   * @param invalidFormatString an invalid formatter string
   * @return an {@link I18nInvalidFormat} annotation with the given string as its value
   */
  // package-private
  AnnotationMirror stringToInvalidFormatAnnotation(String invalidFormatString) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, I18nInvalidFormat.class);
    builder.setValue("value", invalidFormatString);
    return builder.build();
  }

  /**
   * Gets the value() element/field out of an I18nInvalidFormat annotation.
   *
   * @param anno an I18nInvalidFormat annotation
   * @return its value() element/field, or null if it does not have one
   */
  /*package-visible*/
  @Nullable String getI18nInvalidFormatValue(AnnotationMirror anno) {
    return AnnotationUtils.getElementValue(anno, i18nInvalidFormatValueElement, String.class, null);
  }

  /**
   * Gets the value() element/field out of an I18NFormatFor annotation.
   *
   * @param anno an I18NFormatFor annotation
   * @return its value() element/field
   */
  /*package-visible*/ String getI18nFormatForValue(AnnotationMirror anno) {
    return AnnotationUtils.getElementValue(anno, i18nFormatForValueElement, String.class);
  }

  /**
   * Takes a syntax tree element that represents a {@link I18nInvalidFormat} annotation, and returns
   * its value.
   *
   * @param anno an I18nInvalidFormat annotation
   * @return its value() element/field, within double-quotes
   */
  public String invalidFormatAnnotationToErrorMessage(AnnotationMirror anno) {
    return "\"" + getI18nInvalidFormatValue(anno) + "\"";
  }

  /**
   * Creates a {@code @}{@link I18nFormat} annotation with the given list as its value.
   *
   * @param args conversion categories for the {@code @Format} annotation
   * @return a {@code @}{@link I18nFormat} annotation with the given list as its value
   */
  public AnnotationMirror categoriesToFormatAnnotation(I18nConversionCategory[] args) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, I18nFormat.class);
    builder.setValue("value", args);
    return builder.build();
  }

  /**
   * Takes an {@code @}{@link I18nFormat} annotation, and returns its {@code value} element
   *
   * @param anno an {@code @}{@link I18nFormat} annotation
   * @return the {@code @}{@link I18nFormat} annotation's {@code value} element
   */
  public I18nConversionCategory[] formatAnnotationToCategories(AnnotationMirror anno) {
    return AnnotationUtils.getElementValueEnumArray(
        anno, i18nFormatValueElement, I18nConversionCategory.class);
  }

  /**
   * Returns true if the call is to a method with the @I18nChecksFormat annotation. An example of
   * such a method is I18nFormatUtil.hasFormat.
   */
  public boolean isHasFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
    ExecutableElement method = node.getTarget().getMethod();
    AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nChecksFormat.class);
    return anno != null;
  }

  /**
   * Returns true if the call is to a method with the @I18nValidFormat annotation. An example of
   * such a method is I18nFormatUtil.isFormat.
   */
  public boolean isIsFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
    ExecutableElement method = node.getTarget().getMethod();
    AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nValidFormat.class);
    return anno != null;
  }

  /**
   * Returns true if the call is to a method with the @I18nMakeFormat annotation. An example of such
   * a method is ResourceBundle.getString.
   */
  public boolean isMakeFormatCall(MethodInvocationNode node, AnnotatedTypeFactory atypeFactory) {
    ExecutableElement method = node.getTarget().getMethod();
    AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, I18nMakeFormat.class);
    return anno != null;
  }

  /**
   * Reports an error.
   *
   * @param res used for source location information
   * @param msgKey the diagnostic message key
   * @param args arguments to the diagnostic message
   */
  public final void failure(Result<?> res, @CompilerMessageKey String msgKey, Object... args) {
    checker.reportError(res.location, msgKey, args);
  }

  /**
   * Reports a warning.
   *
   * @param res used for source location information
   * @param msgKey the diagnostic message key
   * @param args arguments to the diagnostic message
   */
  public final void warning(Result<?> res, @CompilerMessageKey String msgKey, Object... args) {
    checker.reportWarning(res.location, msgKey, args);
  }

  private I18nConversionCategory[] asFormatCallCategoriesLowLevel(MethodInvocationNode node) {
    Node vararg = node.getArgument(1);
    if (vararg instanceof ArrayCreationNode) {
      List<Node> convs = ((ArrayCreationNode) vararg).getInitializers();
      I18nConversionCategory[] res = new I18nConversionCategory[convs.size()];
      for (int i = 0; i < convs.size(); i++) {
        Node conv = convs.get(i);
        if (conv instanceof FieldAccessNode) {
          if (typeMirrorToClass(((FieldAccessNode) conv).getType())
              == I18nConversionCategory.class) {
            res[i] = I18nConversionCategory.valueOf(((FieldAccessNode) conv).getFieldName());
            continue; /* avoid returning null */
          }
        }
        return null;
      }
      return res;
    }
    return null;
  }

  public Result<I18nConversionCategory[]> getHasFormatCallCategories(MethodInvocationNode node) {
    return new Result<>(asFormatCallCategoriesLowLevel(node), node.getTree());
  }

  public Result<I18nConversionCategory[]> makeFormatCallCategories(
      MethodInvocationNode node, I18nFormatterAnnotatedTypeFactory atypeFactory) {
    Map<String, String> translations = atypeFactory.translations;
    Node firstParam = node.getArgument(0);
    Result<I18nConversionCategory[]> ret = new Result<>(null, node.getTree());

    // Now only work with a literal string
    if (firstParam instanceof StringLiteralNode) {
      String s = ((StringLiteralNode) firstParam).getValue();
      if (translations.containsKey(s)) {
        String value = translations.get(s);
        ret = new Result<>(I18nFormatUtil.formatParameterCategories(value), node.getTree());
      }
    }
    return ret;
  }

  /**
   * Returns an I18nFormatCall instance, only if there is an {@code @I18nFormatFor} annotation.
   * Otherwise, returns null.
   *
   * @param tree method invocation tree
   * @param atypeFactory type factory
   * @return an I18nFormatCall instance, only if there is an {@code @I18nFormatFor} annotation.
   *     Otherwise, returns null.
   */
  public @Nullable I18nFormatCall createFormatForCall(
      MethodInvocationTree tree, I18nFormatterAnnotatedTypeFactory atypeFactory) {
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    AnnotatedExecutableType methodAnno = atypeFactory.getAnnotatedType(method);
    for (AnnotatedTypeMirror paramType : methodAnno.getParameterTypes()) {
      // find @FormatFor
      if (paramType.getAnnotation(I18nFormatFor.class) != null) {
        return atypeFactory.treeUtil.new I18nFormatCall(tree, atypeFactory);
      }
    }
    return null;
  }

  /**
   * Represents a format method invocation in the syntax tree.
   *
   * <p>An I18nFormatCall instance can only be instantiated by the createFormatForCall method.
   */
  public class I18nFormatCall {

    /** The AST node for the call. */
    private final MethodInvocationTree tree;
    /** The format string argument. */
    private ExpressionTree formatArg;
    /** The type factory. */
    private final AnnotatedTypeFactory atypeFactory;
    /** The arguments to the format string. */
    private List<? extends ExpressionTree> args;
    /** Extra description for error messages. */
    private String invalidMessage;

    /** The type of the format string formal parameter. */
    private AnnotatedTypeMirror formatAnno;

    /**
     * Creates an {@code I18nFormatCall} for the given method invocation tree.
     *
     * @param tree method invocation tree
     * @param atypeFactory type factory
     */
    public I18nFormatCall(MethodInvocationTree tree, AnnotatedTypeFactory atypeFactory) {
      this.tree = tree;
      this.atypeFactory = atypeFactory;
      List<? extends ExpressionTree> theargs = tree.getArguments();
      this.args = null;
      ExecutableElement method = TreeUtils.elementFromUse(tree);
      AnnotatedExecutableType methodAnno = atypeFactory.getAnnotatedType(method);
      initialCheck(theargs, method, methodAnno);
    }

    /**
     * Returns the AST node for the call.
     *
     * @return the AST node for the call
     */
    public MethodInvocationTree getTree() {
      return tree;
    }

    @Override
    public String toString() {
      return this.tree.toString();
    }

    /**
     * This method checks the validity of the FormatFor. If it is valid, this.args will be set to
     * the correct parameter arguments. Otherwise, it will be still null.
     *
     * @param theargs arguments to the format method call
     * @param method the ExecutableElement of the format method
     * @param methodAnno annotated type of {@code method}
     */
    private void initialCheck(
        List<? extends ExpressionTree> theargs,
        ExecutableElement method,
        AnnotatedExecutableType methodAnno) {
      // paramIndex is a 0-based index
      int paramIndex = -1;
      int i = 0;
      for (AnnotatedTypeMirror paramType : methodAnno.getParameterTypes()) {
        if (paramType.getAnnotation(I18nFormatFor.class) != null) {
          this.formatArg = theargs.get(i);
          this.formatAnno = atypeFactory.getAnnotatedType(formatArg);

          if (typeMirrorToClass(paramType.getUnderlyingType()) != String.class) {
            // Invalid FormatFor invocation
            return;
          }

          String formatforArg = getI18nFormatForValue(paramType.getAnnotation(I18nFormatFor.class));

          paramIndex = JavaExpressionParseUtil.parameterIndex(formatforArg);
          if (paramIndex == -1) {
            // report errors here
            checker.reportError(tree, "i18nformat.formatfor");
          } else {
            paramIndex--;
          }
          break;
        }
        i++;
      }

      if (paramIndex != -1) {
        VariableElement param = method.getParameters().get(paramIndex);
        if (param.asType().getKind() == TypeKind.ARRAY) {
          this.args = theargs.subList(paramIndex, theargs.size());
        } else {
          this.args = theargs.subList(paramIndex, paramIndex + 1);
        }
      }
    }

    public Result<FormatType> getFormatType() {
      FormatType type;
      if (isValidFormatForInvocation()) {
        if (formatAnno.hasAnnotation(I18nFormat.class)) {
          type = FormatType.I18NFORMAT;
        } else if (formatAnno.hasAnnotation(I18nFormatFor.class)) {
          type = FormatType.I18NFORMATFOR;
        } else {
          type = FormatType.I18NINVALID;
          invalidMessage = "(is a @I18nFormat annotation missing?)";
          AnnotationMirror inv = formatAnno.getAnnotation(I18nInvalidFormat.class);
          if (inv != null) {
            invalidMessage = getI18nInvalidFormatValue(inv);
          }
        }
      } else {
        // If the FormatFor is invalid, it's still I18nFormatFor type but invalid,
        // and we can't do anything else
        type = FormatType.I18NFORMATFOR;
      }
      return new Result<>(type, formatArg);
    }

    public final String getInvalidError() {
      return invalidMessage;
    }

    public boolean isValidFormatForInvocation() {
      return this.args != null;
    }

    /**
     * Returns the type of method invocation.
     *
     * @see InvocationType
     */
    public final Result<InvocationType> getInvocationType() {
      InvocationType type = InvocationType.VARARG;

      if (args.size() == 1) {
        final ExpressionTree first = args.get(0);
        TypeMirror argType = atypeFactory.getAnnotatedType(first).getUnderlyingType();
        // figure out if argType is an array
        type =
            argType.accept(
                new SimpleTypeVisitor7<InvocationType, Class<Void>>() {
                  @Override
                  protected InvocationType defaultAction(TypeMirror e, Class<Void> p) {
                    // not an array
                    return InvocationType.VARARG;
                  }

                  @Override
                  public InvocationType visitArray(ArrayType t, Class<Void> p) {
                    // it's an array, now figure out if it's a
                    // (Object[])null array
                    return first.accept(
                        new SimpleTreeVisitor<InvocationType, Class<Void>>() {
                          @Override
                          protected InvocationType defaultAction(Tree node, Class<Void> p) {
                            // just a normal array
                            return InvocationType.ARRAY;
                          }

                          @Override
                          public InvocationType visitTypeCast(TypeCastTree node, Class<Void> p) {
                            // it's a (Object[])null
                            return atypeFactory
                                        .getAnnotatedType(node.getExpression())
                                        .getUnderlyingType()
                                        .getKind()
                                    == TypeKind.NULL
                                ? InvocationType.NULLARRAY
                                : InvocationType.ARRAY;
                          }
                        },
                        p);
                  }

                  @Override
                  public InvocationType visitNull(NullType t, Class<Void> p) {
                    return InvocationType.NULLARRAY;
                  }
                },
                Void.TYPE);
      }

      ExpressionTree loc;
      loc = tree.getMethodSelect();
      if (type != InvocationType.VARARG && !args.isEmpty()) {
        loc = args.get(0);
      }
      return new Result<>(type, loc);
    }

    public Result<FormatType> getInvalidInvocationType() {
      return new Result<>(FormatType.I18NFORMATFOR, formatArg);
    }

    /**
     * Returns the conversion category for every parameter.
     *
     * @see I18nConversionCategory
     */
    public final I18nConversionCategory[] getFormatCategories() {
      AnnotationMirror anno = formatAnno.getAnnotation(I18nFormat.class);
      return formatAnnotationToCategories(anno);
    }

    public final Result<TypeMirror>[] getParamTypes() {
      // One to suppress warning in javac, the other to suppress warning in Eclipse...
      @SuppressWarnings({"rawtypes", "unchecked"})
      Result<TypeMirror>[] res = new Result[args.size()];
      for (int i = 0; i < res.length; ++i) {
        ExpressionTree arg = args.get(i);
        TypeMirror argType = atypeFactory.getAnnotatedType(arg).getUnderlyingType();
        res[i] = new Result<>(argType, arg);
      }
      return res;
    }

    public boolean isValidParameter(I18nConversionCategory formatCat, TypeMirror paramType) {
      Class<? extends Object> type = typeMirrorToClass(paramType);
      if (type == null) {
        // we did not recognize the parameter type
        return false;
      }
      return formatCat.isAssignableFrom(type);
    }
  }

  /** Converts a TypeMirror to a Class. */
  private static class TypeMirrorToClassVisitor
      extends SimpleTypeVisitor7<Class<? extends Object>, Class<Void>> {
    @Override
    public Class<? extends Object> visitPrimitive(PrimitiveType t, Class<Void> v) {
      switch (t.getKind()) {
        case BOOLEAN:
          return Boolean.class;
        case BYTE:
          return Byte.class;
        case CHAR:
          return Character.class;
        case SHORT:
          return Short.class;
        case INT:
          return Integer.class;
        case LONG:
          return Long.class;
        case FLOAT:
          return Float.class;
        case DOUBLE:
          return Double.class;
        default:
          return null;
      }
    }

    @Override
    public Class<? extends Object> visitDeclared(DeclaredType dt, Class<Void> v) {
      return dt.asElement()
          .accept(
              new SimpleElementVisitor7<Class<? extends Object>, Class<Void>>() {
                @Override
                public Class<? extends Object> visitType(TypeElement e, Class<Void> v) {
                  try {
                    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658:
                    // Name.toString should be @PolySignature
                    @BinaryName String cname = e.getQualifiedName().toString();
                    return Class.forName(cname);
                  } catch (ClassNotFoundException e1) {
                    return null; // the lookup should work for all
                    // the classes we care about
                  }
                }
              },
              Void.TYPE);
    }
  }

  /** The singleton instance of TypeMirrorToClassVisitor. */
  private static TypeMirrorToClassVisitor typeMirrorToClassVisitor = new TypeMirrorToClassVisitor();

  /**
   * Converts a TypeMirror to a Class.
   *
   * @param type a TypeMirror
   * @return the class corresponding to the argument
   */
  private static final Class<? extends Object> typeMirrorToClass(final TypeMirror type) {
    return type.accept(typeMirrorToClassVisitor, Void.TYPE);
  }
}
