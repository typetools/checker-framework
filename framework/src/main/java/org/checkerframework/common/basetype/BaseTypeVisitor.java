package org.checkerframework.common.basetype;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
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
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMemberReference.ReferenceKind;
import com.sun.tools.javac.tree.TreeInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.VisitorState;
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
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.ArraysPlume;
import org.plumelib.util.CollectionsPlume;

/**
 * A {@link SourceVisitor} that performs assignment and pseudo-assignment checking, method
 * invocation checking, and assignability checking.
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
 *       using {@code TypeHierarchy.isSubtype} method. This includes method invocation and method
 *       overriding checks.
 *   <li><b>Type Validity Check</b>: It verifies that any user-supplied type is a valid type, using
 *       one of the {@code isValidUse} methods.
 *   <li><b>(Re-)Assignability Check</b>: It verifies that any assignment is valid, using {@code
 *       Checker.isAssignable} method.
 * </ol>
 *
 * @see "JLS $4"
 * @see TypeHierarchy#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)
 * @see AnnotatedTypeFactory
 */
/*
 * Note how the handling of VisitorState is duplicated in AbstractFlow. In
 * particular, the handling of the assignment context has to be done correctly
 * in both classes. This is a pain and we should see how to handle this in the
 * DFF version.
 *
 * TODO: missing assignment context: array initializer
 * expressions should have the component type as context
 */
public class BaseTypeVisitor<Factory extends GenericAnnotatedTypeFactory<?, ?, ?, ?>>
    extends SourceVisitor<Void, Void> {

  /** The {@link BaseTypeChecker} for error reporting. */
  protected final BaseTypeChecker checker;

  /** The factory to use for obtaining "parsed" version of annotations. */
  protected final Factory atypeFactory;

  /** For obtaining line numbers in -Ashowchecks debugging output. */
  protected final SourcePositions positions;

  /** For storing visitor state. */
  protected final VisitorState visitorState;

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

  /** The {@code value} element/field of the @java.lang.annotation.Target annotation. */
  protected final ExecutableElement targetValueElement;
  /** The {@code when} element/field of the @Unused annotation. */
  protected final ExecutableElement unusedWhenElement;

  /** True if "-Ashowchecks" was passed on the command line. */
  private final boolean showchecks;
  /** True if "-Ainfer" was passed on the command line. */
  private final boolean infer;
  /** True if "-AsuggestPureMethods" or "-Ainfer" was passed on the command line. */
  private final boolean suggestPureMethods;
  /**
   * True if "-AcheckPurityAnnotations" or "-AsuggestPureMethods" or "-Ainfer" was passed on the
   * command line.
   */
  private final boolean checkPurity;
  /**
   * True if purity annotations should be inferred. Should be set to false if both the Lock Checker
   * (or some other checker that overrides {@link CFAbstractStore#isSideEffectFree} in a
   * non-standard way) and some other checker is being run.
   */
  protected boolean inferPurity = true;

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
  protected BaseTypeVisitor(BaseTypeChecker checker, Factory typeFactory) {
    super(checker);

    this.checker = checker;
    this.atypeFactory = typeFactory == null ? createTypeFactory() : typeFactory;
    this.positions = trees.getSourcePositions();
    this.visitorState = atypeFactory.getVisitorState();
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
    checkPurity = checker.hasOption("checkPurityAnnotations") || suggestPureMethods;
  }

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
  @SuppressWarnings("unchecked") // unchecked cast to type variable
  protected Factory createTypeFactory() {
    // Try to reflectively load the type factory.
    Class<?> checkerClass = checker.getClass();
    while (checkerClass != BaseTypeChecker.class) {
      AnnotatedTypeFactory result =
          BaseTypeChecker.invokeConstructorFor(
              BaseTypeChecker.getRelatedClassName(checkerClass, "AnnotatedTypeFactory"),
              new Class<?>[] {BaseTypeChecker.class},
              new Object[] {checker});
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
  public void setRoot(CompilationUnitTree root) {
    atypeFactory.setRoot(root);
    super.setRoot(root);
    testJointJavacJavaParserVisitor();
    testAnnotationInsertion();
  }

  @Override
  public Void scan(@Nullable Tree tree, Void p) {
    if (tree != null && getCurrentPath() != null) {
      this.visitorState.setPath(new TreePath(getCurrentPath(), tree));
    }
    return super.scan(tree, p);
  }

  /**
   * Test {@link org.checkerframework.framework.ajava.JointJavacJavaParserVisitor} if the checker
   * has the "ajavaChecks" option.
   *
   * <p>Parse the current source file with JavaParser and check that the AST can be matched with the
   * Tree prodoced by javac. Crash if not.
   *
   * <p>Subclasses may override this method to disable the test if even the option is provided.
   */
  protected void testJointJavacJavaParserVisitor() {
    if (root == null || !checker.hasOption("ajavaChecks")) {
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
              "Javac tree not matched to JavaParser node: %s, in file: %s",
              expected, root.getSourceFile().getName());
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
    if (root == null || !checker.hasOption("ajavaChecks")) {
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
      // This check only runs on files from the Checker Framework test suite, which should all use
      // UNIX line separators. Using System.lineSeparator instead of "\n" could cause the test to
      // fail on Mac or Windows.
      withAnnotations =
          new InsertAjavaAnnotations(elements)
              .insertAnnotations(annotationInputStream, withoutAnnotations, "\n");
    } catch (IOException e) {
      throw new BugInCF("Error while reading Java file: " + root.getSourceFile().toUri(), e);
    }

    CompilationUnit modifiedAst = null;
    try {
      modifiedAst = JavaParserUtil.parseCompilationUnit(withAnnotations);
    } catch (ParseProblemException e) {
      throw new BugInCF("Failed to parse annotation insertion:\n" + withAnnotations, e);
    }

    AnnotationEqualityVisitor visitor = new AnnotationEqualityVisitor();
    originalAst.accept(visitor, modifiedAst);
    if (!visitor.getAnnotationsMatch()) {
      throw new BugInCF(
          String.join(
              System.lineSeparator(),
              "Sanity check of erasing then reinserting annotations produced a different AST.",
              "File: " + root.getSourceFile(),
              "Original node: " + visitor.getMismatchedNode1(),
              "Node with annotations re-inserted: " + visitor.getMismatchedNode2(),
              "Original annotations: " + visitor.getMismatchedNode1().getAnnotations(),
              "Re-inserted annotations: " + visitor.getMismatchedNode2().getAnnotations(),
              "Original AST:",
              originalAst.toString(),
              "Ast with annotations re-inserted: " + modifiedAst));
    }
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
    if (checker.shouldSkipDefs(classTree)) {
      // Not "return super.visitClass(classTree, p);" because that would recursively call visitors
      // on subtrees; we want to skip the class entirely.
      return null;
    }
    atypeFactory.preProcessClassTree(classTree);

    TreePath preTreePath = visitorState.getPath();
    AnnotatedDeclaredType preACT = visitorState.getClassType();
    ClassTree preCT = visitorState.getClassTree();
    AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
    MethodTree preMT = visitorState.getMethodTree();
    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();

    // Don't use atypeFactory.getPath, because that depends on the visitorState path.
    visitorState.setPath(TreePath.getPath(root, classTree));
    visitorState.setClassType(
        atypeFactory.getAnnotatedType(TreeUtils.elementFromDeclaration(classTree)));
    visitorState.setClassTree(classTree);
    visitorState.setMethodReceiver(null);
    visitorState.setMethodTree(null);
    visitorState.setAssignmentContext(null);

    try {
      processClassTree(classTree);
      atypeFactory.postProcessClassTree(classTree);
    } finally {
      visitorState.setPath(preTreePath);
      visitorState.setClassType(preACT);
      visitorState.setClassTree(preCT);
      visitorState.setMethodReceiver(preAMT);
      visitorState.setMethodTree(preMT);
      visitorState.setAssignmentContext(preAssignmentContext);
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
      validateTypeOf(ext);
    }

    List<? extends Tree> impls = classTree.getImplementsClause();
    if (impls != null) {
      for (Tree im : impls) {
        validateTypeOf(im);
      }
    }

    checkForPolymorphicQualifiers(classTree);

    checkExtendsImplements(classTree);

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
          QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
          AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(annoTree);
          if (atypeFactory.isSupportedQualifier(anno)
              && qualifierHierarchy.isPolymorphicQualifier(anno)) {
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
  protected void checkForPolymorphicQualifiers(ClassTree classTree) {
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
  protected void checkForPolymorphicQualifiers(
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
    // therefor cannot appear on a field.
    Set<AnnotationMirror> illegalOnFieldsPolyQual = AnnotationUtils.createAnnotationSet();
    // Set of polymorphic annotations for all hierarchies
    Set<AnnotationMirror> polys = AnnotationUtils.createAnnotationSet();
    TypeElement classElement = TreeUtils.elementFromDeclaration(classTree);
    for (AnnotationMirror top : atypeFactory.getQualifierHierarchy().getTopAnnotations()) {
      AnnotationMirror poly = atypeFactory.getQualifierHierarchy().getPolymorphicAnnotation(top);
      if (poly != null) {
        polys.add(poly);
      }
      // else {
      // If there is no polymorphic qualifier in the hierarchy, it could still have a
      // @HasQualifierParameter that must be checked.
      // }

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
        List<DiagMessage> hasIllegalPoly;
        if (ElementUtils.isStatic(TreeUtils.elementFromDeclaration((VariableTree) mem))) {
          // A polymorphic qualifier is not allowed on a static field even if the class
          // has a qualifier parameter.
          hasIllegalPoly = polyScanner.visit(fieldType, polys);
        } else {
          hasIllegalPoly = polyScanner.visit(fieldType, illegalOnFieldsPolyQual);
        }
        for (DiagMessage dm : hasIllegalPoly) {
          checker.report(mem, dm);
        }
      }
    }
  }

  /**
   * A scanner that given a set of polymorphic qualifiers, returns a list of errors reporting a use
   * of one of the polymorphic qualifiers.
   */
  private final PolyTypeScanner polyScanner = new PolyTypeScanner();

  /**
   * A scanner that given a set of polymorphic qualifiers, returns a list of errors reporting a use
   * of one of the polymorphic qualifiers.
   */
  static class PolyTypeScanner
      extends SimpleAnnotatedTypeScanner<List<DiagMessage>, Set<AnnotationMirror>> {

    /** Create PolyTypeScanner. */
    private PolyTypeScanner() {
      super(DiagMessage::mergeLists, Collections.emptyList());
    }

    @Override
    protected List<DiagMessage> defaultAction(
        AnnotatedTypeMirror type, Set<AnnotationMirror> polys) {
      if (type == null) {
        return Collections.emptyList();
      }

      for (AnnotationMirror poly : polys) {
        if (type.hasAnnotationRelaxed(poly)) {
          return Collections.singletonList(
              new DiagMessage(Kind.ERROR, "invalid.polymorphic.qualifier.use", poly));
        }
      }
      return Collections.emptyList();
    }
  }

  /**
   * If "@B class Y extends @A X {}", then enforce that @B must be a subtype of @A.
   *
   * <p>Also validate the types of the extends and implements clauses.
   *
   * @param classTree class tree to check
   */
  protected void checkExtendsImplements(ClassTree classTree) {
    if (TypesUtils.isAnonymous(TreeUtils.typeOf(classTree))) {
      // Don't check extends clause on anonymous classes.
      return;
    }
    Set<AnnotationMirror> classBounds =
        atypeFactory.getTypeDeclarationBounds(TreeUtils.typeOf(classTree));
    QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    // If "@B class Y extends @A X {}", then enforce that @B must be a subtype of @A.
    // classTree.getExtendsClause() is null when there is no explicitly-written extends clause,
    // as in "class X {}". This is equivalent to writing "class X extends @Top Object {}", so
    // there is no need to do any subtype checking.
    if (classTree.getExtendsClause() != null) {
      Set<AnnotationMirror> extendsAnnos =
          atypeFactory.getTypeOfExtendsImplements(classTree.getExtendsClause()).getAnnotations();
      for (AnnotationMirror classAnno : classBounds) {
        AnnotationMirror extendsAnno =
            qualifierHierarchy.findAnnotationInSameHierarchy(extendsAnnos, classAnno);
        if (!qualifierHierarchy.isSubtype(classAnno, extendsAnno)) {
          checker.reportError(
              classTree.getExtendsClause(),
              "declaration.inconsistent.with.extends.clause",
              classAnno,
              extendsAnno);
        }
      }
    }
    // Do the same check as above for implements clauses.
    for (Tree implementsClause : classTree.getImplementsClause()) {
      Set<AnnotationMirror> implementsClauseAnnos =
          atypeFactory.getTypeOfExtendsImplements(implementsClause).getAnnotations();

      for (AnnotationMirror classAnno : classBounds) {
        AnnotationMirror implementsAnno =
            qualifierHierarchy.findAnnotationInSameHierarchy(implementsClauseAnnos, classAnno);
        if (!qualifierHierarchy.isSubtype(classAnno, implementsAnno)) {
          checker.reportError(
              implementsClause,
              "declaration.inconsistent.with.implements.clause",
              classAnno,
              implementsAnno);
        }
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
      DiagMessage superError = invariants.isSuperInvariant(superInvar, atypeFactory);
      if (superError != null) {
        checker.report(errorTree, superError);
      }
    }

    List<String> notFinal = new ArrayList<>();
    for (VariableElement field : fieldElts) {
      String fieldName = field.getSimpleName().toString();
      if (!ElementUtils.isFinal(field)) {
        notFinal.add(fieldName);
      }
      AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(field);

      List<AnnotationMirror> annos = invariants.getQualifiersFor(field.getSimpleName());
      for (AnnotationMirror invariantAnno : annos) {
        AnnotationMirror declaredAnno = type.getEffectiveAnnotationInHierarchy(invariantAnno);
        if (declaredAnno == null) {
          // invariant anno isn't in this hierarchy
          continue;
        }

        if (!atypeFactory.getQualifierHierarchy().isSubtype(invariantAnno, declaredAnno)) {
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

  protected void checkDefaultConstructor(ClassTree node) {}

  /**
   * Checks that the method obeys override and subtype rules to all overridden methods. (Uses the
   * pseudo-assignment logic to do so.)
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
  public Void visitMethod(MethodTree node, Void p) {
    // We copy the result from getAnnotatedType to ensure that circular types (e.g. K extends
    // Comparable<K>) are represented by circular AnnotatedTypeMirrors, which avoids problems with
    // later checks.
    // TODO: Find a cleaner way to ensure circular AnnotatedTypeMirrors.
    AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(node).deepCopy();
    AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
    MethodTree preMT = visitorState.getMethodTree();
    visitorState.setMethodReceiver(methodType.getReceiverType());
    visitorState.setMethodTree(node);
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);

    warnAboutTypeAnnotationsTooEarly(node, node.getModifiers());

    if (node.getReturnType() != null) {
      visitAnnotatedType(node.getModifiers().getAnnotations(), node.getReturnType());
    }

    try {
      if (TreeUtils.isAnonymousConstructor(node)) {
        // We shouldn't dig deeper
        return null;
      }

      if (TreeUtils.isConstructor(node)) {
        checkConstructorResult(methodType, methodElement);
      }

      checkPurity(node);

      // Passing the whole method/constructor validates the return type
      validateTypeOf(node);

      // Validate types in throws clauses
      for (ExpressionTree thr : node.getThrows()) {
        validateTypeOf(thr);
      }

      atypeFactory.getDependentTypesHelper().checkMethodForErrorExpressions(node, methodType);

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
        if (!checkOverride(node, enclosingType, overriddenMethodType, overriddenType)) {
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
              (VariableTree param) -> param.getName().toString(), node.getParameters());
      checkContractsAtMethodDeclaration(node, methodElement, formalParamNames, abstractMethod);

      // Infer postconditions
      if (atypeFactory.getWholeProgramInference() != null) {
        assert ElementUtils.isElementFromSourceCode(methodElement);

        // TODO: Infer conditional postconditions too.
        CFAbstractStore<?, ?> store = atypeFactory.getRegularExitStore(node);
        // The store is null if the method has no normal exit, for example if its body is a
        // throw statement.
        if (store != null) {
          atypeFactory
              .getWholeProgramInference()
              .updateContracts(Analysis.BeforeOrAfter.AFTER, methodElement, store);
        }
      }

      checkForPolymorphicQualifiers(node.getTypeParameters());

      return super.visitMethod(node, p);
    } finally {
      visitorState.setMethodReceiver(preMRT);
      visitorState.setMethodTree(preMT);
    }
  }

  /**
   * Check method purity if needed. Note that overriding rules are checked as part of {@link
   * #checkOverride(MethodTree, AnnotatedTypeMirror.AnnotatedExecutableType,
   * AnnotatedTypeMirror.AnnotatedDeclaredType, AnnotatedTypeMirror.AnnotatedExecutableType,
   * AnnotatedTypeMirror.AnnotatedDeclaredType)}.
   *
   * @param node the method tree to check
   */
  protected void checkPurity(MethodTree node) {
    if (!checkPurity) {
      return;
    }

    if (!suggestPureMethods && !PurityUtils.hasPurityAnnotation(atypeFactory, node)) {
      // There is nothing to check.
      return;
    }

    // check "no" purity
    EnumSet<Pure.Kind> kinds = PurityUtils.getPurityKinds(atypeFactory, node);
    // @Deterministic makes no sense for a void method or constructor
    boolean isDeterministic = kinds.contains(Pure.Kind.DETERMINISTIC);
    if (isDeterministic) {
      if (TreeUtils.isConstructor(node)) {
        checker.reportWarning(node, "purity.deterministic.constructor");
      } else if (TreeUtils.typeOf(node.getReturnType()).getKind() == TypeKind.VOID) {
        checker.reportWarning(node, "purity.deterministic.void.method");
      }
    }

    TreePath body = atypeFactory.getPath(node.getBody());
    PurityResult r;
    if (body == null) {
      r = new PurityResult();
    } else {
      r =
          PurityChecker.checkPurity(
              body,
              atypeFactory,
              checker.hasOption("assumeSideEffectFree") || checker.hasOption("assumePure"),
              checker.hasOption("assumeDeterministic") || checker.hasOption("assumePure"));
    }
    if (!r.isPure(kinds)) {
      reportPurityErrors(r, node, kinds);
    }

    if (suggestPureMethods && !TreeUtils.isSynthetic(node)) {
      // Issue a warning if the method is pure, but not annotated as such.
      EnumSet<Pure.Kind> additionalKinds = r.getKinds().clone();
      additionalKinds.removeAll(kinds);
      if (TreeUtils.isConstructor(node)) {
        additionalKinds.remove(Pure.Kind.DETERMINISTIC);
      }
      if (!additionalKinds.isEmpty()) {
        if (infer) {
          if (inferPurity) {
            WholeProgramInference wpi = atypeFactory.getWholeProgramInference();
            ExecutableElement methodElt = TreeUtils.elementFromDeclaration(node);
            if (additionalKinds.size() == 2) {
              wpi.addMethodDeclarationAnnotation(methodElt, PURE);
            } else if (additionalKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
              wpi.addMethodDeclarationAnnotation(methodElt, SIDE_EFFECT_FREE);
            } else if (additionalKinds.contains(Pure.Kind.DETERMINISTIC)) {
              wpi.addMethodDeclarationAnnotation(methodElt, DETERMINISTIC);
            } else {
              throw new BugInCF("Unexpected purity kind in " + additionalKinds);
            }
          }
        } else {
          if (additionalKinds.size() == 2) {
            checker.reportWarning(node, "purity.more.pure", node.getName());
          } else if (additionalKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
            checker.reportWarning(node, "purity.more.sideeffectfree", node.getName());
          } else if (additionalKinds.contains(Pure.Kind.DETERMINISTIC)) {
            checker.reportWarning(node, "purity.more.deterministic", node.getName());
          } else {
            throw new BugInCF("Unexpected purity kind in " + additionalKinds);
          }
        }
      }
    }
  }

  /**
   * Issue a warning if the result type of the constructor is not top. If it is a supertype of the
   * class, then a conflicting.annos error will also be issued by {@link
   * #isValidUse(AnnotatedTypeMirror.AnnotatedDeclaredType,AnnotatedTypeMirror.AnnotatedDeclaredType,Tree)}.
   *
   * @param constructorType AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    Set<AnnotationMirror> constructorAnnotations = constructorType.getReturnType().getAnnotations();
    Set<? extends AnnotationMirror> tops = qualifierHierarchy.getTopAnnotations();

    for (AnnotationMirror top : tops) {
      AnnotationMirror constructorAnno =
          qualifierHierarchy.findAnnotationInHierarchy(constructorAnnotations, top);
      if (!qualifierHierarchy.isSubtype(top, constructorAnno)) {
        checker.reportWarning(
            constructorElement, "inconsistent.constructor.type", constructorAnno, top);
      }
    }
  }

  /**
   * Reports errors found during purity checking.
   *
   * @param result whether the method is deterministic and/or side-effect-free
   * @param node the method
   * @param expectedKinds the expected purity for the method
   */
  protected void reportPurityErrors(
      PurityResult result, MethodTree node, EnumSet<Pure.Kind> expectedKinds) {
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
      for (Pair<Tree, String> r : result.getNotBothReasons()) {
        reportPurityError(msgKeyPrefix, r);
      }
      if (violations.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
        for (Pair<Tree, String> r : result.getNotSEFreeReasons()) {
          reportPurityError("purity.not.sideeffectfree.", r);
        }
      }
      if (violations.contains(Pure.Kind.DETERMINISTIC)) {
        for (Pair<Tree, String> r : result.getNotDetReasons()) {
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
  private void reportPurityError(String msgKeyPrefix, Pair<Tree, String> r) {
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
    Set<Element> parameters = new HashSet<>(1);
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
      // If there is no regular exitStore, then the method cannot reach the regular exit and there
      // is no need to check anything.
    } else {
      CFAbstractValue<?> value = exitStore.getValue(expression);
      AnnotationMirror inferredAnno = null;
      if (value != null) {
        QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
        Set<AnnotationMirror> annos = value.getAnnotations();
        inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, annotation);
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
  private String contractExpressionAndType(
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

    for (Pair<ReturnNode, ?> pair : atypeFactory.getReturnStatementStores(methodTree)) {
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
        QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
        Set<AnnotationMirror> annos = value.getAnnotations();
        inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, annotation);
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
  public Void visitTypeParameter(TypeParameterTree node, Void p) {
    validateTypeOf(node);
    // Check the bounds here and not with every TypeParameterTree.
    // For the latter, we only need to check annotations on the type variable itself.
    // Why isn't this covered by the super call?
    for (Tree tpb : node.getBounds()) {
      validateTypeOf(tpb);
    }

    if (node.getBounds().size() > 1) {
      // The upper bound of the type parameter is an intersection
      AnnotatedTypeVariable type =
          (AnnotatedTypeVariable) atypeFactory.getAnnotatedTypeFromTypeTree(node);
      AnnotatedIntersectionType intersection = (AnnotatedIntersectionType) type.getUpperBound();
      checkExplicitAnnotationsOnIntersectionBounds(intersection, node.getBounds());
    }

    return super.visitTypeParameter(node, p);
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
          AnnotationMirror anno = intersection.getAnnotationInHierarchy(explictAnno);
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
  public Void visitVariable(VariableTree node, Void p) {
    warnAboutTypeAnnotationsTooEarly(node, node.getModifiers());

    visitAnnotatedType(node.getModifiers().getAnnotations(), node.getType());

    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();
    AnnotatedTypeMirror variableType;
    if (getCurrentPath().getParentPath() != null
        && getCurrentPath().getParentPath().getLeaf().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
      // Calling getAnnotatedTypeLhs on a lambda parameter node is possibly expensive
      // because caching is turned off.  This should be fixed by #979.
      // See https://github.com/typetools/checker-framework/issues/2853 for an example.
      variableType = atypeFactory.getAnnotatedType(node);
    } else {
      variableType = atypeFactory.getAnnotatedTypeLhs(node);
    }
    visitorState.setAssignmentContext(Pair.of(node, variableType));

    try {
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(variableType, node);
      // If there's no assignment in this variable declaration, skip it.
      if (node.getInitializer() != null) {
        commonAssignmentCheck(node, node.getInitializer(), "assignment");
      } else {
        // commonAssignmentCheck validates the type of node,
        // so only validate if commonAssignmentCheck wasn't called
        validateTypeOf(node);
      }
      return super.visitVariable(node, p);
    } finally {
      visitorState.setAssignmentContext(preAssignmentContext);
    }
  }

  /**
   * Warn if a type annotation is written before a modifier such as "public" or before a declaration
   * annotation.
   *
   * @param node a VariableTree or a MethodTree
   * @param modifiersTree the modifiers sub-tree of node
   */
  private void warnAboutTypeAnnotationsTooEarly(Tree node, ModifiersTree modifiersTree) {

    // Don't issue warnings about compiler-inserted modifiers.
    // This simple code completely igonores enum constants and try-with-resources declarations.
    // It could be made to catch some user errors in those locations, but it doesn't seem worth
    // the effort to do so.
    if (node.getKind() == Tree.Kind.VARIABLE) {
      ElementKind varKind = TreeUtils.elementFromDeclaration((VariableTree) node).getKind();
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
          // Nothing to do
      }
    }

    Set<Modifier> modifierSet = modifiersTree.getFlags();
    List<? extends AnnotationTree> annotations = modifiersTree.getAnnotations();

    if (annotations.isEmpty()) {
      return;
    }

    // Warn about type annotations written before modifiers such as "public".  javac retains no
    // information about modifier locations.  So, this is a very partial check:  Issue a warning if
    // a type annotation is at the very beginning of the VariableTree, and a modifier follows it.

    // Check if a type annotation precedes a declaration annotation.
    int lastDeclAnnoIndex = -1;
    for (int i = annotations.size() - 1; i > 0; i--) { // no need to check index 0
      if (!isTypeAnnotation(annotations.get(i))) {
        lastDeclAnnoIndex = i;
        break;
      }
    }
    if (lastDeclAnnoIndex != -1) {
      List<AnnotationTree> badTypeAnnos = new ArrayList<>();
      for (int i = 0; i < lastDeclAnnoIndex; i++) {
        AnnotationTree anno = annotations.get(i);
        if (isTypeAnnotation(anno)) {
          badTypeAnnos.add(anno);
        }
      }
      if (!badTypeAnnos.isEmpty()) {
        checker.reportWarning(
            node, "type.anno.before.decl.anno", badTypeAnnos, annotations.get(lastDeclAnnoIndex));
      }
    }

    // Determine the length of the text that ought to precede the first type annotation.
    // If the type annotation appears before that text could appear, then warn that a
    // modifier appears after the type annotation.
    // TODO: in the future, account for the lengths of declaration annotations.  Length of toString
    // of the annotation isn't useful, as it might be different length than original input.  Can use
    // JCTree.getEndPosition(EndPosTable) and com.sun.tools.javac.tree.EndPosTable, but it requires
    // -Xjcov.
    AnnotationTree firstAnno = annotations.get(0);
    if (!modifierSet.isEmpty() && isTypeAnnotation(firstAnno)) {
      int precedingTextLength = 0;
      for (Modifier m : modifierSet) {
        precedingTextLength += m.toString().length() + 1; // +1 for the space
      }
      int annoStartPos = ((JCTree) firstAnno).getStartPosition();
      int varStartPos = ((JCTree) node).getStartPosition();
      if (annoStartPos < varStartPos + precedingTextLength) {
        checker.reportWarning(node, "type.anno.before.modifier", firstAnno, modifierSet);
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
        throw new Error("Unhandled kind: " + annoType.getKind() + " for " + anno);
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
   * <p>If the subtype check fails, it issues a "assignment" error.
   */
  @Override
  public Void visitAssignment(AssignmentTree node, Void p) {
    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();
    visitorState.setAssignmentContext(
        Pair.of((Tree) node.getVariable(), atypeFactory.getAnnotatedType(node.getVariable())));
    try {
      commonAssignmentCheck(node.getVariable(), node.getExpression(), "assignment");
      return super.visitAssignment(node, p);
    } finally {
      visitorState.setAssignmentContext(preAssignmentContext);
    }
  }

  /**
   * Performs a subtype check, to test whether the node expression iterable type is a subtype of the
   * variable type in the enhanced for loop.
   *
   * <p>If the subtype check fails, it issues a "enhancedfor" error.
   */
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
    AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(node.getVariable());
    AnnotatedTypeMirror iteratedType = atypeFactory.getIterableElementType(node.getExpression());
    boolean valid = validateTypeOf(node.getVariable());
    if (valid) {
      commonAssignmentCheck(var, iteratedType, node.getExpression(), "enhancedfor");
    }
    return super.visitEnhancedForLoop(node, p);
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
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

    // Skip calls to the Enum constructor (they're generated by javac and
    // hard to check), also see CFGBuilder.visitMethodInvocation.
    if (TreeUtils.elementFromUse(node) == null || TreeUtils.isEnumSuper(node)) {
      return super.visitMethodInvocation(node, p);
    }

    if (shouldSkipUses(node)) {
      return super.visitMethodInvocation(node, p);
    }

    ParameterizedExecutableType mType = atypeFactory.methodFromUse(node);
    AnnotatedExecutableType invokedMethod = mType.executableType;
    List<AnnotatedTypeMirror> typeargs = mType.typeArgs;

    if (!atypeFactory.ignoreUninferredTypeArguments) {
      for (AnnotatedTypeMirror typearg : typeargs) {
        if (typearg.getKind() == TypeKind.WILDCARD
            && ((AnnotatedWildcardType) typearg).isUninferredTypeArgument()) {
          checker.reportError(
              node, "type.arguments.not.inferred", invokedMethod.getElement().getSimpleName());
          break; // only issue error once per method
        }
      }
    }

    List<AnnotatedTypeParameterBounds> paramBounds =
        CollectionsPlume.mapList(
            AnnotatedTypeVariable::getBounds, invokedMethod.getTypeVariables());

    ExecutableElement method = invokedMethod.getElement();
    CharSequence methodName = ElementUtils.getSimpleNameOrDescription(method);
    try {
      checkTypeArguments(
          node,
          paramBounds,
          typeargs,
          node.getTypeArguments(),
          methodName,
          invokedMethod.getTypeVariables());
      List<AnnotatedTypeMirror> params =
          AnnotatedTypes.expandVarArgsParameters(atypeFactory, invokedMethod, node.getArguments());
      checkArguments(params, node.getArguments(), methodName, method.getParameters());
      checkVarargs(invokedMethod, node);

      if (ElementUtils.isMethod(
          invokedMethod.getElement(), vectorCopyInto, atypeFactory.getProcessingEnv())) {
        typeCheckVectorCopyIntoArgument(node, params);
      }

      ExecutableElement invokedMethodElement = invokedMethod.getElement();
      if (!ElementUtils.isStatic(invokedMethodElement) && !TreeUtils.isSuperConstructorCall(node)) {
        checkMethodInvocability(invokedMethod, node);
      }

      // check precondition annotations
      checkPreconditions(
          node, atypeFactory.getContractsFromMethod().getPreconditions(invokedMethodElement));

      if (TreeUtils.isSuperConstructorCall(node)) {
        checkSuperConstructorCall(node);
      } else if (TreeUtils.isThisConstructorCall(node)) {
        checkThisConstructorCall(node);
      }
    } catch (RuntimeException t) {
      // Sometimes the type arguments are inferred incorrect which causes crashes. Once #979
      // is fixed this should be removed and crashes should be reported normally.
      if (node.getTypeArguments().size() == typeargs.size()) {
        // They type arguments were explicitly written.
        throw t;
      }
      if (!atypeFactory.ignoreUninferredTypeArguments) {
        checker.reportError(
            node, "type.arguments.not.inferred", invokedMethod.getElement().getSimpleName());
      } // else ignore the crash.
    }

    // Do not call super, as that would observe the arguments without
    // a set assignment context.
    scan(node.getMethodSelect(), p);
    return null; // super.visitMethodInvocation(node, p);
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
    Set<? extends AnnotationMirror> topAnnotations =
        atypeFactory.getQualifierHierarchy().getTopAnnotations();
    for (AnnotationMirror topAnno : topAnnotations) {
      AnnotationMirror superTypeMirror = superType.getAnnotationInHierarchy(topAnno);
      AnnotationMirror constructorTypeMirror =
          constructorType.getReturnType().getAnnotationInHierarchy(topAnno);

      if (!atypeFactory.getQualifierHierarchy().isSubtype(superTypeMirror, constructorTypeMirror)) {
        checker.reportError(call, errorKey, constructorTypeMirror, call, superTypeMirror);
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
    if (!TreeUtils.isVarArgs(tree)) {
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
        QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
        Set<AnnotationMirror> annos = value.getAnnotations();
        inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, anno);
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
    return inferredAnnotation != null
        && atypeFactory.getQualifierHierarchy().isSubtype(inferredAnnotation, necessaryAnnotation);
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
   * @param node a method invocation of {@code Vector.copyInto()}
   * @param params the types of the parameters of {@code Vectory.copyInto()}
   */
  protected void typeCheckVectorCopyIntoArgument(
      MethodInvocationTree node, List<? extends AnnotatedTypeMirror> params) {
    assert params.size() == 1
        : "invalid no. of parameters " + params + " found for method invocation " + node;
    assert node.getArguments().size() == 1
        : "invalid no. of arguments in method invocation " + node;

    AnnotatedTypeMirror passed = atypeFactory.getAnnotatedType(node.getArguments().get(0));
    AnnotatedArrayType passedAsArray = (AnnotatedArrayType) passed;

    AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);
    AnnotatedDeclaredType receiverAsVector =
        AnnotatedTypes.asSuper(atypeFactory, receiver, vectorType);
    if (receiverAsVector.getTypeArguments().isEmpty()) {
      return;
    }

    AnnotatedTypeMirror argComponent = passedAsArray.getComponentType();
    AnnotatedTypeMirror vectorTypeArg = receiverAsVector.getTypeArguments().get(0);
    Tree errorLocation = node.getArguments().get(0);
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
  public Void visitNewClass(NewClassTree node, Void p) {
    if (checker.shouldSkipUses(TreeUtils.constructor(node))) {
      return super.visitNewClass(node, p);
    }

    ParameterizedExecutableType fromUse = atypeFactory.constructorFromUse(node);
    AnnotatedExecutableType constructorType = fromUse.executableType;
    List<AnnotatedTypeMirror> typeargs = fromUse.typeArgs;

    List<? extends ExpressionTree> passedArguments = node.getArguments();
    List<AnnotatedTypeMirror> params =
        AnnotatedTypes.expandVarArgsParameters(atypeFactory, constructorType, passedArguments);

    ExecutableElement constructor = constructorType.getElement();
    CharSequence constructorName = ElementUtils.getSimpleNameOrDescription(constructor);

    checkArguments(params, passedArguments, constructorName, constructor.getParameters());
    checkVarargs(constructorType, node);

    List<AnnotatedTypeParameterBounds> paramBounds =
        CollectionsPlume.mapList(
            AnnotatedTypeVariable::getBounds, constructorType.getTypeVariables());

    checkTypeArguments(
        node,
        paramBounds,
        typeargs,
        node.getTypeArguments(),
        constructorName,
        constructor.getTypeParameters());

    boolean valid = validateTypeOf(node);

    if (valid) {
      AnnotatedDeclaredType dt = atypeFactory.getAnnotatedType(node);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(dt, node);
      checkConstructorInvocation(dt, constructorType, node);
    }
    // Do not call super, as that would observe the arguments without
    // a set assignment context.
    scan(node.getEnclosingExpression(), p);
    scan(node.getIdentifier(), p);
    scan(node.getClassBody(), p);

    return null;
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {

    AnnotatedExecutableType functionType = atypeFactory.getFunctionTypeFromTree(node);

    if (node.getBody().getKind() != Tree.Kind.BLOCK) {
      // Check return type for single statement returns here.
      AnnotatedTypeMirror ret = functionType.getReturnType();
      if (ret.getKind() != TypeKind.VOID) {
        visitorState.setAssignmentContext(Pair.of((Tree) node, ret));
        commonAssignmentCheck(ret, (ExpressionTree) node.getBody(), "return");
      }
    }

    // Check parameters
    for (int i = 0; i < functionType.getParameterTypes().size(); ++i) {
      AnnotatedTypeMirror lambdaParameter =
          atypeFactory.getAnnotatedType(node.getParameters().get(i));
      commonAssignmentCheck(
          lambdaParameter,
          functionType.getParameterTypes().get(i),
          node.getParameters().get(i),
          "lambda.param",
          i);
    }

    // TODO: Postconditions?
    // https://github.com/typetools/checker-framework/issues/801

    return super.visitLambdaExpression(node, p);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree node, Void p) {
    this.checkMethodReferenceAsOverride(node, p);
    return super.visitMemberReference(node, p);
  }

  /**
   * Checks that the type of the return expression is a subtype of the enclosing method required
   * return type. If not, it issues a "return" error.
   */
  @Override
  public Void visitReturn(ReturnTree node, Void p) {
    // Don't try to check return expressions for void methods.
    if (node.getExpression() == null) {
      return super.visitReturn(node, p);
    }

    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();
    try {

      Tree enclosing =
          TreePathUtil.enclosingOfKind(
              getCurrentPath(),
              new HashSet<>(Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

      AnnotatedTypeMirror ret = null;
      if (enclosing.getKind() == Tree.Kind.METHOD) {

        MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getCurrentPath());
        boolean valid = validateTypeOf(enclosing);
        if (valid) {
          ret = atypeFactory.getMethodReturnType(enclosingMethod, node);
        }
      } else {
        AnnotatedExecutableType result =
            atypeFactory.getFunctionTypeFromTree((LambdaExpressionTree) enclosing);
        ret = result.getReturnType();
      }

      if (ret != null) {
        visitorState.setAssignmentContext(Pair.of((Tree) node, ret));

        commonAssignmentCheck(ret, node.getExpression(), "return");
      }
      return super.visitReturn(node, p);
    } finally {
      visitorState.setAssignmentContext(preAssignmentContext);
    }
  }

  /**
   * Ensure that the annotation arguments comply to their declarations. This needs some special
   * casing, as annotation arguments form special trees.
   */
  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    List<? extends ExpressionTree> args = node.getArguments();
    if (args.isEmpty()) {
      // Nothing to do if there are no annotation arguments.
      return null;
    }

    TypeElement anno = (TypeElement) TreeInfo.symbol((JCTree) node.getAnnotationType());

    Name annoName = anno.getQualifiedName();
    if (annoName.contentEquals(DefaultQualifier.class.getName())
        || annoName.contentEquals(SuppressWarnings.class.getName())) {
      // Skip these two annotations, as we don't care about the arguments to them.
      return null;
    }

    List<ExecutableElement> methods = ElementFilter.methodsIn(anno.getEnclosedElements());
    // Mapping from argument simple name to its annotated type.
    Map<String, AnnotatedTypeMirror> annoTypes = new HashMap<>(methods.size());
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
      Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();

      {
        // Determine and set the new assignment context.
        ExpressionTree var = at.getVariable();
        assert var instanceof IdentifierTree : "Expected IdentifierTree as context. Found: " + var;
        AnnotatedTypeMirror meth = atypeFactory.getAnnotatedType(var);
        assert meth instanceof AnnotatedExecutableType
            : "Expected AnnotatedExecutableType as context. Found: " + meth;
        AnnotatedTypeMirror newctx = ((AnnotatedExecutableType) meth).getReturnType();
        visitorState.setAssignmentContext(Pair.of((Tree) null, newctx));
      }

      try {
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
      } finally {
        visitorState.setAssignmentContext(preAssignmentContext);
      }
    }
    return null;
  }

  /**
   * If the computation of the type of the ConditionalExpressionTree in
   * org.checkerframework.framework.type.TypeFromTree.TypeFromExpression.visitConditionalExpression(ConditionalExpressionTree,
   * AnnotatedTypeFactory) is correct, the following checks are redundant. However, let's add
   * another failsafe guard and do the checks.
   */
  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
    AnnotatedTypeMirror cond = atypeFactory.getAnnotatedType(node);
    this.commonAssignmentCheck(cond, node.getTrueExpression(), "conditional");
    this.commonAssignmentCheck(cond, node.getFalseExpression(), "conditional");
    return super.visitConditionalExpression(node, p);
  }

  // **********************************************************************
  // Check for illegal re-assignment
  // **********************************************************************

  /** Performs assignability check. */
  @Override
  public Void visitUnary(UnaryTree node, Void p) {
    Tree.Kind nodeKind = node.getKind();
    if ((nodeKind == Tree.Kind.PREFIX_DECREMENT)
        || (nodeKind == Tree.Kind.PREFIX_INCREMENT)
        || (nodeKind == Tree.Kind.POSTFIX_DECREMENT)
        || (nodeKind == Tree.Kind.POSTFIX_INCREMENT)) {
      AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(node.getExpression());
      AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedTypeRhsUnaryAssign(node);
      String errorKey =
          (nodeKind == Tree.Kind.PREFIX_INCREMENT || nodeKind == Tree.Kind.POSTFIX_INCREMENT)
              ? "unary.increment"
              : "unary.decrement";
      commonAssignmentCheck(varType, valueType, node, errorKey);
    }
    return super.visitUnary(node, p);
  }

  /** Performs assignability check. */
  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
    // If node is the tree representing the compounds assignment s += expr,
    // Then this method should check whether s + expr can be assigned to s,
    // but the "s + expr" tree does not exist.  So instead, check that
    // s += expr can be assigned to s.
    commonAssignmentCheck(node.getVariable(), node, "compound.assignment");
    return super.visitCompoundAssignment(node, p);
  }

  // **********************************************************************
  // Check for invalid types inserted by the user
  // **********************************************************************

  @Override
  public Void visitNewArray(NewArrayTree node, Void p) {
    boolean valid = validateTypeOf(node);

    if (valid && node.getType() != null) {
      AnnotatedArrayType arrayType = atypeFactory.getAnnotatedType(node);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(arrayType, node);
      if (node.getInitializers() != null) {
        checkArrayInitialization(arrayType.getComponentType(), node.getInitializers());
      }
    }

    return super.visitNewArray(node, p);
  }

  /**
   * If the lint option "cast:redundant" is set, this methods issues a warning if the cast is
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
    boolean calledOnce = false;
    for (AnnotationMirror top : atypeFactory.getQualifierParameterHierarchies(castType)) {
      if (!isInvariantTypeCastSafe(castType, exprType, top)) {
        checker.reportError(
            typeCastTree,
            "invariant.cast.unsafe",
            exprType.toString(true),
            castType.toString(true));
      }
      calledOnce = true; // don't issue cast unsafe warning.
    }
    // We cannot do a simple test of casting, as isSubtypeOf requires
    // the input types to be subtypes according to Java.
    if (!calledOnce && !isTypeCastSafe(castType, exprType)) {
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

    final TypeKind castTypeKind = castType.getKind();
    if (castTypeKind == TypeKind.DECLARED) {
      // Don't issue an error if the annotations are equivalent to the qualifier upper bound of the
      // type.
      AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType) castType;
      Set<AnnotationMirror> bounds =
          atypeFactory.getTypeDeclarationBounds(castDeclared.getUnderlyingType());

      if (AnnotationUtils.areSame(castDeclared.getAnnotations(), bounds)) {
        return true;
      }
    }

    QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();

    Set<AnnotationMirror> castAnnos;
    if (!checker.hasOption("checkCastElementType")) {
      // checkCastElementType option wasn't specified, so only check effective annotations.
      castAnnos = castType.getEffectiveAnnotations();
    } else {
      AnnotatedTypeMirror newCastType;
      if (castTypeKind == TypeKind.TYPEVAR) {
        newCastType = ((AnnotatedTypeVariable) castType).getUpperBound();
      } else {
        newCastType = castType;
      }
      AnnotatedTypeMirror newExprType;
      if (exprType.getKind() == TypeKind.TYPEVAR) {
        newExprType = ((AnnotatedTypeVariable) exprType).getUpperBound();
      } else {
        newExprType = exprType;
      }

      if (!atypeFactory.getTypeHierarchy().isSubtype(newExprType, newCastType)) {
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
          // Always warn if the cast and expression contain a different number of type arguments,
          // e.g. to catch a cast from "Object" to "List<@NonNull Object>".
          // TODO: the same number of arguments actually doesn't guarantee anything.
          return false;
        }
      } else if (castTypeKind == TypeKind.TYPEVAR && exprType.getKind() == TypeKind.TYPEVAR) {
        // If both the cast type and the casted expression are type variables, then check the
        // bounds.
        Set<AnnotationMirror> lowerBoundAnnotationsCast =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, castType);
        Set<AnnotationMirror> lowerBoundAnnotationsExpr =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, exprType);
        return qualifierHierarchy.isSubtype(lowerBoundAnnotationsExpr, lowerBoundAnnotationsCast)
            && qualifierHierarchy.isSubtype(
                exprType.getEffectiveAnnotations(), castType.getEffectiveAnnotations());
      }
      if (castTypeKind == TypeKind.TYPEVAR) {
        // If the cast type is a type var, but the expression is not, then check that the
        // type of the expression is a subtype of the lower bound.
        castAnnos = AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, castType);
      } else {
        castAnnos = castType.getAnnotations();
      }
    }

    AnnotatedTypeMirror exprTypeWidened = atypeFactory.getWidenedType(exprType, castType);
    return qualifierHierarchy.isSubtype(exprTypeWidened.getEffectiveAnnotations(), castAnnos);
  }

  /**
   * Return whether or not casting the exprType to castType is legal.
   *
   * @param castType an invariant type
   * @param exprType type of the expressions that is cast which may or may not be invariant
   * @param top the top qualifier of the hierarchy to check
   * @return whether or not casting the exprType to castType is legal
   */
  private boolean isInvariantTypeCastSafe(
      AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType, AnnotationMirror top) {
    if (!isTypeCastSafe(castType, exprType)) {
      return false;
    }
    AnnotationMirror castTypeAnno = castType.getEffectiveAnnotationInHierarchy(top);
    AnnotationMirror exprTypeAnno = exprType.getEffectiveAnnotationInHierarchy(top);

    if (atypeFactory.hasQualifierParameterInHierarchy(exprType, top)) {
      // The isTypeCastSafe call above checked that the exprType is a subtype of castType,
      // so just check the reverse to check that the qualifiers are equivalent.
      return atypeFactory.getQualifierHierarchy().isSubtype(castTypeAnno, exprTypeAnno);
    }
    // Otherwise the cast is unsafe, unless the qualifiers on both cast and expr are bottom.
    AnnotationMirror bottom = atypeFactory.getQualifierHierarchy().getBottomAnnotation(top);
    return AnnotationUtils.areSame(castTypeAnno, bottom)
        && AnnotationUtils.areSame(exprTypeAnno, bottom);
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, Void p) {
    // validate "node" instead of "node.getType()" to prevent duplicate errors.
    boolean valid = validateTypeOf(node) && validateTypeOf(node.getExpression());
    if (valid) {
      checkTypecastSafety(node);
      checkTypecastRedundancy(node);
    }
    if (atypeFactory.getDependentTypesHelper().hasDependentAnnotations()) {
      AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
      atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(type, node.getType());
    }

    if (node.getType().getKind() == Tree.Kind.INTERSECTION_TYPE) {
      AnnotatedIntersectionType intersection =
          (AnnotatedIntersectionType) atypeFactory.getAnnotatedType(node);
      checkExplicitAnnotationsOnIntersectionBounds(
          intersection, ((IntersectionTypeTree) node.getType()).getBounds());
    }
    return super.visitTypeCast(node, p);
    // return scan(node.getExpression(), p);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, Void p) {
    // The "reference type" is the type after "instanceof".
    Tree refTypeTree = node.getType();
    validateTypeOf(refTypeTree);
    if (refTypeTree.getKind() == Tree.Kind.ANNOTATED_TYPE) {
      AnnotatedTypeMirror refType = atypeFactory.getAnnotatedType(refTypeTree);
      AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(node.getExpression());
      if (!refType.hasAnnotation(NonNull.class)
          && atypeFactory.getTypeHierarchy().isSubtype(refType, expType)
          && !refType.getAnnotations().equals(expType.getAnnotations())) {
        checker.reportWarning(node, "instanceof.unsafe", expType, refType);
      }
    }
    return super.visitInstanceOf(node, p);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree node, Void p) {
    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();
    try {
      visitorState.setAssignmentContext(null);
      scan(node.getExpression(), p);
      scan(node.getIndex(), p);
    } finally {
      visitorState.setAssignmentContext(preAssignmentContext);
    }
    return null;
  }

  /**
   * Checks the type of the exception parameter. Subclasses should override {@link
   * #checkExceptionParameter} rather than this method to change the behavior of this check.
   */
  @Override
  public Void visitCatch(CatchTree node, Void p) {
    checkExceptionParameter(node);
    return super.visitCatch(node, p);
  }

  /**
   * Checks the type of a thrown exception. Subclasses should override
   * checkThrownExpression(ThrowTree node) rather than this method to change the behavior of this
   * check.
   */
  @Override
  public Void visitThrow(ThrowTree node, Void p) {
    checkThrownExpression(node);
    return super.visitThrow(node, p);
  }

  /**
   * Rather than overriding this method, clients should often override {@link
   * #visitAnnotatedType(List,Tree)}. That method also handles the case of annotations at the
   * beginning of a variable or method declaration. javac parses all those annotations as being on
   * the variable or method declaration, even though the ones that are type annotations logically
   * belong to the variable type or method return type.
   */
  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree node, Void p) {
    visitAnnotatedType(null, node);
    return super.visitAnnotatedType(node, p);
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
    if (atypeFactory.relevantJavaTypes == null) {
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
          List<AnnotationTree> supportedAnnoTrees = supportedAnnoTrees(annoTrees);
          if (!supportedAnnoTrees.isEmpty() && !atypeFactory.isRelevant(TreeUtils.typeOf(t))) {
            checker.reportError(t, "anno.on.irrelevant", supportedAnnoTrees, t);
          }
          return;
        case ANNOTATED_TYPE:
          AnnotatedTypeTree at = (AnnotatedTypeTree) t;
          ExpressionTree underlying = at.getUnderlyingType();
          List<AnnotationTree> annos = supportedAnnoTrees(at.getAnnotations());
          if (!annos.isEmpty() && !atypeFactory.isRelevant(TreeUtils.typeOf(underlying))) {
            checker.reportError(t, "anno.on.irrelevant", annos, underlying);
          }
          return;

        default:
          return;
      }
    }
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
  private Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotationsCache = null;
  /** The same as {@link #getExceptionParameterLowerBoundAnnotations}, but uses a cache. */
  private Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotationsCached() {
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
   * @param node CatchTree to check
   */
  protected void checkExceptionParameter(CatchTree node) {

    Set<? extends AnnotationMirror> requiredAnnotations =
        getExceptionParameterLowerBoundAnnotationsCached();
    AnnotatedTypeMirror exPar = atypeFactory.getAnnotatedType(node.getParameter());

    for (AnnotationMirror required : requiredAnnotations) {
      AnnotationMirror found = exPar.getAnnotationInHierarchy(required);
      assert found != null;
      if (!atypeFactory.getQualifierHierarchy().isSubtype(required, found)) {
        checker.reportError(node.getParameter(), "exception.parameter", found, required);
      }

      if (exPar.getKind() == TypeKind.UNION) {
        AnnotatedUnionType aut = (AnnotatedUnionType) exPar;
        for (AnnotatedTypeMirror alterntive : aut.getAlternatives()) {
          AnnotationMirror foundAltern = alterntive.getAnnotationInHierarchy(required);
          if (!atypeFactory.getQualifierHierarchy().isSubtype(required, foundAltern)) {
            checker.reportError(node.getParameter(), "exception.parameter", foundAltern, required);
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
  protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    return atypeFactory.getQualifierHierarchy().getTopAnnotations();
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
   * @param node ThrowTree to check
   */
  protected void checkThrownExpression(ThrowTree node) {
    AnnotatedTypeMirror throwType = atypeFactory.getAnnotatedType(node.getExpression());
    Set<? extends AnnotationMirror> required = getThrowUpperBoundAnnotations();
    switch (throwType.getKind()) {
      case NULL:
      case DECLARED:
        Set<AnnotationMirror> found = throwType.getAnnotations();
        if (!atypeFactory.getQualifierHierarchy().isSubtype(found, required)) {
          checker.reportError(node.getExpression(), "throw", found, required);
        }
        break;
      case TYPEVAR:
      case WILDCARD:
        // TODO: this code might change after the type var changes.
        Set<AnnotationMirror> foundEffective = throwType.getEffectiveAnnotations();
        if (!atypeFactory.getQualifierHierarchy().isSubtype(foundEffective, required)) {
          checker.reportError(node.getExpression(), "throw", foundEffective, required);
        }
        break;
      case UNION:
        AnnotatedUnionType unionType = (AnnotatedUnionType) throwType;
        Set<AnnotationMirror> foundPrimary = unionType.getAnnotations();
        if (!atypeFactory.getQualifierHierarchy().isSubtype(foundPrimary, required)) {
          checker.reportError(node.getExpression(), "throw", foundPrimary, required);
        }
        for (AnnotatedTypeMirror altern : unionType.getAlternatives()) {
          if (!atypeFactory.getQualifierHierarchy().isSubtype(altern.getAnnotations(), required)) {
            checker.reportError(node.getExpression(), "throw", altern.getAnnotations(), required);
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
  protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
    return getExceptionParameterLowerBoundAnnotations();
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varTree the AST node for the lvalue (usually a variable)
   * @param valueExp the AST node for the rvalue (the new value)
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   */
  protected void commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(varTree);
    assert varType != null : "no variable found for tree: " + varTree;

    if (!validateType(varTree, varType)) {
      return;
    }

    commonAssignmentCheck(varType, valueExp, errorKey, extraArgs);
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varType the annotated type for the lvalue (usually a variable)
   * @param valueExp the AST node for the rvalue (the new value)
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   */
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (shouldSkipUses(valueExp)) {
      return;
    }
    if (valueExp.getKind() == Tree.Kind.MEMBER_REFERENCE
        || valueExp.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
      // Member references and lambda expressions are type checked separately
      // and do not need to be checked again as arguments.
      return;
    }
    if (varType.getKind() == TypeKind.ARRAY
        && valueExp instanceof NewArrayTree
        && ((NewArrayTree) valueExp).getType() == null) {
      AnnotatedTypeMirror compType = ((AnnotatedArrayType) varType).getComponentType();
      NewArrayTree arrayTree = (NewArrayTree) valueExp;
      assert arrayTree.getInitializers() != null
          : "array initializers are not expected to be null in: " + valueExp;
      checkArrayInitialization(compType, arrayTree.getInitializers());
    }
    if (!validateTypeOf(valueExp)) {
      return;
    }
    AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueExp);
    assert valueType != null : "null type for expression: " + valueExp;
    commonAssignmentCheck(varType, valueType, valueExp, errorKey, extraArgs);
  }

  /**
   * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
   * emits an error message (through the compiler's messaging interface) if it is not valid.
   *
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueTree the location to use when reporting the error message
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   */
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    commonAssignmentCheckStartDiagnostic(varType, valueType, valueTree);

    AnnotatedTypeMirror widenedValueType = atypeFactory.getWidenedType(valueType, varType);
    boolean success = atypeFactory.getTypeHierarchy().isSubtype(widenedValueType, varType);

    // TODO: integrate with subtype test.
    if (success) {
      for (Class<? extends Annotation> mono : atypeFactory.getSupportedMonotonicTypeQualifiers()) {
        if (valueType.hasAnnotation(mono) && varType.hasAnnotation(mono)) {
          checker.reportError(
              valueTree,
              "monotonic",
              mono.getSimpleName(),
              mono.getSimpleName(),
              valueType.toString());
          return;
        }
      }
    }

    commonAssignmentCheckEndDiagnostic(success, null, varType, valueType, valueTree);

    // Use an error key only if it's overridden by a checker.
    if (!success) {
      FoundRequired pair = FoundRequired.of(valueType, varType);
      String valueTypeString = pair.found;
      String varTypeString = pair.required;
      checker.reportError(
          valueTree, errorKey, ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }
  }

  /**
   * Prints a diagnostic about entering commonAssignmentCheck, if the showchecks option was set.
   *
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckStartDiagnostic(
      AnnotatedTypeMirror varType, AnnotatedTypeMirror valueType, Tree valueTree) {
    if (showchecks) {
      long valuePos = positions.getStartPosition(root, valueTree);
      System.out.printf(
          "%s %s (line %3d): actual tree = %s %s%n     actual: %s %s%n   expected: %s %s%n",
          this.getClass().getSimpleName(),
          "about to test whether actual is a subtype of expected",
          (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
          valueTree.getKind(),
          valueTree,
          valueType.getKind(),
          valueType.toString(),
          varType.getKind(),
          varType.toString());
    }
  }

  /**
   * Prints a diagnostic about exiting commonAssignmentCheck, if the showchecks option was set.
   *
   * @param success whether the check succeeded or failed
   * @param extraMessage information about why the result is what it is; may be null
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckEndDiagnostic(
      boolean success,
      String extraMessage,
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree) {
    if (showchecks) {
      commonAssignmentCheckEndDiagnostic(
          (success
                  ? "success: actual is subtype of expected"
                  : "FAILURE: actual is not subtype of expected")
              + (extraMessage == null ? "" : " because " + extraMessage),
          varType,
          valueType,
          valueTree);
    }
  }

  /**
   * Prints a diagnostic about exiting commonAssignmentCheck, if the showchecks option was set.
   *
   * <p>Most clients should call {@link #commonAssignmentCheckEndDiagnostic(boolean, String,
   * AnnotatedTypeMirror, AnnotatedTypeMirror, Tree)}. The purpose of this method is to permit
   * customizing the message that is printed.
   *
   * @param message the result, plus information about why the result is what it is; may be null
   * @param varType the annotated type of the variable
   * @param valueType the annotated type of the value
   * @param valueTree the location to use when reporting the error message
   */
  protected final void commonAssignmentCheckEndDiagnostic(
      String message, AnnotatedTypeMirror varType, AnnotatedTypeMirror valueType, Tree valueTree) {
    if (showchecks) {
      long valuePos = positions.getStartPosition(root, valueTree);
      System.out.printf(
          " %s (line %3d): actual tree = %s %s%n     actual: %s %s%n   expected: %s %s%n",
          message,
          (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
          valueTree.getKind(),
          valueTree,
          valueType.getKind(),
          valueType.toString(),
          varType.getKind(),
          varType.toString());
    }
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
   * @return true iff neither argumentc contains "@", or there are two annotated types (in either
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
   * A scanner that indicates whether any (sub-)types have the same toString but different verbose
   * toString. If so, the Checker Framework prints types verbosely.
   */
  private static SimpleAnnotatedTypeScanner<Boolean, Map<String, String>>
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

  protected void checkArrayInitialization(
      AnnotatedTypeMirror type, List<? extends ExpressionTree> initializers) {
    // TODO: set assignment context like for method arguments?
    // Also in AbstractFlow.
    for (ExpressionTree init : initializers) {
      commonAssignmentCheck(type, init, "array.initializer");
    }
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

      if (isIgnoredUninferredWildcard(bounds.getUpperBound())
          || isIgnoredUninferredWildcard(typeArg)) {
        continue;
      }

      if (shouldBeCaptureConverted(typeArg, bounds)) {
        continue;
      }

      AnnotatedTypeMirror paramUpperBound = bounds.getUpperBound();
      if (typeArg.getKind() == TypeKind.WILDCARD) {
        paramUpperBound =
            atypeFactory.widenToUpperBound(paramUpperBound, (AnnotatedWildcardType) typeArg);
      }

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

      if (!atypeFactory.getTypeHierarchy().isSubtype(bounds.getLowerBound(), typeArg)) {
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
    for (AnnotationMirror top : atypeFactory.getQualifierHierarchy().getTopAnnotations()) {
      if (atypeFactory.hasQualifierParameterInHierarchy(typeArgument, top)
          && !getTypeFactory().hasQualifierParameterInHierarchy(typeParameterUpperBound, top)) {
        checker.reportError(reportError, "type.argument.hasqualparam", top);
      }
    }
  }

  private boolean isIgnoredUninferredWildcard(AnnotatedTypeMirror type) {
    return atypeFactory.ignoreUninferredTypeArguments
        && type.getKind() == TypeKind.WILDCARD
        && ((AnnotatedWildcardType) type).isUninferredTypeArgument();
  }

  // TODO: REMOVE WHEN CAPTURE CONVERSION IS IMPLEMENTED
  // TODO: This may not occur only in places where capture conversion occurs but in those cases
  // TODO: The containment check provided by this method should be enough
  /**
   * Identifies cases that would not happen if capture conversion were implemented. These special
   * cases should be removed when capture conversion is implemented.
   */
  private boolean shouldBeCaptureConverted(
      final AnnotatedTypeMirror typeArg, final AnnotatedTypeParameterBounds bounds) {
    return typeArg.getKind() == TypeKind.WILDCARD
        && bounds.getUpperBound().getKind() == TypeKind.WILDCARD;
  }

  /**
   * Indicates whether to skip subtype checks on the receiver when checking method invocability. A
   * visitor may, for example, allow a method to be invoked even if the receivers are siblings in a
   * hierarchy, provided that some other condition (implemented by the visitor) is satisfied.
   *
   * @param node the method invocation node
   * @param methodDefinitionReceiver the ATM of the receiver of the method definition
   * @param methodCallReceiver the ATM of the receiver of the method call
   * @return whether to skip subtype checks on the receiver
   */
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree node,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    return false;
  }

  /**
   * Tests whether the method can be invoked using the receiver of the 'node' method invocation, and
   * issues a "method.invocation" if the invocation is invalid.
   *
   * <p>This implementation tests whether the receiver in the method invocation is a subtype of the
   * method receiver type. This behavior can be specialized by overriding skipReceiverSubtypeCheck.
   *
   * @param method the type of the invoked method
   * @param node the method invocation node
   */
  protected void checkMethodInvocability(
      AnnotatedExecutableType method, MethodInvocationTree node) {
    if (method.getReceiverType() == null) {
      // Static methods don't have a receiver to check.
      return;
    }
    if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
      // TODO: Explicit "this()" calls of constructors have an implicit passed
      // from the enclosing constructor. We must not use the self type, but
      // instead should find a way to determine the receiver of the enclosing constructor.
      // rcv =
      // ((AnnotatedExecutableType)atypeFactory.getAnnotatedType(atypeFactory.getEnclosingMethod(node))).getReceiverType();
      return;
    }

    AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
    AnnotatedTypeMirror treeReceiver = methodReceiver.shallowCopy(false);
    AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);

    treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());

    if (!skipReceiverSubtypeCheck(node, methodReceiver, rcv)) {
      // The diagnostic can be a bit misleading because the check is of the receiver but `node` is
      // the entire method invocation (where the receiver might be implicit).
      commonAssignmentCheckStartDiagnostic(methodReceiver, treeReceiver, node);
      boolean success = atypeFactory.getTypeHierarchy().isSubtype(treeReceiver, methodReceiver);
      commonAssignmentCheckEndDiagnostic(success, null, methodReceiver, treeReceiver, node);
      if (!success) {
        reportMethodInvocabilityError(node, treeReceiver, methodReceiver);
      }
    }
  }

  /**
   * Report a method invocability error. Allows checkers to change how the message is output.
   *
   * @param node the AST node at which to report the error
   * @param found the actual type of the receiver
   * @param expected the expected type of the receiver
   */
  protected void reportMethodInvocabilityError(
      MethodInvocationTree node, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {
    checker.reportError(
        node,
        "method.invocation",
        TreeUtils.elementFromUse(node),
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
    Set<AnnotationMirror> explicitAnnos = atypeFactory.fromNewClass(newClassTree).getAnnotations();
    if (explicitAnnos.isEmpty()) {
      return;
    }
    Set<AnnotationMirror> resultAnnos = constructor.getReturnType().getAnnotations();
    for (AnnotationMirror explicit : explicitAnnos) {
      AnnotationMirror resultAnno =
          atypeFactory.getQualifierHierarchy().findAnnotationInSameHierarchy(resultAnnos, explicit);
      // The return type of the constructor (resultAnnos) must be comparable to the
      // annotations on the constructor invocation (explicitAnnos).
      if (!(atypeFactory.getQualifierHierarchy().isSubtype(explicit, resultAnno)
          || atypeFactory.getQualifierHierarchy().isSubtype(resultAnno, explicit))) {
        checker.reportError(
            newClassTree, "constructor.invocation", constructor.toString(), explicit, resultAnno);
        return;
      } else if (!atypeFactory.getQualifierHierarchy().isSubtype(resultAnno, explicit)) {
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
   * argument, and issues "argument" error for each passed argument that not a subtype of the
   * required one.
   *
   * <p>Note this method requires the lists to have the same length, as it does not handle cases
   * like var args.
   *
   * @see #checkVarargs(AnnotatedTypeMirror.AnnotatedExecutableType, Tree)
   * @param requiredArgs the required types. This may differ from the formal parameter types,
   *     because it replaces a varargs parameter by multiple parameters with the vararg's element
   *     type.
   * @param passedArgs the expressions passed to the corresponding types
   * @param executableName the name of the method or constructor being called
   * @param paramNames the names of the callee's formal parameters
   */
  protected void checkArguments(
      List<? extends AnnotatedTypeMirror> requiredArgs,
      List<? extends ExpressionTree> passedArgs,
      CharSequence executableName,
      List<?> paramNames) {
    int size = requiredArgs.size();
    assert size == passedArgs.size()
        : "mismatch between required args ("
            + requiredArgs
            + ") and passed args ("
            + passedArgs
            + ")";
    int maxParamNamesIndex = paramNames.size() - 1;
    // Rather weak assertion, due to how varargs parameters are treated.
    assert size >= maxParamNamesIndex
        : String.format(
            "mismatched lengths %d %d %d checkArguments(%s, %s, %s, %s)",
            size,
            passedArgs.size(),
            paramNames.size(),
            listToString(requiredArgs),
            listToString(passedArgs),
            executableName,
            listToString(paramNames));

    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = visitorState.getAssignmentContext();
    try {
      for (int i = 0; i < size; ++i) {
        visitorState.setAssignmentContext(
            Pair.of((Tree) null, (AnnotatedTypeMirror) requiredArgs.get(i)));
        commonAssignmentCheck(
            requiredArgs.get(i),
            passedArgs.get(i),
            "argument",
            // TODO: for expanded varargs parameters, maybe adjust the name
            paramNames.get(Math.min(i, maxParamNamesIndex)),
            executableName);
        // Also descend into the argument within the correct assignment context.
        scan(passedArgs.get(i), null);
      }
    } finally {
      visitorState.setAssignmentContext(preAssignmentContext);
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
  protected boolean testTypevarContainment(
      final AnnotatedTypeMirror inner, final AnnotatedTypeMirror outer) {
    if (inner.getKind() == TypeKind.TYPEVAR && outer.getKind() == TypeKind.TYPEVAR) {

      final AnnotatedTypeVariable innerAtv = (AnnotatedTypeVariable) inner;
      final AnnotatedTypeVariable outerAtv = (AnnotatedTypeVariable) outer;

      if (AnnotatedTypes.areCorrespondingTypeVariables(elements, innerAtv, outerAtv)) {
        final TypeHierarchy typeHierarchy = atypeFactory.getTypeHierarchy();
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
   * created by {@link #createOverrideChecker}. This version of the method exposes
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
   * Check that a method reference is allowed. Using the OverrideChecker class.
   *
   * @param memberReferenceTree the tree for the method reference
   * @return true if the method reference is allowed
   */
  protected boolean checkMethodReferenceAsOverride(
      MemberReferenceTree memberReferenceTree, Void p) {

    Pair<AnnotatedTypeMirror, AnnotatedExecutableType> result =
        atypeFactory.getFnInterfaceFromTree(memberReferenceTree);
    // The type to which the member reference is assigned -- also known as the target type of the
    // reference.
    AnnotatedTypeMirror functionalInterface = result.first;
    // The type of the single method that is declared by the functional interface.
    AnnotatedExecutableType functionType = result.second;

    // ========= Overriding Type =========
    // This doesn't get the correct type for a "MyOuter.super" based on the receiver of the
    // enclosing method.
    // That is handled separately in method receiver check.

    // The type of the expression or type use, <expression>::method or <type use>::method.
    final ExpressionTree qualifierExpression = memberReferenceTree.getQualifierExpression();
    final ReferenceKind memRefKind = ((JCMemberReference) memberReferenceTree).kind;
    AnnotatedTypeMirror enclosingType;
    if (memberReferenceTree.getMode() == ReferenceMode.NEW
        || memRefKind == ReferenceKind.UNBOUND
        || memRefKind == ReferenceKind.STATIC) {
      // The "qualifier expression" is a type tree.
      enclosingType = atypeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
    } else {
      // The "qualifier expression" is an expression.
      enclosingType = atypeFactory.getAnnotatedType(qualifierExpression);
    }

    // ========= Overriding Executable =========
    // The ::method element, see JLS 15.13.1 Compile-Time Declaration of a Method Reference
    ExecutableElement compileTimeDeclaration =
        (ExecutableElement) TreeUtils.elementFromTree(memberReferenceTree);

    if (enclosingType.getKind() == TypeKind.DECLARED
        && ((AnnotatedDeclaredType) enclosingType).isUnderlyingTypeRaw()) {
      if (memRefKind == ReferenceKind.UNBOUND) {
        // The method reference is of the form: Type # instMethod and Type is a raw type.
        // If the first parameter of the function type, p1, is a subtype of type, then type should
        // be p1.  This has the effect of "inferring" the class type parameter.
        AnnotatedTypeMirror p1 = functionType.getParameterTypes().get(0);
        TypeMirror asSuper =
            TypesUtils.asSuper(
                p1.getUnderlyingType(),
                enclosingType.getUnderlyingType(),
                atypeFactory.getProcessingEnv());
        if (asSuper != null) {
          enclosingType = AnnotatedTypes.asSuper(atypeFactory, p1, enclosingType);
        }
      }
      // else method reference is something like ArrayList::new
      // TODO: Use diamond, <>, inference to infer the class type arguments.
      // For now this case is skipped below in checkMethodReferenceInference.
    }

    // The type of the compileTimeDeclaration if it were invoked with a receiver expression
    // of type {@code type}
    AnnotatedExecutableType invocationType =
        atypeFactory.methodFromUse(memberReferenceTree, compileTimeDeclaration, enclosingType)
            .executableType;

    if (checkMethodReferenceInference(memberReferenceTree, invocationType, enclosingType)) {
      // Type argument inference is required, skip check.
      // #checkMethodReferenceInference issued a warning.
      return true;
    }

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
      // If the functional interface return type is void, the overriding return type doesn't matter.
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
      // If the functionalInterface is not a declared type, it must be an uninferred wildcard.
      // In that case, only return false if uninferred type arguments should not be ignored.
      return !atypeFactory.ignoreUninferredTypeArguments;
    }
  }

  /** Check if method reference type argument inference is required. Issue an error if it is. */
  private boolean checkMethodReferenceInference(
      MemberReferenceTree memberReferenceTree,
      AnnotatedExecutableType invocationType,
      AnnotatedTypeMirror type) {
    // TODO: Issue #802
    // TODO: https://github.com/typetools/checker-framework/issues/802
    // TODO: Method type argument inference
    // TODO: Enable checks for method reference with inferred type arguments.
    // For now, error on mismatch of class or method type arguments.
    boolean requiresInference = false;
    // If the function to which the member reference refers is generic, but the member
    // reference does not provide method type arguments, then Java 8 inference is required.
    // Issue 979.
    if (!invocationType.getTypeVariables().isEmpty()
        && (memberReferenceTree.getTypeArguments() == null
            || memberReferenceTree.getTypeArguments().isEmpty())) {
      // Method type args
      requiresInference = true;
    } else if (memberReferenceTree.getMode() == ReferenceMode.NEW) {
      if (type.getKind() == TypeKind.DECLARED
          && ((AnnotatedDeclaredType) type).isUnderlyingTypeRaw()) {
        // Class type args
        requiresInference = true;
      }
    }
    if (requiresInference) {
      if (checker.hasOption("conservativeUninferredTypeArguments")) {
        checker.reportWarning(memberReferenceTree, "methodref.inference.unimplemented");
      }
      return true;
    }
    return false;
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

    /** The declaration of an overriding method. */
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
      this.overridden = overridden;
      this.overriddenType = overriddenType;
      this.overriddenReturnType = overriddenReturnType;
      this.overriderReturnType = overriderReturnType;

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

    /** Checks that overrides obey behavioral subtyping. */
    private void checkPreAndPostConditions() {
      String msgKey = isMethodReference ? "methodref" : "override";
      if (isMethodReference) {
        // TODO: Support postconditions and method references.
        // The parse context always expects instance methods, but method references can be static.
        return;
      }

      ContractsFromMethod contractsUtils = atypeFactory.getContractsFromMethod();

      // Check preconditions
      Set<Precondition> superPre = contractsUtils.getPreconditions(overridden.getElement());
      Set<Precondition> subPre = contractsUtils.getPreconditions(overrider.getElement());
      Set<Pair<JavaExpression, AnnotationMirror>> superPre2 =
          parseAndLocalizeContracts(superPre, overridden);
      Set<Pair<JavaExpression, AnnotationMirror>> subPre2 =
          parseAndLocalizeContracts(subPre, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String premsg = "contracts.precondition." + msgKey;
      checkContractsSubset(overriderType, overriddenType, subPre2, superPre2, premsg);

      // Check postconditions
      Set<Postcondition> superPost = contractsUtils.getPostconditions(overridden.getElement());
      Set<Postcondition> subPost = contractsUtils.getPostconditions(overrider.getElement());
      Set<Pair<JavaExpression, AnnotationMirror>> superPost2 =
          parseAndLocalizeContracts(superPost, overridden);
      Set<Pair<JavaExpression, AnnotationMirror>> subPost2 =
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
      Set<Pair<JavaExpression, AnnotationMirror>> superCPostTrue2 =
          parseAndLocalizeContracts(superCPostTrue, overridden);
      Set<Pair<JavaExpression, AnnotationMirror>> subCPostTrue2 =
          parseAndLocalizeContracts(subCPostTrue, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String posttruemsg = "contracts.conditional.postcondition.true." + msgKey;
      checkContractsSubset(
          overriderType, overriddenType, superCPostTrue2, subCPostTrue2, posttruemsg);

      // consider only 'false' postconditions
      Set<Postcondition> superCPostFalse = filterConditionalPostconditions(superCPost, false);
      Set<Postcondition> subCPostFalse = filterConditionalPostconditions(subCPost, false);
      Set<Pair<JavaExpression, AnnotationMirror>> superCPostFalse2 =
          parseAndLocalizeContracts(superCPostFalse, overridden);
      Set<Pair<JavaExpression, AnnotationMirror>> subCPostFalse2 =
          parseAndLocalizeContracts(subCPostFalse, overrider);
      @SuppressWarnings("compilermessages")
      @CompilerMessageKey String postfalsemsg = "contracts.conditional.postcondition.false." + msgKey;
      checkContractsSubset(
          overriderType, overriddenType, superCPostFalse2, subCPostFalse2, postfalsemsg);
    }

    private boolean checkMemberReferenceReceivers() {
      JCTree.JCMemberReference memberTree = (JCTree.JCMemberReference) overriderTree;

      if (overriderType.getKind() == TypeKind.ARRAY) {
        // Assume the receiver for all method on arrays are @Top
        // This simplifies some logic because an AnnotatedExecutableType for an array method
        // (ie String[]::clone) has a receiver of "Array." The UNBOUND check would then
        // have to compare "Array" to "String[]".
        return true;
      }

      // These act like a traditional override
      if (memberTree.kind == JCTree.JCMemberReference.ReferenceKind.UNBOUND) {
        AnnotatedTypeMirror overriderReceiver = overrider.getReceiverType();
        AnnotatedTypeMirror overriddenReceiver = overridden.getParameterTypes().get(0);
        boolean success =
            atypeFactory.getTypeHierarchy().isSubtype(overriddenReceiver, overriderReceiver);
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
      switch (memberTree.kind) {
        case UNBOUND:
          throw new BugInCF("Case UNBOUND should already be handled.");
        case SUPER:
          receiverDecl = overrider.getReceiverType();
          receiverArg = atypeFactory.getAnnotatedType(memberTree.getQualifierExpression());

          final AnnotatedTypeMirror selfType = atypeFactory.getSelfType(memberTree);
          receiverArg.replaceAnnotations(selfType.getAnnotations());
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

      boolean success = atypeFactory.getTypeHierarchy().isSubtype(receiverArg, receiverDecl);
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
      QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
      // Check the receiver type.
      // isSubtype() requires its arguments to be actual subtypes with respect to JLS, but overrider
      // receiver is not a subtype of the overridden receiver.  So, just check primary annotations.
      // TODO: this will need to be improved for generic receivers.
      Set<AnnotationMirror> overriderAnnos = overriderReceiver.getAnnotations();
      Set<AnnotationMirror> overriddenAnnos = overriddenReceiver.getAnnotations();
      if (!qualifierHierarchy.isSubtype(overriddenAnnos, overriderAnnos)) {
        Set<AnnotationMirror> declaredAnnos =
            atypeFactory.getTypeDeclarationBounds(overriderType.getUnderlyingType());
        if (qualifierHierarchy.isSubtype(overriderAnnos, declaredAnnos)
            && qualifierHierarchy.isSubtype(declaredAnnos, overriderAnnos)) {
          // All the type of an object must be no higher than its upper bound. So if the receiver is
          // annotated with the upper bound qualifiers, then the override is safe.
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
      // See https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.13.1
      if (isMethodReference) {
        // The functional interface of an unbound member reference has an extra parameter
        // (the receiver).
        if (((JCTree.JCMemberReference) overriderTree)
            .hasKind(JCTree.JCMemberReference.ReferenceKind.UNBOUND)) {
          overriddenParams = new ArrayList<>(overriddenParams);
          overriddenParams.remove(0);
        }
        // Deal with varargs
        if (overrider.isVarArgs() && !overridden.isVarArgs()) {
          overriderParams =
              AnnotatedTypes.expandVarArgsParametersFromTypes(overrider, overriddenParams);
        }
      }

      boolean result = true;
      for (int i = 0; i < overriderParams.size(); ++i) {
        boolean success =
            atypeFactory
                .getTypeHierarchy()
                .isSubtype(overriddenParams.get(i), overriderParams.get(i));
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
      long valuePos =
          overriderTree instanceof MethodTree
              ? positions.getStartPosition(
                  root, ((MethodTree) overriderTree).getParameters().get(index))
              : positions.getStartPosition(root, overriderTree);
      Tree posTree =
          overriderTree instanceof MethodTree
              ? ((MethodTree) overriderTree).getParameters().get(index)
              : overriderTree;

      if (showchecks) {
        System.out.printf(
            " %s (line %3d):%n"
                + "     overrider: %s %s (parameter %d type %s)%n"
                + "   overridden: %s %s"
                + " (parameter %d type %s)%n",
            (success
                ? "success: overridden parameter type is subtype of overriding"
                : "FAILURE: overridden parameter type is not subtype of overriding"),
            (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
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
      final TypeHierarchy typeHierarchy = atypeFactory.getTypeHierarchy();
      boolean success = typeHierarchy.isSubtype(overriderReturnType, overriddenReturnType);
      if (!success) {
        // If both the overridden method have type variables as return types and both
        // types were defined in their respective methods then, they can be covariant or
        // invariant use super/subtypes for the overrides locations
        success = testTypevarContainment(overriderReturnType, overriddenReturnType);
      }

      // Sometimes the overridden return type of a method reference becomes a captured
      // type.  This leads to defaulting that often makes the overriding return type
      // invalid.  We ignore these.  This happens in Issue403/Issue404.
      if (!success
          && isMethodReference
          && TypesUtils.isCaptured(overriddenReturnType.getUnderlyingType())) {
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
      long valuePos =
          overriderTree instanceof MethodTree
              ? positions.getStartPosition(root, ((MethodTree) overriderTree).getReturnType())
              : positions.getStartPosition(root, overriderTree);
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
            " %s (line %3d):%n"
                + "     overrider: %s %s (return type %s)%n"
                + "   overridden: %s %s (return type %s)%n",
            (success
                ? "success: overriding return type is subtype of overridden"
                : "FAILURE: overriding return type is not subtype of overridden"),
            (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
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

    Set<Postcondition> result = new LinkedHashSet<>(conditionalPostconditions.size());
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
   * <p>This uses field {@link #visitorState} to determine where to issue an error message.
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
      Set<Pair<JavaExpression, AnnotationMirror>> mustSubset,
      Set<Pair<JavaExpression, AnnotationMirror>> set,
      @CompilerMessageKey String messageKey) {
    for (Pair<JavaExpression, AnnotationMirror> weak : mustSubset) {
      boolean found = false;

      for (Pair<JavaExpression, AnnotationMirror> strong : set) {
        // are we looking at a contract of the same receiver?
        if (weak.first.equals(strong.first)) {
          // check subtyping relationship of annotations
          QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
          if (qualifierHierarchy.isSubtype(strong.second, weak.second)) {
            found = true;
            break;
          }
        }
      }

      if (!found) {
        MethodTree methodDeclTree = visitorState.getMethodTree();

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
        for (Pair<JavaExpression, AnnotationMirror> strong : set) {
          if (weak.first.equals(strong.first)) {
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
            methodDeclTree,
            messageKey,
            weak.first,
            methodDeclTree.getName(),
            overriddenTypeString,
            overriddenAnno,
            overriderTypeString,
            overriderAnno);
      }
    }
  }

  /**
   * Localizes some contracts -- that is, viewpoint-adapts them to some method body, according to
   * the value of {@link #visitorState}.
   *
   * <p>The input is a set of {@link Contract}s, each of which contains an expression string and an
   * annotation. In a {@link Contract}, Java expressions are exactly as written in source code, not
   * standardized or viewpoint-adapted.
   *
   * <p>The output is a set of pairs of {@link JavaExpression} (parsed expression string) and
   * standardized annotation (with respect to the path of {@link #visitorState}. This method
   * discards any contract whose expression cannot be parsed into a JavaExpression.
   *
   * @param contractSet a set of contracts
   * @param methodType the type of the method that the contracts are for
   * @return pairs of (expression, AnnotationMirror), which are localized contracts
   */
  private Set<Pair<JavaExpression, AnnotationMirror>> parseAndLocalizeContracts(
      Set<? extends Contract> contractSet, AnnotatedExecutableType methodType) {
    if (contractSet.isEmpty()) {
      return Collections.emptySet();
    }

    // This is the path to a place where the contract is being used, which might or might not be
    // where the contract was defined.  For example, methodTree might be an overriding
    // definition, and the contract might be for a superclass.
    MethodTree methodTree = visitorState.getMethodTree();

    StringToJavaExpression stringToJavaExpr =
        expression -> {
          JavaExpression javaExpr =
              StringToJavaExpression.atMethodDecl(expression, methodType.getElement(), checker);
          // methodType.getElement() is not necessarily the same method as methodTree, so
          // viewpoint-adapt it to methodTree.
          return javaExpr.atMethodBody(methodTree);
        };

    Set<Pair<JavaExpression, AnnotationMirror>> result = new HashSet<>(contractSet.size());
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
      result.add(Pair.of(exprJe, annotation));
    }
    return result;
  }

  /**
   * Call this only when the current path is an identifier.
   *
   * @return the enclosing member select, or null if the identifier is not the field in a member
   *     selection
   */
  protected MemberSelectTree enclosingMemberSelect() {
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
  protected Tree enclosingStatement(@FindDistinct Tree tree) {
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
  public Void visitIdentifier(IdentifierTree node, Void p) {
    checkAccess(node, p);
    return super.visitIdentifier(node, p);
  }

  protected void checkAccess(IdentifierTree node, Void p) {
    MemberSelectTree memberSel = enclosingMemberSelect();
    ExpressionTree tree;
    Element elem;

    if (memberSel == null) {
      tree = node;
      elem = TreeUtils.elementFromUse(node);
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
      Element field, AnnotatedTypeMirror receiverType, @FindDistinct ExpressionTree accessTree) {
    AnnotationMirror unused = atypeFactory.getDeclAnnotation(field, Unused.class);
    if (unused == null) {
      return;
    }

    String when = AnnotationUtils.getElementValueClassName(unused, unusedWhenElement).toString();

    // TODO: Don't just look at the receiver type, but at the declaration annotations on the
    // receiver.  (That will enable handling type annotations that are not part of the type
    // system being checked.)

    // TODO: This requires exactly the same type qualifier, but it should permit subqualifiers.
    if (!AnnotationUtils.containsSameByName(receiverType.getAnnotations(), when)) {
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
    Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
    Set<AnnotationMirror> upperBounds =
        atypeFactory
            .getQualifierUpperBounds()
            .getBoundQualifiers(declarationType.getUnderlyingType());
    for (AnnotationMirror top : tops) {
      AnnotationMirror upperBound =
          atypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(upperBounds, top);
      AnnotationMirror qualifier = useType.getAnnotationInHierarchy(top);
      if (!atypeFactory.getQualifierHierarchy().isSubtype(qualifier, upperBound)) {
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
    Set<AnnotationMirror> bounds = atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
    return atypeFactory.getQualifierHierarchy().isSubtype(type.getAnnotations(), bounds);
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
    Set<AnnotationMirror> bounds = atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());
    return atypeFactory.getQualifierHierarchy().isSubtype(type.getAnnotations(), bounds);
  }

  /**
   * Tests whether the tree expressed by the passed type tree is a valid type, and emits an error if
   * that is not the case (e.g. '@Mutable String'). If the tree is a method or constructor, check
   * the return type.
   *
   * @param tree the AST type supplied by the user
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
   * is not the case (e.g. '@Mutable String'). If the tree is a method or constructor, check the
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
   * qualified name matches @{link checker.skipUses} expression.
   *
   * @param exprTree any expression tree
   * @return true if checker should not test exprTree
   */
  protected final boolean shouldSkipUses(ExpressionTree exprTree) {
    // System.out.printf("shouldSkipUses: %s: %s%n", exprTree.getClass(), exprTree);

    Element elm = TreeUtils.elementFromTree(exprTree);
    return checker.shouldSkipUses(elm);
  }

  // **********************************************************************
  // Overriding to avoid visit part of the tree
  // **********************************************************************

  /** Override Compilation Unit so we won't visit package names or imports. */
  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
    Void r = scan(node.getPackageAnnotations(), p);
    // r = reduce(scan(node.getPackageName(), p), r);
    // r = reduce(scan(node.getImports(), p), r);
    r = reduce(scan(node.getTypeDecls(), p), r);
    return r;
  }
}
