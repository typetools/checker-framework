package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
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
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
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
          lhs.getKind() == Tree.Kind.MEMBER_SELECT
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

  /*
   * SECTION: syntactically match for-loops that iterate over all elements of a collection on
   * the AST
   */

  /** Stores the size variable of the most recent array allocation per array name. */
  private final Map<Name, Name> arrayInitializationSize = new HashMap<>();

  /**
   * Checks through pattern-matching whether the loop either:
   *
   * <ul>
   *   <li>initializes entries of an {@code @OwningCollection}
   *   <li>calls a method on entries of an {@code @OwningCollection} array
   * </ul>
   *
   * If yes, this is marked in some static datastructures in the
   * {@code @MustCallOnElementsAnnotatedTypeFactory}
   */
  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    boolean singleLoopVariable = tree.getUpdate().size() == 1 && tree.getInitializer().size() == 1;
    if (singleLoopVariable) {
      patternMatchFulfillingLoop(tree);
    }
    return super.visitForLoop(tree, p);
  }

  /**
   * Checks whether a for-loop potentially fulfills collection obligations of a collection/array and
   * marks the loop in case the check is successful.
   *
   * @param tree forlooptree
   */
  private void patternMatchFulfillingLoop(ForLoopTree tree) {
    List<? extends StatementTree> loopBodyStatementList;
    if (tree.getStatement() instanceof BlockTree) {
      BlockTree blockT = (BlockTree) tree.getStatement();
      loopBodyStatementList = blockT.getStatements();
    } else {
      loopBodyStatementList = Collections.singletonList(tree.getStatement());
    }
    StatementTree init = tree.getInitializer().get(0);
    ExpressionTree condition = tree.getCondition();
    ExpressionStatementTree update = tree.getUpdate().get(0);
    Name identifierInHeader = verifyAllElementsAreCalledOn(init, (BinaryTree) condition, update);
    Name iterator = getNameFromStatementTree(init);
    if (identifierInHeader == null || iterator == null) {
      return;
    }
    ExpressionTree collectionElementTree =
        preMatchLoop(loopBodyStatementList, identifierInHeader, iterator);
    if (collectionElementTree != null) {
      // pattern match succeeded, now mark the loop in the respective datastructures
      Set<Node> conditionNodes = atypeFactory.getNodesForTree(condition);
      Set<Node> collectionEltNodes = atypeFactory.getNodesForTree(collectionElementTree);
      Set<Node> updateNodes = atypeFactory.getNodesForTree(update.getExpression());
      Block loopConditionBlock = null;
      for (Node node : conditionNodes) {
        Block blockOfNode = node.getBlock();
        if (blockOfNode != null) {
          loopConditionBlock = blockOfNode;
          break;
        }
      }
      Block loopUpdateBlock = null;
      for (Node node : updateNodes) {
        Block blockOfNode = node.getBlock();
        if (blockOfNode != null) {
          loopUpdateBlock = blockOfNode;
          break;
        }
      }
      Node nodeForCollectionElt = null;
      if (collectionEltNodes != null) {
        nodeForCollectionElt = collectionEltNodes.iterator().next();
      }
      if (loopUpdateBlock == null || loopConditionBlock == null) return;
      // add the blocks into a static datastructure in the calledmethodsatf, such that it can
      // analyze
      // them (call MustCallConsistencyAnalyzer.analyzeFulfillingLoops, which in turn adds the trees
      // to the static datastructure in McoeAtf)
      assert (loopConditionBlock instanceof SingleSuccessorBlock);
      Block conditionalBlock = ((SingleSuccessorBlock) loopConditionBlock).getSuccessor();
      assert (conditionalBlock instanceof ConditionalBlock);
      Block loopBodyEntryBlock = ((ConditionalBlock) conditionalBlock).getThenSuccessor();
      RLCCalledMethodsAnnotatedTypeFactory.addPotentiallyFulfillingLoop(
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
   * Decides for a for-loop header whether the loop iterates over all elements of some array based
   * on a pattern-match with one-sided error with the following rules:
   *
   * <ul>
   *   <li>only one loop variable
   *   <li>initialization must be of the form i = 0
   *   <li>condition must be of the form (i &lt; arr.length) or (i &lt; n), where n and arr are
   *       identifiers and n is effectively final
   *   <li>update must be prefix or postfix
   * </ul>
   *
   * Returns:
   *
   * <ul>
   *   <li>null if any rule is violated
   *   <li>the name of the array if the loop condition is of the form (i &lt; arr.length)
   *   <li>n if it is of the form (i &lt; n), where n is an identifier.
   * </ul>
   *
   * @param init the initializer of the loop
   * @param condition the loop condition
   * @param update the loop update
   * @return null if any rule is violated, or the name of the array if the loop condition is of the
   *     form {@code i < arr.length} or n if it is of the form {@code i < n}), where n is an
   *     identifier.
   */
  protected Name verifyAllElementsAreCalledOn(
      StatementTree init, BinaryTree condition, ExpressionStatementTree update) {
    Tree.Kind updateKind = update.getExpression().getKind();
    if (updateKind == Tree.Kind.PREFIX_INCREMENT || updateKind == Tree.Kind.POSTFIX_INCREMENT) {
      UnaryTree inc = (UnaryTree) update.getExpression();
      // verify update is of form i++ or ++i and init is variable initializer
      if (!(init instanceof VariableTree) || !(inc.getExpression() instanceof IdentifierTree))
        return null;
      VariableTree initVar = (VariableTree) init;
      // verify that intializer is i=0
      if (!(initVar.getInitializer() instanceof LiteralTree)
          || !((LiteralTree) initVar.getInitializer()).getValue().equals(0)) {
        return null;
      }
      // verify that condition is of the form: i < something
      if (!(condition.getLeftOperand() instanceof IdentifierTree)) return null;
      if (initVar.getName()
              != ((IdentifierTree) condition.getLeftOperand()).getName() // i=0 and i<n are same "i"
          || initVar.getName()
              != ((IdentifierTree) inc.getExpression()).getName()) { // i=0 and i++ are same "i"
        return null;
      }
      if (TreeUtils.isArrayLengthAccess(condition.getRightOperand())) {
        return getNameFromExpressionTree(condition.getRightOperand());
      } else if ((condition.getRightOperand() instanceof MethodInvocationTree)
          && TreeUtils.isSizeAccess(condition.getRightOperand())) {
        ExpressionTree methodSelect =
            ((MethodInvocationTree) condition.getRightOperand()).getMethodSelect();
        assert methodSelect.getKind() == Tree.Kind.MEMBER_SELECT
            : "method selection of object.size() expected to be memberSelectTree, but is "
                + methodSelect.getKind();
        MemberSelectTree mst = (MemberSelectTree) methodSelect;
        Element elt = TreeUtils.elementFromTree(mst.getExpression());
        if (ResourceLeakUtils.isCollection(elt, atypeFactory)) {
          return getNameFromExpressionTree(mst.getExpression());
        }
      } else if (condition.getRightOperand().getKind() == Tree.Kind.IDENTIFIER) {
        return getNameFromExpressionTree(condition.getRightOperand());
      }
    }
    return null;
  }

  /**
   * Check that the loop does not contain any writes to the loop iterator variable, or to the
   * collection variable itself. return/break statements. Extract the collection access tree ({@code
   * arr[i]} or {@code collection.get(i)} where {@code i} is the iterator variable and {@code
   * collection/arr} is consistent with the loop header) and return the last encountered such tree.
   *
   * @param statements list of statements of the loop body
   * @param identifierInHeader collection name if loop condition is {@code i < collection.size()} or
   *     {@code i < arr.length} and {@code n} if loop condition is {@code i < n}
   * @param iterator the name of the loop iterator variable
   * @return null if any writes to loop iterator variable or return/break statements are in {@code
   *     block}. Else, return the last encountered collection access tree consistent with the loop
   *     heaer if it exists and else null.
   */
  private @Nullable ExpressionTree preMatchLoop(
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

          @Override
          public Void visitBreak(BreakTree bt, Void p) {
            blockIsIllegal.set(true);
            return super.visitBreak(bt, p);
          }

          @Override
          public Void visitReturn(ReturnTree rt, Void p) {
            blockIsIllegal.set(true);
            return super.visitReturn(rt, p);
          }

          // check whether corresponds to collection.get(i)
          @Override
          public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
            if (isIthCollectionElement(mit, iterator)
                && loopHeaderConsistentWithCollection(
                    identifierInHeader, getNameFromExpressionTree(mit))) {
              collectionElementTree[0] = mit;
            }
            return super.visitMethodInvocation(mit, p);
          }

          // check whether corresponds to arr[i]
          @Override
          public Void visitArrayAccess(ArrayAccessTree aat, Void p) {
            boolean isIthArrayElement = getNameFromExpressionTree(aat.getIndex()) == iterator;
            if (isIthArrayElement
                && loopHeaderConsistentWithCollection(
                    identifierInHeader, getNameFromExpressionTree(aat))) {
              collectionElementTree[0] = aat;
            }
            return super.visitArrayAccess(aat, p);
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
   * Get name from an ExpressionTree
   *
   * @param expr ExpressionTree
   * @return Name of the identifier the expression evaluates to or null if it doesn't
   */
  protected Name getNameFromExpressionTree(ExpressionTree expr) {
    if (expr == null) return null;
    switch (expr.getKind()) {
      case IDENTIFIER:
        return ((IdentifierTree) expr).getName();
      case ARRAY_ACCESS:
        return getNameFromExpressionTree(((ArrayAccessTree) expr).getExpression());
      case MEMBER_SELECT:
        Element elt = TreeUtils.elementFromUse((MemberSelectTree) expr);
        if (elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD) {
          return getNameFromExpressionTree(((MemberSelectTree) expr).getExpression());
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
   * Get name from a {@code StatementTree}
   *
   * @param expr the {@code StatementTree}
   * @return Name of the identifier the expression evaluates to or null if it doesn't
   */
  protected Name getNameFromStatementTree(StatementTree expr) {
    if (expr == null) return null;
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
   * Returns the ExpressionTree of the collection/array in the given expression
   *
   * @param expr ExpressionTree
   * @return the expression evaluates to or null if it doesn't
   */
  protected ExpressionTree collectionTreeFromExpression(ExpressionTree expr) {
    switch (expr.getKind()) {
      case IDENTIFIER:
        return expr;
      case ARRAY_ACCESS:
        return ((ArrayAccessTree) expr).getExpression();
      case MEMBER_SELECT:
        Element elt = TreeUtils.elementFromUse((MemberSelectTree) expr);
        if (elt.getKind() == ElementKind.METHOD) {
          return ((MemberSelectTree) expr).getExpression();
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
   * Returns whether the given collection name is consistent with the identifier from the loop
   * header.
   *
   * <p>That is, either the names are equal, or the identifier from the header is the same variable
   * used to initialize the given collection.
   *
   * <p>Returns false if any argument is null.
   *
   * @param idInHeader identifier from loop header
   * @param collectionName name of collection
   * @return whether the given collection name is consistent with the identifier from the loop
   *     header.
   */
  private boolean loopHeaderConsistentWithCollection(Name idInHeader, Name collectionName) {
    if (idInHeader == null || collectionName == null) return false;
    boolean namesAreEqual = collectionName == idInHeader;
    Name initSize = arrayInitializationSize.get(collectionName);
    boolean idInHeaderIsSizeOfCollection = initSize != null && initSize == idInHeader;
    return namesAreEqual || idInHeaderIsSizeOfCollection;
  }

  /**
   * Returns whether the given tree is of the form collection.get(i), where i is the given index
   * name.
   *
   * @param tree the tree to check
   * @param index the index variable name
   * @return whether the given tree is of the form collection.get(index)
   */
  private boolean isIthCollectionElement(Tree tree, Name index) {
    if (tree == null || index == null) return false;
    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION
        && index == getNameFromExpressionTree(TreeUtils.getIdxForGetCall(tree))) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      ExpressionTree methodSelect = mit.getMethodSelect();
      assert methodSelect.getKind() == Tree.Kind.MEMBER_SELECT
          : "method selection of object.get() expected to be memberSelectTree, but is "
              + methodSelect.getKind();
      MemberSelectTree mst = (MemberSelectTree) methodSelect;
      Element receiverElt = TreeUtils.elementFromTree(mst.getExpression());
      return ResourceLeakUtils.isCollection(receiverElt, atypeFactory);
    }
    return false;
  }
}
