package org.checkerframework.common.basetype;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Vector;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionScanner;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Impure;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.dataflow.util.PurityChecker.PurityResult;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.ajava.AnnotationEqualityVisitor;
import org.checkerframework.framework.ajava.ExpectedTreesVisitor;
import org.checkerframework.framework.ajava.InsertAjavaAnnotations;
import org.checkerframework.framework.ajava.JointVisitorWithDefaultAction;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.Unused;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.framework.util.Contract.ConditionalPostcondition;
import org.checkerframework.framework.util.Contract.Postcondition;
import org.checkerframework.framework.util.Contract.Precondition;
import org.checkerframework.framework.util.ContractsFromMethod;
import org.checkerframework.framework.util.FieldInvariants;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.framework.util.typeinference8.InferenceResult;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.BindingPatternUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.InstanceOfUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;
import org.plumelib.util.ArraysPlume;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;

/**
 * A {@link SourceVisitor} that performs assignment and pseudo-assignment checking, method
 * invocation checking, and assignability checking. The visitor visits every construct in a program,
 * not just types.
 *
 * <p>This implementation uses the {@link AnnotatedTypeFactory} implementation provided by an
 * associated {@link BaseTypeChecker}; its visitor methods will invoke this factory on parts of the
 * AST to determine the "annotated type" of an expression. Then, the visitor methods will check the
 * types in assignments and pseudo-assignments using {@link #commonAssignmentCheck}, which
 * ultimately calls the {@link TypeHierarchy#isSubtype} method and reports errors that violate
 * Java's rules of assignment.
 *
 * <p>Note that since this implementation only performs assignment and pseudo-assignment checking,
 * other rules for custom type systems must be added in subclasses (e.g., dereference checking in
 * the {@link org.checkerframework.checker.nullness.NullnessChecker} is implemented in the {@link
 * org.checkerframework.checker.nullness.NullnessChecker}'s {@link TreeScanner#visitMemberSelect}
 * method).
 *
 * <p>This implementation does the following checks:
 *
 * <ol>
 *   <li><b>Assignment and Pseudo-Assignment Check</b>: It verifies that any assignment type-checks,
 *       using the {@link TypeHierarchy#isSubtype} method. This includes method invocation and
 *       method overriding checks.
 *   <li><b>Type Validity Check</b>: It verifies that any user-supplied type is a valid type, using
 *       one of the {@code isValidUse} methods.
 *   <li><b>(Re-)Assignability Check</b>: It verifies that any assignment is valid, using {@code
 *       Checker.isAssignable} method.
 * </ol>
 *
 * @see "JLS $4"
 * @see TypeHierarchy#isSubtype
 * @see AnnotatedTypeFactory
 */
public class BaseTypeVisitor<Factory extends GenericAnnotatedTypeFactory<?, ?, ?, ?>>
    extends SourceVisitor<Void, Void> {

  /** The {@link BaseTypeChecker} for error reporting. */
  protected final BaseTypeChecker checker;

  /** The factory to use for obtaining "parsed" version of annotations. */
  protected final Factory atypeFactory;

  /** The qualifier hierarchy. */
  protected final QualifierHierarchy qualHierarchy;

  /** The Annotated Type Hierarchy. */
  protected final TypeHierarchy typeHierarchy;

  /** For obtaining line numbers in {@code -Ashowchecks} debugging output. */
  protected final SourcePositions positions;

  /** The element for java.util.Vector#copyInto. */
  private final ExecutableElement vectorCopyInto;

  /** The element for java.util.function.Function#apply. */
  private final ExecutableElement functionApply;

  /** The type of java.util.Vector. */
  private final AnnotatedDeclaredType vectorType;

  /** The @java.lang.annotation.Target annotation. */
  protected final AnnotationMirror TARGET =
      AnnotationBuilder.fromClass(
          elements,
          java.lang.annotation.Target.class,
          AnnotationBuilder.elementNamesValues("value", new ElementType[0]));

  /** The @{@link Deterministic} annotation. */
  protected final AnnotationMirror DETERMINISTIC =
      AnnotationBuilder.fromClass(elements, Deterministic.class);

  /** The @{@link SideEffectFree} annotation. */
  protected final AnnotationMirror SIDE_EFFECT_FREE =
      AnnotationBuilder.fromClass(elements, SideEffectFree.class);

  /** The @{@link Pure} annotation. */
  protected final AnnotationMirror PURE = AnnotationBuilder.fromClass(elements, Pure.class);

  /** The @{@link Impure} annotation. */
  protected final AnnotationMirror IMPURE = AnnotationBuilder.fromClass(elements, Impure.class);

  /** The {@code value} element/field of the @java.lang.annotation.Target annotation. */
  protected final ExecutableElement targetValueElement;

  /** The {@code when} element/field of the @Unused annotation. */
  protected final ExecutableElement unusedWhenElement;

  /** True if "-Ashowchecks" was passed on the command line. */
  protected final boolean showchecks;

  /** True if "-Ainfer" was passed on the command line. */
  private final boolean infer;

  /** True if "-AsuggestPureMethods" or "-Ainfer" was passed on the command line. */
  private final boolean suggestPureMethods;

  /**
   * True if "-AcheckPurityAnnotations" or "-AsuggestPureMethods" or "-Ainfer" was passed on the
   * command line.
   */
  private final boolean checkPurityAnnotations;

  /** True if "-AajavaChecks" was passed on the command line. */
  private final boolean ajavaChecks;

  /** True if "-AassumeSideEffectFree" or "-AassumePure" was passed on the command line. */
  private final boolean assumeSideEffectFree;

  /** True if "-AassumeDeterministic" or "-AassumePure" was passed on the command line. */
  private final boolean assumeDeterministic;

  /** True if "-AassumePureGetters" was passed on the command line. */
  public final boolean assumePureGetters;

  /** True if "-AcheckCastElementType" was passed on the command line. */
  private final boolean checkCastElementType;

  /** True if "-AwarnRedundantAnnotations" was passed on the command line */
  protected final boolean warnRedundantAnnotations;

  /** The tree of the enclosing method that is currently being visited, if any. */
  protected @Nullable MethodTree methodTree = null;

  /**
   * @param checker the type-checker associated with this visitor (for callbacks to {@link
   *     TypeHierarchy#isSubtype})
   */
  public BaseTypeVisitor(BaseTypeChecker checker) {
    this(checker, null);
  }

  /**
   * @param checker the type-checker associated with this visitor
   * @param typeFactory the type factory, or null. If null, this calls {@link #createTypeFactory}.
   */
  protected BaseTypeVisitor(BaseTypeChecker checker, @Nullable Factory typeFactory) {
    super(checker);

    this.checker = checker;
    this.atypeFactory = typeFactory == null ? createTypeFactory() : typeFactory;
    this.qualHierarchy = atypeFactory.getQualifierHierarchy();
    this.typeHierarchy = atypeFactory.getTypeHierarchy();
    this.positions = trees.getSourcePositions();
    this.typeValidator = createTypeValidator();
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    this.vectorCopyInto = TreeUtils.getMethod("java.util.Vector", "copyInto", 1, env);
    this.functionApply = TreeUtils.getMethod("java.util.function.Function", "apply", 1, env);
    this.vectorType =
        atypeFactory.fromElement(elements.getTypeElement(Vector.class.getCanonicalName()));
    targetValueElement = TreeUtils.getMethod(Target.class, "value", 0, env);
    unusedWhenElement = TreeUtils.getMethod(Unused.class, "when", 0, env);
    showchecks = checker.hasOption("showchecks");
    infer = checker.hasOption("infer");
    suggestPureMethods = checker.hasOption("suggestPureMethods") || infer;
    checkPurityAnnotations = checker.hasOption("checkPurityAnnotations") || suggestPureMethods;
    ajavaChecks = checker.hasOption("ajavaChecks");
    assumeSideEffectFree =
        checker.hasOption("assumeSideEffectFree") || checker.hasOption("assumePure");
    assumeDeterministic =
        checker.hasOption("assumeDeterministic") || checker.hasOption("assumePure");
    assumePureGetters = checker.hasOption("assumePureGetters");
    checkCastElementType = checker.hasOption("checkCastElementType");
    warnRedundantAnnotations = checker.hasOption("warnRedundantAnnotations");
  }

  /** An array containing just {@code BaseTypeChecker.class}. */
  private static final Class<?>[] baseTypeCheckerClassArray =
      new Class<?>[] {BaseTypeChecker.class};

  /**
   * Constructs an instance of the appropriate type factory for the implemented type system.
   *
   * <p>The default implementation uses the checker naming convention to create the appropriate type
   * factory. If no factory is found, it returns {@link BaseAnnotatedTypeFactory}. It reflectively
   * invokes the constructor that accepts this checker and compilation unit tree (in that order) as
   * arguments.
   *
   * <p>Subclasses have to override this method to create the appropriate visitor if they do not
   * follow the checker naming convention.
   *
   * @return the appropriate type factory
   */
  @SuppressWarnings({
    "unchecked", // unchecked cast to type variable
  })
  protected Factory createTypeFactory() {
    // Try to reflectively load the type factory.
    Class<?> checkerClass = checker.getClass();
    Object[] checkerArray = new Object[] {checker};
    while (checkerClass != BaseTypeChecker.class) {
      AnnotatedTypeFactory result =
          BaseTypeChecker.invokeConstructorFor(
              BaseTypeChecker.getRelatedClassName(checkerClass, "AnnotatedTypeFactory"),
              baseTypeCheckerClassArray,
              checkerArray);
      if (result != null) {
        return (Factory) result;
      }
      checkerClass = checkerClass.getSuperclass();
    }
    try {
      return (Factory) new BaseAnnotatedTypeFactory(checker);
    } catch (Throwable t) {
      throw new BugInCF(
          "Unexpected "
              + t.getClass().getSimpleName()
              + " when invoking BaseAnnotatedTypeFactory for checker "
              + checker.getClass().getSimpleName(),
          t);
    }
  }

  public final Factory getTypeFactory() {
    return atypeFactory;
  }

  /**
   * A public variant of {@link #createTypeFactory}. Only use this if you know what you are doing.
   *
   * @return the appropriate type factory
   */
  public Factory createTypeFactoryPublic() {
    return createTypeFactory();
  }

  // **********************************************************************
  // Responsible for updating the factory for the location (for performance)
  // **********************************************************************

  @Override
  public void setRoot(CompilationUnitTree newRoot) {
    atypeFactory.setRoot(newRoot);
    super.setRoot(newRoot);
    testJointJavacJavaParserVisitor();
    testAnnotationInsertion();
  }

  @Override
  public Void scan(@Nullable Tree tree, Void p) {
    if (tree == null) {
      return null;
    }
    if (getCurrentPath() != null) {
      this.atypeFactory.setVisitorTreePath(new TreePath(getCurrentPath(), tree));
    }
    // TODO: use JCP to add version-specific behavior
    if (tree != null
        && SystemUtil.jreVersion >= 14
        && tree.getKind().name().equals("SWITCH_EXPRESSION")) {
      visitSwitchExpression17(tree);
      return null;
    }
    return super.scan(tree, p);
  }

  /**
   * Test {@link org.checkerframework.framework.ajava.JointJavacJavaParserVisitor} if the checker
   * has the "ajavaChecks" option.
   *
   * <p>Parse the current source file with JavaParser and check that the AST can be matched with the
   * Tree produced by javac. Crash if not.
   *
   * <p>Subclasses may override this method to disable the test if even the option is provided.
   */
  protected void testJointJavacJavaParserVisitor() {
    if (root == null
        || !ajavaChecks
        // TODO: Make annotation insertion work for Java 21.
        || root.getSourceFile().toUri().toString().contains("java21")) {
      return;
    }

    Map<Tree, com.github.javaparser.ast.Node> treePairs = new HashMap<>();
    try (InputStream reader = root.getSourceFile().openInputStream()) {
      CompilationUnit javaParserRoot = JavaParserUtil.parseCompilationUnit(reader);
      JavaParserUtil.concatenateAddedStringLiterals(javaParserRoot);
      new JointVisitorWithDefaultAction() {
        @Override
        public void defaultJointAction(
            Tree javacTree, com.github.javaparser.ast.Node javaParserNode) {
          treePairs.put(javacTree, javaParserNode);
        }
      }.visitCompilationUnit(root, javaParserRoot);
      ExpectedTreesVisitor expectedTreesVisitor = new ExpectedTreesVisitor();
      expectedTreesVisitor.visitCompilationUnit(root, null);
      for (Tree expected : expectedTreesVisitor.getTrees()) {
        if (!treePairs.containsKey(expected)) {
          throw new BugInCF(
              "Javac tree not matched to JavaParser node: %s [%s @ %d], in file: %s",
              expected,
              expected.getClass(),
              positions.getStartPosition(root, expected),
              root.getSourceFile().getName());
        }
      }
    } catch (IOException e) {
      throw new BugInCF("Error reading Java source file", e);
    }
  }

  /**
   * Tests {@link org.checkerframework.framework.ajava.InsertAjavaAnnotations} if the checker has
   * the "ajavaChecks" option.
   *
   * <ol>
   *   <li>Parses the current file with JavaParser.
   *   <li>Removes all annotations.
   *   <li>Reinserts the annotations.
   *   <li>Throws an exception if the ASTs are not the same.
   * </ol>
   *
   * <p>Subclasses may override this method to disable the test even if the option is provided.
   */
  protected void testAnnotationInsertion() {
    if (root == null
        || !ajavaChecks
        // TODO: Make annotation insertion work for Java 21.
        || root.getSourceFile().toUri().toString().contains("java21")) {
      return;
    }

    CompilationUnit originalAst;
    try (InputStream originalInputStream = root.getSourceFile().openInputStream()) {
      originalAst = JavaParserUtil.parseCompilationUnit(originalInputStream);
    } catch (IOException e) {
      throw new BugInCF("Error while reading Java file: " + root.getSourceFile().toUri(), e);
    }

    CompilationUnit astWithoutAnnotations = originalAst.clone();
    JavaParserUtil.clearAnnotations(astWithoutAnnotations);
    String withoutAnnotations = new DefaultPrettyPrinter().print(astWithoutAnnotations);

    String withAnnotations;
    try (InputStream annotationInputStream = root.getSourceFile().openInputStream()) {
      withAnnotations =
          new InsertAjavaAnnotations(elements)
              .insertAnnotations(annotationInputStream, withoutAnnotations, System.lineSeparator());
    } catch (IOException e) {
      throw new BugInCF("Error while reading Java file: " + root.getSourceFile().toUri(), e);
    }

    CompilationUnit modifiedAst = null;
    try {
      modifiedAst = JavaParserUtil.parseCompilationUnit(withAnnotations);
    } catch (ParseProblemException e) {
      throw new BugInCF("Failed to parse code after annotation insertion: " + withAnnotations, e);
    }

    AnnotationEqualityVisitor visitor = new AnnotationEqualityVisitor();
    originalAst.accept(visitor, modifiedAst);
    if (!visitor.getAnnotationsMatch()) {
      throw new BugInCF(
          String.join(
              System.lineSeparator(),
              "Sanity check of erasing then reinserting annotations produced a different AST.",
              "File: " + root.getSourceFile(),
              "Node class: " + visitor.getMismatchedNode1().getClass().getSimpleName(),
              "Original node: " + oneLine(visitor.getMismatchedNode1()),
              "Node with annotations re-inserted: " + oneLine(visitor.getMismatchedNode2()),
              "Original annotations: " + visitor.getMismatchedNode1().getAnnotations(),
              "Re-inserted annotations: " + visitor.getMismatchedNode2().getAnnotations(),
              "Original AST:",
              originalAst.toString(),
              "Ast with annotations re-inserted: " + modifiedAst));
    }
  }

  /**
   * Replace newlines in the printed representation by spaces.
   *
   * @param arg an object to format
   * @return the object's toString representation, on one line
   */
  private String oneLine(Object arg) {
    return arg.toString().replace(System.lineSeparator(), " ");
  }

  /**
   * Type-check classTree and skips classes specified by the skipDef option. Subclasses should
   * override {@link #processClassTree(ClassTree)} instead of this method.
   *
   * @param classTree class to check
   * @param p null
   * @return null
   */
  @Override
  public final Void visitClass(ClassTree classTree, Void p) {
    if (checker.shouldSkipDefs(classTree) || checker.shouldSkipFiles(classTree)) {
      // Not "return super.visitClass(classTree, p);" because that would recursively call
      // visitors on subtrees; we want to skip the class entirely.
      return null;
    }
    atypeFactory.preProcessClassTree(classTree);

    TreePath preTreePath = atypeFactory.getVisitorTreePath();
    MethodTree preMT = methodTree;

    // Don't use atypeFactory.getPath, because that depends on the visitor path.
    atypeFactory.setVisitorTreePath(TreePath.getPath(root, classTree));
    methodTree = null;

    try {
      processClassTree(classTree);
      atypeFactory.postProcessClassTree(classTree);
    } finally {
      atypeFactory.setVisitorTreePath(preTreePath);
      methodTree = preMT;
    }
    return null;
  }

  /**
   * Type-check classTree. Subclasses should override this method instead of {@link
   * #visitClass(ClassTree, Void)}.
   *
   * @param classTree class to check
   */
  public void processClassTree(ClassTree classTree) {
    checkFieldInvariantDeclarations(classTree);
    if (!TreeUtils.hasExplicitConstructor(classTree)) {
      checkDefaultConstructor(classTree);
    }

    AnnotatedDeclaredType classType = atypeFactory.getAnnotatedType(classTree);
    atypeFactory.getDependentTypesHelper().checkClassForErrorExpressions(classTree, classType);
    validateType(classTree, classType);

    Tree ext = classTree.getExtendsClause();
    if (ext != null) {
      AnnotatedTypeMirror superClass = atypeFactory.getTypeOfExtendsImplements(ext);
      validateType(ext, superClass);
    }

    List<? extends Tree> impls = classTree.getImplementsClause();
    if (impls != null) {
      for (Tree im : impls) {
        AnnotatedTypeMirror superInterface = atypeFactory.getTypeOfExtendsImplements(im);
        validateType(im, superInterface);
      }
    }

    warnInvalidPolymorphicQualifier(classTree);

    checkExtendsAndImplements(classTree);

    checkQualifierParameter(classTree);

    super.visitClass(classTree, null);
  }

  /**
   * A TreeScanner that issues an "invalid.polymorphic.qualifier" error for each {@link
   * AnnotationTree} that is a polymorphic qualifier. The second parameter is added to the error
   * message and should explain the location.
   */
  private final TreeScanner<Void, String> polyTreeScanner =
      new TreeScanner<Void, String>() {
        @Override
        public Void visitAnnotation(AnnotationTree annoTree, String location) {
          AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(annoTree);
          if (atypeFactory.isSupportedQualifier(anno)
              && qualHierarchy.isPolymorphicQualifier(anno)) {
            checker.reportError(annoTree, "invalid.polymorphic.qualifier", anno, location);
          }
          return super.visitAnnotation(annoTree, location);
        }
      };

  /**
   * Issues an "invalid.polymorphic.qualifier" error for all polymorphic annotations written on the
   * class declaration.
   *
   * @param classTree the class to check
   */
  protected void warnInvalidPolymorphicQualifier(ClassTree classTree) {
    if (TypesUtils.isAnonymous(TreeUtils.typeOf(classTree))) {
      // Anonymous class can have polymorphic annotations, so don't check them.
      return;
    }
    classTree.getModifiers().accept(polyTreeScanner, "in a class declaration");
    if (classTree.getExtendsClause() != null) {
      classTree.getExtendsClause().accept(polyTreeScanner, "in a class declaration");
    }
    for (Tree tree : classTree.getImplementsClause()) {
      tree.accept(polyTreeScanner, "in a class declaration");
    }
    for (Tree tree : classTree.getTypeParameters()) {
      tree.accept(polyTreeScanner, "in a class declaration");
    }
  }

  /**
   * Issues an "invalid.polymorphic.qualifier" error for all polymorphic annotations written on the
   * type parameters declaration.
   *
   * @param typeParameterTrees the type parameters to check
   */
  protected void warnInvalidPolymorphicQualifier(
      List<? extends TypeParameterTree> typeParameterTrees) {
    for (Tree tree : typeParameterTrees) {
      tree.accept(polyTreeScanner, "in a type parameter");
    }
  }

  /**
   * Issues an error if {@code classTree} has polymorphic fields but is not annotated with
   * {@code @HasQualifierParameter}. Always issue a warning if the type of a static field is
   * annotated with a polymorphic qualifier.
   *
   * <p>Issues an error if {@code classTree} extends or implements a class/interface that has a
   * qualifier parameter, but this class does not.
   *
   * @param classTree the ClassTree to check for polymorphic fields
   */
  protected void checkQualifierParameter(ClassTree classTree) {
    // Set of polymorphic qualifiers for hierarchies that do not have a qualifier parameter and
    // therefore cannot appear on a field.
    AnnotationMirrorSet illegalOnFieldsPolyQual = new AnnotationMirrorSet();
    // Set of polymorphic annotations for all hierarchies
    AnnotationMirrorSet polys = new AnnotationMirrorSet();
    TypeElement classElement = TreeUtils.elementFromDeclaration(classTree);
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      AnnotationMirror poly = qualHierarchy.getPolymorphicAnnotation(top);
      if (poly != null) {
        polys.add(poly);
      }
      // else {
      // If there is no polymorphic qualifier in the hierarchy, it could still have a
      // @HasQualifierParameter that must be checked.
      // }

      if (!atypeFactory.hasExplicitQualifierParameterInHierarchy(classElement, top)
          && atypeFactory.getDeclAnnotation(classElement, HasQualifierParameter.class) != null) {
        // The argument to a @HasQualifierParameter annotation must be the top type in the
        // type system.
        checker.reportError(classTree, "invalid.qual.param", top);
        break;
      }

      if (atypeFactory.hasExplicitQualifierParameterInHierarchy(classElement, top)
          && atypeFactory.hasExplicitNoQualifierParameterInHierarchy(classElement, top)) {
        checker.reportError(classTree, "conflicting.qual.param", top);
      }

      if (atypeFactory.hasQualifierParameterInHierarchy(classElement, top)) {
        continue;
      }

      if (poly != null) {
        illegalOnFieldsPolyQual.add(poly);
      }
      Element extendsEle = TypesUtils.getTypeElement(classElement.getSuperclass());
      if (extendsEle != null && atypeFactory.hasQualifierParameterInHierarchy(extendsEle, top)) {
        checker.reportError(classTree, "missing.has.qual.param", top);
      } else {
        for (TypeMirror interfaceType : classElement.getInterfaces()) {
          Element interfaceEle = TypesUtils.getTypeElement(interfaceType);
          if (atypeFactory.hasQualifierParameterInHierarchy(interfaceEle, top)) {
            checker.reportError(classTree, "missing.has.qual.param", top);
            break; // only issue error once
          }
        }
      }
    }

    for (Tree mem : classTree.getMembers()) {
      if (mem.getKind() == Tree.Kind.VARIABLE) {
        AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(mem);
        List<DiagMessage> hasInvalidPoly;
        if (ElementUtils.isStatic(TreeUtils.elementFromDeclaration((VariableTree) mem))) {
          // A polymorphic qualifier is not allowed on a static field even if the class
          // has a qualifier parameter.
          hasInvalidPoly = hasInvalidPolyScanner.visit(fieldType, polys);
        } else {
          hasInvalidPoly = hasInvalidPolyScanner.visit(fieldType, illegalOnFieldsPolyQual);
        }
        for (DiagMessage dm : hasInvalidPoly) {
          checker.report(mem, dm);
        }
      }
    }
  }

  /**
   * A scanner that given a set of polymorphic qualifiers, returns a list of errors reporting a use
   * of one of the polymorphic qualifiers.
   */
  private final HasInvalidPolyScanner hasInvalidPolyScanner = new HasInvalidPolyScanner();

  /**
   * A scanner that given a set of polymorphic qualifiers, returns a list of errors reporting a use
   * of one of the polymorphic qualifiers.
   */
  static class HasInvalidPolyScanner
      extends SimpleAnnotatedTypeScanner<List<DiagMessage>, AnnotationMirrorSet> {

    /** Create HasInvalidPolyScanner. */
    private HasInvalidPolyScanner() {
      super(DiagMessage::mergeLists, Collections.emptyList());
    }

    @Override
    protected List<DiagMessage> defaultAction(AnnotatedTypeMirror type, AnnotationMirrorSet polys) {
      if (type == null) {
        return Collections.emptyList();
      }

      for (AnnotationMirror poly : polys) {
        if (type.hasPrimaryAnnotationRelaxed(poly)) {
          return Collections.singletonList(
              DiagMessage.error("invalid.polymorphic.qualifier.use", poly));
        }
      }
      return Collections.emptyList();
    }
  }

  /**
   * In {@code @A class X extends @B Y implements @C Z {}}, enforce that {@code @A} must be a
   * subtype of {@code @B} and {@code @C}.
   *
   * <p>Also validate the types of the extends and implements clauses.
   *
   * @param classTree class tree to check
   */
  protected void checkExtendsAndImplements(ClassTree classTree) {
    if (TypesUtils.isAnonymous(TreeUtils.typeOf(classTree))) {
      // Don't check extends clause on anonymous classes.
      return;
    }
    if (classTree.getExtendsClause() == null && classTree.getImplementsClause().isEmpty()) {
      // Nothing to do
      return;
    }

    TypeMirror classType = TreeUtils.typeOf(classTree);
    AnnotationMirrorSet classBounds = atypeFactory.getTypeDeclarationBounds(classType);
    // No explicitly-written extends clause, as in "class X {}", is equivalent to writing "class
    // X extends @Top Object {}", so there is no need to do any subtype checking.
    if (classTree.getExtendsClause() != null) {
      Tree superClause = classTree.getExtendsClause();
      checkExtendsOrImplements(superClause, classBounds, classType, true);
    }
    // Do the same check as above for implements clauses.
    for (Tree superClause : classTree.getImplementsClause()) {
      checkExtendsOrImplements(superClause, classBounds, classType, false);
    }
  }

  /**
   * Helper for {@link #checkExtendsAndImplements} that checks one extends or implements clause.
   *
   * @param superClause an extends or implements clause
   * @param classBounds the type declarations bounds to check for consistency with {@code
   *     superClause}
   * @param classType the type being declared
   * @param isExtends true for an extends clause, false for an implements clause
   */
  protected void checkExtendsOrImplements(
      Tree superClause, AnnotationMirrorSet classBounds, TypeMirror classType, boolean isExtends) {
    AnnotatedTypeMirror superType = atypeFactory.getTypeOfExtendsImplements(superClause);
    TypeMirror superTM = superType.getUnderlyingType();
    for (AnnotationMirror classAnno : classBounds) {
      AnnotationMirror superAnno = superType.getPrimaryAnnotationInHierarchy(classAnno);
      if (!qualHierarchy.isSubtypeShallow(classAnno, classType, superAnno, superTM)) {
        checker.reportError(
            superClause,
            (isExtends
                ? "declaration.inconsistent.with.extends.clause"
                : "declaration.inconsistent.with.implements.clause"),
            classAnno,
            superAnno);
      }
    }
  }

  /**
   * Check that the field invariant declaration annotations meet the following requirements:
   *
   * <ol>
   *   <!-- The item numbering is referred to in the body of the method.-->
   *   <li value="1">If the superclass of {@code classTree} has a field invariant, then the field
   *       invariant for {@code classTree} must include all the fields in the superclass invariant
   *       and those fields' annotations must be a subtype (or equal) to the annotations for those
   *       fields in the superclass.
   *   <li value="2">The fields in the invariant must be a.) final and b.) declared in a superclass
   *       of {@code classTree}.
   *   <li value="3">The qualifier for each field must be a subtype of the annotation on the
   *       declaration of that field.
   *   <li value="4">The field invariant has an equal number of fields and qualifiers, or it has one
   *       qualifier and at least one field.
   * </ol>
   *
   * @param classTree class that might have a field invariant
   * @checker_framework.manual #field-invariants Field invariants
   */
  protected void checkFieldInvariantDeclarations(ClassTree classTree) {
    TypeElement elt = TreeUtils.elementFromDeclaration(classTree);
    FieldInvariants invariants = atypeFactory.getFieldInvariants(elt);
    if (invariants == null) {
      // No invariants to check
      return;
    }

    // Where to issue an error, if any.
    Tree errorTree =
        atypeFactory.getFieldInvariantAnnotationTree(classTree.getModifiers().getAnnotations());
    if (errorTree == null) {
      // If the annotation was inherited, then there is no annotation tree, so issue the
      // error on the class.
      errorTree = classTree;
    }

    // Checks #4 (see method Javadoc)
    if (!invariants.isWellFormed()) {
      checker.reportError(errorTree, "field.invariant.not.wellformed");
      return;
    }

    TypeMirror superClass = elt.getSuperclass();
    List<String> fieldsNotFound = new ArrayList<>(invariants.getFields());
    Set<VariableElement> fieldElts =
        ElementUtils.findFieldsInTypeOrSuperType(superClass, fieldsNotFound);

    // Checks that fields are declared in super class. (#2b)
    if (!fieldsNotFound.isEmpty()) {
      String notFoundString = String.join(", ", fieldsNotFound);
      checker.reportError(errorTree, "field.invariant.not.found", notFoundString);
    }

    FieldInvariants superInvar =
        atypeFactory.getFieldInvariants(TypesUtils.getTypeElement(superClass));
    if (superInvar != null) {
      // Checks #3 (see method Javadoc)
      DiagMessage superError = invariants.isStrongerThan(superInvar);
      if (superError != null) {
        checker.report(errorTree, superError);
      }
    }

    List<String> notFinal = new ArrayList<>(fieldElts.size());
    for (VariableElement field : fieldElts) {
      String fieldName = field.getSimpleName().toString();
      if (!ElementUtils.isFinal(field)) {
        notFinal.add(fieldName);
      }
      AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(field);

      List<AnnotationMirror> annos = invariants.getQualifiersFor(field.getSimpleName());
      for (AnnotationMirror invariantAnno : annos) {
        AnnotationMirror declaredAnno = fieldType.getEffectiveAnnotationInHierarchy(invariantAnno);
        if (declaredAnno == null) {
          // invariant anno isn't in this hierarchy
          continue;
        }

        if (!typeHierarchy.isSubtypeShallowEffective(invariantAnno, fieldType)) {
          // Checks #3
          checker.reportError(
              errorTree, "field.invariant.not.subtype", fieldName, invariantAnno, declaredAnno);
        }
      }
    }

    // Checks #2a
    if (!notFinal.isEmpty()) {
      String notFinalString = String.join(", ", notFinal);
      checker.reportError(errorTree, "field.invariant.not.final", notFinalString);
    }
  }

  /**
   * Check the default constructor.
   *
   * @param tree a class declaration
   */
  protected void checkDefaultConstructor(ClassTree tree) {}

  /**
   * Checks that the method or constructor obeys override and subtype rules to all overridden
   * methods. (Uses the pseudo-assignment logic to do so.)
   *
   * <p>The override rule specifies that a method, m1, may override a method m2 only if:
   *
   * <ul>
   *   <li>m1 return type is a subtype of m2
   *   <li>m1 receiver type is a supertype of m2
   *   <li>m1 parameters are supertypes of corresponding m2 parameters
   * </ul>
   *
   * Also, it issues a "missing.this" error for static method annotated receivers.
   */
  @Override
  public final Void visitMethod(MethodTree tree, Void p) {
    ClassTree enclosingClass = TreePathUtil.enclosingClass(getCurrentPath());
    if (checker.shouldSkipDefs(enclosingClass, tree)) {
      return null;
    }
    processMethodTree("<unknown from visitMethod>", tree);
    return null;
  }

  /**
   * Type-check {@literal methodTree}. Subclasses should override this method instead of {@link
   * #visitMethod(MethodTree, Void)}.
   *
   * @param className the class that contains the method, for diagnostics only
   * @param tree the method to type-check
   */
  public void processMethodTree(String className, MethodTree tree) {

    // We copy the result from getAnnotatedType to ensure that circular types (e.g. K extends
    // Comparable<K>) are represented by circular AnnotatedTypeMirrors, which avoids problems
    // with later checks.
    // TODO: Find a cleaner way to ensure circular AnnotatedTypeMirrors.
    AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(tree).deepCopy();
    MethodTree preMT = methodTree;
    methodTree = tree;
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(tree);

    warnAboutTypeAnnotationsTooEarly(tree, tree.getModifiers());

    if (tree.getReturnType() != null) {
      visitAnnotatedType(tree.getModifiers().getAnnotations(), tree.getReturnType());
      warnRedundantAnnotations(tree.getReturnType(), methodType.getReturnType());
    } else if (TreeUtils.isConstructor(tree)) {
      maybeReportAnnoOnIrrelevant(
          tree.getModifiers(),
          methodType.getReturnType().getUnderlyingType(),
          tree.getModifiers().getAnnotations());
    }

    try {
      if (TreeUtils.isAnonymousConstructor(tree)) {
        // We shouldn't dig deeper
        return;
      }

      if (TreeUtils.isConstructor(tree)) {
        checkConstructorResult(methodType, methodElement);
      }

      checkPurityAnnotations(tree);

      // Passing the whole method/constructor validates the return type
      validateTypeOf(tree);

      // Validate types in throws clauses
      for (ExpressionTree thr : tree.getThrows()) {
        validateTypeOf(thr);
      }

      atypeFactory.getDependentTypesHelper().checkMethodForErrorExpressions(tree, methodType);

      // Check method overrides
      AnnotatedDeclaredType enclosingType =
          (AnnotatedDeclaredType)
              atypeFactory.getAnnotatedType(methodElement.getEnclosingElement());

      // Find which methods this method overrides
      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
      for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
          overriddenMethods.entrySet()) {
        AnnotatedDeclaredType overriddenType = pair.getKey();
        ExecutableElement overriddenMethodElt = pair.getValue();
        AnnotatedExecutableType overriddenMethodType =
            AnnotatedTypes.asMemberOf(types, atypeFactory, overriddenType, overriddenMethodElt);
        if (!checkOverride(tree, enclosingType, overriddenMethodType, overriddenType)) {
          // Stop at the first mismatch; this makes a difference only if
          // -Awarns is passed, in which case multiple warnings might be raised on
          // the same method, not adding any value. See Issue 373.
          break;
        }
      }

      // Check well-formedness of pre/postcondition
      boolean abstractMethod =
          methodElement.getModifiers().contains(Modifier.ABSTRACT)
              || methodElement.getModifiers().contains(Modifier.NATIVE);

      List<String> formalParamNames =
          CollectionsPlume.mapList(
              (VariableTree param) -> param.getName().toString(), tree.getParameters());
      checkContractsAtMethodDeclaration(tree, methodElement, formalParamNames, abstractMethod);

      // Infer postconditions
      if (shouldPerformContractInference()) {
        assert ElementUtils.isElementFromSourceCode(methodElement);

        // TODO: Infer conditional postconditions too.
        CFAbstractStore<?, ?> store = atypeFactory.getRegularExitStore(tree);
        // The store is null if the method has no normal exit, for example if its body is a
        // throw statement.
        if (store != null) {
          atypeFactory
              .getWholeProgramInference()
              .updateContracts(className, Analysis.BeforeOrAfter.AFTER, methodElement, store);
        }
      }

      warnInvalidPolymorphicQualifier(tree.getTypeParameters());

      super.visitMethod(tree, null);
    } finally {
      methodTree = preMT;
    }
  }

  /**
   * Should Whole Program Inference attempt to infer contract annotations? Typically, the answer is
   * "yes" whenever WPI is enabled, but this method exists to allow subclasses to customize that
   * behavior.
   *
   * @return true if contract inference should be performed, false if it should be disabled (even
   *     when WPI is enabled)
   */
  protected boolean shouldPerformContractInference() {
    return atypeFactory.getWholeProgramInference() != null;
  }

  /**
   * Check method purity if needed. Note that overriding rules are checked as part of {@link
   * #checkOverride(MethodTree, AnnotatedTypeMirror.AnnotatedExecutableType,
   * AnnotatedTypeMirror.AnnotatedDeclaredType, AnnotatedTypeMirror.AnnotatedExecutableType,
   * AnnotatedTypeMirror.AnnotatedDeclaredType)}.
   *
   * @param tree the method tree to check
   */
  protected void checkPurityAnnotations(MethodTree tree) {
    if (!checkPurityAnnotations) {
      return;
    }

    if (!suggestPureMethods && !PurityUtils.hasPurityAnnotation(atypeFactory, tree)) {
      // There is nothing to check.
      return;
    }

    if (isExplicitlySideEffectFreeAndDeterministic(tree)) {
      checker.reportWarning(tree, "purity.effectively.pure", tree.getName());
    }

    // `body` is lazily assigned.
    TreePath body = null;
    boolean bodyAssigned = false;

    if (suggestPureMethods || PurityUtils.hasPurityAnnotation(atypeFactory, tree)) {

      // check "no" purity
      EnumSet<Pure.Kind> kinds = PurityUtils.getPurityKinds(atypeFactory, tree);
      // @Deterministic makes no sense for a void method or constructor
      boolean isDeterministic = kinds.contains(Pure.Kind.DETERMINISTIC);
      if (isDeterministic) {
        if (TreeUtils.isConstructor(tree)) {
          checker.reportWarning(tree, "purity.deterministic.constructor");
        } else if (TreeUtils.isVoidReturn(tree)) {
          checker.reportWarning(tree, "purity.deterministic.void.method");
        }
      }

      body = atypeFactory.getPath(tree.getBody());
      bodyAssigned = true;
      PurityResult r;
      if (body == null) {
        r = new PurityResult();
      } else {
        r =
            PurityChecker.checkPurity(
                body, atypeFactory, assumeSideEffectFree, assumeDeterministic, assumePureGetters);
      }
      if (!r.isPure(kinds)) {
        reportPurityErrors(r, tree, kinds);
      }

      if (suggestPureMethods && !TreeUtils.isSynthetic(tree)) {
        // Issue a warning if the method is pure, but not annotated as such.
        EnumSet<Pure.Kind> additionalKinds = r.getKinds().clone();
        if (!infer) {
          // During WPI, propagate all purity kinds, even those that are already
          // present (because they were inferred in a previous WPI round).
          additionalKinds.removeAll(kinds);
        }
        if (TreeUtils.isConstructor(tree) || TreeUtils.isVoidReturn(tree)) {
          additionalKinds.remove(Pure.Kind.DETERMINISTIC);
        }
        if (infer) {
          WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
          ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
          inferPurityAnno(additionalKinds, wpi, methodElt);
          // The purity of overridden methods is impacted by the purity of this method. If
          // a superclass method is pure, but an implementation in a subclass is not, WPI
          // ought to treat **neither** as pure. The purity kind of the superclass method
          // is the LUB of its own purity and the purity of all the methods that override
          // it. Logically, this rule is the same as the WPI rule for overrides, but
          // purity isn't a type system and therefore must be special-cased.
          Set<? extends ExecutableElement> overriddenMethods =
              ElementUtils.getOverriddenMethods(methodElt, types);
          for (ExecutableElement overriddenElt : overriddenMethods) {
            inferPurityAnno(additionalKinds, wpi, overriddenElt);
          }
        } else if (additionalKinds.isEmpty()) {
          // No need to suggest @Impure, since it is equivalent to no annotation.
        } else if (additionalKinds.size() == 2) {
          checker.reportWarning(tree, "purity.more.pure", tree.getName());
        } else if (additionalKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
          checker.reportWarning(tree, "purity.more.sideeffectfree", tree.getName());
        } else if (additionalKinds.contains(Pure.Kind.DETERMINISTIC)) {
          checker.reportWarning(tree, "purity.more.deterministic", tree.getName());
        } else {
          throw new BugInCF("Unexpected purity kind in " + additionalKinds);
        }
      }
    }

    // There will be code here that *may* use `body` (and may set `body` before using it).
    // The below is just a placeholder so `bodyAssigned` is not a dead variable.
    // ...
    if (!bodyAssigned) {
      body = atypeFactory.getPath(tree.getBody());
      bodyAssigned = true;
    }
    // ...
  }

  /**
   * Returns true if the given method is explicitly annotated with both @{@link SideEffectFree}
   * and @{@link Deterministic}. Those annotations can be replaced by @{@link Pure}.
   *
   * @param tree a method
   * @return true if a method is explicitly annotated with both @{@link SideEffectFree} and @{@link
   *     Deterministic}
   */
  private boolean isExplicitlySideEffectFreeAndDeterministic(MethodTree tree) {
    List<AnnotationMirror> annotationMirrors =
        TreeUtils.annotationsFromTypeAnnotationTrees(tree.getModifiers().getAnnotations());
    return AnnotationUtils.containsSame(annotationMirrors, SIDE_EFFECT_FREE)
        && AnnotationUtils.containsSame(annotationMirrors, DETERMINISTIC);
  }

  /**
   * Infer a purity annotation for {@code elt} by converting {@code kinds} into a method annotation.
   *
   * <p>This method delegates to {@code WholeProgramInference.addMethodDeclarationAnnotation}, which
   * special-cases purity annotations: that method lubs a purity argument with whatever purity
   * annotation is already present on {@code elt}.
   *
   * @param kinds the set of purity kinds to use to infer the annotation
   * @param wpi the whole program inference instance to use to do the inferring
   * @param elt the element whose purity is being inferred
   */
  private void inferPurityAnno(
      EnumSet<Pure.Kind> kinds, WholeProgramInference wpi, ExecutableElement elt) {
    if (kinds.size() == 2) {
      wpi.addMethodDeclarationAnnotation(elt, PURE, true);
    } else if (kinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
      wpi.addMethodDeclarationAnnotation(elt, SIDE_EFFECT_FREE, true);
    } else if (kinds.contains(Pure.Kind.DETERMINISTIC)) {
      wpi.addMethodDeclarationAnnotation(elt, DETERMINISTIC, true);
    } else {
      assert kinds.isEmpty();
      wpi.addMethodDeclarationAnnotation(elt, IMPURE, true);
    }
  }

  /**
   * Issue a warning if the result type of the constructor declaration is not top. If it is a
   * supertype of the class, then a conflicting.annos error will also be issued by {@link
   * #isValidUse(AnnotatedTypeMirror.AnnotatedDeclaredType,AnnotatedTypeMirror.AnnotatedDeclaredType,Tree)}.
   *
   * @param constructorType the AnnotatedExecutableType for the constructor
   * @param constructorElement the element that declares the constructor
   */
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror returnType = constructorType.getReturnType();
    AnnotationMirrorSet constructorAnnotations = returnType.getPrimaryAnnotations();
    AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();

    for (AnnotationMirror top : tops) {
      AnnotationMirror constructorAnno =
          qualHierarchy.findAnnotationInHierarchy(constructorAnnotations, top);
      if (!AnnotationUtils.areSame(top, constructorAnno)) {
        checker.reportWarning(
            constructorElement, "inconsistent.constructor.type", constructorAnno, top);
      }
    }
  }

  /**
   * Reports errors found during purity checking.
   *
   * @param result whether the method is deterministic and/or side-effect-free
   * @param tree the method
   * @param expectedKinds the expected purity for the method
   */
  protected void reportPurityErrors(
      PurityResult result, MethodTree tree, EnumSet<Pure.Kind> expectedKinds) {
    assert !result.isPure(expectedKinds);
    EnumSet<Pure.Kind> violations = EnumSet.copyOf(expectedKinds);
    violations.removeAll(result.getKinds());
    if (violations.contains(Pure.Kind.DETERMINISTIC)
        || violations.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
      String msgKeyPrefix;
      if (!violations.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
        msgKeyPrefix = "purity.not.deterministic.";
      } else if (!violations.contains(Pure.Kind.DETERMINISTIC)) {
        msgKeyPrefix = "purity.not.sideeffectfree.";
      } else {
        msgKeyPrefix = "purity.not.deterministic.not.sideeffectfree.";
      }
      for (IPair<Tree, String> r : result.getNotBothReasons()) {
        reportPurityError(msgKeyPrefix, r);
      }
      if (violations.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
        for (IPair<Tree, String> r : result.getNotSEFreeReasons()) {
          reportPurityError("purity.not.sideeffectfree.", r);
        }
      }
      if (violations.contains(Pure.Kind.DETERMINISTIC)) {
        for (IPair<Tree, String> r : result.getNotDetReasons()) {
          reportPurityError("purity.not.deterministic.", r);
        }
      }
    }
  }

  /**
   * Reports a single purity error.
   *
   * @param msgKeyPrefix the prefix of the message key to use when reporting
   * @param r the result to report
   */
  private void reportPurityError(String msgKeyPrefix, IPair<Tree, String> r) {
    String reason = r.second;
    @SuppressWarnings("compilermessages")
    @CompilerMessageKey String msgKey = msgKeyPrefix + reason;
    if (reason.equals("call")) {
      if (r.first.getKind() == Tree.Kind.METHOD_INVOCATION) {
        MethodInvocationTree mitree = (MethodInvocationTree) r.first;
        checker.reportError(r.first, msgKey, mitree.getMethodSelect());
      } else {
        NewClassTree nctree = (NewClassTree) r.first;
        checker.reportError(r.first, msgKey, nctree.getIdentifier());
      }
    } else {
      checker.reportError(r.first, msgKey);
    }
  }

  /**
   * Check the contracts written on a method declaration. Ensures that the postconditions hold on
   * exit, and that the contracts are well-formed.
   *
   * @param methodTree the method declaration
   * @param methodElement the method element
   * @param formalParamNames the formal parameter names
   * @param abstractMethod whether the method is abstract
   */
  private void checkContractsAtMethodDeclaration(
      MethodTree methodTree,
      ExecutableElement methodElement,
      List<String> formalParamNames,
      boolean abstractMethod) {
    Set<Contract> contracts = atypeFactory.getContractsFromMethod().getContracts(methodElement);

    if (contracts.isEmpty()) {
      return;
    }
    StringToJavaExpression stringToJavaExpr =
        stringExpr -> StringToJavaExpression.atMethodBody(stringExpr, methodTree, checker);
    for (Contract contract : contracts) {
      String expressionString = contract.expressionString;
      AnnotationMirror annotation =
          contract.viewpointAdaptDependentTypeAnnotation(
              atypeFactory, stringToJavaExpr, methodTree);

      JavaExpression exprJe;
      try {
        exprJe = StringToJavaExpression.atMethodBody(expressionString, methodTree, checker);
      } catch (JavaExpressionParseException e) {
        DiagMessage diagMessage = e.getDiagMessage();
        if (diagMessage.getMessageKey().equals("flowexpr.parse.error")) {
          String s =
              String.format(
                  "'%s' in the %s %s on the declaration of method '%s': ",
                  expressionString,
                  contract.kind.errorKey,
                  contract.contractAnnotation.getAnnotationType().asElement().getSimpleName(),
                  methodTree.getName().toString());
          checker.reportError(methodTree, "flowexpr.parse.error", s + diagMessage.getArgs()[0]);
        } else {
          checker.report(methodTree, e.getDiagMessage());
        }
        continue;
      }
      if (!CFAbstractStore.canInsertJavaExpression(exprJe)) {
        checker.reportError(methodTree, "flowexpr.parse.error", expressionString);
        continue;
      }
      if (!abstractMethod && contract.kind != Contract.Kind.PRECONDITION) {
        // Check the contract, which is a postcondition.
        // Preconditions are checked at method invocations, not declarations.

        switch (contract.kind) {
          case POSTCONDITION:
            checkPostcondition(methodTree, annotation, exprJe);
            break;
          case CONDITIONALPOSTCONDITION:
            checkConditionalPostcondition(
                methodTree, annotation, exprJe, ((ConditionalPostcondition) contract).resultValue);
            break;
          default:
            throw new BugInCF("Impossible: " + contract.kind);
        }
      }

      if (formalParamNames != null && formalParamNames.contains(expressionString)) {
        String locationOfExpression =
            contract.kind.errorKey
                + " "
                + contract.contractAnnotation.getAnnotationType().asElement().getSimpleName()
                + " on the declaration";
        checker.reportWarning(
            methodTree,
            "expression.parameter.name.shadows.field",
            locationOfExpression,
            methodTree.getName().toString(),
            expressionString,
            expressionString,
            formalParamNames.indexOf(expressionString) + 1);
      }

      checkParametersAreEffectivelyFinal(methodTree, exprJe);
    }
  }

  /**
   * Scans a {@link JavaExpression} and adds all the parameters in the {@code JavaExpression} to the
   * passed set.
   */
  private final JavaExpressionScanner<Set<Element>> findParameters =
      new JavaExpressionScanner<Set<Element>>() {
        @Override
        protected Void visitLocalVariable(LocalVariable localVarExpr, Set<Element> parameters) {
          if (localVarExpr.getElement().getKind() == ElementKind.PARAMETER) {
            parameters.add(localVarExpr.getElement());
          }
          return super.visitLocalVariable(localVarExpr, parameters);
        }
      };

  /**
   * Check that the parameters used in {@code javaExpression} are effectively final for method
   * {@code method}.
   *
   * @param methodDeclTree a method declaration
   * @param javaExpression a Java expression
   */
  private void checkParametersAreEffectivelyFinal(
      MethodTree methodDeclTree, JavaExpression javaExpression) {
    // check that all parameters used in the expression are
    // effectively final, so that they cannot be modified
    Set<Element> parameters = new ArraySet<>(2);
    findParameters.scan(javaExpression, parameters);
    for (Element parameter : parameters) {
      if (!ElementUtils.isEffectivelyFinal(parameter)) {
        checker.reportError(
            methodDeclTree,
            "flowexpr.parameter.not.final",
            parameter.getSimpleName(),
            javaExpression);
      }
    }
  }

  /**
   * Check that the expression's type is annotated with {@code annotation} at the regular exit
   * store.
   *
   * @param methodTree declaration of the method
   * @param annotation expression's type must have this annotation
   * @param expression the expression that must have an annotation
   */
  protected void checkPostcondition(
      MethodTree methodTree, AnnotationMirror annotation, JavaExpression expression) {
    CFAbstractStore<?, ?> exitStore = atypeFactory.getRegularExitStore(methodTree);
    if (exitStore == null) {
      // If there is no regular exitStore, then the method cannot reach the regular exit and
      // there is no need to check anything.
    } else {
      CFAbstractValue<?> value = exitStore.getValue(expression);
      AnnotationMirror inferredAnno = null;
      if (value != null) {
        AnnotationMirrorSet annos = value.getAnnotations();
        inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, annotation);
      }
      if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
        checker.reportError(
            methodTree,
            "contracts.postcondition",
            methodTree.getName(),
            contractExpressionAndType(expression.toString(), inferredAnno),
            contractExpressionAndType(expression.toString(), annotation));
      }
    }
  }

  /**
   * Returns a string representation of an expression and type qualifier.
   *
   * @param expression a Java expression
   * @param qualifier the expression's type, or null if no information is available
   * @return a string representation of the expression and type qualifier
   */
  protected String contractExpressionAndType(
      String expression, @Nullable AnnotationMirror qualifier) {
    if (qualifier == null) {
      return "no information about " + expression;
    } else {
      return expression
          + " is "
          + atypeFactory.getAnnotationFormatter().formatAnnotationMirror(qualifier);
    }
  }

  /**
   * Check that the expression's type is annotated with {@code annotation} at every regular exit
   * that returns {@code result}.
   *
   * @param methodTree tree of method with the postcondition
   * @param annotation expression's type must have this annotation
   * @param expression the expression that the postcondition concerns
   * @param result result for which the postcondition is valid
   */
  protected void checkConditionalPostcondition(
      MethodTree methodTree,
      AnnotationMirror annotation,
      JavaExpression expression,
      boolean result) {
    boolean booleanReturnType =
        TypesUtils.isBooleanType(TreeUtils.typeOf(methodTree.getReturnType()));
    if (!booleanReturnType) {
      checker.reportError(methodTree, "contracts.conditional.postcondition.returntype");
      // No reason to go ahead with further checking. The
      // annotation is invalid.
      return;
    }

    for (IPair<ReturnNode, ?> pair : atypeFactory.getReturnStatementStores(methodTree)) {
      ReturnNode returnStmt = pair.first;

      Node retValNode = returnStmt.getResult();
      Boolean retVal =
          retValNode instanceof BooleanLiteralNode
              ? ((BooleanLiteralNode) retValNode).getValue()
              : null;

      TransferResult<?, ?> transferResult = (TransferResult<?, ?>) pair.second;
      if (transferResult == null) {
        // Unreachable return statements have no stores, but there is no need to check them.
        continue;
      }
      CFAbstractStore<?, ?> exitStore =
          (CFAbstractStore<?, ?>)
              (result ? transferResult.getThenStore() : transferResult.getElseStore());
      CFAbstractValue<?> value = exitStore.getValue(expression);

      // don't check if return statement certainly does not match 'result'. at the moment,
      // this means the result is a boolean literal
      if (!(retVal == null || retVal == result)) {
        continue;
      }
      AnnotationMirror inferredAnno = null;
      if (value != null) {
        AnnotationMirrorSet annos = value.getAnnotations();
        inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, annotation);
      }

      if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
        checker.reportError(
            returnStmt.getTree(),
            "contracts.conditional.postcondition",
            methodTree.getName(),
            result,
            contractExpressionAndType(expression.toString(), inferredAnno),
            contractExpressionAndType(expression.toString(), annotation));
      }
    }
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, Void p) {
    if (tree.getBounds().size() > 1) {
      // The upper bound of the type parameter is an intersection
      AnnotatedTypeVariable type =
          (AnnotatedTypeVariable) atypeFactory.getAnnotatedTypeFromTypeTree(tree);
      AnnotatedIntersectionType intersection = (AnnotatedIntersectionType) type.getUpperBound();
      checkExplicitAnnotationsOnIntersectionBounds(intersection, tree.getBounds());
    }
    validateTypeOf(tree);

    return super.visitTypeParameter(tree, p);
  }

  /**
   * Issues "explicit.annotation.ignored" warning if any explicit annotation on an intersection
   * bound is not the same as the primary annotation of the given intersection type.
   *
   * @param intersection type to use
   * @param boundTrees trees of {@code intersection} bounds
   */
  protected void checkExplicitAnnotationsOnIntersectionBounds(
      AnnotatedIntersectionType intersection, List<? extends Tree> boundTrees) {
    for (Tree boundTree : boundTrees) {
      if (boundTree.getKind() != Tree.Kind.ANNOTATED_TYPE) {
        continue;
      }
      List<? extends AnnotationMirror> explictAnnos =
          TreeUtils.annotationsFromTree((AnnotatedTypeTree) boundTree);
      for (AnnotationMirror explictAnno : explictAnnos) {
        if (atypeFactory.isSupportedQualifier(explictAnno)) {
          AnnotationMirror anno = intersection.getPrimaryAnnotationInHierarchy(explictAnno);
          if (!AnnotationUtils.areSame(anno, explictAnno)) {
            checker.reportWarning(
                boundTree, "explicit.annotation.ignored", explictAnno, anno, explictAnno, anno);
          }
        }
      }
    }
  }

  // **********************************************************************
  // Assignment checkers and pseudo-assignments
  // **********************************************************************

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    warnAboutTypeAnnotationsTooEarly(tree, tree.getModifiers());

    // VariableTree#getType returns null for binding variables from a DeconstructionPatternTree.
    if (tree.getType() != null) {
      visitAnnotatedType(tree.getModifiers().getAnnotations(), tree.getType());
    }

    AnnotatedTypeMirror variableType = atypeFactory.getAnnotatedTypeLhs(tree);

    atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(variableType, tree);
    Element varElt = TreeUtils.elementFromDeclaration(tree);
    if (varElt.getKind() == ElementKind.ENUM_CONSTANT) {
      commonAssignmentCheck(tree, tree.getInitializer(), "enum.declaration");
    } else if (tree.getInitializer() != null) {
      // If there's no assignment in this variable declaration, skip it.
      commonAssignmentCheck(tree, tree.getInitializer(), "assignment");
    } else {
      // commonAssignmentCheck validates the type of `tree`,
      // so only validate if commonAssignmentCheck wasn't called
      validateTypeOf(tree);
    }
    warnRedundantAnnotations(tree, variableType);
    return super.visitVariable(tree, p);
  }

  /**
   * Issues a "redundant.anno" warning if the annotation written on the type is the same as the
   * default annotation for this type and location.
   *
   * @param tree an AST node
   * @param type get the explicit annotation on this type and compare it with the default one for
   *     this type and location.
   */
  protected void warnRedundantAnnotations(Tree tree, AnnotatedTypeMirror type) {
    if (!warnRedundantAnnotations) {
      return;
    }
    Set<AnnotationMirror> explicitAnnos = type.getExplicitAnnotations();
    if (explicitAnnos.isEmpty()) {
      return;
    }
    if (tree == null) {
      throw new BugInCF("unexpected null tree argument!");
    }

    AnnotatedTypeMirror defaultAtm = atypeFactory.getDefaultAnnotationsForWarnRedundant(tree, type);
    for (AnnotationMirror explicitAnno : explicitAnnos) {
      AnnotationMirror defaultAm = defaultAtm.getPrimaryAnnotationInHierarchy(explicitAnno);
      if (defaultAm != null && AnnotationUtils.areSame(defaultAm, explicitAnno)) {
        checker.reportWarning(tree, "redundant.anno", defaultAtm);
      }
    }
  }

  /**
   * Warn if a type annotation is written before a modifier such as "public" or before a declaration
   * annotation.
   *
   * @param tree a VariableTree or a MethodTree
   * @param modifiersTree the modifiers sub-tree of tree
   */
  private void warnAboutTypeAnnotationsTooEarly(Tree tree, ModifiersTree modifiersTree) {

    // Don't issue warnings about compiler-inserted modifiers.
    // This simple code completely igonores enum constants and try-with-resources declarations.
    // It could be made to catch some user errors in those locations, but it doesn't seem worth
    // the effort to do so.
    if (tree.getKind() == Tree.Kind.VARIABLE) {
      ElementKind varKind = TreeUtils.elementFromDeclaration((VariableTree) tree).getKind();
      switch (varKind) {
        case ENUM_CONSTANT:
          // Enum constants are "public static final" by default, so the annotation always
          // appears to be before "public".
          return;
        case RESOURCE_VARIABLE:
          // Try-with-resources variables are "final" by default, so the annotation always
          // appears to be before "final".
          return;
        default:
          if (TreeUtils.isAutoGeneratedRecordMember(tree)) {
            // Annotations can appear on record fields before the class body, so don't
            // issue a warning about those.
            return;
          }
          // Nothing to do
      }
    }

    Set<Modifier> modifierSet = modifiersTree.getFlags();
    List<? extends AnnotationTree> annotations = modifiersTree.getAnnotations();

    if (annotations.isEmpty()) {
      return;
    }

    // Warn about type annotations written before modifiers such as "public".  javac retains no
    // information about modifier locations.  So, this is a very partial check:  Issue a warning
    // if a type annotation is at the very beginning of the VariableTree, and a modifier follows
    // it.

    // Check if a type annotation precedes a declaration annotation.
    int lastDeclAnnoIndex = -1;
    for (int i = annotations.size() - 1; i > 0; i--) { // no need to check index 0
      if (!isTypeAnnotation(annotations.get(i))) {
        lastDeclAnnoIndex = i;
        break;
      }
    }
    if (lastDeclAnnoIndex != -1) {
      // Usually, there are few bad invariant annotations.
      List<AnnotationTree> badTypeAnnos = new ArrayList<>(2);
      for (int i = 0; i < lastDeclAnnoIndex; i++) {
        AnnotationTree anno = annotations.get(i);
        if (isTypeAnnotation(anno)) {
          badTypeAnnos.add(anno);
        }
      }
      if (!badTypeAnnos.isEmpty()) {
        checker.reportWarning(
            tree, "type.anno.before.decl.anno", badTypeAnnos, annotations.get(lastDeclAnnoIndex));
      }
    }

    // Determine the length of the text that ought to precede the first type annotation.
    // If the type annotation appears before that text could appear, then warn that a
    // modifier appears after the type annotation.
    // TODO: in the future, account for the lengths of declaration annotations.  Length of
    // toString of the annotation isn't useful, as it might be different length than original
    // input.  Can use JCTree.getEndPosition(EndPosTable) and
    // com.sun.tools.javac.tree.EndPosTable, but it requires -Xjcov.
    AnnotationTree firstAnno = annotations.get(0);
    if (!modifierSet.isEmpty() && isTypeAnnotation(firstAnno)) {
      int precedingTextLength = 0;
      for (Modifier m : modifierSet) {
        precedingTextLength += m.toString().length() + 1; // +1 for the space
      }
      int annoStartPos = ((JCTree) firstAnno).getStartPosition();
      int varStartPos = ((JCTree) tree).getStartPosition();
      if (annoStartPos < varStartPos + precedingTextLength) {
        checker.reportWarning(tree, "type.anno.before.modifier", firstAnno, modifierSet);
      }
    }
  }

  /**
   * Return true if the given annotation is a type annotation: that is, its definition is
   * meta-annotated with {@code @Target({TYPE_USE,....})}.
   */
  private boolean isTypeAnnotation(AnnotationTree anno) {
    Tree annoType = anno.getAnnotationType();
    ClassSymbol annoSymbol;
    switch (annoType.getKind()) {
      case IDENTIFIER:
        annoSymbol = (ClassSymbol) ((JCIdent) annoType).sym;
        break;
      case MEMBER_SELECT:
        annoSymbol = (ClassSymbol) ((JCFieldAccess) annoType).sym;
        break;
      default:
        throw new BugInCF("Unhandled kind: " + annoType.getKind() + " for " + anno);
    }
    for (AnnotationMirror metaAnno : annoSymbol.getAnnotationMirrors()) {
      if (AnnotationUtils.areSameByName(metaAnno, TARGET)) {
        AnnotationValue av = metaAnno.getElementValues().get(targetValueElement);
        return AnnotationUtils.annotationValueContainsToString(av, "TYPE_USE");
      }
    }

    return false;
  }

  /**
   * Performs two checks: subtyping and assignability checks, using {@link
   * #commonAssignmentCheck(Tree, ExpressionTree, String, Object[])}.
   *
   * <p>If the subtype check fails, it issues an "assignment" error.
   */
  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    commonAssignmentCheck(tree.getVariable(), tree.getExpression(), "assignment");
    return super.visitAssignment(tree, p);
  }

  /**
   * Performs a subtype check, to test whether the tree expression iterable type is a subtype of the
   * variable type in the enhanced for loop.
   *
   * <p>If the subtype check fails, it issues a "enhancedfor" error.
   */
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(tree.getVariable());
    AnnotatedTypeMirror iteratedType = atypeFactory.getIterableElementType(tree.getExpression());
    boolean valid = validateTypeOf(tree.getVariable());
    if (valid) {
      commonAssignmentCheck(var, iteratedType, tree.getExpression(), "enhancedfor");
    }
    return super.visitEnhancedForLoop(tree, p);
  }

  /**
   * Performs a method invocation check.
   *
   * <p>An invocation of a method, m, on the receiver, r is valid only if:
   *
   * <ul>
   *   <li>passed arguments are subtypes of corresponding m parameters
   *   <li>r is a subtype of m receiver type
   *   <li>if m is generic, passed type arguments are subtypes of m type variables
   * </ul>
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {

    // Skip calls to the Enum constructor (they're generated by javac and
    // hard to check), also see CFGBuilder.visitMethodInvocation.
    if (TreeUtils.elementFromUse(tree) == null || TreeUtils.isEnumSuperCall(tree)) {
      return super.visitMethodInvocation(tree, p);
    }

    if (shouldSkipUses(tree)) {
      return super.visitMethodInvocation(tree, p);
    }
    ParameterizedExecutableType preInference =
        atypeFactory.methodFromUseWithoutTypeArgInference(tree);
    if (!preInference.executableType.getElement().getTypeParameters().isEmpty()
        && preInference.typeArgs.isEmpty()) {
      if (!checkTypeArgumentInference(tree, preInference.executableType)) {
        return null;
      }
    }
    ParameterizedExecutableType mType = atypeFactory.methodFromUse(tree);
    AnnotatedExecutableType invokedMethod = mType.executableType;
    List<AnnotatedTypeMirror> typeargs = mType.typeArgs;

    List<AnnotatedTypeParameterBounds> paramBounds =
        CollectionsPlume.mapList(
            AnnotatedTypeVariable::getBounds, invokedMethod.getTypeVariables());

    ExecutableElement method = invokedMethod.getElement();
    CharSequence methodName = ElementUtils.getSimpleDescription(method);
    checkTypeArguments(
        tree,
        paramBounds,
        typeargs,
        tree.getTypeArguments(),
        methodName,
        invokedMethod.getTypeVariables());
    List<AnnotatedTypeMirror> params =
        AnnotatedTypes.adaptParameters(atypeFactory, invokedMethod, tree.getArguments(), tree);
    checkArguments(params, tree.getArguments(), methodName, method.getParameters());
    checkVarargs(invokedMethod, tree);

    if (ElementUtils.isMethod(
        invokedMethod.getElement(), vectorCopyInto, atypeFactory.getProcessingEnv())) {
      typeCheckVectorCopyIntoArgument(tree, params);
    }

    ExecutableElement invokedMethodElement = invokedMethod.getElement();
    if (!ElementUtils.isStatic(invokedMethodElement) && !TreeUtils.isSuperConstructorCall(tree)) {
      checkMethodInvocability(invokedMethod, tree);
    }

    // check precondition annotations
    checkPreconditions(
        tree, atypeFactory.getContractsFromMethod().getPreconditions(invokedMethodElement));

    if (TreeUtils.isSuperConstructorCall(tree)) {
      checkSuperConstructorCall(tree);
    } else if (TreeUtils.isThisConstructorCall(tree)) {
      checkThisConstructorCall(tree);
    }

    // Do not call super, as that would observe the arguments without
    // a set assignment context.
    scan(tree.getMethodSelect(), p);
    return null; // super.visitMethodInvocation(tree, p);
  }

  /**
   * Reports a "type.arguments.not.inferred" error if type argument inference fails and returns
   * false if inference fails.
   *
   * @param tree a tree that requires type argument inference
   * @param methodType the type of the method before type argument substitution
   * @return whether type argument inference succeeds
   */
  private boolean checkTypeArgumentInference(
      ExpressionTree tree, AnnotatedExecutableType methodType) {
    InferenceResult args =
        atypeFactory.getTypeArgumentInference().inferTypeArgs(atypeFactory, tree, methodType);
    if (args != null && !args.inferenceFailed()) {
      return true;
    }
    if (args.inferenceCrashed()) {
      checker.reportError(
          tree,
          "type.argument.inference.crashed",
          ElementUtils.getSimpleDescription(methodType.getElement()),
          args == null ? "" : args.getErrorMsg());
      return false;
    }
    checker.reportError(
        tree,
        "type.arguments.not.inferred",
        ElementUtils.getSimpleDescription(methodType.getElement()),
        args == null ? "" : args.getErrorMsg());
    return false;
  }

  /**
   * Checks that the following rule is satisfied: The type on a constructor declaration must be a
   * supertype of the return type of "this()" invocation within that constructor.
   *
   * <p>Subclasses can override this method to change the behavior for just "this" constructor
   * class. Or override {@link #checkThisOrSuperConstructorCall(MethodInvocationTree, String)} to
   * change the behavior for "this" and "super" constructor calls.
   *
   * @param thisCall the AST node for the constructor call
   */
  protected void checkThisConstructorCall(MethodInvocationTree thisCall) {
    checkThisOrSuperConstructorCall(thisCall, "this.invocation");
  }

  /**
   * Checks that the following rule is satisfied: The type on a constructor declaration must be a
   * supertype of the return type of "super()" invocation within that constructor.
   *
   * <p>Subclasses can override this method to change the behavior for just "super" constructor
   * class. Or override {@link #checkThisOrSuperConstructorCall(MethodInvocationTree, String)} to
   * change the behavior for "this" and "super" constructor calls.
   *
   * @param superCall the AST node for the super constructor call
   */
  protected void checkSuperConstructorCall(MethodInvocationTree superCall) {
    checkThisOrSuperConstructorCall(superCall, "super.invocation");
  }

  /**
   * Checks that the following rule is satisfied: The type on a constructor declaration must be a
   * supertype of the return type of "this()" or "super()" invocation within that constructor.
   *
   * @param call the AST node for the constructor call
   * @param errorKey the error message key to use if the check fails
   */
  protected void checkThisOrSuperConstructorCall(
      MethodInvocationTree call, @CompilerMessageKey String errorKey) {
    TreePath path = atypeFactory.getPath(call);
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(path);
    AnnotatedTypeMirror superType = atypeFactory.getAnnotatedType(call);
    AnnotatedExecutableType constructorType = atypeFactory.getAnnotatedType(enclosingMethod);
    AnnotatedTypeMirror returnType = constructorType.getReturnType();
    AnnotationMirrorSet topAnnotations = qualHierarchy.getTopAnnotations();
    for (AnnotationMirror topAnno : topAnnotations) {
      if (!typeHierarchy.isSubtypeShallowEffective(superType, returnType, topAnno)) {
        AnnotationMirror superAnno = superType.getPrimaryAnnotationInHierarchy(topAnno);
        AnnotationMirror constructorReturnAnno =
            returnType.getPrimaryAnnotationInHierarchy(topAnno);
        checker.reportError(call, errorKey, constructorReturnAnno, call, superAnno);
      }
    }
  }

  /**
   * If the given invocation is a varargs invocation, check that the array type of actual varargs is
   * a subtype of the corresponding formal parameter; issues "argument" error if not.
   *
   * <p>The caller must type-check for each element in varargs before or after calling this method.
   *
   * @see #checkArguments
   * @param invokedMethod the method type to be invoked
   * @param tree method or constructor invocation tree
   */
  protected void checkVarargs(AnnotatedExecutableType invokedMethod, Tree tree) {
    if (!TreeUtils.isVarargsCall(tree)) {
      // If not a varargs invocation, type checking is already done in checkArguments.
      return;
    }

    List<AnnotatedTypeMirror> formals = invokedMethod.getParameterTypes();
    int numFormals = formals.size();
    int lastArgIndex = numFormals - 1;
    // This is the varags type, an array.
    AnnotatedArrayType lastParamAnnotatedType = (AnnotatedArrayType) formals.get(lastArgIndex);

    AnnotatedTypeMirror wrappedVarargsType = atypeFactory.getAnnotatedTypeVarargsArray(tree);

    // When dataflow analysis is not enabled, it will be null and we can suppose there is no
    // annotation to be checked for generated varargs array.
    if (wrappedVarargsType == null) {
      return;
    }

    // The component type of wrappedVarargsType might not be a subtype of the component type of
    // lastParamAnnotatedType due to the difference of type inference between for an expression
    // and an invoked method element. We can consider that the component type of actual is same
    // with formal one because type checking for elements will be done in checkArguments. This
    // is also needed to avoid duplicating error message caused by elements in varargs.
    if (wrappedVarargsType.getKind() == TypeKind.ARRAY) {
      ((AnnotatedArrayType) wrappedVarargsType)
          .setComponentType(lastParamAnnotatedType.getComponentType());
    }

    commonAssignmentCheck(lastParamAnnotatedType, wrappedVarargsType, tree, "varargs");
  }

  /**
   * Checks that all the given {@code preconditions} hold true immediately prior to the method
   * invocation or variable access at {@code tree}.
   *
   * @param tree the method invocation; immediately prior to it, the preconditions must hold true
   * @param preconditions the preconditions to be checked
   */
  protected void checkPreconditions(MethodInvocationTree tree, Set<Precondition> preconditions) {
    // This check is needed for the GUI effects and Units Checkers tests to pass.
    // TODO: Remove this check and investigate the root cause.
    if (preconditions.isEmpty()) {
      return;
    }

    StringToJavaExpression stringToJavaExpr =
        stringExpr -> StringToJavaExpression.atMethodInvocation(stringExpr, tree, checker);
    for (Contract c : preconditions) {
      Precondition p = (Precondition) c;
      String expressionString = p.expressionString;
      AnnotationMirror anno =
          c.viewpointAdaptDependentTypeAnnotation(atypeFactory, stringToJavaExpr, tree);
      JavaExpression exprJe;
      try {
        exprJe = StringToJavaExpression.atMethodInvocation(expressionString, tree, checker);
      } catch (JavaExpressionParseException e) {
        // report errors here
        checker.report(tree, e.getDiagMessage());
        return;
      }

      CFAbstractStore<?, ?> store = atypeFactory.getStoreBefore(tree);
      CFAbstractValue<?> value = null;
      if (CFAbstractStore.canInsertJavaExpression(exprJe)) {
        value = store.getValue(exprJe);
      }
      AnnotationMirror inferredAnno = null;
      if (value != null) {
        AnnotationMirrorSet annos = value.getAnnotations();
        inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, anno);
      } else {
        // If the expression is "this", then get the type of the method receiver.
        // TODO: There are other expressions that can be converted to trees, "#1" for
        // example.
        if (expressionString.equals("this")) {
          AnnotatedTypeMirror atype = atypeFactory.getReceiverType(tree);
          if (atype != null) {
            AnnotationMirrorSet annos = atype.getEffectiveAnnotations();
            inferredAnno = qualHierarchy.findAnnotationInSameHierarchy(annos, anno);
          }
        }

        if (inferredAnno == null) {
          // If there is no information in the store (possible if e.g., no refinement
          // of the field has occurred), use top instead of automatically
          // issuing a warning. This is not perfectly precise: for example,
          // if jeExpr is a field it would be more precise to use the field's
          // declared type rather than top. However, doing so would be unsound
          // in at least three circumstances where the type of the field depends
          // on the type of the receiver: (1) all fields in Nullness Checker,
          // because of possibility that the receiver is under initialization,
          // (2) polymorphic fields, and (3) fields whose type is a type variable.
          // Using top here instead means that there is no need for special cases
          // for these situations.
          inferredAnno = qualHierarchy.getTopAnnotation(anno);
        }
      }
      if (!checkContract(exprJe, anno, inferredAnno, store)) {
        if (exprJe != null) {
          expressionString = exprJe.toString();
        }
        checker.reportError(
            tree,
            "contracts.precondition",
            tree.getMethodSelect().toString(),
            contractExpressionAndType(expressionString, inferredAnno),
            contractExpressionAndType(expressionString, anno));
      }
    }
  }

  /**
   * Returns true if and only if {@code inferredAnnotation} is valid for a given expression to match
   * the {@code necessaryAnnotation}.
   *
   * <p>By default, {@code inferredAnnotation} must be a subtype of {@code necessaryAnnotation}, but
   * subclasses might override this behavior.
   */
  protected boolean checkContract(
      JavaExpression expr,
      AnnotationMirror necessaryAnnotation,
      AnnotationMirror inferredAnnotation,
      CFAbstractStore<?, ?> store) {
    if (inferredAnnotation == null) {
      return false;
    }
    TypeMirror exprTM = expr.getType();
    return qualHierarchy.isSubtypeShallow(inferredAnnotation, necessaryAnnotation, exprTM);
  }

  /**
   * Type checks the method arguments of {@code Vector.copyInto()}.
   *
   * <p>The Checker Framework special-cases the method invocation, as its type safety cannot be
   * expressed by Java's type system.
   *
   * <p>For a Vector {@code v} of type {@code Vector<E>}, the method invocation {@code
   * v.copyInto(arr)} is type-safe iff {@code arr} is an array of type {@code T[]}, where {@code T}
   * is a subtype of {@code E}.
   *
   * <p>In other words, this method checks that the type argument of the receiver method is a
   * subtype of the component type of the passed array argument.
   *
   * @param tree a method invocation of {@code Vector.copyInto()}
   * @param params the types of the parameters of {@code Vectory.copyInto()}
   */
  protected void typeCheckVectorCopyIntoArgument(
      MethodInvocationTree tree, List<? extends AnnotatedTypeMirror> params) {
    assert params.size() == 1
        : "invalid no. of parameters " + params + " found for method invocation " + tree;
    assert tree.getArguments().size() == 1
        : "invalid no. of arguments in method invocation " + tree;

    AnnotatedTypeMirror passed = atypeFactory.getAnnotatedType(tree.getArguments().get(0));
    AnnotatedArrayType passedAsArray = (AnnotatedArrayType) passed;

    AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);
    AnnotatedDeclaredType receiverAsVector =
        AnnotatedTypes.asSuper(atypeFactory, receiver, vectorType);
    if (receiverAsVector.getTypeArguments().isEmpty()) {
      return;
    }

    AnnotatedTypeMirror argComponent = passedAsArray.getComponentType();
    AnnotatedTypeMirror vectorTypeArg = receiverAsVector.getTypeArguments().get(0);
    Tree errorLocation = tree.getArguments().get(0);
    if (TypesUtils.isErasedSubtype(
        vectorTypeArg.getUnderlyingType(), argComponent.getUnderlyingType(), types)) {
      commonAssignmentCheck(argComponent, vectorTypeArg, errorLocation, "vector.copyinto");
    } else {
      checker.reportError(errorLocation, "vector.copyinto", vectorTypeArg, argComponent);
    }
  }

  /**
   * Performs a new class invocation check.
   *
   * <p>An invocation of a constructor, c, is valid only if:
   *
   * <ul>
   *   <li>passed arguments are subtypes of corresponding c parameters
   *   <li>if c is generic, passed type arguments are subtypes of c type variables
   * </ul>
   */
  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    if (checker.shouldSkipUses(TreeUtils.elementFromUse(tree))) {
      return super.visitNewClass(tree, p);
    }

    ParameterizedExecutableType preInference =
        atypeFactory.constructorFromUseWithoutTypeArgInference(tree);
    if (!preInference.executableType.getElement().getTypeParameters().isEmpty()
        || TreeUtils.isDiamondTree(tree)) {
      if (!checkTypeArgumentInference(tree, preInference.executableType)) {
        return null;
      }
    }

    ParameterizedExecutableType fromUse = atypeFactory.constructorFromUse(tree);
    AnnotatedExecutableType constructorType = fromUse.executableType;
    List<AnnotatedTypeMirror> typeargs = fromUse.typeArgs;

    List<? extends ExpressionTree> passedArguments = tree.getArguments();
    List<AnnotatedTypeMirror> params =
        AnnotatedTypes.adaptParameters(atypeFactory, constructorType, passedArguments, tree);

    ExecutableElement constructor = constructorType.getElement();
    CharSequence constructorName = ElementUtils.getSimpleDescription(constructor);

    checkArguments(params, passedArguments, constructorName, constructor.getParameters());
    checkVarargs(constructorType, tree);

    List<AnnotatedTypeParameterBounds> paramBounds =
        CollectionsPlume.mapList(
            AnnotatedTypeVariable::getBounds, constructorType.getTypeVariables());

    checkTypeArguments(
        tree,
        paramBounds,
        typeargs,
        tree.getTypeArguments(),
        constructorName,
        constructor.getTypeParameters());

    boolean valid = validateTypeOf(tree);

    if (valid) {
      AnnotatedDeclaredType dt = atypeFactory.getAnnotatedType(tree);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(dt, tree);
      checkConstructorInvocation(dt, constructorType, tree);
    }
    // Do not call super, as that would observe the arguments without
    // a set assignment context.
    scan(tree.getEnclosingExpression(), p);
    scan(tree.getIdentifier(), p);
    scan(tree.getClassBody(), p);

    return null;
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {

    AnnotatedExecutableType functionType = atypeFactory.getFunctionTypeFromTree(tree);

    if (tree.getBody().getKind() != Tree.Kind.BLOCK) {
      // Check return type for single statement returns here.
      AnnotatedTypeMirror ret = functionType.getReturnType();
      if (ret.getKind() != TypeKind.VOID) {
        commonAssignmentCheck(ret, (ExpressionTree) tree.getBody(), "return");
      }
    }

    // Check parameters
    for (int i = 0; i < functionType.getParameterTypes().size(); ++i) {
      AnnotatedTypeMirror lambdaParameter =
          atypeFactory.getAnnotatedType(tree.getParameters().get(i));
      commonAssignmentCheck(
          lambdaParameter,
          functionType.getParameterTypes().get(i),
          tree.getParameters().get(i),
          "lambda.param",
          i);
    }

    // TODO: Postconditions?
    // https://github.com/typetools/checker-framework/issues/801

    return super.visitLambdaExpression(tree, p);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree tree, Void p) {
    this.checkMethodReferenceAsOverride(tree, p);
    return super.visitMemberReference(tree, p);
  }

  /** A set containing {@code Tree.Kind.METHOD} and {@code Tree.Kind.LAMBDA_EXPRESSION}. */
  private ArraySet<Tree.Kind> methodAndLambdaExpression =
      new ArraySet<>(Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION));

  /**
   * Checks that the type of the return expression is a subtype of the enclosing method required
   * return type. If not, it issues a "return" error.
   */
  @Override
  public Void visitReturn(ReturnTree tree, Void p) {
    // Don't try to check return expressions for void methods.
    if (tree.getExpression() == null) {
      return super.visitReturn(tree, p);
    }

    Tree enclosing = TreePathUtil.enclosingOfKind(getCurrentPath(), methodAndLambdaExpression);

    AnnotatedTypeMirror declaredReturnType = null;
    if (enclosing.getKind() == Tree.Kind.METHOD) {
      MethodTree enclosingMethod = (MethodTree) enclosing;
      boolean valid = validateTypeOf(enclosingMethod);
      if (valid) {
        declaredReturnType = atypeFactory.getMethodReturnType(enclosingMethod, tree);
      }
    } else {
      AnnotatedExecutableType result =
          atypeFactory.getFunctionTypeFromTree((LambdaExpressionTree) enclosing);
      declaredReturnType = result.getReturnType();
    }

    if (declaredReturnType != null) {
      commonAssignmentCheck(declaredReturnType, tree.getExpression(), "return");
    }
    return super.visitReturn(tree, p);
  }

  /**
   * Ensure that the annotation arguments comply to their declarations. This needs some special
   * casing, as annotation arguments form special trees.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    List<? extends ExpressionTree> args = tree.getArguments();
    if (args.isEmpty()) {
      // Nothing to do if there are no annotation arguments.
      return null;
    }

    TypeElement annoType = (TypeElement) TreeInfo.symbol((JCTree) tree.getAnnotationType());

    Name annoName = annoType.getQualifiedName();
    if (annoName.contentEquals(DefaultQualifier.class.getName())
        || annoName.contentEquals(SuppressWarnings.class.getName())) {
      // Skip these two annotations, as we don't care about the arguments to them.
      return null;
    }

    List<ExecutableElement> methods = ElementFilter.methodsIn(annoType.getEnclosedElements());
    // Mapping from argument simple name to its annotated type.
    Map<String, AnnotatedTypeMirror> annoTypes = ArrayMap.newArrayMapOrHashMap(methods.size());
    for (ExecutableElement meth : methods) {
      AnnotatedExecutableType exeatm = atypeFactory.getAnnotatedType(meth);
      AnnotatedTypeMirror retty = exeatm.getReturnType();
      annoTypes.put(meth.getSimpleName().toString(), retty);
    }

    for (ExpressionTree arg : args) {
      if (!(arg instanceof AssignmentTree)) {
        // TODO: when can this happen?
        continue;
      }

      AssignmentTree at = (AssignmentTree) arg;
      // Ensure that we never ask for the annotated type of an annotation, because
      // we don't have a type for annotations.
      if (at.getExpression().getKind() == Tree.Kind.ANNOTATION) {
        visitAnnotation((AnnotationTree) at.getExpression(), p);
        continue;
      }
      if (at.getExpression().getKind() == Tree.Kind.NEW_ARRAY) {
        NewArrayTree nat = (NewArrayTree) at.getExpression();
        boolean isAnno = false;
        for (ExpressionTree init : nat.getInitializers()) {
          if (init.getKind() == Tree.Kind.ANNOTATION) {
            visitAnnotation((AnnotationTree) init, p);
            isAnno = true;
          }
        }
        if (isAnno) {
          continue;
        }
      }

      AnnotatedTypeMirror expected = annoTypes.get(at.getVariable().toString());
      AnnotatedTypeMirror actual = atypeFactory.getAnnotatedType(at.getExpression());
      if (expected.getKind() != TypeKind.ARRAY) {
        // Expected is not an array -> direct comparison.
        commonAssignmentCheck(expected, actual, at.getExpression(), "annotation");
      } else if (actual.getKind() == TypeKind.ARRAY) {
        // Both actual and expected are arrays.
        commonAssignmentCheck(expected, actual, at.getExpression(), "annotation");
      } else {
        // The declaration is an array type, but just a single element is given.
        commonAssignmentCheck(
            ((AnnotatedArrayType) expected).getComponentType(),
            actual,
            at.getExpression(),
            "annotation");
      }
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
    if (TreeUtils.isPolyExpression(tree)) {
      // From the JLS:
      // A poly reference conditional expression is compatible with a target type T if its
      // second and third operand expressions are compatible with T.  In the Checker
      // Framework this check happens in #commonAssignmentCheck.
      return super.visitConditionalExpression(tree, p);
    }

    // If the computation of the type of the ConditionalExpressionTree in
    // org.checkerframework.framework.type.TypeFromTree.TypeFromExpression.visitConditionalExpression(ConditionalExpressionTree,
    // AnnotatedTypeFactory) is correct, the following checks are redundant. However, let's add
    // another failsafe guard and do the checks.
    AnnotatedTypeMirror cond = atypeFactory.getAnnotatedType(tree);
    this.commonAssignmentCheck(cond, tree.getTrueExpression(), "conditional");
    this.commonAssignmentCheck(cond, tree.getFalseExpression(), "conditional");
    return super.visitConditionalExpression(tree, p);
  }

  /**
   * This method validates the type of the switch expression. It issues an error if the type of a
   * value that the switch expression can result is not a subtype of the switch type.
   *
   * <p>If a subclass overrides this method, it must call {@code super.scan(switchExpressionTree,
   * null)} so that the blocks and statements in the cases are checked.
   *
   * @param switchExpressionTree a {@code SwitchExpressionTree}
   */
  public void visitSwitchExpression17(Tree switchExpressionTree) {
    boolean valid = validateTypeOf(switchExpressionTree);
    if (valid) {
      AnnotatedTypeMirror switchType = atypeFactory.getAnnotatedType(switchExpressionTree);
      SwitchExpressionScanner<Void, Void> scanner =
          new FunctionalSwitchExpressionScanner<>(
              (ExpressionTree valueTree, Void unused) -> {
                BaseTypeVisitor.this.commonAssignmentCheck(
                    switchType, valueTree, "switch.expression");
                return null;
              },
              (r1, r2) -> null);

      scanner.scanSwitchExpression(switchExpressionTree, null);
    }
    super.scan(switchExpressionTree, null);
  }

  // **********************************************************************
  // Check for illegal re-assignment
  // **********************************************************************

  /** Performs assignability check. */
  @Override
  public Void visitUnary(UnaryTree tree, Void p) {
    Tree.Kind treeKind = tree.getKind();
    if (treeKind == Tree.Kind.PREFIX_DECREMENT
        || treeKind == Tree.Kind.PREFIX_INCREMENT
        || treeKind == Tree.Kind.POSTFIX_DECREMENT
        || treeKind == Tree.Kind.POSTFIX_INCREMENT) {
      // Check the assignment that occurs at the increment/decrement. i.e.:
      // exp = exp + 1 or exp = exp - 1
      AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(tree.getExpression());
      AnnotatedTypeMirror valueType;
      if (treeKind == Tree.Kind.POSTFIX_DECREMENT || treeKind == Tree.Kind.POSTFIX_INCREMENT) {
        // For postfixed increments or decrements, the type of the tree the type of the
        // expression before 1 is added or subtracted. So, use a special method to get the
        // type after 1 has been added or subtracted.
        valueType = atypeFactory.getAnnotatedTypeRhsUnaryAssign(tree);
      } else {
        // For prefixed increments or decrements, the type of the tree the type of the
        // expression after 1 is added or subtracted. So, its type can be found using the
        // usual method.
        valueType = atypeFactory.getAnnotatedType(tree);
      }
      String errorKey =
          (treeKind == Tree.Kind.PREFIX_INCREMENT || treeKind == Tree.Kind.POSTFIX_INCREMENT)
              ? "unary.increment"
              : "unary.decrement";
      commonAssignmentCheck(varType, valueType, tree, errorKey);
    }
    return super.visitUnary(tree, p);
  }

  /** Performs assignability check. */
  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    // If tree is the tree representing the compounds assignment s += expr,
    // Then this method should check whether s + expr can be assigned to s,
    // but the "s + expr" tree does not exist.  So instead, check that
    // s += expr can be assigned to s.
    commonAssignmentCheck(tree.getVariable(), tree, "compound.assignment");
    return super.visitCompoundAssignment(tree, p);
  }

  // **********************************************************************
  // Check for invalid types inserted by the user
  // **********************************************************************

  @Override
  public Void visitNewArray(NewArrayTree tree, Void p) {
    boolean valid = validateTypeOf(tree);

    if (valid && tree.getType() != null) {
      AnnotatedArrayType arrayType = atypeFactory.getAnnotatedType(tree);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(arrayType, tree);
      if (tree.getInitializers() != null) {
        checkArrayInitialization(arrayType.getComponentType(), tree.getInitializers());
      }
    }

    return super.visitNewArray(tree, p);
  }

  /**
   * If the lint option "cast:redundant" is set, this method issues a warning if the cast is
   * redundant.
   */
  protected void checkTypecastRedundancy(TypeCastTree typeCastTree) {
    if (!checker.getLintOption("cast:redundant", false)) {
      return;
    }

    AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(typeCastTree);
    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(typeCastTree.getExpression());

    if (castType.equals(exprType)) {
      checker.reportWarning(typeCastTree, "cast.redundant", castType);
    }
  }

  /**
   * Issues a warning if the given explicitly-written typecast is unsafe. Does nothing if the lint
   * option "cast:unsafe" is not set. Only primary qualifiers are checked unless the command line
   * option "checkCastElementType" is supplied.
   *
   * @param typeCastTree an explicitly-written typecast
   */
  protected void checkTypecastSafety(TypeCastTree typeCastTree) {
    if (!checker.getLintOption("cast:unsafe", true)) {
      return;
    }
    AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(typeCastTree);
    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(typeCastTree.getExpression());
    boolean reported = false;
    for (AnnotationMirror top : atypeFactory.getQualifierParameterHierarchies(castType)) {
      if (!isTypeCastSafeInvariant(castType, exprType, top)) {
        checker.reportError(
            typeCastTree,
            "invariant.cast.unsafe",
            exprType.toString(true),
            castType.toString(true));
      }
      reported = true; // don't issue cast unsafe warning.
    }

    // Don't call TypeHierarchy#isSubtype(exprType, castType) because the underlying Java types
    // will not be in the correct subtyping relationship if this is a downcast.
    if (!reported && !isTypeCastSafe(castType, exprType)) {
      checker.reportWarning(
          typeCastTree, "cast.unsafe", exprType.toString(true), castType.toString(true));
    }
  }

  /**
   * Returns true if the cast is safe.
   *
   * <p>Only primary qualifiers are checked unless the command line option "checkCastElementType" is
   * supplied.
   *
   * @param castType annotated type of the cast
   * @param exprType annotated type of the casted expression
   * @return true if the type cast is safe, false otherwise
   */
  protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {

    TypeKind castTypeKind = castType.getKind();
    if (castTypeKind == TypeKind.DECLARED) {
      // Don't issue an error if the annotations are equivalent to the qualifier upper bound
      // of the type.
      AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType) castType;
      AnnotationMirrorSet bounds =
          atypeFactory.getTypeDeclarationBounds(castDeclared.getUnderlyingType());

      if (AnnotationUtils.areSame(castDeclared.getPrimaryAnnotations(), bounds)) {
        return true;
      }
    }

    AnnotationMirrorSet castAnnos;
    AnnotatedTypeMirror newCastType;
    TypeMirror newCastTM;
    if (!checkCastElementType) {
      // checkCastElementType option wasn't specified, so only check effective annotations.
      castAnnos = castType.getEffectiveAnnotations();
      newCastType = castType;
      newCastTM = newCastType.getUnderlyingType();
    } else {
      if (castTypeKind == TypeKind.TYPEVAR) {
        newCastType = ((AnnotatedTypeVariable) castType).getUpperBound();
      } else {
        newCastType = castType;
      }
      newCastTM = newCastType.getUnderlyingType();
      AnnotatedTypeMirror newExprType;
      if (exprType.getKind() == TypeKind.TYPEVAR) {
        newExprType = ((AnnotatedTypeVariable) exprType).getUpperBound();
      } else {
        newExprType = exprType;
      }
      TypeMirror newExprTM = newExprType.getUnderlyingType();

      if (!typeHierarchy.isSubtype(newExprType, newCastType)) {
        return false;
      }
      if (newCastType.getKind() == TypeKind.ARRAY && newExprType.getKind() != TypeKind.ARRAY) {
        // Always warn if the cast contains an array, but the expression
        // doesn't, as in "(Object[]) o" where o is of type Object
        return false;
      } else if (newCastType.getKind() == TypeKind.DECLARED
          && newExprType.getKind() == TypeKind.DECLARED) {
        int castSize = ((AnnotatedDeclaredType) newCastType).getTypeArguments().size();
        int exprSize = ((AnnotatedDeclaredType) newExprType).getTypeArguments().size();

        if (castSize != exprSize) {
          // Always warn if the cast and expression contain a different number of type
          // arguments, e.g. to catch a cast from "Object" to "List<@NonNull Object>".
          // TODO: the same number of arguments actually doesn't guarantee anything.
          return false;
        }
      } else if (castTypeKind == TypeKind.TYPEVAR && exprType.getKind() == TypeKind.TYPEVAR) {
        // If both the cast type and the casted expression are type variables, then check
        // the bounds.
        AnnotationMirrorSet lowerBoundAnnotationsCast =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, castType);
        AnnotationMirrorSet lowerBoundAnnotationsExpr =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, exprType);
        return qualHierarchy.isSubtypeShallow(
                lowerBoundAnnotationsExpr, newExprTM, lowerBoundAnnotationsCast, newCastTM)
            && typeHierarchy.isSubtypeShallowEffective(exprType, castType);
      }
      if (castTypeKind == TypeKind.TYPEVAR) {
        // If the cast type is a type var, but the expression is not, then check that the
        // type of the expression is a subtype of the lower bound.
        castAnnos = AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, castType);
      } else {
        castAnnos = castType.getPrimaryAnnotations();
      }
    }

    AnnotatedTypeMirror exprTypeWidened = atypeFactory.getWidenedType(exprType, castType);
    return qualHierarchy.isSubtypeShallow(
        exprTypeWidened.getEffectiveAnnotations(),
        exprTypeWidened.getUnderlyingType(),
        castAnnos,
        newCastTM);
  }

  /**
   * Return whether casting the {@code exprType} to {@code castType}, a type with a qualifier
   * parameter, is legal.
   *
   * <p>If {@code exprType} has qualifier parameter, the cast is legal if the qualifiers are
   * invariant. Otherwise, the cast is legal is if the qualifier on both types is bottom.
   *
   * @param castType a type with a qualifier parameter
   * @param exprType type of the expressions that is cast which may or may not have a qualifier
   *     parameter
   * @param top the top qualifier of the hierarchy to check
   * @return whether casting the {@code exprType} to {@code castType}, a type with a qualifier
   *     parameter, is legal.
   */
  private boolean isTypeCastSafeInvariant(
      AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType, AnnotationMirror top) {
    if (!isTypeCastSafe(castType, exprType)) {
      return false;
    }

    if (atypeFactory.hasQualifierParameterInHierarchy(exprType, top)) {
      // The isTypeCastSafe call above checked that the exprType is a subtype of castType,
      // so just check the reverse to check that the qualifiers are equivalent.
      return typeHierarchy.isSubtypeShallowEffective(castType, exprType, top);
    }
    AnnotationMirror castTypeAnno = castType.getEffectiveAnnotationInHierarchy(top);
    AnnotationMirror exprTypeAnno = exprType.getEffectiveAnnotationInHierarchy(top);
    // Otherwise the cast is unsafe, unless the qualifiers on both cast and expr are bottom.
    AnnotationMirror bottom = qualHierarchy.getBottomAnnotation(top);
    return AnnotationUtils.areSame(castTypeAnno, bottom)
        && AnnotationUtils.areSame(exprTypeAnno, bottom);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, Void p) {
    // validate "tree" instead of "tree.getType()" to prevent duplicate errors.
    boolean valid = validateTypeOf(tree) && validateTypeOf(tree.getExpression());
    if (valid) {
      checkTypecastSafety(tree);
      checkTypecastRedundancy(tree);
    }
    if (atypeFactory.getDependentTypesHelper().hasDependentAnnotations()) {
      AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(type, tree.getType());
    }

    if (tree.getType().getKind() == Tree.Kind.INTERSECTION_TYPE) {
      AnnotatedIntersectionType intersection =
          (AnnotatedIntersectionType) atypeFactory.getAnnotatedType(tree);
      checkExplicitAnnotationsOnIntersectionBounds(
          intersection, ((IntersectionTypeTree) tree.getType()).getBounds());
    }
    return super.visitTypeCast(tree, p);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree tree, Void p) {
    // The "reference type" is the type after "instanceof".
    Tree patternTree = InstanceOfUtils.getPattern(tree);
    if (patternTree != null) {
      if (TreeUtils.isBindingPatternTree(patternTree)) {
        VariableTree variableTree = BindingPatternUtils.getVariable(patternTree);
        validateTypeOf(variableTree);
        if (variableTree.getModifiers() != null) {
          AnnotatedTypeMirror variableType = atypeFactory.getAnnotatedType(variableTree);
          AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(tree.getExpression());
          if (!isTypeCastSafe(variableType, expType)) {
            checker.reportWarning(tree, "instanceof.pattern.unsafe", expType, variableTree);
          }
        }
      } else {
        // TODO: implement deconstructed patterns.
      }
    } else {
      Tree refTypeTree = tree.getType();
      validateTypeOf(refTypeTree);
      if (refTypeTree.getKind() == Tree.Kind.ANNOTATED_TYPE) {
        AnnotatedTypeMirror refType = atypeFactory.getAnnotatedType(refTypeTree);
        AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(tree.getExpression());
        if (typeHierarchy.isSubtype(refType, expType)
            && !refType.getPrimaryAnnotations().equals(expType.getPrimaryAnnotations())) {
          checker.reportWarning(tree, "instanceof.unsafe", expType, refType);
        }
      }
    }

    return super.visitInstanceOf(tree, p);
  }

  /**
   * Checks the type of the exception parameter. Subclasses should override {@link
   * #checkExceptionParameter} rather than this method to change the behavior of this check.
   */
  @Override
  public Void visitCatch(CatchTree tree, Void p) {
    checkExceptionParameter(tree);
    return super.visitCatch(tree, p);
  }

  /**
   * Checks the type of a thrown exception. Subclasses should override
   * checkThrownExpression(ThrowTree tree) rather than this method to change the behavior of this
   * check.
   */
  @Override
  public Void visitThrow(ThrowTree tree, Void p) {
    checkThrownExpression(tree);
    return super.visitThrow(tree, p);
  }

  /**
   * Rather than overriding this method, clients should often override {@link
   * #visitAnnotatedType(List,Tree)}. That method also handles the case of annotations at the
   * beginning of a variable or method declaration. javac parses all those annotations as being on
   * the variable or method declaration, even though the ones that are type annotations logically
   * belong to the variable type or method return type.
   */
  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
    visitAnnotatedType(null, tree);
    return super.visitAnnotatedType(tree, p);
  }

  /**
   * Checks an annotated type. Invoked by {@link #visitAnnotatedType(AnnotatedTypeTree, Void)},
   * {@link #visitVariable}, and {@link #visitMethod}. Exists to prevent code duplication among the
   * three. Checking in {@code visitVariable} and {@code visitMethod} is needed because there isn't
   * an AnnotatedTypeTree within a variable declaration or for a method return type -- all the
   * annotations are attached to the VariableTree or MethodTree, respectively.
   *
   * @param annoTrees annotations written before a variable/method declaration, if this type is from
   *     one; null otherwise. This might contain type annotations that the Java parser attached to
   *     the declaration rather than to the type.
   * @param typeTree the type that any type annotations in annoTrees apply to
   */
  public void visitAnnotatedType(
      @Nullable List<? extends AnnotationTree> annoTrees, Tree typeTree) {
    warnAboutIrrelevantJavaTypes(annoTrees, typeTree);
  }

  /**
   * Warns if a type annotation is written on a Java type that is not listed in
   * the @RelevantJavaTypes annotation.
   *
   * @param annoTrees annotations written before a variable/method declaration, if this type is from
   *     one; null otherwise. This might contain type annotations that the Java parser attached to
   *     the declaration rather than to the type.
   * @param typeTree the type that any type annotations in annoTrees apply to
   */
  public void warnAboutIrrelevantJavaTypes(
      @Nullable List<? extends AnnotationTree> annoTrees, Tree typeTree) {
    if (!shouldWarnAboutIrrelevantJavaTypes()) {
      return;
    }

    Tree t = typeTree;
    while (true) {
      switch (t.getKind()) {

          // Recurse for compound types whose top level is not at the far left.
        case ARRAY_TYPE:
          t = ((ArrayTypeTree) t).getType();
          continue;
        case MEMBER_SELECT:
          t = ((MemberSelectTree) t).getExpression();
          continue;
        case PARAMETERIZED_TYPE:
          t = ((ParameterizedTypeTree) t).getType();
          continue;

          // Base cases
        case PRIMITIVE_TYPE:
        case IDENTIFIER:
          maybeReportAnnoOnIrrelevant(t, TreeUtils.typeOf(t), annoTrees);
          return;
        case ANNOTATED_TYPE:
          AnnotatedTypeTree at = (AnnotatedTypeTree) t;
          ExpressionTree underlying = at.getUnderlyingType();
          maybeReportAnnoOnIrrelevant(t, TreeUtils.typeOf(underlying), at.getAnnotations());
          return;

        default:
          return;
      }
    }
  }

  /**
   * If the given Java basetype is not relevant, report an "anno.on.irrelevant" if it is annotated.
   * This method does not necessarily issue an error, but it might.
   *
   * @param errorLocation where to repor the error
   * @param type the Java basetype
   * @param annos the annotation on the type
   */
  private void maybeReportAnnoOnIrrelevant(
      Tree errorLocation, TypeMirror type, List<? extends AnnotationTree> annos) {
    List<AnnotationTree> supportedAnnoTrees = supportedAnnoTrees(annos);
    if (!supportedAnnoTrees.isEmpty() && !atypeFactory.isRelevant(type)) {
      String extraInfo = atypeFactory.irrelevantExtraMessage();
      checker.reportError(errorLocation, "anno.on.irrelevant", annos, type, extraInfo);
    }
  }

  /**
   * Returns true if the checker should issue warnings about irrelevant java types.
   *
   * @return true if the checker should issue warnings about irrelevant java types
   */
  protected boolean shouldWarnAboutIrrelevantJavaTypes() {
    return atypeFactory.relevantJavaTypes != null;
  }

  /**
   * Returns a new list containing only the supported annotations from its argument -- that is,
   * those that are part of the current type system.
   *
   * <p>This method ignores aliases of supported annotations that are declaration annotations,
   * because they may apply to inner types.
   *
   * @param annoTrees annotation trees
   * @return a new list containing only the supported annotations from its argument
   */
  private List<AnnotationTree> supportedAnnoTrees(List<? extends AnnotationTree> annoTrees) {
    List<AnnotationTree> result = new ArrayList<>(1);
    for (AnnotationTree at : annoTrees) {
      AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(at);
      if (!AnnotationUtils.isDeclarationAnnotation(anno)
          && atypeFactory.isSupportedQualifier(anno)) {
        result.add(at);
      }
    }
    return result;
  }

  // **********************************************************************
  // Helper methods to provide a single overriding point
  // **********************************************************************

  /** Cache to avoid calling {@link #getExceptionParameterLowerBoundAnnotations} more than once. */
  private @MonotonicNonNull AnnotationMirrorSet getExceptionParameterLowerBoundAnnotationsCache;

  /**
   * Returns a set of AnnotationMirrors that is a lower bound for exception parameters. The same as
   * {@link #getExceptionParameterLowerBoundAnnotations}, but uses a cache.
   *
   * @return a set of AnnotationMirrors that is a lower bound for exception parameters
   */
  private AnnotationMirrorSet getExceptionParameterLowerBoundAnnotationsCached() {
    if (getExceptionParameterLowerBoundAnnotationsCache == null) {
      getExceptionParameterLowerBoundAnnotationsCache =
          getExceptionParameterLowerBoundAnnotations();
    }
    return getExceptionParameterLowerBoundAnnotationsCache;
  }

  /**
   * Issue error if the exception parameter is not a supertype of the annotation specified by {@link
   * #getExceptionParameterLowerBoundAnnotations()}, which is top by default.
   *
   * <p>Subclasses may override this method to change the behavior of this check. Subclasses wishing
   * to enforce that exception parameter be annotated with other annotations can just override
   * {@link #getExceptionParameterLowerBoundAnnotations()}.
   *
   * @param tree a CatchTree to check
   */
  protected void checkExceptionParameter(CatchTree tree) {

    AnnotationMirrorSet requiredAnnotations = getExceptionParameterLowerBoundAnnotationsCached();
    VariableTree excParamTree = tree.getParameter();
    AnnotatedTypeMirror excParamType = atypeFactory.getAnnotatedType(excParamTree);

    for (AnnotationMirror required : requiredAnnotations) {
      AnnotationMirror found = excParamType.getPrimaryAnnotationInHierarchy(required);
      assert found != null;
      if (!typeHierarchy.isSubtypeShallowEffective(required, excParamType)) {
        checker.reportError(excParamTree, "exception.parameter", found, required);
      }

      if (excParamType.getKind() == TypeKind.UNION) {
        AnnotatedUnionType aut = (AnnotatedUnionType) excParamType;
        for (AnnotatedTypeMirror alternativeType : aut.getAlternatives()) {
          if (!typeHierarchy.isSubtypeShallowEffective(required, alternativeType)) {
            AnnotationMirror alternativeAnno =
                alternativeType.getPrimaryAnnotationInHierarchy(required);
            checker.reportError(excParamTree, "exception.parameter", alternativeAnno, required);
          }
        }
      }
    }
  }

  /**
   * Returns a set of AnnotationMirrors that is a lower bound for exception parameters.
   *
   * <p>This implementation returns top; subclasses can change this behavior.
   *
   * <p>Note: by default this method is called by {@link #getThrowUpperBoundAnnotations()}, so that
   * this annotation is enforced.
   *
   * @return set of annotation mirrors, one per hierarchy, that form a lower bound of annotations
   *     that can be written on an exception parameter
   */
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return qualHierarchy.getTopAnnotations();
  }

  /**
   * Checks the type of the thrown expression.
   *
   * <p>By default, this method checks that the thrown expression is a subtype of top.
   *
   * <p>Issue error if the thrown expression is not a sub type of the annotation given by {@link
   * #getThrowUpperBoundAnnotations()}, the same as {@link
   * #getExceptionParameterLowerBoundAnnotations()} by default.
   *
   * <p>Subclasses may override this method to change the behavior of this check. Subclasses wishing
   * to enforce that the thrown expression be a subtype of a type besides {@link
   * #getExceptionParameterLowerBoundAnnotations}, should override {@link
   * #getThrowUpperBoundAnnotations()}.
   *
   * @param tree a ThrowTree to check
   */
  protected void checkThrownExpression(ThrowTree tree) {
    AnnotatedTypeMirror throwType = atypeFactory.getAnnotatedType(tree.getExpression());
    TypeMirror throwTM = throwType.getUnderlyingType();
    Set<? extends AnnotationMirror> required = getThrowUpperBoundAnnotations();
    switch (throwType.getKind()) {
      case NULL:
      case DECLARED:
      case TYPEVAR:
      case WILDCARD:
        if (!typeHierarchy.isSubtypeShallowEffective(throwType, required)) {
          AnnotationMirrorSet found = throwType.getEffectiveAnnotations();
          checker.reportError(tree.getExpression(), "throw", found, required);
        }
        break;

      case UNION:
        AnnotatedUnionType unionType = (AnnotatedUnionType) throwType;
        AnnotationMirrorSet foundPrimary = unionType.getPrimaryAnnotations();
        if (!qualHierarchy.isSubtypeShallow(foundPrimary, required, throwTM)) {
          checker.reportError(tree.getExpression(), "throw", foundPrimary, required);
        }
        for (AnnotatedTypeMirror altern : unionType.getAlternatives()) {
          TypeMirror alternTM = altern.getUnderlyingType();
          if (!qualHierarchy.isSubtypeShallow(altern.getPrimaryAnnotations(), required, alternTM)) {
            checker.reportError(
                tree.getExpression(), "throw", altern.getPrimaryAnnotations(), required);
          }
        }
        break;
      default:
        throw new BugInCF("Unexpected throw expression type: " + throwType.getKind());
    }
  }

  /**
   * Returns a set of AnnotationMirrors that is a upper bound for thrown exceptions.
   *
   * <p>Note: by default this method is returns by getExceptionParameterLowerBoundAnnotations(), so
   * that this annotation is enforced.
   *
   * <p>(Default is top)
   *
   * @return set of annotation mirrors, one per hierarchy, that form an upper bound of thrown
   *     expressions
   */
  protected AnnotationMirrorSet getThrowUpperBoundAnnotations() {
    return getExceptionParameterLowerBoundAnnotations();
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varTree the AST node for the lvalue (usually a variable)
   * @param valueExpTree the AST node for the rvalue (the new value)
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   * @return true if the check succeeds, false if an error message was issued
   */
  protected boolean commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (valueExpTree.getKind() == Kind.CONDITIONAL_EXPRESSION) {
      ConditionalExpressionTree condExprTree = (ConditionalExpressionTree) valueExpTree;
      boolean trueResult =
          commonAssignmentCheck(varTree, condExprTree.getTrueExpression(), "assignment");
      boolean falseResult =
          commonAssignmentCheck(varTree, condExprTree.getFalseExpression(), "assignment");
      return trueResult && falseResult;
    }

    AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(varTree);
    assert varType != null : "no variable found for tree: " + varTree;

    if (!validateType(varTree, varType)) {
      if (showchecks) {
        System.out.printf(
            "%s %s (at %s): actual tree = %s %s%n   expected: %s %s%n",
            this.getClass().getSimpleName(),
            "skipping test whether actual is a subtype of expected"
                + " because validateType() returned false",
            fileAndLineNumber(valueExpTree),
            valueExpTree.getKind(),
            valueExpTree,
            varType.getKind(),
            varType.toString());
      }
      return true;
    }

    return commonAssignmentCheck(varType, valueExpTree, errorKey, extraArgs);
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varType the annotated type for the lvalue (usually a variable)
   * @param valueExpTree the AST node for the rvalue (the new value)
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   * @return true if the check succeeds, false if an error message was issued
   */
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (shouldSkipUses(valueExpTree)) {
      if (showchecks) {
        System.out.printf(
            "%s %s (at %s): actual tree = %s %s%n   expected: %s %s%n",
            this.getClass().getSimpleName(),
            "skipping test whether actual is a subtype of expected"
                + " because shouldSkipUses() returned true",
            fileAndLineNumber(valueExpTree),
            valueExpTree.getKind(),
            valueExpTree,
            varType.getKind(),
            varType.toString());
      }
      return true;
    }
    if (valueExpTree.getKind() == Tree.Kind.MEMBER_REFERENCE
        || valueExpTree.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
      // Member references and lambda expressions are type checked separately
      // and do not need to be checked again as arguments.
      if (showchecks) {
        System.out.printf(
            "%s %s (at %s): actual tree = %s %s%n   expected: %s %s%n",
            this.getClass().getSimpleName(),
            "skipping test whether actual is a subtype of expected"
                + " because member reference and lambda expression are type checked separately",
            fileAndLineNumber(valueExpTree),
            valueExpTree.getKind(),
            valueExpTree,
            varType.getKind(),
            varType.toString());
      }
      return true;
    }
    boolean result = true;
    if (varType.getKind() == TypeKind.ARRAY
        && valueExpTree instanceof NewArrayTree
        && ((NewArrayTree) valueExpTree).getType() == null) {
      AnnotatedTypeMirror compType = ((AnnotatedArrayType) varType).getComponentType();
      NewArrayTree arrayTree = (NewArrayTree) valueExpTree;
      assert arrayTree.getInitializers() != null
          : "array initializers are not expected to be null in: " + valueExpTree;
      result = checkArrayInitialization(compType, arrayTree.getInitializers()) && result;
    }
    if (!validateTypeOf(valueExpTree)) {
      if (showchecks) {
        System.out.printf(
            "%s %s (at %s): actual tree = %s %s%n   expected: %s %s%n",
            this.getClass().getSimpleName(),
            "skipping test whether actual is a subtype of expected"
                + " because validateType() returned false",
            fileAndLineNumber(valueExpTree),
            valueExpTree.getKind(),
            valueExpTree,
            varType.getKind(),
            varType.toString());
      }
      return result;
    }
    AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueExpTree);
    atypeFactory.logGat(
        "BTV: %s.getAnnotatedType(%s) => %s%n",
        atypeFactory.getClass().getSimpleName(), valueExpTree, valueType);
    assert valueType != null : "null type for expression: " + valueExpTree;
    result = commonAssignmentCheck(varType, valueType, valueExpTree, errorKey, extraArgs) && result;
    return result;
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueExpTree the location to use when reporting the error message
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   * @return true if the check succeeds, false if an error message was issued
   */
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    commonAssignmentCheckStartDiagnostic(varType, valueType, valueExpTree);

    AnnotatedTypeMirror widenedValueType = atypeFactory.getWidenedType(valueType, varType);
    boolean result = typeHierarchy.isSubtype(widenedValueType, varType);

    // TODO: integrate with subtype test.
    if (result) {
      for (Class<? extends Annotation> mono : atypeFactory.getSupportedMonotonicTypeQualifiers()) {
        if (valueType.hasPrimaryAnnotation(mono) && varType.hasPrimaryAnnotation(mono)) {
          checker.reportError(
              valueExpTree,
              "monotonic",
              mono.getSimpleName(),
              mono.getSimpleName(),
              valueType.toString());
          result = false;
        }
      }
    } else {
      // `result` is false.
      // Use an error key only if it's overridden by a checker.
      FoundRequired pair = FoundRequired.of(valueType, varType);
      String valueTypeString = pair.found;
      String varTypeString = pair.required;
      checker.reportError(
          valueExpTree,
          errorKey,
          ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }

    commonAssignmentCheckEndDiagnostic(result, null, varType, valueType, valueExpTree);

    return result;
  }

  /**
   * Prints a diagnostic about entering {@code commonAssignmentCheck()}, if the showchecks option
   * was set.
   *
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueExpTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckStartDiagnostic(
      AnnotatedTypeMirror varType, AnnotatedTypeMirror valueType, Tree valueExpTree) {
    if (showchecks) {
      System.out.printf(
          "%s %s (at %s): actual tree = %s %s%n     actual: %s %s%n   expected: %s %s%n",
          this.getClass().getSimpleName(),
          "about to test whether actual is a subtype of expected",
          fileAndLineNumber(valueExpTree),
          valueExpTree.getKind(),
          valueExpTree,
          valueType.getKind(),
          valueType.toString(),
          varType.getKind(),
          varType.toString());
    }
  }

  /**
   * Prints a diagnostic about exiting {@code commonAssignmentCheck()}, if the showchecks option was
   * set.
   *
   * @param success whether the check succeeded or failed
   * @param extraMessage information about why the result is what it is; may be null
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueExpTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckEndDiagnostic(
      boolean success,
      @Nullable String extraMessage,
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueExpTree) {
    if (showchecks) {
      commonAssignmentCheckEndDiagnostic(
          (success
                  ? "success: actual is subtype of expected"
                  : "FAILURE: actual is not subtype of expected")
              + (extraMessage == null ? "" : " because " + extraMessage),
          varType,
          valueType,
          valueExpTree);
    }
  }

  /**
   * Helper method for printing a diagnostic about exiting {@code commonAssignmentCheck()}, if the
   * showchecks option was set.
   *
   * <p>Most clients should call {@link #commonAssignmentCheckEndDiagnostic(boolean, String,
   * AnnotatedTypeMirror, AnnotatedTypeMirror, Tree)}. The purpose of this method is to permit
   * customizing the message that is printed.
   *
   * @param message the result, plus information about why the result is what it is
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueExpTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckEndDiagnostic(
      String message,
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueExpTree) {
    if (showchecks) {
      System.out.printf(
          " %s  (at %s): actual tree = %s %s%n     actual: %s %s%n   expected: %s %s%n",
          message,
          fileAndLineNumber(valueExpTree),
          valueExpTree.getKind(),
          valueExpTree,
          valueType.getKind(),
          valueType.toString(),
          varType.getKind(),
          varType.toString());
    }
  }

  /**
   * Returns "filename:linenumber:columnnumber" for the given tree. For brevity, the filename is
   * given as a simple name, without any directory components. If the line and column numbers are
   * unknown, they are omitted.
   *
   * @param tree a tree
   * @return the location of the given tree in source code
   */
  private String fileAndLineNumber(Tree tree) {
    StringBuilder result = new StringBuilder();
    result.append(Paths.get(root.getSourceFile().getName()).getFileName().toString());
    long valuePos = positions.getStartPosition(root, tree);
    LineMap lineMap = root.getLineMap();
    if (valuePos != -1 && lineMap != null) {
      result.append(":");
      result.append(lineMap.getLineNumber(valuePos));
      result.append(":");
      result.append(lineMap.getColumnNumber(valuePos));
    }
    return result.toString();
  }

  /**
   * Class that creates string representations of {@link AnnotatedTypeMirror}s which are only
   * verbose if required to differentiate the two types.
   */
  private static class FoundRequired {
    public final String found;
    public final String required;

    private FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
      if (shouldPrintVerbose(found, required)) {
        this.found = found.toString(true);
        this.required = required.toString(true);
      } else {
        this.found = found.toString();
        this.required = required.toString();
      }
    }

    /** Create a FoundRequired for a type and bounds. */
    private FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeParameterBounds required) {
      if (shouldPrintVerbose(found, required)) {
        this.found = found.toString(true);
        this.required = required.toString(true);
      } else {
        this.found = found.toString();
        this.required = required.toString();
      }
    }

    /**
     * Creates string representations of {@link AnnotatedTypeMirror}s which are only verbose if
     * required to differentiate the two types.
     */
    static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
      return new FoundRequired(found, required);
    }

    /**
     * Creates string representations of {@link AnnotatedTypeMirror} and {@link
     * AnnotatedTypeParameterBounds}s which are only verbose if required to differentiate the two
     * types.
     */
    static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeParameterBounds required) {
      return new FoundRequired(found, required);
    }
  }

  /**
   * Return whether or not the verbose toString should be used when printing the two annotated
   * types.
   *
   * @param atm1 the first AnnotatedTypeMirror
   * @param atm2 the second AnnotatedTypeMirror
   * @return true iff neither argument contains "@", or there are two annotated types (in either
   *     ATM) such that their toStrings are the same but their verbose toStrings differ
   */
  private static boolean shouldPrintVerbose(AnnotatedTypeMirror atm1, AnnotatedTypeMirror atm2) {
    if (!atm1.toString().contains("@") && !atm2.toString().contains("@")) {
      return true;
    }
    return containsSameToString(atm1, atm2);
  }

  /**
   * Return whether or not the verbose toString should be used when printing the annotated type and
   * the bounds it is not within.
   *
   * @param atm the type
   * @param bounds the bounds
   * @return true iff bounds does not contain "@", or there are two annotated types (in either
   *     argument) such that their toStrings are the same but their verbose toStrings differ
   */
  private static boolean shouldPrintVerbose(
      AnnotatedTypeMirror atm, AnnotatedTypeParameterBounds bounds) {
    if (!atm.toString().contains("@") && !bounds.toString().contains("@")) {
      return true;
    }
    return containsSameToString(atm, bounds.getUpperBound(), bounds.getLowerBound());
  }

  /**
   * A scanner that indicates whether any (component) types have the same toString but different
   * verbose toString. If so, the Checker Framework prints types verbosely.
   */
  private static final SimpleAnnotatedTypeScanner<Boolean, Map<String, String>>
      checkContainsSameToString =
          new SimpleAnnotatedTypeScanner<>(
              (AnnotatedTypeMirror type, Map<String, String> map) -> {
                if (type == null) {
                  return false;
                }
                String simple = type.toString();
                String verbose = map.get(simple);
                if (verbose == null) {
                  map.put(simple, type.toString(true));
                  return false;
                } else {
                  return !verbose.equals(type.toString(true));
                }
              },
              Boolean::logicalOr,
              false);

  /**
   * Return true iff there are two annotated types (anywhere in any ATM) such that their toStrings
   * are the same but their verbose toStrings differ. If so, the Checker Framework prints types
   * verbosely.
   *
   * @param atms annotated type mirrors to compare
   * @return true iff there are two annotated types (anywhere in any ATM) such that their toStrings
   *     are the same but their verbose toStrings differ
   */
  private static boolean containsSameToString(AnnotatedTypeMirror... atms) {
    Map<String, String> simpleToVerbose = new HashMap<>();
    for (AnnotatedTypeMirror atm : atms) {
      if (checkContainsSameToString.visit(atm, simpleToVerbose)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks that the array initializers are consistent with the array type.
   *
   * @param type the array elemen type
   * @param initializers the initializers
   * @return true if the check succeeds, false if an error message was issued
   */
  protected boolean checkArrayInitialization(
      AnnotatedTypeMirror type, List<? extends ExpressionTree> initializers) {
    // TODO: set assignment context like for method arguments?
    // Also in AbstractFlow.
    boolean result = true;
    for (ExpressionTree init : initializers) {
      result = commonAssignmentCheck(type, init, "array.initializer") && result;
    }
    return result;
  }

  /**
   * Checks that the annotations on the type arguments supplied to a type or a method invocation are
   * within the bounds of the type variables as declared, and issues the "type.argument" error if
   * they are not.
   *
   * @param toptree the tree for error reporting, only used for inferred type arguments
   * @param paramBounds the bounds of the type parameters from a class or method declaration
   * @param typeargs the type arguments from the type or method invocation
   * @param typeargTrees the type arguments as trees, used for error reporting
   */
  protected void checkTypeArguments(
      Tree toptree,
      List<? extends AnnotatedTypeParameterBounds> paramBounds,
      List<? extends AnnotatedTypeMirror> typeargs,
      List<? extends Tree> typeargTrees,
      CharSequence typeOrMethodName,
      List<?> paramNames) {

    // System.out.printf("BaseTypeVisitor.checkTypeArguments: %s, TVs: %s, TAs: %s, TATs: %s%n",
    //         toptree, paramBounds, typeargs, typeargTrees);

    // If there are no type variables, do nothing.
    if (paramBounds.isEmpty()) {
      return;
    }

    int size = paramBounds.size();
    assert size == typeargs.size()
        : "BaseTypeVisitor.checkTypeArguments: mismatch between type arguments: "
            + typeargs
            + " and type parameter bounds"
            + paramBounds;

    for (int i = 0; i < size; i++) {

      AnnotatedTypeParameterBounds bounds = paramBounds.get(i);
      AnnotatedTypeMirror typeArg = typeargs.get(i);

      if (atypeFactory.ignoreRawTypeArguments
          && AnnotatedTypes.isTypeArgOfRawType(bounds.getUpperBound())) {
        continue;
      }

      AnnotatedTypeMirror paramUpperBound = bounds.getUpperBound();

      Tree reportErrorToTree;
      if (typeargTrees == null || typeargTrees.isEmpty()) {
        // The type arguments were inferred, report the error on the method invocation.
        reportErrorToTree = toptree;
      } else {
        reportErrorToTree = typeargTrees.get(i);
      }

      checkHasQualifierParameterAsTypeArgument(typeArg, paramUpperBound, toptree);
      commonAssignmentCheck(
          paramUpperBound,
          typeArg,
          reportErrorToTree,
          "type.argument",
          paramNames.get(i),
          typeOrMethodName);

      if (!typeHierarchy.isSubtype(bounds.getLowerBound(), typeArg)) {
        FoundRequired fr = FoundRequired.of(typeArg, bounds);
        checker.reportError(
            reportErrorToTree,
            "type.argument",
            paramNames.get(i),
            typeOrMethodName,
            fr.found,
            fr.required);
      }
    }
  }

  /**
   * Reports an error if the type argument has a qualifier parameter and the type parameter upper
   * bound does not have a qualifier parameter.
   *
   * @param typeArgument type argument
   * @param typeParameterUpperBound upper bound of the type parameter
   * @param reportError where to report the error
   */
  private void checkHasQualifierParameterAsTypeArgument(
      AnnotatedTypeMirror typeArgument,
      AnnotatedTypeMirror typeParameterUpperBound,
      Tree reportError) {
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      if (atypeFactory.hasQualifierParameterInHierarchy(typeArgument, top)
          && !getTypeFactory().hasQualifierParameterInHierarchy(typeParameterUpperBound, top)) {
        checker.reportError(reportError, "type.argument.hasqualparam", top);
      }
    }
  }

  /**
   * Indicates whether to skip subtype checks on the receiver when checking method invocability. A
   * visitor may, for example, allow a method to be invoked even if the receivers are siblings in a
   * hierarchy, provided that some other condition (implemented by the visitor) is satisfied.
   *
   * @param tree the method invocation tree
   * @param methodDefinitionReceiver the ATM of the receiver of the method definition
   * @param methodCallReceiver the ATM of the receiver of the method call
   * @return whether to skip subtype checks on the receiver
   */
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree tree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    return false;
  }

  /**
   * Tests whether the method can be invoked using the receiver of the 'tree' method invocation, and
   * issues a "method.invocation" if the invocation is invalid.
   *
   * <p>This implementation tests whether the receiver in the method invocation is a subtype of the
   * method receiver type. This behavior can be specialized by overriding skipReceiverSubtypeCheck.
   *
   * @param method the type of the invoked method
   * @param tree the method invocation tree
   */
  protected void checkMethodInvocability(
      AnnotatedExecutableType method, MethodInvocationTree tree) {
    if (method.getReceiverType() == null) {
      // Static methods don't have a receiver to check.
      return;
    }
    if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
      // TODO: Explicit "this()" calls of constructors have an implicit passed
      // from the enclosing constructor. We must not use the self type, but
      // instead should find a way to determine the receiver of the enclosing constructor.
      // rcv =
      // ((AnnotatedExecutableType)atypeFactory.getAnnotatedType(atypeFactory.getEnclosingMethod(tree))).getReceiverType();
      return;
    }

    AnnotatedTypeMirror erasedMethodReceiver = method.getReceiverType().getErased();
    AnnotatedTypeMirror erasedTreeReceiver = erasedMethodReceiver.shallowCopy(false);
    AnnotatedTypeMirror treeReceiver = atypeFactory.getReceiverType(tree);

    erasedTreeReceiver.addAnnotations(treeReceiver.getEffectiveAnnotations());

    if (!skipReceiverSubtypeCheck(tree, erasedMethodReceiver, treeReceiver)) {
      // The diagnostic can be a bit misleading because the check is of the receiver but
      // `tree` is the entire method invocation (where the receiver might be implicit).
      commonAssignmentCheckStartDiagnostic(erasedMethodReceiver, erasedTreeReceiver, tree);
      boolean success = typeHierarchy.isSubtype(erasedTreeReceiver, erasedMethodReceiver);
      commonAssignmentCheckEndDiagnostic(
          success, null, erasedMethodReceiver, erasedTreeReceiver, tree);
      if (!success) {
        // Don't report the erased types because they show up with '</*RAW*/>' as type args.
        reportMethodInvocabilityError(tree, treeReceiver, method.getReceiverType());
      }
    }
  }

  /**
   * Report a method invocability error. Allows checkers to change how the message is output.
   *
   * @param tree the AST node at which to report the error
   * @param found the actual type of the receiver
   * @param expected the expected type of the receiver
   */
  protected void reportMethodInvocabilityError(
      MethodInvocationTree tree, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {
    checker.reportError(
        tree,
        "method.invocation",
        TreeUtils.elementFromUse(tree),
        found.toString(),
        expected.toString());
  }

  /**
   * Check that the (explicit) annotations on a new class tree are comparable to the result type of
   * the constructor. Issue an error if not.
   *
   * <p>Issue a warning if the annotations on the constructor invocation is a subtype of the
   * constructor result type. This is equivalent to down-casting.
   */
  protected void checkConstructorInvocation(
      AnnotatedDeclaredType invocation,
      AnnotatedExecutableType constructor,
      NewClassTree newClassTree) {
    // Only check the primary annotations, the type arguments are checked elsewhere.
    AnnotationMirrorSet explicitAnnos = atypeFactory.getExplicitNewClassAnnos(newClassTree);
    if (explicitAnnos.isEmpty()) {
      return;
    }
    for (AnnotationMirror explicit : explicitAnnos) {
      // The return type of the constructor (resultAnnos) must be comparable to the
      // annotations on the constructor invocation (explicitAnnos).
      boolean resultIsSubtypeOfExplicit =
          typeHierarchy.isSubtypeShallowEffective(constructor.getReturnType(), explicit);
      if (!(typeHierarchy.isSubtypeShallowEffective(explicit, constructor.getReturnType())
          || resultIsSubtypeOfExplicit)) {
        AnnotationMirror resultAnno =
            constructor.getReturnType().getPrimaryAnnotationInHierarchy(explicit);
        checker.reportError(
            newClassTree, "constructor.invocation", constructor.toString(), explicit, resultAnno);
        return;
      } else if (!resultIsSubtypeOfExplicit) {
        AnnotationMirror resultAnno =
            constructor.getReturnType().getPrimaryAnnotationInHierarchy(explicit);
        // Issue a warning if the annotations on the constructor invocation is a subtype of
        // the constructor result type. This is equivalent to down-casting.
        checker.reportWarning(
            newClassTree, "cast.unsafe.constructor.invocation", resultAnno, explicit);
        return;
      }
    }

    // TODO: what properties should hold for constructor receivers for
    // inner type instantiations?
  }

  /**
   * A helper method to check that each passed argument is a subtype of the corresponding required
   * argument. Issues an "argument" error for each passed argument that not a subtype of the
   * required one.
   *
   * <p>Note this method requires the lists to have the same length, as it does not handle cases
   * like var args.
   *
   * @see #checkVarargs(AnnotatedTypeMirror.AnnotatedExecutableType, Tree)
   * @param requiredTypes the required types. This may differ from the formal parameter types,
   *     because it replaces a varargs parameter by multiple parameters with the vararg's element
   *     type.
   * @param passedArgs the expressions passed to the corresponding types
   * @param executableName the name of the method or constructor being called
   * @param paramNames the names of the callee's formal parameters
   */
  protected void checkArguments(
      List<? extends AnnotatedTypeMirror> requiredTypes,
      List<? extends ExpressionTree> passedArgs,
      CharSequence executableName,
      List<?> paramNames) {
    int numRequired = requiredTypes.size();
    assert numRequired == passedArgs.size()
        : String.format(
            "numRequired %d should equal %d in checkArguments(%s, %s, %s, %s)",
            numRequired,
            passedArgs.size(),
            listToString(requiredTypes),
            listToString(passedArgs),
            executableName,
            listToString(paramNames));
    int maxParamNamesIndex = paramNames.size() - 1;
    // Rather weak assertion, due to how varargs parameters are treated.
    assert numRequired >= maxParamNamesIndex
        : String.format(
            "mismatched lengths %d %d %d checkArguments(%s, %s, %s, %s)",
            numRequired,
            passedArgs.size(),
            paramNames.size(),
            listToString(requiredTypes),
            listToString(passedArgs),
            executableName,
            listToString(paramNames));

    for (int i = 0; i < numRequired; ++i) {
      AnnotatedTypeMirror requiredType = requiredTypes.get(i);
      ExpressionTree passedArg = passedArgs.get(i);
      Object paramName = paramNames.get(Math.min(i, maxParamNamesIndex));
      commonAssignmentCheck(
          requiredType,
          passedArg,
          "argument",
          // TODO: for expanded varargs parameters, maybe adjust the name
          paramName,
          executableName);
      scan(passedArg, null);
    }
  }

  // com.sun.tools.javac.util.List has a toString that does not include surrounding "[...]",
  // making it hard to interpret in messages.
  /**
   * Produce a printed representation of a list, in the standard format with surrounding "[...]".
   *
   * @param lst a list to format
   * @return the printed representation of the list
   */
  private String listToString(List<?> lst) {
    StringJoiner result = new StringJoiner(",", "[", "]");
    for (Object elt : lst) {
      result.add(elt.toString());
    }
    return result.toString();
  }

  /**
   * Returns true if both types are type variables and outer contains inner. Outer contains inner
   * implies: {@literal inner.upperBound <: outer.upperBound outer.lowerBound <: inner.lowerBound}.
   *
   * @return true if both types are type variables and outer contains inner
   */
  protected boolean testTypevarContainment(AnnotatedTypeMirror inner, AnnotatedTypeMirror outer) {
    if (inner.getKind() == TypeKind.TYPEVAR && outer.getKind() == TypeKind.TYPEVAR) {

      AnnotatedTypeVariable innerAtv = (AnnotatedTypeVariable) inner;
      AnnotatedTypeVariable outerAtv = (AnnotatedTypeVariable) outer;

      if (AnnotatedTypes.areCorrespondingTypeVariables(elements, innerAtv, outerAtv)) {
        return typeHierarchy.isSubtype(innerAtv.getUpperBound(), outerAtv.getUpperBound())
            && typeHierarchy.isSubtype(outerAtv.getLowerBound(), innerAtv.getLowerBound());
      }
    }

    return false;
  }

  /**
   * Create an OverrideChecker.
   *
   * <p>This exists so that subclasses can subclass OverrideChecker and use their subclass instead
   * of using OverrideChecker itself.
   *
   * @param overriderTree the AST node of the overriding method or method reference
   * @param overriderMethodType the type of the overriding method
   * @param overriderType the type enclosing the overrider method, usually an AnnotatedDeclaredType;
   *     for Method References may be something else
   * @param overriderReturnType the return type of the overriding method
   * @param overriddenMethodType the type of the overridden method
   * @param overriddenType the declared type enclosing the overridden method
   * @param overriddenReturnType the return type of the overridden method
   * @return an OverrideChecker
   */
  protected OverrideChecker createOverrideChecker(
      Tree overriderTree,
      AnnotatedExecutableType overriderMethodType,
      AnnotatedTypeMirror overriderType,
      AnnotatedTypeMirror overriderReturnType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType,
      AnnotatedTypeMirror overriddenReturnType) {
    return new OverrideChecker(
        overriderTree,
        overriderMethodType,
        overriderType,
        overriderReturnType,
        overriddenMethodType,
        overriddenType,
        overriddenReturnType);
  }

  /**
   * Type checks that a method may override another method. Uses an OverrideChecker subclass as
   * created by {@link #createOverrideChecker}. This version of the method uses the annotated type
   * factory to get the annotated type of the overriding method, and does NOT expose that type.
   *
   * @see #checkOverride(MethodTree, AnnotatedTypeMirror.AnnotatedExecutableType,
   *     AnnotatedTypeMirror.AnnotatedDeclaredType, AnnotatedTypeMirror.AnnotatedExecutableType,
   *     AnnotatedTypeMirror.AnnotatedDeclaredType)
   * @param overriderTree declaration tree of overriding method
   * @param overriderType type of overriding class
   * @param overriddenMethodType type of overridden method
   * @param overriddenType type of overridden class
   * @return true if the override is allowed
   */
  protected boolean checkOverride(
      MethodTree overriderTree,
      AnnotatedDeclaredType overriderType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType) {

    // Get the type of the overriding method.
    AnnotatedExecutableType overriderMethodType = atypeFactory.getAnnotatedType(overriderTree);

    // Call the other version of the method, which takes overriderMethodType. Both versions
    // exist to allow checkers to override one or the other depending on their needs.
    return checkOverride(
        overriderTree, overriderMethodType, overriderType, overriddenMethodType, overriddenType);
  }

  /**
   * Type checks that a method may override another method. Uses an OverrideChecker subclass as
   * created by {@link #createOverrideChecker}. This version of the method exposes the
   * AnnotatedExecutableType of the overriding method. Override this version of the method if you
   * need to access that type.
   *
   * @see #checkOverride(MethodTree, AnnotatedTypeMirror.AnnotatedDeclaredType,
   *     AnnotatedTypeMirror.AnnotatedExecutableType, AnnotatedTypeMirror.AnnotatedDeclaredType)
   * @param overriderTree declaration tree of overriding method
   * @param overriderMethodType type of the overriding method
   * @param overriderType type of overriding class
   * @param overriddenMethodType type of overridden method
   * @param overriddenType type of overridden class
   * @return true if the override is allowed
   */
  protected boolean checkOverride(
      MethodTree overriderTree,
      AnnotatedExecutableType overriderMethodType,
      AnnotatedDeclaredType overriderType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType) {

    // This needs to be done before overriderMethodType.getReturnType() and
    // overriddenMethodType.getReturnType()
    if (overriderMethodType.getTypeVariables().isEmpty()
        && !overriddenMethodType.getTypeVariables().isEmpty()) {
      overriddenMethodType = overriddenMethodType.getErased();
    }

    OverrideChecker overrideChecker =
        createOverrideChecker(
            overriderTree,
            overriderMethodType,
            overriderType,
            overriderMethodType.getReturnType(),
            overriddenMethodType,
            overriddenType,
            overriddenMethodType.getReturnType());

    return overrideChecker.checkOverride();
  }

  /**
   * Check that a method reference is allowed. Uses the OverrideChecker class.
   *
   * @param memberReferenceTree the tree for the method reference
   * @return true if the method reference is allowed
   */
  protected boolean checkMethodReferenceAsOverride(
      MemberReferenceTree memberReferenceTree, Void p) {

    IPair<AnnotatedTypeMirror, AnnotatedExecutableType> result =
        atypeFactory.getFnInterfaceFromTree(memberReferenceTree);
    // The type to which the member reference is assigned -- also known as the target type of
    // the reference.
    AnnotatedTypeMirror functionalInterface = result.first;
    // The type of the single method that is declared by the functional interface.
    AnnotatedExecutableType functionType = result.second;

    // ========= Overriding Type =========
    // This doesn't get the correct type for a "MyOuter.super" based on the receiver of the
    // enclosing method.
    // That is handled separately in method receiver check.

    // The tree before :: is an expression or type use.
    ExpressionTree preColonTree = memberReferenceTree.getQualifierExpression();
    MemberReferenceKind memRefKind =
        MemberReferenceKind.getMemberReferenceKind(memberReferenceTree);
    AnnotatedTypeMirror enclosingType;
    if (TreeUtils.isLikeDiamondMemberReference(memberReferenceTree)) {
      TypeElement typeElt = TypesUtils.getTypeElement(TreeUtils.typeOf(preColonTree));
      enclosingType = atypeFactory.getAnnotatedType(typeElt);
    } else if (memberReferenceTree.getMode() == ReferenceMode.NEW
        || memRefKind == MemberReferenceKind.UNBOUND
        || memRefKind == MemberReferenceKind.STATIC) {
      // The tree before :: is a type tree.
      enclosingType = atypeFactory.getAnnotatedTypeFromTypeTree(preColonTree);
    } else {
      // The tree before :: is an expression.
      enclosingType = atypeFactory.getAnnotatedType(preColonTree);
    }

    // ========= Overriding Executable =========
    // The ::method element, see JLS 15.13.1 Compile-Time Declaration of a Method Reference
    ExecutableElement compileTimeDeclaration = TreeUtils.elementFromUse(memberReferenceTree);

    ParameterizedExecutableType preInference =
        atypeFactory.methodFromUseWithoutTypeArgInference(
            memberReferenceTree, compileTimeDeclaration, enclosingType);
    if (TreeUtils.needsTypeArgInference(memberReferenceTree)) {
      if (!checkTypeArgumentInference(memberReferenceTree, preInference.executableType)) {
        return true;
      }
    }

    // The type of the compileTimeDeclaration if it were invoked with a receiver expression
    // of type {@code type}
    AnnotatedExecutableType invocationType =
        atypeFactory.methodFromUse(memberReferenceTree, compileTimeDeclaration, enclosingType)
            .executableType;

    // This needs to be done before invocationType.getReturnType() and
    // functionType.getReturnType()
    if (invocationType.getTypeVariables().isEmpty() && !functionType.getTypeVariables().isEmpty()) {
      functionType = functionType.getErased();
    }

    // Use the function type's parameters to resolve polymorphic qualifiers.
    QualifierPolymorphism poly = atypeFactory.getQualifierPolymorphism();
    poly.resolve(functionType, invocationType);

    AnnotatedTypeMirror invocationReturnType;
    if (compileTimeDeclaration.getKind() == ElementKind.CONSTRUCTOR) {
      if (enclosingType.getKind() == TypeKind.ARRAY) {
        // Special casing for the return of array constructor
        invocationReturnType = enclosingType;
      } else {
        invocationReturnType =
            atypeFactory.getResultingTypeOfConstructorMemberReference(
                memberReferenceTree, invocationType);
      }
    } else {
      invocationReturnType = invocationType.getReturnType();
    }

    AnnotatedTypeMirror functionTypeReturnType = functionType.getReturnType();
    if (functionTypeReturnType.getKind() == TypeKind.VOID) {
      // If the functional interface return type is void, the overriding return type doesn't
      // matter.
      functionTypeReturnType = invocationReturnType;
    }

    if (functionalInterface.getKind() == TypeKind.DECLARED) {
      // Check the member reference as if invocationType overrides functionType.
      OverrideChecker overrideChecker =
          createOverrideChecker(
              memberReferenceTree,
              invocationType,
              enclosingType,
              invocationReturnType,
              functionType,
              (AnnotatedDeclaredType) functionalInterface,
              functionTypeReturnType);
      return overrideChecker.checkOverride();
    } else {
      // If the functionalInterface is not a declared type, it must be from a wildcard from a
      // raw type. In that case, only return false if raw types should not be ignored.
      return !atypeFactory.ignoreRawTypeArguments;
    }
  }

  /**
   * Class to perform method override and method reference checks.
   *
   * <p>Method references are checked similarly to method overrides, with the method reference
   * viewed as overriding the functional interface's method.
   *
   * <p>Checks that an overriding method's return type, parameter types, and receiver type are
   * correct with respect to the annotations on the overridden method's return type, parameter
   * types, and receiver type.
   *
   * <p>Furthermore, any contracts on the method must satisfy behavioral subtyping, that is,
   * postconditions must be at least as strong as the postcondition on the superclass, and
   * preconditions must be at most as strong as the condition on the superclass.
   *
   * <p>This method returns the result of the check, but also emits error messages as a side effect.
   */
  public class OverrideChecker {

    /**
     * The declaration of an overriding method. Or, it could be a method reference that is being
     * passed to a method.
     */
    protected final Tree overriderTree;

    /** True if {@link #overriderTree} is a MEMBER_REFERENCE. */
    protected final boolean isMethodReference;

    /** The type of the overriding method. */
    protected final AnnotatedExecutableType overrider;

    /** The subtype that declares the overriding method. */
    protected final AnnotatedTypeMirror overriderType;

    /** The type of the overridden method. */
    protected final AnnotatedExecutableType overridden;

    /** The supertype that declares the overridden method. */
    protected final AnnotatedDeclaredType overriddenType;

    /** The teturn type of the overridden method. */
    protected final AnnotatedTypeMirror overriddenReturnType;

    /** The return type of the overriding method. */
    protected final AnnotatedTypeMirror overriderReturnType;

    /**
     * Create an OverrideChecker.
     *
     * <p>Notice that the return types are passed in separately. This is to support some types of
     * method references where the overrider's return type is not the appropriate type to check.
     *
     * @param overriderTree the AST node of the overriding method or method reference
     * @param overrider the type of the overriding method
     * @param overriderType the type enclosing the overrider method, usually an
     *     AnnotatedDeclaredType; for Method References may be something else
     * @param overriderReturnType the return type of the overriding method
     * @param overridden the type of the overridden method
     * @param overriddenType the declared type enclosing the overridden method
     * @param overriddenReturnType the return type of the overridden method
     */
    public OverrideChecker(
        Tree overriderTree,
        AnnotatedExecutableType overrider,
        AnnotatedTypeMirror overriderType,
        AnnotatedTypeMirror overriderReturnType,
        AnnotatedExecutableType overridden,
        AnnotatedDeclaredType overriddenType,
        AnnotatedTypeMirror overriddenReturnType) {

      this.overriderTree = overriderTree;
      this.overrider = overrider;
      this.overriderType = overriderType;
      this.overriderReturnType = overriderReturnType;
      this.overridden = overridden;
      this.overriddenType = overriddenType;
      this.overriddenReturnType = overriddenReturnType;

      this.isMethodReference = overriderTree.getKind() == Tree.Kind.MEMBER_REFERENCE;
    }

    /**
     * Perform the check.
     *
     * @return true if the override is allowed
     */
    public boolean checkOverride() {
      if (checker.shouldSkipUses(overriddenType.getUnderlyingType().asElement())) {
        return true;
      }

      boolean result = checkReturn();
      result &= checkParameters();
      if (isMethodReference) {
        result &= checkMemberReferenceReceivers();
      } else {
        result &= checkReceiverOverride();
      }
      checkPreAndPostConditions();
      checkPurity();

      return result;
    }

    /** Check that an override respects purity. */
    private void checkPurity() {
      String msgKey = isMethodReference ? "purity.methodref" : "purity.overriding";

      // check purity annotations
      EnumSet<Pure.Kind> superPurity =
          PurityUtils.getPurityKinds(atypeFactory, overridden.getElement());
      EnumSet<Pure.Kind> subPurity =
          PurityUtils.getPurityKinds(atypeFactory, overrider.getElement());
      if (!subPurity.containsAll(superPurity)) {
        checker.reportError(
            overriderTree,
            msgKey,
            overriderType,
            overrider,
            overriddenType,
            overridden,
            subPurity,
            superPurity);
      }
    }

    /**
     * Checks that overrides obey behavioral subtyping, that is, postconditions must be at least as
     * strong as the postcondition on the superclass, and preconditions must be at most as strong as
     * the condition on the superclass.
     */
    private void checkPreAndPostConditions() {
      String msgKey = isMethodReference ? "methodref" : "override";
      if (isMethodReference) {
        // TODO: Support postconditions and method references.
        // The parse context always expects instance methods, but method references can be
        // static.
        return;
      }

      ContractsFromMethod contractsUtils = atypeFactory.getContractsFromMethod();

      // Check preconditions
      Set<Precondition> superPre = contractsUtils.getPreconditions(overridden.getElement());
      Set<Precondition> subPre = contractsUtils.getPreconditions(overrider.getElement());
      Set<IPair<JavaExpression, AnnotationMirror>> superPre2 =
          parseAndLocalizeContracts(superPre, overridden);
      Set<IPair<JavaExpression, AnnotationMirror>> subPre2 =
          parseAndLocalizeContracts(subPre, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String premsg = "contracts.precondition." + msgKey;
      checkContractsSubset(overriderType, overriddenType, subPre2, superPre2, premsg);

      // Check postconditions
      Set<Postcondition> superPost = contractsUtils.getPostconditions(overridden.getElement());
      Set<Postcondition> subPost = contractsUtils.getPostconditions(overrider.getElement());
      Set<IPair<JavaExpression, AnnotationMirror>> superPost2 =
          parseAndLocalizeContracts(superPost, overridden);
      Set<IPair<JavaExpression, AnnotationMirror>> subPost2 =
          parseAndLocalizeContracts(subPost, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String postmsg = "contracts.postcondition." + msgKey;
      checkContractsSubset(overriderType, overriddenType, superPost2, subPost2, postmsg);

      // Check conditional postconditions
      Set<ConditionalPostcondition> superCPost =
          contractsUtils.getConditionalPostconditions(overridden.getElement());
      Set<ConditionalPostcondition> subCPost =
          contractsUtils.getConditionalPostconditions(overrider.getElement());
      // consider only 'true' postconditions
      Set<Postcondition> superCPostTrue = filterConditionalPostconditions(superCPost, true);
      Set<Postcondition> subCPostTrue = filterConditionalPostconditions(subCPost, true);
      Set<IPair<JavaExpression, AnnotationMirror>> superCPostTrue2 =
          parseAndLocalizeContracts(superCPostTrue, overridden);
      Set<IPair<JavaExpression, AnnotationMirror>> subCPostTrue2 =
          parseAndLocalizeContracts(subCPostTrue, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String posttruemsg = "contracts.conditional.postcondition.true." + msgKey;
      checkContractsSubset(
          overriderType, overriddenType, superCPostTrue2, subCPostTrue2, posttruemsg);

      // consider only 'false' postconditions
      Set<Postcondition> superCPostFalse = filterConditionalPostconditions(superCPost, false);
      Set<Postcondition> subCPostFalse = filterConditionalPostconditions(subCPost, false);
      Set<IPair<JavaExpression, AnnotationMirror>> superCPostFalse2 =
          parseAndLocalizeContracts(superCPostFalse, overridden);
      Set<IPair<JavaExpression, AnnotationMirror>> subCPostFalse2 =
          parseAndLocalizeContracts(subCPostFalse, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String postfalsemsg = "contracts.conditional.postcondition.false." + msgKey;
      checkContractsSubset(
          overriderType, overriddenType, superCPostFalse2, subCPostFalse2, postfalsemsg);
    }

    /**
     * Issue a "methodref.receiver" or "methodref.receiver.bound" error if the receiver for the
     * method reference does not satify overriding rules.
     *
     * @return true if the override is legal
     */
    private boolean checkMemberReferenceReceivers() {
      if (overriderType.getKind() == TypeKind.ARRAY) {
        // Assume the receiver for all method on arrays are @Top.
        // This simplifies some logic because an AnnotatedExecutableType for an array method
        // (ie String[]::clone) has a receiver of "Array." The UNBOUND check would then
        // have to compare "Array" to "String[]".
        return true;
      }
      MemberReferenceTree memberTree = (MemberReferenceTree) overriderTree;
      MemberReferenceKind methodRefKind =
          MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree);
      // These act like a traditional override
      if (methodRefKind == MemberReferenceKind.UNBOUND) {
        AnnotatedTypeMirror overriderReceiver = overrider.getReceiverType();
        AnnotatedTypeMirror overriddenReceiver = overridden.getParameterTypes().get(0);
        boolean success = typeHierarchy.isSubtype(overriddenReceiver, overriderReceiver);
        if (!success) {
          checker.reportError(
              overriderTree,
              "methodref.receiver",
              overriderReceiver,
              overriddenReceiver,
              overriderType,
              overrider,
              overriddenType,
              overridden);
        }
        return success;
      }

      // The rest act like method invocations
      AnnotatedTypeMirror receiverDecl;
      AnnotatedTypeMirror receiverArg;
      switch (methodRefKind) {
        case UNBOUND:
          throw new BugInCF("Case UNBOUND should already be handled.");
        case SUPER:
          receiverDecl = overrider.getReceiverType();
          receiverArg = atypeFactory.getAnnotatedType(memberTree.getQualifierExpression());

          AnnotatedTypeMirror selfType = atypeFactory.getSelfType(memberTree);
          receiverArg.replaceAnnotations(selfType.getPrimaryAnnotations());
          break;
        case BOUND:
          receiverDecl = overrider.getReceiverType();
          receiverArg = overriderType;
          break;
        case IMPLICIT_INNER:
          // JLS 15.13.1 "It is a compile-time error if the method reference expression is
          // of the form ClassType :: [TypeArguments] new and a compile-time error would
          // occur when determining an enclosing instance for ClassType as specified in
          // 15.9.2 (treating the method reference expression as if it were an unqualified
          // class instance creation expression)."

          // So a member reference can only refer to an inner class constructor if a type
          // that encloses the inner class can be found. So either "this" is that
          // enclosing type or "this" has an enclosing type that is that type.
          receiverDecl = overrider.getReceiverType();
          receiverArg = atypeFactory.getSelfType(memberTree);
          while (!TypesUtils.isErasedSubtype(
              receiverArg.getUnderlyingType(), receiverDecl.getUnderlyingType(), types)) {
            receiverArg = ((AnnotatedDeclaredType) receiverArg).getEnclosingType();
          }

          break;
        case TOPLEVEL:
        case STATIC:
        case ARRAY_CTOR:
        default:
          // Intentional fallthrough
          // These don't have receivers
          return true;
      }

      boolean success = typeHierarchy.isSubtype(receiverArg, receiverDecl);
      if (!success) {
        checker.reportError(
            overriderTree,
            "methodref.receiver.bound",
            receiverArg,
            receiverDecl,
            receiverArg,
            overriderType,
            overrider);
      }

      return success;
    }

    /**
     * Issue an "override.receiver" error if the receiver override is not valid.
     *
     * @return true if the override is legal
     */
    protected boolean checkReceiverOverride() {
      AnnotatedDeclaredType overriderReceiver = overrider.getReceiverType();
      AnnotatedDeclaredType overriddenReceiver = overridden.getReceiverType();
      // Check the receiver type.
      // isSubtype() requires its arguments to be actual subtypes with respect to the JLS, but
      // an overrider receiver is not a subtype of the overridden receiver.  So, just check
      // primary annotations.
      // TODO: this will need to be improved for generic receivers.
      if (!typeHierarchy.isSubtypeShallowEffective(overriddenReceiver, overriderReceiver)) {
        AnnotationMirrorSet declaredAnnos =
            atypeFactory.getTypeDeclarationBounds(overriderType.getUnderlyingType());
        if (typeHierarchy.isSubtypeShallowEffective(overriderReceiver, declaredAnnos)
            && typeHierarchy.isSubtypeShallowEffective(declaredAnnos, overriderReceiver)) {
          // All the type of an object must be no higher than its upper bound. So if the
          // receiver is annotated with the upper bound qualifiers, then the override is
          // safe.
          return true;
        }
        FoundRequired pair = FoundRequired.of(overriderReceiver, overriddenReceiver);
        checker.reportError(
            overriderTree,
            "override.receiver",
            pair.found,
            pair.required,
            overriderType,
            overrider,
            overriddenType,
            overridden);
        return false;
      }
      return true;
    }

    private boolean checkParameters() {
      List<AnnotatedTypeMirror> overriderParams = overrider.getParameterTypes();
      List<AnnotatedTypeMirror> overriddenParams = overridden.getParameterTypes();

      // Fix up method reference parameters.
      // See https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.13.1
      if (isMethodReference) {
        // The functional interface of an unbound member reference has an extra parameter
        // (the receiver).
        if (MemberReferenceKind.getMemberReferenceKind((MemberReferenceTree) overriderTree)
            == MemberReferenceKind.UNBOUND) {
          overriddenParams = new ArrayList<>(overriddenParams);
          overriddenParams.remove(0);
        }
        // Deal with varargs
        if (overrider.isVarargs() && !overridden.isVarargs()) {
          overriderParams =
              AnnotatedTypes.expandVarargsParametersFromTypes(overrider, overriddenParams);
        }
      }

      boolean result = true;
      for (int i = 0; i < overriderParams.size(); ++i) {
        AnnotatedTypeMirror capturedParam =
            atypeFactory.applyCaptureConversion(overriddenParams.get(i));
        boolean success = typeHierarchy.isSubtype(capturedParam, overriderParams.get(i));
        if (!success) {
          success = testTypevarContainment(overriddenParams.get(i), overriderParams.get(i));
        }

        checkParametersMsg(success, i, overriderParams, overriddenParams);
        result &= success;
      }
      return result;
    }

    private void checkParametersMsg(
        boolean success,
        int index,
        List<AnnotatedTypeMirror> overriderParams,
        List<AnnotatedTypeMirror> overriddenParams) {
      if (success && !showchecks) {
        return;
      }

      String msgKey = isMethodReference ? "methodref.param" : "override.param";
      Tree posTree =
          overriderTree instanceof MethodTree
              ? ((MethodTree) overriderTree).getParameters().get(index)
              : overriderTree;

      if (showchecks) {
        System.out.printf(
            " %s (at %s):%n"
                + "     overrider: %s %s (parameter %d type %s)%n"
                + "    overridden: %s %s"
                + " (parameter %d type %s)%n",
            (success
                ? "success: overridden parameter type is subtype of overriding"
                : "FAILURE: overridden parameter type is not subtype of overriding"),
            fileAndLineNumber(posTree),
            overrider,
            overriderType,
            index,
            overriderParams.get(index).toString(),
            overridden,
            overriddenType,
            index,
            overriddenParams.get(index).toString());
      }
      if (!success) {
        FoundRequired pair =
            FoundRequired.of(overriderParams.get(index), overriddenParams.get(index));
        checker.reportError(
            posTree,
            msgKey,
            overrider.getElement().getParameters().get(index).toString(),
            pair.found,
            pair.required,
            overriderType,
            overrider,
            overriddenType,
            overridden);
      }
    }

    /**
     * Returns true if the return type of the overridden method is a subtype of the return type of
     * the overriding method.
     *
     * @return true if the return type is correct
     */
    private boolean checkReturn() {
      if ((overriderReturnType.getKind() == TypeKind.VOID)) {
        // Nothing to check.
        return true;
      }
      boolean success = typeHierarchy.isSubtype(overriderReturnType, overriddenReturnType);
      if (!success) {
        // If both the overridden method have type variables as return types and both
        // types were defined in their respective methods then, they can be covariant or
        // invariant use super/subtypes for the overrides locations
        success = testTypevarContainment(overriderReturnType, overriddenReturnType);
      }

      // Sometimes the overridden return type of a method reference becomes a captured
      // type variable.  This leads to defaulting that often makes the overriding return type
      // invalid.  We ignore these.  This happens in Issue403/Issue404.
      if (!success
          && isMethodReference
          && TypesUtils.isCapturedTypeVariable(overriddenReturnType.getUnderlyingType())) {
        if (ElementUtils.isMethod(
            overridden.getElement(), functionApply, atypeFactory.getProcessingEnv())) {
          success =
              typeHierarchy.isSubtype(
                  overriderReturnType,
                  ((AnnotatedTypeVariable) overriddenReturnType).getUpperBound());
        }
      }

      checkReturnMsg(success);
      return success;
    }

    /**
     * Issue an error message or log message about checking an overriding return type.
     *
     * @param success whether the check succeeded or failed
     */
    private void checkReturnMsg(boolean success) {
      if (success && !showchecks) {
        return;
      }

      String msgKey = isMethodReference ? "methodref.return" : "override.return";
      Tree posTree =
          overriderTree instanceof MethodTree
              ? ((MethodTree) overriderTree).getReturnType()
              : overriderTree;
      // The return type of a MethodTree is null for a constructor.
      if (posTree == null) {
        posTree = overriderTree;
      }

      if (showchecks) {
        System.out.printf(
            " %s (at %s):%n"
                + "     overrider: %s %s (return type %s)%n"
                + "    overridden: %s %s (return type %s)%n",
            (success
                ? "success: overriding return type is subtype of overridden"
                : "FAILURE: overriding return type is not subtype of overridden"),
            fileAndLineNumber(posTree),
            overrider,
            overriderType,
            overrider.getReturnType().toString(),
            overridden,
            overriddenType,
            overridden.getReturnType().toString());
      }
      if (!success) {
        FoundRequired pair = FoundRequired.of(overriderReturnType, overriddenReturnType);
        checker.reportError(
            posTree,
            msgKey,
            pair.found,
            pair.required,
            overriderType,
            overrider,
            overriddenType,
            overridden);
      }
    }
  }

  /**
   * Filters the set of conditional postconditions to return only those whose annotation result
   * value matches the value of the given boolean {@code b}. For example, if {@code b == true}, then
   * the following {@code @EnsuresNonNullIf} conditional postcondition would match:<br>
   * {@code @EnsuresNonNullIf(expression="#1", result=true)}<br>
   * {@code boolean equals(@Nullable Object o)}
   *
   * @param conditionalPostconditions each is a ConditionalPostcondition
   * @param b the value required for the {@code result} element
   * @return all the given conditional postconditions whose {@code result} is {@code b}
   */
  private Set<Postcondition> filterConditionalPostconditions(
      Set<ConditionalPostcondition> conditionalPostconditions, boolean b) {
    if (conditionalPostconditions.isEmpty()) {
      return Collections.emptySet();
    }

    Set<Postcondition> result =
        ArraySet.newArraySetOrLinkedHashSet(conditionalPostconditions.size());
    for (Contract c : conditionalPostconditions) {
      ConditionalPostcondition p = (ConditionalPostcondition) c;
      if (p.resultValue == b) {
        result.add(new Postcondition(p.expressionString, p.annotation, p.contractAnnotation));
      }
    }
    return result;
  }

  /**
   * Checks that {@code mustSubset} is a subset of {@code set} in the following sense: For every
   * expression in {@code mustSubset} there must be the same expression in {@code set}, with the
   * same (or a stronger) annotation.
   *
   * <p>This uses field {@link #methodTree} to determine where to issue an error message.
   *
   * @param overriderType the subtype
   * @param overriddenType the supertype
   * @param mustSubset annotations that should be weaker
   * @param set anontations that should be stronger
   * @param messageKey message key for error messages
   */
  private void checkContractsSubset(
      AnnotatedTypeMirror overriderType,
      AnnotatedDeclaredType overriddenType,
      Set<IPair<JavaExpression, AnnotationMirror>> mustSubset,
      Set<IPair<JavaExpression, AnnotationMirror>> set,
      @CompilerMessageKey String messageKey) {

    for (IPair<JavaExpression, AnnotationMirror> weak : mustSubset) {
      JavaExpression jexpr = weak.first;
      boolean found = false;

      for (IPair<JavaExpression, AnnotationMirror> strong : set) {
        // are we looking at a contract of the same receiver?
        if (jexpr.equals(strong.first)) {
          // check subtyping relationship of annotations
          TypeMirror jexprTM = jexpr.getType();
          if (qualHierarchy.isSubtypeShallow(strong.second, jexprTM, weak.second, jexprTM)) {
            found = true;
            break;
          }
        }
      }

      if (!found) {

        String overriddenTypeString = overriddenType.getUnderlyingType().asElement().toString();
        String overriderTypeString;
        if (overriderType.getKind() == TypeKind.DECLARED) {
          DeclaredType overriderTypeMirror =
              ((AnnotatedDeclaredType) overriderType).getUnderlyingType();
          overriderTypeString = overriderTypeMirror.asElement().toString();
        } else {
          overriderTypeString = overriderType.toString();
        }

        // weak.second is the AnnotationMirror that is too strong.  It might be from the
        // precondition or the postcondition.

        // These are the annotations that are too weak.
        StringJoiner strongRelevantAnnos = new StringJoiner(" ").setEmptyValue("no information");
        for (IPair<JavaExpression, AnnotationMirror> strong : set) {
          if (jexpr.equals(strong.first)) {
            strongRelevantAnnos.add(strong.second.toString());
          }
        }

        Object overriddenAnno;
        Object overriderAnno;
        if (messageKey.contains(".precondition.")) {
          overriddenAnno = strongRelevantAnnos;
          overriderAnno = weak.second;
        } else {
          overriddenAnno = weak.second;
          overriderAnno = strongRelevantAnnos;
        }

        checker.reportError(
            methodTree,
            messageKey,
            jexpr,
            methodTree.getName(),
            overriddenTypeString,
            overriddenAnno,
            overriderTypeString,
            overriderAnno);
      }
    }
  }

  /**
   * Localizes some contracts -- that is, viewpoint-adapts them to some method body, according to
   * the value of {@link #methodTree}.
   *
   * <p>The input is a set of {@link Contract}s, each of which contains an expression string and an
   * annotation. In a {@link Contract}, Java expressions are exactly as written in source code, not
   * standardized or viewpoint-adapted.
   *
   * <p>The output is a set of pairs of {@link JavaExpression} (parsed expression string) and
   * standardized annotation (with respect to the path of {@link #methodTree}. This method discards
   * any contract whose expression cannot be parsed into a JavaExpression.
   *
   * @param contractSet a set of contracts
   * @param methodType the type of the method that the contracts are for
   * @return pairs of (expression, AnnotationMirror), which are localized contracts
   */
  private Set<IPair<JavaExpression, AnnotationMirror>> parseAndLocalizeContracts(
      Set<? extends Contract> contractSet, AnnotatedExecutableType methodType) {
    if (contractSet.isEmpty()) {
      return Collections.emptySet();
    }

    // This is the path to a place where the contract is being used, which might or might not be
    // where the contract was defined.  For example, methodTree might be an overriding
    // definition, and the contract might be for a superclass.
    MethodTree methodTree = this.methodTree;

    StringToJavaExpression stringToJavaExpr =
        expression -> {
          JavaExpression javaExpr =
              StringToJavaExpression.atMethodDecl(expression, methodType.getElement(), checker);
          // methodType.getElement() is not necessarily the same method as methodTree, so
          // viewpoint-adapt it to methodTree.
          return javaExpr.atMethodBody(methodTree);
        };

    Set<IPair<JavaExpression, AnnotationMirror>> result =
        ArraySet.newArraySetOrHashSet(contractSet.size());
    for (Contract p : contractSet) {
      String expressionString = p.expressionString;
      AnnotationMirror annotation =
          p.viewpointAdaptDependentTypeAnnotation(atypeFactory, stringToJavaExpr, methodTree);
      JavaExpression exprJe;
      try {
        // TODO: currently, these expressions are parsed many times.
        // This could be optimized to store the result the first time.
        // (same for other annotations)
        exprJe = stringToJavaExpr.toJavaExpression(expressionString);
      } catch (JavaExpressionParseException e) {
        // report errors here
        checker.report(methodTree, e.getDiagMessage());
        continue;
      }
      result.add(IPair.of(exprJe, annotation));
    }
    return result;
  }

  /**
   * Call this only when the current path is an identifier.
   *
   * @return the enclosing member select, or null if the identifier is not the field in a member
   *     selection
   */
  protected @Nullable MemberSelectTree enclosingMemberSelect() {
    TreePath path = this.getCurrentPath();
    assert path.getLeaf().getKind() == Tree.Kind.IDENTIFIER
        : "expected identifier, found: " + path.getLeaf();
    if (path.getParentPath().getLeaf().getKind() == Tree.Kind.MEMBER_SELECT) {
      return (MemberSelectTree) path.getParentPath().getLeaf();
    } else {
      return null;
    }
  }

  /**
   * Returns the statement that encloses the given one.
   *
   * @param tree an AST node that is on the current path
   * @return the statement that encloses the given one
   */
  protected @Nullable Tree enclosingStatement(@FindDistinct Tree tree) {
    TreePath path = this.getCurrentPath();
    while (path != null && path.getLeaf() != tree) {
      path = path.getParentPath();
    }

    if (path != null) {
      return path.getParentPath().getLeaf();
    } else {
      return null;
    }
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void p) {
    checkAccess(tree, p);
    return super.visitIdentifier(tree, p);
  }

  /**
   * Issues an error if access is not allowed, based on an {@code @Unused} annotation.
   *
   * @param identifierTree the identifier being accessed; the method does nothing if it is not a
   *     field
   * @param p ignored
   */
  protected void checkAccess(IdentifierTree identifierTree, Void p) {
    MemberSelectTree memberSel = enclosingMemberSelect();
    ExpressionTree tree;
    Element elem;

    if (memberSel == null) {
      tree = identifierTree;
      elem = TreeUtils.elementFromUse(identifierTree);
    } else {
      tree = memberSel;
      elem = TreeUtils.elementFromUse(memberSel);
    }

    if (elem == null || !elem.getKind().isField()) {
      return;
    }

    AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);

    checkAccessAllowed(elem, receiver, tree);
  }

  /**
   * Issues an error if access not allowed, based on an @Unused annotation.
   *
   * @param field the field to be accessed, whose declaration might be annotated by @Unused. It can
   *     also be (for example) {@code this}, in which case {@code receiverType} is null.
   * @param receiverType the type of the expression whose field is accessed; null if the field is
   *     static
   * @param accessTree the access expression
   */
  protected void checkAccessAllowed(
      Element field,
      @Nullable AnnotatedTypeMirror receiverType,
      @FindDistinct ExpressionTree accessTree) {
    AnnotationMirror unused = atypeFactory.getDeclAnnotation(field, Unused.class);
    if (unused == null) {
      return;
    }

    String when = AnnotationUtils.getElementValueClassName(unused, unusedWhenElement).toString();

    // TODO: Don't just look at the receiver type, but at the declaration annotations on the
    // receiver.  (That will enable handling type annotations that are not part of the type
    // system being checked.)

    // TODO: This requires exactly the same type qualifier, but it should permit subqualifiers.
    if (!AnnotationUtils.containsSameByName(receiverType.getPrimaryAnnotations(), when)) {
      return;
    }

    Tree tree = this.enclosingStatement(accessTree);

    if (tree != null
        && tree.getKind() == Tree.Kind.ASSIGNMENT
        && ((AssignmentTree) tree).getVariable() == accessTree
        && ((AssignmentTree) tree).getExpression().getKind() == Tree.Kind.NULL_LITERAL) {
      // Assigning unused to null is OK.
      return;
    }

    checker.reportError(accessTree, "unallowed.access", field, receiverType);
  }

  /**
   * Tests that the qualifiers present on {@code useType} are valid qualifiers, given the qualifiers
   * on the declaration of the type, {@code declarationType}.
   *
   * <p>The check is shallow, as it does not descend into generic or array types (i.e. only
   * performing the validity check on the raw type or outermost array dimension). {@link
   * BaseTypeVisitor#validateTypeOf(Tree)} would call this for each type argument or array dimension
   * separately.
   *
   * <p>In most cases, {@code useType} simply needs to be a subtype of {@code declarationType}. If a
   * type system makes exceptions to this rule, its implementation should override this method.
   *
   * <p>This method is not called if {@link
   * BaseTypeValidator#shouldCheckTopLevelDeclaredOrPrimitiveType(AnnotatedTypeMirror, Tree)}
   * returns false -- by default, it is not called on the top level for locals and expressions. To
   * enforce a type validity property everywhere, override methods such as {@link
   * BaseTypeValidator#visitDeclared} rather than this method.
   *
   * @param declarationType the type of the class (TypeElement)
   * @param useType the use of the class (instance type)
   * @param tree the tree where the type is used
   * @return true if the useType is a valid use of elemType
   */
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // Don't use isSubtype(ATM, ATM) because it will return false if the types have qualifier
    // parameters.
    AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
    TypeMirror declarationTM = declarationType.getUnderlyingType();
    AnnotationMirrorSet upperBounds =
        atypeFactory.getQualifierUpperBounds().getBoundQualifiers(declarationTM);
    for (AnnotationMirror top : tops) {
      AnnotationMirror upperBound = qualHierarchy.findAnnotationInHierarchy(upperBounds, top);
      if (!typeHierarchy.isSubtypeShallowEffective(useType, upperBound)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests that the qualifiers present on the primitive type are valid.
   *
   * @param type the use of the primitive type
   * @param tree the tree where the type is used
   * @return true if the type is a valid use of the primitive type
   */
  public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
    AnnotationMirrorSet bounds = atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
    return typeHierarchy.isSubtypeShallowEffective(type, bounds);
  }

  /**
   * Tests that the qualifiers present on the array type are valid. This method will be invoked for
   * each array level independently, i.e. this method only needs to check the top-level qualifiers
   * of an array.
   *
   * @param type the array type use
   * @param tree the tree where the type is used
   * @return true if the type is a valid array type
   */
  public boolean isValidUse(AnnotatedArrayType type, Tree tree) {
    AnnotationMirrorSet bounds = atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
    return typeHierarchy.isSubtypeShallowEffective(type, bounds);
  }

  /**
   * Tests whether the tree expressed by the passed type tree is a valid type, and emits an error if
   * that is not the case (e.g. '@Mutable String'). If the tree is a method or constructor, check
   * the return type.
   *
   * @param tree the AST type supplied by the user
   * @return true if the tree is a valid type
   */
  public boolean validateTypeOf(Tree tree) {
    AnnotatedTypeMirror type;
    // It's quite annoying that there is no TypeTree.
    switch (tree.getKind()) {
      case PRIMITIVE_TYPE:
      case PARAMETERIZED_TYPE:
      case TYPE_PARAMETER:
      case ARRAY_TYPE:
      case UNBOUNDED_WILDCARD:
      case EXTENDS_WILDCARD:
      case SUPER_WILDCARD:
      case ANNOTATED_TYPE:
        type = atypeFactory.getAnnotatedTypeFromTypeTree(tree);
        break;
      case METHOD:
        type = atypeFactory.getMethodReturnType((MethodTree) tree);
        if (type == null || type.getKind() == TypeKind.VOID) {
          // Nothing to do for void methods.
          // Note that for a constructor the AnnotatedExecutableType does
          // not use void as return type.
          return true;
        }
        break;
      default:
        type = atypeFactory.getAnnotatedType(tree);
    }
    return validateType(tree, type);
  }

  /**
   * Tests whether the type and corresponding type tree is a valid type, and emits an error if that
   * is not the case (e.g. '@Mutable String'). If the tree is a method or constructor, tests the
   * return type.
   *
   * @param tree the type tree supplied by the user
   * @param type the type corresponding to tree
   * @return true if the type is valid
   */
  protected boolean validateType(Tree tree, AnnotatedTypeMirror type) {
    return typeValidator.isValid(type, tree);
  }

  // This is a test to ensure that all types are valid
  protected final TypeValidator typeValidator;

  protected TypeValidator createTypeValidator() {
    return new BaseTypeValidator(checker, this, atypeFactory);
  }

  // **********************************************************************
  // Random helper methods
  // **********************************************************************

  /**
   * Tests whether the expression should not be checked because of the tree referring to unannotated
   * classes, as specified in the {@code checker.skipUses} property.
   *
   * <p>It returns true if exprTree is a method invocation or a field access to a class whose
   * qualified name matches the {@code checker.skipUses} property.
   *
   * @param exprTree any expression tree
   * @return true if checker should not test exprTree
   */
  protected final boolean shouldSkipUses(ExpressionTree exprTree) {
    // System.out.printf("shouldSkipUses: %s: %s%n", exprTree.getClass(), exprTree);
    if (atypeFactory.isUnreachable(exprTree)) {
      return true;
    }
    Element elm = TreeUtils.elementFromTree(exprTree);
    return checker.shouldSkipUses(elm);
  }

  // **********************************************************************
  // Overriding to avoid visit part of the tree
  // **********************************************************************

  /** Override Compilation Unit so we won't visit package names or imports. */
  @Override
  public Void visitCompilationUnit(CompilationUnitTree identifierTree, Void p) {
    Void r = scan(identifierTree.getPackageAnnotations(), p);
    // r = reduce(scan(identifierTree.getPackageName(), p), r);
    // r = reduce(scan(identifierTree.getImports(), p), r);
    r = reduce(scan(identifierTree.getTypeDecls(), p), r);
    return r;
  }
}
