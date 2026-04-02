package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Must Call Checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
 */
public class MustCallVisitor extends BaseTypeVisitor<MustCallAnnotatedTypeFactory> {

  /** True if -AnoLightweightOwnership was passed on the command line. */
  private final boolean noLightweightOwnership;

  /**
   * Creates a new MustCallVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public MustCallVisitor(BaseTypeChecker checker) {
    super(checker);
    noLightweightOwnership = checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP);
  }

  @Override
  public Void visitReturn(ReturnTree tree, Void p) {
    // Only check return types if ownership is being transferred.
    if (!noLightweightOwnership) {
      MethodTree enclosingMethod = TreePathUtil.enclosingMethod(this.getCurrentPath());
      // enclosingMethod is null if this return site is inside a lambda. TODO: handle lambdas
      // more precisely?
      if (enclosingMethod != null) {
        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
        AnnotationMirror notOwningAnno = atypeFactory.getDeclAnnotation(methodElt, NotOwning.class);
        if (notOwningAnno != null) {
          // Skip return type subtyping check, because not-owning pointer means Object
          // Construction Checker won't check anyway.
          return null;
        }
      }
    }
    return super.visitReturn(tree, p);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    // This code implements the following rule:
    //  * It is always safe to assign a MustCallAlias parameter of a constructor
    //    to an owning field of the enclosing class.
    // It is necessary to special case this because MustCallAlias is translated
    // into @PolyMustCall, so the common assignment check will fail when assigning
    // an @MustCallAlias parameter to an owning field: the parameter is polymorphic,
    // but the field is not.
    ExpressionTree lhs = tree.getVariable();
    ExpressionTree rhs = tree.getExpression();
    Element lhsElt = TreeUtils.elementFromTree(lhs);
    Element rhsElt = TreeUtils.elementFromTree(rhs);
    if (lhsElt != null && rhsElt != null) {
      // Note that it is not necessary to check that the assignment is to a field of this,
      // because that is implied by the other conditions:
      // * if the field is final, then the only place it can be assigned to is in the
      //   constructor of the proper object (enforced by javac).
      // * if the field is not final, then it cannot be assigned to in a constructor at all:
      //   the @CreatesMustCallFor annotation cannot be written on a constructor (it has
      //   @Target({ElementType.METHOD})), so this code relies on the standard rules for
      //   non-final owning field reassignment, which prevent it without an
      //   @CreatesMustCallFor annotation except in the constructor of the object containing
      //   the field.
      boolean lhsIsOwningField =
          lhs instanceof MemberSelectTree
              && atypeFactory.getDeclAnnotation(lhsElt, Owning.class) != null;
      boolean rhsIsMCA =
          atypeFactory.containsSameByClass(rhsElt.getAnnotationMirrors(), MustCallAlias.class);
      boolean rhsIsConstructorParam =
          rhsElt.getKind() == ElementKind.PARAMETER
              && rhsElt.getEnclosingElement().getKind() == ElementKind.CONSTRUCTOR;
      if (lhsIsOwningField && rhsIsMCA && rhsIsConstructorParam) {
        // Do not execute common assignment check.
        return null;
      }
    }

    return super.visitAssignment(tree, p);
  }

  /** An empty string list. */
  private static final List<String> emptyStringList = Collections.emptyList();

  @Override
  protected boolean validateType(Tree tree, AnnotatedTypeMirror type) {
    if (TreeUtils.isClassTree(tree)) {
      TypeElement classEle = TreeUtils.elementFromDeclaration((ClassTree) tree);
      // If no @InheritableMustCall annotation is written here, `getDeclAnnotation()` gets one
      // from stub files and supertypes.
      AnnotationMirror anyInheritableMustCall =
          atypeFactory.getDeclAnnotation(classEle, InheritableMustCall.class);
      // An @InheritableMustCall annotation that is directly present.
      AnnotationMirror directInheritableMustCall =
          AnnotationUtils.getAnnotationByClass(
              classEle.getAnnotationMirrors(), InheritableMustCall.class);
      if (anyInheritableMustCall == null) {
        if (!ElementUtils.isFinal(classEle)) {
          // There is no @InheritableMustCall annotation on this or any superclass and
          // this is a non-final class.
          // If an explicit @MustCall annotation is present, issue a warning suggesting
          // that @InheritableMustCall is probably what the programmer means, for
          // usability.
          if (atypeFactory.getDeclAnnotation(classEle, MustCall.class) != null) {
            checker.reportWarning(
                tree, "mustcall.not.inheritable", ElementUtils.getQualifiedName(classEle));
          }
        }
      } else {
        // There is an @InheritableMustCall annotation on this, on a superclass, or in an
        // annotation file.
        // There are two possible problems:
        //  1. There is an inconsistent @MustCall on this.
        //  2. There is an explicit @InheritableMustCall here, and it is inconsistent with
        //     an @InheritableMustCall annotation on a supertype.

        // Check for problem 1.
        AnnotationMirror explicitMustCall =
            atypeFactory.fromElement(classEle).getPrimaryAnnotation();
        if (explicitMustCall != null) {
          // There is a @MustCall annotation here.

          List<String> inheritableMustCallVal =
              AnnotationUtils.getElementValueArray(
                  anyInheritableMustCall,
                  atypeFactory.inheritableMustCallValueElement,
                  String.class,
                  emptyStringList);
          AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritableMustCallVal);

          // Issue an error if there is an inconsistent, user-written @MustCall annotation
          // here.
          AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();
          TypeMirror tm = type.getUnderlyingType();
          if (effectiveMCAnno != null
              && !qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

            checker.reportError(
                tree,
                "inconsistent.mustcall.subtype",
                ElementUtils.getQualifiedName(classEle),
                effectiveMCAnno,
                anyInheritableMustCall);
            return false;
          }
        }

        // Check for problem 2.
        if (directInheritableMustCall != null) {

          // `inheritedImcs` is inherited @InheritableMustCall annotations.
          List<AnnotationMirror> inheritedImcs = new ArrayList<>();
          for (TypeElement elt : ElementUtils.getDirectSuperTypeElements(classEle, elements)) {
            AnnotationMirror imc = atypeFactory.getDeclAnnotation(elt, InheritableMustCall.class);
            if (imc != null) {
              inheritedImcs.add(imc);
            }
          }
          if (!inheritedImcs.isEmpty()) {
            // There is an inherited @InheritableMustCall annotation, in addition to the
            // one written explicitly here.
            List<String> inheritedMustCallVal = new ArrayList<>();
            for (AnnotationMirror inheritedImc : inheritedImcs) {
              inheritedMustCallVal.addAll(
                  AnnotationUtils.getElementValueArray(
                      inheritedImc, atypeFactory.inheritableMustCallValueElement, String.class));
            }
            AnnotationMirror inheritedMCAnno = atypeFactory.createMustCall(inheritedMustCallVal);

            AnnotationMirror effectiveMCAnno = type.getPrimaryAnnotation();

            TypeMirror tm = type.getUnderlyingType();

            if (!qualHierarchy.isSubtypeShallow(inheritedMCAnno, effectiveMCAnno, tm)) {

              checker.reportError(
                  tree,
                  "inconsistent.mustcall.subtype",
                  ElementUtils.getQualifiedName(classEle),
                  effectiveMCAnno,
                  inheritedMCAnno);
              return false;
            }
          }
        }
      }
    }
    return super.validateType(tree, type);
  }

  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // MustCallAlias annotations are always permitted on type uses, despite not technically
    // being a part of the type hierarchy. It's necessary to get the annotation from the element
    // because MustCallAlias is aliased to PolyMustCall, which is what useType would contain.
    // Note that isValidUse does not need to consider component types, on which it should be
    // called separately.
    Element elt = TreeUtils.elementFromTree(tree);
    if (elt != null) {
      if (AnnotationUtils.containsSameByClass(elt.getAnnotationMirrors(), MustCallAlias.class)) {
        return true;
      }
      // Need to check the type mirror for ajava-derived annotations and the element itself
      // for human-written annotations from the source code. Getting to the ajava file
      // directly at this point is impossible, so we approximate "the ajava file has an
      // @MustCallAlias annotation" with "there is an @PolyMustCall annotation on the use
      // type, but not in the source code". This only works because none of our inference
      // techniques infer @PolyMustCall, so if @PolyMustCall is present but wasn't in the
      // source, it must have been derived from an @MustCallAlias annotation (which we do
      // infer).
      boolean ajavaFileHasMustCallAlias =
          useType.hasPrimaryAnnotation(PolyMustCall.class)
              && !atypeFactory.containsSameByClass(elt.getAnnotationMirrors(), PolyMustCall.class);
      if (ajavaFileHasMustCallAlias) {
        return true;
      }
    }
    return super.isValidUse(declarationType, useType, tree);
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree tree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    // If you think of the receiver of the method call as an implicit parameter, it has some
    // MustCall type. For example, consider the method call:
    //   void foo(@MustCall("bar") ThisClass this)
    // If we now call o.foo() where o has @MustCall({"bar, baz"}), the receiver subtype check
    // would throw an error, since o is not a subtype of @MustCall("bar"). However, since foo
    // cannot take ownership of its receiver, it does not matter what it 'thinks' the @MustCall
    // methods of the receiver are. Hence, it is always sound to skip this check.
    return true;
  }

  /**
   * This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The Must Call Checker
   * does not need this warning, because it expects the type of all constructors to be {@code
   * MustCall({})} (by default) or some other {@code MustCall} type, not the top type.
   *
   * <p>Instead, this method checks that the result type of a constructor is a supertype of the
   * declared type on the class, if one exists.
   *
   * @param constructorType an AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror defaultType =
        atypeFactory.getAnnotatedType(ElementUtils.enclosingTypeElement(constructorElement));
    AnnotationMirror defaultAnno = defaultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    AnnotatedTypeMirror resultType = constructorType.getReturnType();
    AnnotationMirror resultAnno = resultType.getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
    if (!qualHierarchy.isSubtypeShallow(
        defaultAnno, defaultType.getUnderlyingType(), resultAnno, resultType.getUnderlyingType())) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives. This is unsound; see the discussion on
   * https://github.com/typetools/checker-framework/issues/3839.
   *
   * <p>TODO: change checking of throws clauses to require that the thrown exception
   * is @MustCall({}). This would probably eliminate most of the same false positives, without
   * adding undue false positives.
   *
   * @return a set containing only the @MustCall({}) annotation
   */
  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(atypeFactory.BOTTOM);
  }

  /**
   * Does not issue any warnings.
   *
   * <p>This implementation prevents recursing into annotation arguments. Annotation arguments are
   * literals, which don't have must-call obligations.
   *
   * <p>Annotation arguments are treated as return locations for the purposes of defaulting, rather
   * than parameter locations. This causes them to default incorrectly when the annotation is
   * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
   * explanation of why this is necessary to avoid false positives.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    return null;
  }

  /**
   * Syntactically matches indexed for-loops that iterate over all elements of a collection.
   *
   * <p>This logic lives in the Must Call visitor because matching must complete before collection
   * ownership transfer runs.
   */
  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    boolean singleLoopVariable = tree.getUpdate().size() == 1 && tree.getInitializer().size() == 1;
    if (singleLoopVariable) {
      detectCollectionObligationFulfillingLoop(tree);
    }
    return super.visitForLoop(tree, p);
  }

  /**
   * Performs AST-only matching for while-loops that may fulfill collection obligations.
   *
   * <p>RLCC resolves the remaining CFG-local loop facts later, during post-analysis of the
   * enclosing method.
   */
  @Override
  public Void visitWhileLoop(WhileLoopTree tree, Void p) {
    detectCollectionObligationFulfillingWhileLoop(tree);
    return super.visitWhileLoop(tree, p);
  }

  /**
   * Performs AST-only matching for enhanced-for-loops that may fulfill collection obligations.
   *
   * <p>The visitor records only the loop tree here. RLCC resolves the desugared iterator CFG shape
   * later during post-analysis of the enclosing method.
   *
   * @param tree the enhanced-for-loop to inspect
   */
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    detectPotentiallyFulfillingEnhancedForLoop(tree);
    return super.visitEnhancedForLoop(tree, p);
  }

  /**
   * Records an enhanced-for-loop that potentially fulfills collection obligations.
   *
   * <p>This method only performs AST matching. RLCC resolves the CFG-specific loop facts later,
   * during post-analysis of the enclosing method.
   *
   * @param tree the enhanced-for-loop to inspect
   */
  private void detectPotentiallyFulfillingEnhancedForLoop(EnhancedForLoopTree tree) {
    MethodTree enclosingMethodTree = getEnclosingMethodForCollectionLoop();
    if (enclosingMethodTree == null) {
      return;
    }
    ExpressionTree collectionTree = collectionTreeFromExpression(tree.getExpression());
    if (collectionTree == null) {
      return;
    }
    if (!ResourceLeakUtils.getCollectionOwnershipAnnotatedTypeFactory(atypeFactory)
        .isResourceCollection(collectionTree)) {
      return;
    }
    atypeFactory.recordPotentiallyFulfillingEnhancedForLoop(enclosingMethodTree, tree);
  }

  /**
   * Description of an accepted while-loop header form.
   *
   * <p>Each header form determines which extraction methods are allowed in the loop body.
   */
  private static final class WhileSpec {
    final Set<String> extractMethods;

    WhileSpec(Set<String> extractMethods) {
      this.extractMethods = extractMethods;
    }
  }

  // Iterator: while (it.hasNext()) { ... it.next() ... }
  private static final WhileSpec ITERATOR_SPEC = new WhileSpec(Collections.singleton("next"));

  // Queue/Deque/Stack: while (!c.isEmpty()) { ... c.poll()/pop/removeFirst/... ... }
  // Also size() > 0 / 0 < size()
  private static final WhileSpec NONEMPTY_SPEC =
      new WhileSpec(
          new HashSet<>(
              Arrays.asList(
                  // Queue/Deque (null-returning)
                  "poll",
                  "pollFirst",
                  "pollLast",
                  // Deque (throws on empty, but guarded by condition)
                  "remove",
                  "removeFirst",
                  "removeLast",
                  // Stack
                  "pop")));

  /**
   * AST facts recovered from a matched while-loop header.
   *
   * <p>{@link #collectionTree} is the collection whose element obligations may be discharged.
   * {@link #headerVar} is the iterator or collection variable constrained by the header. {@link
   * #collectionVarNameForBailout} names the collection variable whose writes should invalidate the
   * match when present.
   */
  private static final class WhileHeaderMatch {
    final ExpressionTree collectionTree; // the owning collection expression to mark
    final @Nullable Name collectionVarNameForBailout; // for writes/bailouts
    final Name headerVar; // iterator var (it) OR collection var (q)
    final WhileSpec spec;

    WhileHeaderMatch(
        ExpressionTree collectionTree,
        @Nullable Name collectionVarNameForBailout,
        Name headerVar,
        WhileSpec spec) {
      this.collectionTree = collectionTree;
      this.collectionVarNameForBailout = collectionVarNameForBailout;
      this.headerVar = headerVar;
      this.spec = spec;
    }
  }

  /**
   * Records a while-loop that may fulfill collection obligations.
   *
   * <p>This method performs AST matching plus the small amount of CFG lookup needed to identify the
   * condition block, the conditional successor, the body entry block, and the extracted element
   * node. RLCC resolves the remaining CFG-local fact, the loop update block, later during
   * post-analysis.
   *
   * <p>Supported header shapes are iterator loops such as {@code while (it.hasNext())} and
   * non-empty collection loops such as {@code while (!q.isEmpty())}, {@code while (q.size() > 0)},
   * and {@code while (0 < q.size())}.
   *
   * @param tree the while-loop to inspect
   */
  private void detectCollectionObligationFulfillingWhileLoop(WhileLoopTree tree) {
    MethodTree enclosingMethodTree = getEnclosingMethodForCollectionLoop();
    if (enclosingMethodTree == null) {
      return;
    }
    // 1) Match header
    ExpressionTree condNoParens = TreeUtils.withoutParens(tree.getCondition());
    WhileHeaderMatch header = matchWhileHeader(condNoParens);
    if (header == null) {
      return;
    }
    // 2) Extract body statements.
    List<? extends StatementTree> bodyStatements = getLoopBodyStatements(tree.getStatement());
    if (bodyStatements == null) {
      return;
    }
    // 3) Find exactly one extraction call in the body to be sound.
    BodyExtraction extraction =
        findSingleExtractionInWhileBody(
            bodyStatements,
            header.headerVar,
            header.collectionVarNameForBailout,
            header.spec.extractMethods);
    if (extraction == null) {
      return;
    }
    // Resolve CFG-local metadata (except loopUpdateBlock).
    Block condBlock = firstBlockForTree(condNoParens);
    if (condBlock == null) {
      return;
    }
    // Find the ConditionalBlock that branches on the while condition.
    ConditionalBlock cblock = findConditionalSuccessor(condBlock);
    if (cblock == null) {
      // condition often lives in ExceptionBlocks; try walking up preds and retry
      Block peeled = peelExceptionBlocksToPred(condBlock);
      if (peeled != null) {
        cblock = findConditionalSuccessor(peeled);
      }
    }
    if (cblock == null) {
      return;
    }

    Block loopBodyEntryBlock = cblock.getThenSuccessor();

    // Node for the extraction call tree (it.next()/q.poll()/s.pop()).
    Node elementNode = anyNodeForTree(extraction.extractionCall);
    if (elementNode == null) {
      return;
    }

    // 4) Record a potentially fulfilling collection loop.
    //
    // Store:
    //   - collectionTree (resources / q / s)
    //   - collectionElementTree (it.next() / q.poll() / s.pop())
    //   - condition tree (the while condition)
    atypeFactory.recordPotentiallyFulfillingCollectionLoop(
        enclosingMethodTree,
        header.collectionTree,
        extraction.extractionCall,
        condNoParens,
        loopBodyEntryBlock,
        cblock,
        elementNode);
  }

  /**
   * Returns the enclosing method for the current loop, or {@code null} if the loop is inside a
   * lambda expression.
   *
   * <p>The per-method loop-state refactor records only loops that are part of the enclosing method
   * analysis. Lambda-local loop support can be added separately if needed.
   *
   * @return the enclosing method for the current loop, or {@code null} if it is inside a lambda
   */
  private @Nullable MethodTree getEnclosingMethodForCollectionLoop() {
    Tree enclosingMethodOrLambda = TreePathUtil.enclosingMethodOrLambda(getCurrentPath());
    if (enclosingMethodOrLambda instanceof MethodTree) {
      return (MethodTree) enclosingMethodOrLambda;
    }
    return null;
  }

  /**
   * Returns the statements in a loop body, regardless of whether the body is a block.
   *
   * @param statement the loop body statement
   * @return the loop body statements, or {@code null} if {@code statement} is {@code null}
   */
  private @Nullable List<? extends StatementTree> getLoopBodyStatements(
      @Nullable StatementTree statement) {
    if (statement == null) {
      return null;
    }
    return statement instanceof BlockTree
        ? ((BlockTree) statement).getStatements()
        : Collections.singletonList(statement);
  }

  /**
   * Returns the first CFG block associated with the given tree.
   *
   * @param tree a tree
   * @return the first CFG block associated with {@code tree}, or {@code null} if none is known
   */
  private @Nullable Block firstBlockForTree(Tree tree) {
    Set<Node> nodes = atypeFactory.getNodesForTree(tree);
    if (nodes == null || nodes.isEmpty()) {
      return null;
    }
    for (Node n : nodes) {
      Block block = n.getBlock();
      if (block != null) {
        return block;
      }
    }
    return null;
  }

  /**
   * Returns an arbitrary CFG node associated with the given tree.
   *
   * @param tree a tree
   * @return a CFG node associated with {@code tree}, or {@code null} if none is known
   */
  private @Nullable Node anyNodeForTree(Tree tree) {
    Set<Node> nodes = atypeFactory.getNodesForTree(tree);
    if (nodes == null || nodes.isEmpty()) {
      return null;
    }
    return nodes.iterator().next();
  }

  /**
   * Returns the conditional successor reached from the given block, if one is immediately visible.
   *
   * @param block a CFG block
   * @return the conditional successor of {@code block}, or {@code null} if none is found
   */
  private @Nullable ConditionalBlock findConditionalSuccessor(Block block) {
    for (Block succ : block.getSuccessors()) {
      if (succ instanceof ConditionalBlock) {
        return (ConditionalBlock) succ;
      }
    }
    if (block instanceof SingleSuccessorBlock) {
      Block succ = ((SingleSuccessorBlock) block).getSuccessor();
      if (succ instanceof ConditionalBlock) {
        return (ConditionalBlock) succ;
      }
    }
    return null;
  }

  /**
   * Walks backward through exception blocks to recover the predecessor block that leads to the
   * actual loop conditional.
   *
   * <p>This is needed because loop conditions such as {@code iterator.hasNext()} may be represented
   * by exception blocks before reaching the conditional branch.
   *
   * @param block a CFG block
   * @return a predecessor block to retry from, or {@code null} if no such block is found
   */
  private @Nullable Block peelExceptionBlocksToPred(Block block) {
    Block cur = block;
    Set<Block> visitedBlocks = new HashSet<>();
    while (cur instanceof ExceptionBlock && visitedBlocks.add(cur)) {
      Set<Block> preds = cur.getPredecessors();
      if (preds.size() != 1) {
        break;
      }
      Block p = preds.iterator().next();
      if (p == null) {
        break;
      }
      cur = p;
    }
    return cur;
  }

  /**
   * Matches supported while-loop header forms and returns the recovered loop facts.
   *
   * <p>Supported forms are: {@code while (it.hasNext())}, {@code while (!c.isEmpty())}, {@code
   * while (c.size() > 0)}, and {@code while (0 < c.size())}.
   *
   * @param cond the while-loop condition with parentheses removed
   * @return the recovered header facts, or {@code null} if the header is unsupported
   */
  private @Nullable WhileHeaderMatch matchWhileHeader(ExpressionTree cond) {
    // Case A: while (it.hasNext())
    if (cond instanceof MethodInvocationTree) {
      MethodInvocationTree mit = (MethodInvocationTree) cond;
      if (TreeUtils.isHasNextCall(mit)) {
        ExpressionTree recv = receiverOfInvocation(mit);
        Name itName = getNameFromExpressionTree(recv);
        if (itName == null) {
          return null;
        }
        ExpressionTree colExpr = recoverCollectionFromIteratorReceiver(recv);
        if (colExpr == null) {
          return null;
        }
        Name colName = getNameFromExpressionTree(colExpr);
        return new WhileHeaderMatch(colExpr, colName, itName, ITERATOR_SPEC);
      }
    }

    // Case B1: while (!c.isEmpty())
    if (cond instanceof UnaryTree && cond.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
      ExpressionTree inner = TreeUtils.withoutParens(((UnaryTree) cond).getExpression());
      WhileHeaderMatch m = matchNonEmptyFromExpr(inner);
      if (m != null) {
        return m;
      }
    }

    // Case B2: while (c.size() > 0) or while (0 < c.size())
    if (cond instanceof BinaryTree) {
      WhileHeaderMatch m = matchNonEmptyFromSize((BinaryTree) cond);
      if (m != null) {
        return m;
      }
    }

    return null;
  }

  /**
   * Matches a non-empty collection condition of the form {@code !c.isEmpty()}.
   *
   * @param inner the expression under the logical complement
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderMatch matchNonEmptyFromExpr(ExpressionTree inner) {
    if (!(inner instanceof MethodInvocationTree)) {
      return null;
    }
    MethodInvocationTree mit = (MethodInvocationTree) inner;
    if (!isIsEmptyCall(mit)) {
      return null;
    }
    ExpressionTree recv = receiverOfInvocation(mit);
    if (recv == null) {
      return null;
    }
    Name varName = getNameFromExpressionTree(recv);
    if (varName == null) {
      return null;
    }
    Element recvElt = TreeUtils.elementFromTree(recv);
    if (!ResourceLeakUtils.isCollection(recvElt, atypeFactory)) {
      return null;
    }
    ExpressionTree colTree = collectionTreeFromExpression(recv);
    if (colTree == null) {
      return null;
    }
    return new WhileHeaderMatch(colTree, varName, varName, NONEMPTY_SPEC);
  }

  /**
   * Matches a non-empty collection condition of the form {@code c.size() > 0} or {@code 0 <
   * c.size()}.
   *
   * @param condition the binary condition
   * @return the recovered header facts, or {@code null} if the expression does not match
   */
  private @Nullable WhileHeaderMatch matchNonEmptyFromSize(BinaryTree condition) {
    Tree.Kind k = condition.getKind();
    if (k != Tree.Kind.GREATER_THAN && k != Tree.Kind.LESS_THAN) {
      return null;
    }

    ExpressionTree left = TreeUtils.withoutParens(condition.getLeftOperand());
    ExpressionTree right = TreeUtils.withoutParens(condition.getRightOperand());

    // Normalize: accept "c.size() > 0" or "0 < c.size()"
    MethodInvocationTree sizeCall = null;
    LiteralTree zero = null;

    if (k == Tree.Kind.GREATER_THAN) {
      // left must be size(), right must be 0
      if (left instanceof MethodInvocationTree && right instanceof LiteralTree) {
        sizeCall = (MethodInvocationTree) left;
        zero = (LiteralTree) right;
      }
    } else { // LESS_THAN
      // left must be 0, right must be size()
      if (left instanceof LiteralTree && right instanceof MethodInvocationTree) {
        zero = (LiteralTree) left;
        sizeCall = (MethodInvocationTree) right;
      }
    }

    if (sizeCall == null
        || !(zero.getValue() instanceof Integer)
        || (Integer) zero.getValue() != 0) {
      return null;
    }
    if (!TreeUtils.isSizeAccess(sizeCall)) {
      return null;
    }

    ExpressionTree recv = receiverOfInvocation(sizeCall);
    if (recv == null) {
      return null;
    }

    Name varName = getNameFromExpressionTree(recv);
    if (varName == null) {
      return null;
    }

    Element recvElt = TreeUtils.elementFromTree(recv);
    if (!ResourceLeakUtils.isCollection(recvElt, atypeFactory)) {
      return null;
    }

    ExpressionTree colTree = collectionTreeFromExpression(recv);
    if (colTree == null) {
      return null;
    }

    return new WhileHeaderMatch(colTree, varName, varName, NONEMPTY_SPEC);
  }

  /**
   * Returns whether the given invocation is an {@code isEmpty()} call with no arguments.
   *
   * @param invocation a method invocation
   * @return true if {@code invocation} is an {@code isEmpty()} call with no arguments
   */
  private boolean isIsEmptyCall(MethodInvocationTree invocation) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (!(sel instanceof MemberSelectTree)) {
      return false;
    }
    MemberSelectTree ms = (MemberSelectTree) sel;
    return ms.getIdentifier().contentEquals("isEmpty") && invocation.getArguments().isEmpty();
  }

  /**
   * Returns the explicit receiver of the given invocation, if present.
   *
   * @param invocation a method invocation
   * @return the explicit receiver, or {@code null} if none exists
   */
  private @Nullable ExpressionTree receiverOfInvocation(MethodInvocationTree invocation) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (sel instanceof MemberSelectTree) {
      return ((MemberSelectTree) sel).getExpression();
    }
    return null;
  }

  /**
   * Recovers the collection expression from an iterator receiver in a header such as {@code while
   * (it.hasNext())}.
   *
   * <p>This only recognizes local iterator variables initialized by {@code col.iterator()}.
   *
   * @param iteratorExpr the iterator receiver expression
   * @return the collection expression, or {@code null} if it cannot be recovered
   */
  private @Nullable ExpressionTree recoverCollectionFromIteratorReceiver(
      ExpressionTree iteratorExpr) {
    if (iteratorExpr == null) {
      return null;
    }

    Element itElt = TreeUtils.elementFromTree(iteratorExpr);
    if (!(itElt instanceof VariableElement)) {
      return null;
    }

    // Only recover from local variable declaration with initializer "col.iterator()"
    if (itElt.getKind() != ElementKind.LOCAL_VARIABLE) {
      return null;
    }

    Tree decl = atypeFactory.declarationFromElement(itElt);
    if (!(decl instanceof VariableTree)) {
      return null;
    }

    ExpressionTree init = ((VariableTree) decl).getInitializer();
    if (!(init instanceof MethodInvocationTree)) {
      return null;
    }

    MethodInvocationTree initCall = (MethodInvocationTree) init;
    ExpressionTree sel = initCall.getMethodSelect();
    if (!(sel instanceof MemberSelectTree)) {
      return null;
    }

    MemberSelectTree ms = (MemberSelectTree) sel;
    if (!ms.getIdentifier().contentEquals("iterator") || !initCall.getArguments().isEmpty()) {
      return null;
    }

    ExpressionTree colExpr = ms.getExpression();
    Element colElt = TreeUtils.elementFromTree(colExpr);
    if (!ResourceLeakUtils.isCollection(colElt, atypeFactory)) {
      return null;
    }

    return collectionTreeFromExpression(colExpr);
  }

  /**
   * One extracted element use recovered from a while-loop body.
   *
   * <p>The extraction call is the expression that removes or advances to the next element, such as
   * {@code it.next()}, {@code q.poll()}, or {@code s.pop()}.
   */
  private static final class BodyExtraction {
    final MethodInvocationTree extractionCall; // it.next()/q.poll()/s.pop()

    BodyExtraction(MethodInvocationTree extractionCall) {
      this.extractionCall = extractionCall;
    }
  }

  /**
   * Finds exactly one extraction in the loop body. If 0 or >1 extractions occur, returns {@code
   * null}.
   *
   * <p>This matcher rejects writes to the iterator/header variable and, when present, to the
   * collection variable itself, because such writes invalidate the header/body correspondence used
   * by later CFG verification.
   *
   * @param statements the loop body statements
   * @param headerVar the iterator or collection variable constrained by the header
   * @param collectionVarName the collection variable to protect from writes, if any
   * @param allowedExtractMethods the extraction methods allowed by the matched header
   * @return the unique extraction in the loop body, or {@code null} if the body is unsupported
   */
  private @Nullable BodyExtraction findSingleExtractionInWhileBody(
      List<? extends StatementTree> statements,
      Name headerVar,
      @Nullable Name collectionVarName,
      Set<String> allowedExtractMethods) {

    AtomicBoolean illegal = new AtomicBoolean(false);
    final MethodInvocationTree[] extraction = new MethodInvocationTree[] {null};
    final int[] extractionCount = new int[] {0};

    TreeScanner<Void, Void> scanner =
        new TreeScanner<Void, Void>() {

          private void markWriteIfTargetsHeaderOrCollection(ExpressionTree lhs) {
            Name assigned = getNameFromExpressionTree(lhs);
            if (assigned != null) {
              if (assigned == headerVar) illegal.set(true);
              if (collectionVarName != null && assigned == collectionVarName) illegal.set(true);
            }
          }

          private void recordExtractionIfAny(ExpressionTree expr) {
            expr = TreeUtils.withoutParens(expr);
            if (!(expr instanceof MethodInvocationTree)) {
              return;
            }

            MethodInvocationTree mit = (MethodInvocationTree) expr;
            if (!isExtractionCallOnHeaderVar(mit, headerVar, allowedExtractMethods)) {
              return;
            }

            extractionCount[0]++;
            if (extractionCount[0] > 1) {
              illegal.set(true);
              return;
            }
            extraction[0] = mit;
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
            markWriteIfTargetsHeaderOrCollection(node.getVariable());
            return super.visitCompoundAssignment(node, p);
          }

          @Override
          public Void visitAssignment(AssignmentTree node, Void p) {
            markWriteIfTargetsHeaderOrCollection(node.getVariable());
            recordExtractionIfAny(node.getExpression()); // r = it.next()
            return super.visitAssignment(node, p);
          }

          @Override
          public Void visitVariable(VariableTree vt, Void p) {
            ExpressionTree init = vt.getInitializer();
            if (init != null) {
              recordExtractionIfAny(init); // T r = it.next()
            }
            return super.visitVariable(vt, p);
          }

          @Override
          public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
            // Direct use: it.next().close() => receiver is it.next()
            ExpressionTree sel = mit.getMethodSelect();
            if (sel instanceof MemberSelectTree) {
              ExpressionTree recv = ((MemberSelectTree) sel).getExpression();
              recordExtractionIfAny(recv);
            }
            return super.visitMethodInvocation(mit, p);
          }
        };

    for (StatementTree st : statements) {
      scanner.scan(st, null);
      if (illegal.get()) break;
    }

    if (illegal.get() || extraction[0] == null || extractionCount[0] != 1) {
      return null;
    }
    return new BodyExtraction(extraction[0]);
  }

  /**
   * Returns whether the given invocation is an allowed extraction call on the matched header
   * variable.
   *
   * @param invocation a method invocation
   * @param headerVar the iterator or collection variable constrained by the header
   * @param allowedExtractMethods extraction methods permitted by the matched header form
   * @return true if {@code invocation} is an allowed extraction call on {@code headerVar}
   */
  private boolean isExtractionCallOnHeaderVar(
      MethodInvocationTree invocation, Name headerVar, Set<String> allowedExtractMethods) {
    ExpressionTree sel = invocation.getMethodSelect();
    if (!(sel instanceof MemberSelectTree)) {
      return false;
    }
    MemberSelectTree ms = (MemberSelectTree) sel;
    String methodName = ms.getIdentifier().toString();
    if (!allowedExtractMethods.contains(methodName)) {
      return false;
    }
    if (!invocation.getArguments().isEmpty()) {
      return false;
    }
    Name recv = getNameFromExpressionTree(ms.getExpression());
    return recv != null && recv == headerVar;
  }

  /**
   * Marks the for-loop if it potentially fulfills collection obligations of a collection.
   *
   * @param tree a `for` loop with exactly one loop variable
   */
  private void detectCollectionObligationFulfillingLoop(ForLoopTree tree) {
    MethodTree enclosingMethodTree = getEnclosingMethodForCollectionLoop();
    if (enclosingMethodTree == null) {
      return;
    }

    List<? extends StatementTree> loopBodyStatements = getLoopBodyStatements(tree.getStatement());
    if (loopBodyStatements == null) {
      return;
    }
    StatementTree init = tree.getInitializer().get(0);
    ExpressionTree condition = TreeUtils.withoutParens(tree.getCondition());
    ExpressionStatementTree update = tree.getUpdate().get(0);
    if (!(condition instanceof BinaryTree)) {
      return;
    }
    Name identifierInHeader =
        nameOfCollectionThatAllElementsAreCalledOn(init, (BinaryTree) condition, update);
    Name iterator = getNameFromStatementTree(init);
    if (identifierInHeader == null || iterator == null) {
      return;
    }
    ExpressionTree collectionElementTree =
        getLastElementAccessIfLoopValid(loopBodyStatements, identifierInHeader, iterator);
    if (collectionElementTree != null) {
      // Pattern match succeeded, now mark the loop in the respective datastructures.

      Block loopConditionBlock = null;
      for (Node node : atypeFactory.getNodesForTree(condition)) {
        Block blockOfNode = node.getBlock();
        if (blockOfNode != null) {
          loopConditionBlock = blockOfNode;
          break;
        }
      }

      Block loopUpdateBlock = null;
      for (Node node : atypeFactory.getNodesForTree(update.getExpression())) {
        Block blockOfNode = node.getBlock();
        if (blockOfNode != null) {
          loopUpdateBlock = blockOfNode;
          break;
        }
      }

      Set<Node> collectionEltNodes = atypeFactory.getNodesForTree(collectionElementTree);
      Node nodeForCollectionElt = null;
      if (collectionEltNodes != null) {
        nodeForCollectionElt = collectionEltNodes.iterator().next();
      }
      if (loopUpdateBlock == null || loopConditionBlock == null) {
        return;
      }
      // Record the loop in the RLCCalledMethods ATF's per-method loop state so that it can
      // analyze it later.
      // MustCallConsistencyAnalyzer.analyzeResolvedPotentiallyFulfillingCollectionLoop will then
      // add verified fulfilling loops to the collection-ownership ATF.
      Block conditionalBlock = ((SingleSuccessorBlock) loopConditionBlock).getSuccessor();
      Block loopBodyEntryBlock = ((ConditionalBlock) conditionalBlock).getThenSuccessor();
      atypeFactory.recordResolvedPotentiallyFulfillingCollectionLoop(
          enclosingMethodTree,
          collectionTreeFromExpression(collectionElementTree),
          collectionElementTree,
          tree.getCondition(),
          loopBodyEntryBlock,
          loopUpdateBlock,
          (ConditionalBlock) conditionalBlock,
          nodeForCollectionElt);
    }
  }

  /**
   * Conservatively decides whether a loop iterates over all elements of some collection, using the
   * following rules:
   *
   * <ul>
   *   <li>only one loop variable
   *   <li>initialization must be of the form i = 0
   *   <li>condition must be of the form (i &lt; col.size())
   *   <li>update must be prefix or postfix {@code ++}
   * </ul>
   *
   * Returns:
   *
   * <ul>
   *   <li>null, if any of the above rules is violated
   *   <li>the name of the collection if the loop condition is of the form (i &lt; col.size())
   * </ul>
   *
   * @param init the initializer of the loop
   * @param condition the loop condition
   * @param update the loop update
   * @return the name of the collection that the loop iterates over all elements of, or null
   */
  protected Name nameOfCollectionThatAllElementsAreCalledOn(
      StatementTree init, BinaryTree condition, ExpressionStatementTree update) {
    Tree.Kind updateKind = update.getExpression().getKind();
    if (updateKind == Tree.Kind.PREFIX_INCREMENT || updateKind == Tree.Kind.POSTFIX_INCREMENT) {
      UnaryTree inc = (UnaryTree) update.getExpression();

      // Verify update is of form i++ or ++i and init is variable initializer.
      if (!(init instanceof VariableTree) || !(inc.getExpression() instanceof IdentifierTree))
        return null;
      VariableTree initVar = (VariableTree) init;

      // Verify that intializer is i=0.
      if (!(initVar.getInitializer() instanceof LiteralTree)
          || !((LiteralTree) initVar.getInitializer()).getValue().equals(0)) {
        return null;
      }

      // Verify that condition is of the form: i < something.
      if (!(condition.getLeftOperand() instanceof IdentifierTree)) {
        return null;
      }

      // Verify that i=0, i<n, and i++ have the same "i".
      Name initVarName = initVar.getName();
      if (initVarName != ((IdentifierTree) condition.getLeftOperand()).getName()) {
        return null;
      }
      if (initVarName != ((IdentifierTree) inc.getExpression()).getName()) {
        return null;
      }

      if ((condition.getRightOperand() instanceof MethodInvocationTree)
          && TreeUtils.isSizeAccess(condition.getRightOperand())) {
        ExpressionTree methodSelect =
            ((MethodInvocationTree) condition.getRightOperand()).getMethodSelect();
        if (methodSelect instanceof MemberSelectTree) {
          MemberSelectTree mst = (MemberSelectTree) methodSelect;
          Element elt = TreeUtils.elementFromTree(mst.getExpression());
          if (ResourceLeakUtils.isCollection(elt, atypeFactory)) {
            return getNameFromExpressionTree(mst.getExpression());
          }
        }
      }
    }
    return null;
  }

  /**
   * Check that the loop does not contain any writes to the loop iterator variable or to the
   * collection variable itself. Extract the collection access tree ({@code arr[i]} or {@code
   * collection.get(i)} where {@code i} is the iterator variable and {@code collection/arr} is
   * consistent with the loop header) and return the last encountered such tree.
   *
   * @param statements list of statements of the loop body
   * @param identifierInHeader collection name if loop condition is {@code i < collection.size()} or
   *     {@code i < arr.length} and {@code n} if loop condition is {@code i < n}
   * @param iterator the name of the loop iterator variable
   * @return {@code null} if the loop body writes to the iterator or collection variable; otherwise
   *     the last collection element access tree consistent with the loop header, if one exists
   */
  private @Nullable ExpressionTree getLastElementAccessIfLoopValid(
      List<? extends StatementTree> statements, Name identifierInHeader, Name iterator) {
    AtomicBoolean blockIsIllegal = new AtomicBoolean(false);
    final ExpressionTree[] collectionElementTree = {null};

    TreeScanner<Void, Void> scanner =
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitUnary(UnaryTree tree, Void p) {
            switch (tree.getKind()) {
              case PREFIX_DECREMENT:
              case POSTFIX_DECREMENT:
              case PREFIX_INCREMENT:
              case POSTFIX_INCREMENT:
                if (getNameFromExpressionTree(tree.getExpression()) == iterator) {
                  blockIsIllegal.set(true);
                }
                break;
              default:
                break;
            }
            return super.visitUnary(tree, p);
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
            if (getNameFromExpressionTree(tree.getVariable()) == iterator) {
              blockIsIllegal.set(true);
            }
            return super.visitCompoundAssignment(tree, p);
          }

          @Override
          public Void visitAssignment(AssignmentTree tree, Void p) {
            Name assignedVariable = getNameFromExpressionTree(tree.getVariable());
            if (assignedVariable == iterator || assignedVariable == identifierInHeader) {
              blockIsIllegal.set(true);
            }

            return super.visitAssignment(tree, p);
          }

          // check whether corresponds to collection.get(i)
          @Override
          public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
            if (isIthCollectionElement(mit, iterator)
                && identifierInHeader == getNameFromExpressionTree(mit)
                && identifierInHeader != null) {
              collectionElementTree[0] = mit;
            }
            return super.visitMethodInvocation(mit, p);
          }
        };

    for (StatementTree stmt : statements) {
      scanner.scan(stmt, null);
    }
    if (!blockIsIllegal.get() && collectionElementTree[0] != null) {
      return collectionElementTree[0];
    }
    return null;
  }

  /**
   * Returns the simple name of the identifier referenced by the given expression, or {@code null}
   * if the expression does not reference an identifier.
   *
   * @param expr an expression
   * @return the name of the referenced identifier, or {@code null} if none
   */
  protected Name getNameFromExpressionTree(ExpressionTree expr) {
    if (expr == null) {
      return null;
    }
    switch (expr.getKind()) {
      case IDENTIFIER:
        return ((IdentifierTree) expr).getName();
      case MEMBER_SELECT:
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.FIELD) {
          // this.files  ==> "files"  (NOT "this")
          return mst.getIdentifier();
        } else if (elt.getKind() == ElementKind.METHOD) {
          // resources.size ==> "resources"
          return getNameFromExpressionTree(mst.getExpression());
        } else {
          return null;
        }
      case METHOD_INVOCATION:
        return getNameFromExpressionTree(((MethodInvocationTree) expr).getMethodSelect());
      default:
        return null;
    }
  }

  /**
   * Returns the simple name of the identifier declared or referenced by the given statement, or
   * {@code null} if the statement does not declare or reference an identifier.
   *
   * @param expr the {@code StatementTree}
   * @return the name of the identifier declared or referenced by the statement, or {@code null} if
   *     none
   */
  protected Name getNameFromStatementTree(StatementTree expr) {
    if (expr == null) {
      return null;
    }
    switch (expr.getKind()) {
      case VARIABLE:
        return ((VariableTree) expr).getName();
      case EXPRESSION_STATEMENT:
        return getNameFromExpressionTree(((ExpressionStatementTree) expr).getExpression());
      default:
        return null;
    }
  }

  /**
   * Returns the ExpressionTree of the collection in the given expression.
   *
   * @param expr ExpressionTree
   * @return the expression evaluates to or null if it doesn't
   */
  protected ExpressionTree collectionTreeFromExpression(ExpressionTree expr) {
    switch (expr.getKind()) {
      case IDENTIFIER:
        return expr;
      case MEMBER_SELECT:
        MemberSelectTree mst = (MemberSelectTree) expr;
        Element elt = TreeUtils.elementFromUse(mst);
        if (elt.getKind() == ElementKind.METHOD) {
          return ((MemberSelectTree) expr).getExpression();
        } else if (elt.getKind() == ElementKind.FIELD) {
          return expr;
        } else {
          return null;
        }
      case METHOD_INVOCATION:
        return collectionTreeFromExpression(((MethodInvocationTree) expr).getMethodSelect());
      default:
        return null;
    }
  }

  /**
   * Returns true if the given tree is of the form collection.get(i), where i is the given index
   * name.
   *
   * @param tree the tree to check
   * @param index the index variable name
   * @return true if the given tree is of the form collection.get(index)
   */
  private boolean isIthCollectionElement(Tree tree, Name index) {
    if (tree == null || index == null) {
      return false;
    }
    if (tree instanceof MethodInvocationTree
        && index == getNameFromExpressionTree(TreeUtils.getIdxForGetCall(tree))) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      ExpressionTree methodSelect = mit.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree) {
        MemberSelectTree mst = (MemberSelectTree) methodSelect;
        Element receiverElt = TreeUtils.elementFromTree(mst.getExpression());
        return ResourceLeakUtils.isCollection(receiverElt, atypeFactory);
      }
    }
    return false;
  }
}
