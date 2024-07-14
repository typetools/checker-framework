package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CatchTree;
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
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory.PotentiallyAssigningLoop;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.mustcallonelements.MustCallOnElementsAnnotatedTypeFactory;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.cfg.block.Block;
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

  /** Stores the size variable of the most recent array allocation per array name. */
  private final Map<Name, Name> arrayInitializationSize = new HashMap<>();

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

  /**
   * If the VariableTree initializes a new array, the dimension variable of this array is put into
   * the arrayInitializationSize datastructure, such that when pattern-matching a loop, the size of
   * this array can be used as a loop bound. Check whether the declaration contains an aliasing
   * assignment where the LHS is non-@OwnignArray, but the RHS is. In this case, stores a mapping
   * from the alias name to the RHS (as a Tree).
   */
  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    ExpressionTree initializer = tree.getInitializer();

    if (initializer instanceof NewArrayTree) {
      VariableElement arrayElement = TreeUtils.elementFromDeclaration(tree);
      if (arrayElement.getAnnotation(OwningArray.class) != null) {
        NewArrayTree nat = (NewArrayTree) tree.getInitializer();
        if (nat.getDimensions().size() == 1) {
          ExpressionTree dim = nat.getDimensions().get(0);
          if (dim instanceof IdentifierTree) {
            IdentifierTree dimVariable = (IdentifierTree) dim;
            Element varElement = TreeUtils.elementFromTree(dimVariable);
            Name varName = dimVariable.getName();
            if (ElementUtils.isEffectivelyFinal(varElement)) {
              arrayInitializationSize.put(tree.getName(), varName);
            }
          }
        }
      }
    }
    return super.visitVariable(tree, p);
  }

  // @Override
  // public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
  //   MustCallAnnotatedTypeFactory mcatf = new MustCallAnnotatedTypeFactory(checker);
  //   ExpressionTree collectionExpr = tree.getExpression();
  //   if (mcatf.getDeclAnnotation(TreeUtils.elementFromTree(collectionExpr), OwningArray.class)
  //       != null) {
  //     // mark the loop
  //   }
  //   return super.visitEnhancedForLoop(tree, p);
  // }

  // /**
  //  * Returns a <code>Map</code> from the ExpressionTree of a collection/array to the set of
  // methods
  //  * (as {@code MethodInvocationTree}) that are called on its elements in the loop body united
  // with
  //  * the set of methods (as {@code MemberSelectTree}) Or null if any of the following rules are
  //  * violated:
  //  *
  //  * <ul>
  //  *   <li>there may be no writes to the iterator variable
  //  *   <li>no assert, return or break statements
  //  * </ul>
  //  *
  //  * @param block the list of statements of a for-loop
  //  * @param identifierInHeader the name of the identifier in the condition of the loop, either a
  //  *     size variable or a collection identifier
  //  * @param iterator the name of the single iterator variable of the for-loop
  //  * @return Map of ExpressionTree of collection/array, on whose elements methods are called in
  // the
  //  *     loop body to the set of method names that have been called, or null if any of the checks
  //  *     fail
  //  */
  // private Map<ExpressionTree, Set<ExpressionTree>> getValidMethodsCalledOnCollectionElement(
  //     BlockTree block, Name identifierInHeader, Name iterator) {
  //   AtomicBoolean blockIsIllegal = new AtomicBoolean(false);
  //   final Map<ExpressionTree, Set<ExpressionTree>> collectionToCalledMethods = new HashMap<>();
  //   TreeScanner<Void, Void> scanner =
  //       new TreeScanner<Void, Void>() {
  //         @Override
  //         public Void visitUnary(UnaryTree tree, Void p) {
  //           switch (tree.getKind()) {
  //             case PREFIX_DECREMENT:
  //               blockIsIllegal.set(true);
  //               break;
  //             case POSTFIX_DECREMENT:
  //               blockIsIllegal.set(true);
  //               break;
  //             case PREFIX_INCREMENT:
  //               blockIsIllegal.set(true);
  //               break;
  //             case POSTFIX_INCREMENT:
  //               blockIsIllegal.set(true);
  //               break;
  //             default:
  //               break;
  //           }
  //           return super.visitUnary(tree, p);
  //         }

  //         @Override
  //         public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
  //           blockIsIllegal.set(true);
  //           return super.visitCompoundAssignment(tree, p);
  //         }

  //         @Override
  //         public Void visitAssignment(AssignmentTree tree, Void p) {
  //           blockIsIllegal.set(true);
  //           return super.visitAssignment(tree, p);
  //         }

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
    boolean namesAreEqual = collectionName.equals(idInHeader);
    Name initSize = arrayInitializationSize.get(collectionName);
    boolean idInHeaderIsSizeOfCollection = initSize != null && initSize.equals(idInHeader);
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
        && index.equals(nameFromExpression(TreeUtils.isGetCall(tree)))) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      ExpressionTree methodSelect = mit.getMethodSelect();
      assert methodSelect.getKind() == Tree.Kind.MEMBER_SELECT
          : "method selection of object.get() expected to be memberSelectTree, but is "
              + methodSelect.getKind();
      MemberSelectTree mst = (MemberSelectTree) methodSelect;
      Element receiverElt = TreeUtils.elementFromTree(mst.getExpression());
      return MustCallOnElementsAnnotatedTypeFactory.isCollection(receiverElt, atypeFactory);
    }
    return false;
  }

  //         @Override
  //         public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
  //           List<? extends ExpressionTree> args = mit.getArguments();
  //           boolean firstArgIsCollectionElement =
  //               args.size() >= 1 // at least one argument
  //                   && collectionTreeFromExpression(args.get(0)) != null
  //                   && (MustCallOnElementsAnnotatedTypeFactory.isCollection(
  //                           TreeUtils.elementFromTree(collectionTreeFromExpression(args.get(0))),
  //                           atypeFactory)
  //                       || args.get(0).getKind() == Tree.Kind.ARRAY_ACCESS)
  //                   && loopHeaderConsistentWithCollection(
  //                       identifierInHeader, nameFromExpression(args.get(0)));
  //           ExpressionTree receiver = null;
  //           ExpressionTree method = mit;
  //           if (firstArgIsCollectionElement) {
  //             // method(collection.get(i)) or method(arr[i])
  //             System.out.println("method select: " + mit.getMethodSelect());
  //             System.out.println("method select: " + mit.getMethodSelect().getKind());
  //             receiver = args.get(0);
  //           } else {
  //             // collection.get(i).method() or arr[i].method()
  //             ExpressionTree methodSelect = mit.getMethodSelect();
  //             assert methodSelect.getKind() == Tree.Kind.MEMBER_SELECT
  //                 : "method selection of "
  //                     + methodSelect
  //                     + " expected to be memberSelectTree, but is "
  //                     + methodSelect.getKind();
  //             MemberSelectTree mst = (MemberSelectTree) methodSelect;
  //             receiver = mst.getExpression();
  //             method = mst;
  //           }
  //           boolean receiverIsIthArrayElement =
  //               receiver.getKind() == Tree.Kind.ARRAY_ACCESS
  //                   && nameFromExpression(((ArrayAccessTree)
  // receiver).getIndex()).equals(iterator);
  //           boolean receiverIsIthCollectionElement = isIthCollectionElement(receiver, iterator);
  //           if (receiverIsIthArrayElement || receiverIsIthCollectionElement) {
  //             ExpressionTree collectionExpr = collectionTreeFromExpression(receiver);
  //             collectionToCalledMethods
  //                 .computeIfAbsent(collectionExpr, k -> new HashSet<>())
  //                 .add(method);
  //           }
  //           return super.visitMethodInvocation(mit, p);
  //         }

  //         @Override
  //         public Void visitBreak(BreakTree bt, Void p) {
  //           blockIsIllegal.set(true);
  //           return super.visitBreak(bt, p);
  //         }

  //         @Override
  //         public Void visitReturn(ReturnTree rt, Void p) {
  //           blockIsIllegal.set(true);
  //           return super.visitReturn(rt, p);
  //         }
  //       };
  //   for (StatementTree stmt : block.getStatements()) {
  //     scanner.scan(stmt, null);
  //   }
  //   if (!blockIsIllegal.get()) {
  //     return collectionToCalledMethods;
  //   } else {
  //     return null;
  //   }
  // }

  /**
   * Check that the loop does not contain any writes to the loop iterator variable and no
   * return/break statements. Extract the collection access tree ({@code arr[i]} or {@code
   * collection.get(i)} where {@code i} is the iterator variable and {@code collection/arr} is
   * consistent with the loop header) and return the last encountered such tree.
   *
   * @param block the loop body tree
   * @param identifierInHeader collection name if loop condition is {@code i < collection.size()} or
   *     {@code i < arr.length} and {@code n} if loop condition is {@code i < n}
   * @param iterator the name of the loop iterator variable
   * @return null if any writes to loop iterator variable or return/break statements are in {@code
   *     block}. Else, return the last encountered collection access tree consistent with the loop
   *     heaer if it exists and else null.
   */
  private @Nullable ExpressionTree preMatchLoop(
      BlockTree block, Name identifierInHeader, Name iterator) {
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
                if (nameFromExpression(tree.getExpression()).equals(iterator)) {
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
            if (nameFromExpression(tree.getVariable()).equals(iterator)) {
              blockIsIllegal.set(true);
            }
            return super.visitCompoundAssignment(tree, p);
          }

          @Override
          public Void visitAssignment(AssignmentTree tree, Void p) {
            if (nameFromExpression(tree.getVariable()).equals(iterator)) {
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
                    identifierInHeader, nameFromExpression(mit))) {
              collectionElementTree[0] = mit;
            }
            return super.visitMethodInvocation(mit, p);
          }

          // check whether corresponds to arr[i]
          @Override
          public Void visitArrayAccess(ArrayAccessTree aat, Void p) {
            boolean isIthArrayElement = nameFromExpression(aat.getIndex()).equals(iterator);
            if (isIthArrayElement
                && loopHeaderConsistentWithCollection(
                    identifierInHeader, nameFromExpression(aat))) {
              collectionElementTree[0] = aat;
            }
            return super.visitArrayAccess(aat, p);
          }
        };
    for (StatementTree stmt : block.getStatements()) {
      scanner.scan(stmt, null);
    }
    if (!blockIsIllegal.get() && collectionElementTree[0] != null) {
      return collectionElementTree[0];
    }
    return null;
  }

  /**
   * Checks whether a for-loop potentially fulfills {@code @MustCallOnElements} obligations of a
   * collection/array and marks the loop in case the check is successful.
   *
   * @param tree forlooptree
   */
  private void patternMatchFulfillingLoop(ForLoopTree tree) {
    BlockTree blockT = (BlockTree) tree.getStatement();
    StatementTree init = tree.getInitializer().get(0);
    ExpressionTree condition = tree.getCondition();
    ExpressionStatementTree update = tree.getUpdate().get(0);
    Name identifierInHeader = verifyAllElementsAreCalledOn(init, (BinaryTree) condition, update);
    System.out.println("identifierInHeader: " + identifierInHeader);
    Name iterator = ((VariableTree) init).getName();
    if (identifierInHeader == null || iterator == null) {
      return;
    }
    ExpressionTree collectionElementTree = preMatchLoop(blockT, identifierInHeader, iterator);
    if (collectionElementTree != null) {
      // pattern match succeeded, now mark the loop in the respective datastructures
      System.out.println("prematch succeeded");
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
      CalledMethodsAnnotatedTypeFactory.addPotentiallyFulfillingLoop(
          loopConditionBlock,
          loopUpdateBlock,
          tree.getCondition(),
          collectionElementTree,
          nodeForCollectionElt,
          collectionTreeFromExpression(collectionElementTree));
    }
  }

  /**
   * Checks through pattern-matching whether the loop either:
   *
   * <ul>
   *   <li>initializes entries of an {@code @OwningArray}
   *   <li>calls a method on entries of an {@code @OwningArray} array
   * </ul>
   *
   * If yes, this is marked in some static datastructures in the
   * {@code @MustCallOnElementsAnnotatedTypeFactory}
   */
  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    // ensure there's only one loop variable
    boolean singleLoopVariable = tree.getUpdate().size() == 1 && tree.getInitializer().size() == 1;
    if (singleLoopVariable) {
      patternMatchFulfillingLoop(tree);
      patternMatchAssigningLoop(tree);
    }
    return super.visitForLoop(tree, p);
  }

  /**
   * Checks whether a for-loop possibly assigns the elements of an {@code @OwningArray}
   * collection/array and marks the loop in case the check is successful.
   *
   * @param tree ForLoopTree
   */
  private void patternMatchAssigningLoop(ForLoopTree tree) {
    BlockTree blockT = (BlockTree) tree.getStatement();
    // pattern match the initializer, condition and update
    if (blockT.getStatements().size() != 1
        || tree.getCondition().getKind()
            != Tree.Kind.LESS_THAN) { // ensure loop body has only one statement
      return;
    }

    ExpressionTree stmtTree = null;
    StatementTree topLevelStmt = blockT.getStatements().get(0);
    if (topLevelStmt instanceof TryTree) {
      stmtTree = getSingleStmtFromTryBlock((TryTree) topLevelStmt);
    } else if (topLevelStmt instanceof ExpressionStatementTree) {
      stmtTree = ((ExpressionStatementTree) topLevelStmt).getExpression();
    }

    if (stmtTree == null) {
      return;
    }

    // extract the array access tree in case it exists in the statement
    ArrayAccessTree arrayAccess = extractArrayAccessFromStatement(stmtTree);
    if (arrayAccess == null) {
      return;
    }

    // ensure array access index is same as the one initialized in the loop header
    StatementTree init = tree.getInitializer().get(0);
    ExpressionTree idx = arrayAccess.getIndex();
    if (!(init instanceof VariableTree)
        || !(idx instanceof IdentifierTree)
        || ((IdentifierTree) idx).getName() != ((VariableTree) init).getName()) return;
    // ensure indexed array is the same as the one we took the length of in loop condition
    Name arrayNameInBody = nameFromExpression(arrayAccess.getExpression());
    if (arrayNameInBody == null) {
      // expected array, but does not directly evaluate to an identifier
      checker.reportWarning(arrayAccess, "unexpected.array.expression");
      return;
    }

    // ensure that the loop bound agrees with the array in the body, i.e. ensure that the returned
    // identifier from the loop header "n" is either equal to the size variable of the array in the
    // body (if condition is i < n) or it's equal to the name "arr" of the array in the body itself
    // (if condition is i < arr.length)
    Name arrayInBodySizeVariable = arrayInitializationSize.get(arrayNameInBody);
    Name identifierInHeader =
        verifyAllElementsAreCalledOn(
            (StatementTree) tree.getInitializer().get(0),
            (BinaryTree) tree.getCondition(),
            (ExpressionStatementTree) tree.getUpdate().get(0));
    if (identifierInHeader == null
        || (identifierInHeader != arrayNameInBody
            && identifierInHeader != arrayInBodySizeVariable)) {
      return;
    }

    // pattern match succeeded, now mark the loop in the respective datastructures
    if (stmtTree instanceof AssignmentTree) {
      AssignmentTree assgn = (AssignmentTree) stmtTree;
      if (!(assgn.getExpression() instanceof NewClassTree)) {
        // checker.reportWarning(assgn, "unexpected.rhs.allocatingassignment");
        return;
      }
      // mark for-loop as 'allocating-for-loop'
      ExpressionTree className = ((NewClassTree) assgn.getExpression()).getIdentifier();
      Element rhsElt = TreeUtils.elementFromTree(className);
      MustCallAnnotatedTypeFactory mcTypeFactory = new MustCallAnnotatedTypeFactory(checker);
      AnnotationMirror mcAnno =
          mcTypeFactory.getAnnotatedType(rhsElt).getPrimaryAnnotation(MustCall.class);
      List<String> mcValues =
          AnnotationUtils.getElementValueArray(
              mcAnno, mcTypeFactory.getMustCallValueElement(), String.class);
      System.out.println("detected mustcall: " + mcValues);
      // check whether the RHS actually has must-call obligations
      if (mcValues != null) {
        PotentiallyAssigningLoop loop =
            new PotentiallyAssigningLoop(
                arrayAccess.getExpression(),
                arrayAccess,
                tree.getCondition(),
                assgn,
                new HashSet<String>(mcValues));
        MustCallOnElementsAnnotatedTypeFactory.markAssigningLoop(loop);
      }
    }
  }

  /**
   * Returns the ExpressionTree from the single statement contained in the innermost try-block or
   * null if any of the following rules is violated:
   *
   * <ul>
   *   <li>the try-block may only contain one statement, either an ExpressionStatement or another
   *       try-catch-construct
   *   <li>the catch-blocks and the finally-block may not contain any break, throw or return
   *       statements or method calls
   * </ul>
   *
   * @param tree the top-level TryTree
   * @return the ExpressionTree from the single statement contained in the innermost try-block or
   *     null if any rule is violated
   */
  private ExpressionTree getSingleStmtFromTryBlock(TryTree tree) {
    AtomicBoolean blockIsIllegal = new AtomicBoolean(false);
    StatementTree[] wrappedStmt = new StatementTree[1];
    TreeScanner<Void, Void> scanner =
        new TreeScanner<Void, Void>() {
          /*
           * Recursively extracts the single statement from the innermost try-block or flags the block
           * as illegal if there's more than one statement at any point in a try-block.
           * Calls visitBlock(BlockTree) on the catch and finally blocks to verify them.
           */
          @Override
          public Void visitTry(TryTree tree, Void o) {
            if (tree.getBlock() == null) {
              blockIsIllegal.set(true);
            }
            List<? extends StatementTree> stmtList = tree.getBlock().getStatements();
            if (stmtList == null || stmtList.size() != 1) {
              blockIsIllegal.set(true);
            } else {
              for (StatementTree stmt : stmtList) {
                if (stmt instanceof TryTree) {
                  visitTry((TryTree) stmt, o);
                } else {
                  if (wrappedStmt[0] == null) {
                    wrappedStmt[0] = stmt;
                  }
                }
              }
            }
            if (tree.getCatches() != null) {
              for (CatchTree c : tree.getCatches()) {
                visitBlock(c.getBlock(), o);
              }
            }
            if (tree.getFinallyBlock() != null) {
              visitBlock(tree.getFinallyBlock(), o);
            }
            // if I return super.visitTry(tree, o), somehow the StatementScanner is applied to
            // the top-level try-block, I don't understand why. Returning null fixes the issue.
            // return super.visitTry(tree, o);
            return null;
          }

          /*
           * Handles statement blocks inside catch/finally blocks by running a StatementScanner on them.
           */
          @Override
          public Void visitBlock(BlockTree tree, Void o) {
            if (tree != null) {
              tree.accept(new StatementScanner(), null);
            }
            return super.visitBlock(tree, o);
          }

          /**
           * Sets the blockIsIllegal boolean for any throw, return, break, method invocation,
           * assignment, compound assignment and assert.
           */
          class StatementScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitUnary(UnaryTree tree, Void p) {
              switch (tree.getKind()) {
                case PREFIX_DECREMENT:
                  blockIsIllegal.set(true);
                  break;
                case POSTFIX_DECREMENT:
                  blockIsIllegal.set(true);
                  break;
                case PREFIX_INCREMENT:
                  blockIsIllegal.set(true);
                  break;
                case POSTFIX_INCREMENT:
                  blockIsIllegal.set(true);
                  break;
                default:
                  break;
              }
              return super.visitUnary(tree, p);
            }

            @Override
            public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
              blockIsIllegal.set(true);
              return super.visitCompoundAssignment(tree, p);
            }

            @Override
            public Void visitAssert(AssertTree tree, Void p) {
              blockIsIllegal.set(true);
              return super.visitAssert(tree, p);
            }

            @Override
            public Void visitAssignment(AssignmentTree tree, Void p) {
              blockIsIllegal.set(true);
              return super.visitAssignment(tree, p);
            }

            @Override
            public Void visitThrow(ThrowTree tt, Void p) {
              blockIsIllegal.set(true);
              return super.visitThrow(tt, p);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
              blockIsIllegal.set(true);
              // ExecutableElement method = TreeUtils.elementFromUse(mit);
              // if (method.getThrownTypes() != null) {
              //   blockIsIllegal.set(true);
              // }
              return super.visitMethodInvocation(mit, p);
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
          }
        };
    scanner.scan(tree, null);
    if (blockIsIllegal.get()
        || wrappedStmt[0] == null
        || !(wrappedStmt[0] instanceof ExpressionStatementTree)) {
      return null;
    }
    return ((ExpressionStatementTree) wrappedStmt[0]).getExpression();
  }

  /**
   * Extracts the ArrayAccessTree within the statement if the statement is:
   *
   * <ul>
   *   <li>an assignment and the LHS is the array access
   *   <li>a method invocation on an array element
   *   <li>a method invocation with an array element as the ONLY argument
   * </ul>
   *
   * @param tree the statement tree
   * @return the ArrayAccessTree within the tree or null if there is none
   */
  private ArrayAccessTree extractArrayAccessFromStatement(ExpressionTree tree) {
    if (tree == null) return null;
    ExpressionTree operand = null;
    if (tree instanceof AssignmentTree) { // possibly allocating loop
      operand = ((AssignmentTree) tree).getVariable();
    } else if (tree instanceof MethodInvocationTree) { // possiblity deallocating loop
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      operand = mit.getMethodSelect();
      if (operand instanceof MemberSelectTree) {
        operand = ((MemberSelectTree) operand).getExpression();
      } else if (mit.getArguments() != null && mit.getArguments().size() == 1) {
        for (ExpressionTree arg : mit.getArguments()) {
          if (arg instanceof ArrayAccessTree) {
            return (ArrayAccessTree) arg;
          }
        }
      } else {
        operand = null;
      }
    }

    if (operand != null && operand.getKind() == Tree.Kind.ARRAY_ACCESS) {
      return (ArrayAccessTree) operand;
    } else {
      return null;
    }
  }

  /**
   * Returns the names of methods ensured to be called on the elements of the first argument of the
   * method in the given <code>MethodInvocationTree</code>. For example:
   *
   * <p><code>m(arr)</code> where <code>m</code> has an {@code @EnsuresCalledMethods(value="#1",
   * methods="close")} annotation, the method would return <code>{"close"}</code>.
   *
   * @param methodInvocation the method invocation tree
   * @return names of methods ensured to be called on the elements of the first argument of the
   *     given method invocation tree
   */
  // private Set<String> getCoeMethodNames(MethodInvocationTree methodInvocation) {
  //   ExpressionTree methodCall = methodInvocation.getMethodSelect();
  //   if (methodCall instanceof MemberSelectTree) {
  //     return Collections.singleton(((MemberSelectTree) methodCall).getIdentifier().toString());
  //   } else if (methodCall instanceof IdentifierTree) {
  //     ExecutableElement method = TreeUtils.elementFromUse(methodInvocation);
  //     ResourceLeakChecker rlc = new ResourceLeakChecker();
  //     rlc.init(checker.getProcessingEnvironment());
  //     ResourceLeakAnnotatedTypeFactory rlAtf = new ResourceLeakAnnotatedTypeFactory(rlc);
  //     AnnotationMirrorSet allEnsuresCalledMethodsAnnos =
  //         ResourceLeakVisitor.getEnsuresCalledMethodsAnnotations(method, rlAtf);
  //     Set<String> methodsCalledOnFirstArg = new HashSet<>();
  //     for (AnnotationMirror ensuresCalledMethodsAnno : allEnsuresCalledMethodsAnnos) {
  //       List<String> values =
  //           AnnotationUtils.getElementValueArray(
  //               ensuresCalledMethodsAnno,
  //               rlAtf.getEnsuresCalledMethodsValueElement(),
  //               String.class);
  //       for (String value : values) {
  //         if (value.equals("#1")) {
  //           List<String> methods =
  //               AnnotationUtils.getElementValueArray(
  //                   ensuresCalledMethodsAnno,
  //                   rlAtf.getEnsuresCalledMethodsMethodsElement(),
  //                   String.class);
  //           methodsCalledOnFirstArg.addAll(methods);
  //         }
  //       }
  //     }
  //     return methodsCalledOnFirstArg;
  //   } else {
  //     return null;
  //   }
  // }

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
   *     form (i &lt; arr.length) or n if it is of the form (i &lt; n), where n is an identifier.
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
      if (!initVar
              .getName()
              .equals(
                  ((IdentifierTree) condition.getLeftOperand())
                      .getName()) // i=0 and i<n are same "i"
          || !initVar
              .getName()
              .equals(
                  ((IdentifierTree) inc.getExpression()).getName())) { // i=0 and i++ are same "i"
        return null;
      }
      if (TreeUtils.isArrayLengthAccess(condition.getRightOperand())) {
        return nameFromExpression(condition.getRightOperand());
      } else if ((condition.getRightOperand() instanceof MethodInvocationTree)
          && TreeUtils.isSizeAccess(condition.getRightOperand())) {
        ExpressionTree methodSelect =
            ((MethodInvocationTree) condition.getRightOperand()).getMethodSelect();
        assert methodSelect.getKind() == Tree.Kind.MEMBER_SELECT
            : "method selection of object.size() expected to be memberSelectTree, but is "
                + methodSelect.getKind();
        MemberSelectTree mst = (MemberSelectTree) methodSelect;
        Element elt = TreeUtils.elementFromTree(mst.getExpression());
        if (MustCallOnElementsAnnotatedTypeFactory.isCollection(elt, atypeFactory)) {
          return nameFromExpression(mst.getExpression());
        }
      } else if (condition.getRightOperand().getKind() == Tree.Kind.IDENTIFIER) {
        return nameFromExpression(condition.getRightOperand());
      }
    }
    return null;
  }

  /**
   * Get name from an ExpressionTree
   *
   * @param expr ExpressionTree
   * @return Name of the identifier the expression evaluates to or null if it doesn't
   */
  protected Name nameFromExpression(ExpressionTree expr) {
    if (expr == null) return null;
    switch (expr.getKind()) {
      case IDENTIFIER:
        return ((IdentifierTree) expr).getName();
      case ARRAY_ACCESS:
        return nameFromExpression(((ArrayAccessTree) expr).getExpression());
      case MEMBER_SELECT:
        Element elt = TreeUtils.elementFromUse((MemberSelectTree) expr);
        if (elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD) {
          return nameFromExpression(((MemberSelectTree) expr).getExpression());
        } else {
          return null;
        }
      case METHOD_INVOCATION:
        return nameFromExpression(((MethodInvocationTree) expr).getMethodSelect());
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

  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    ExpressionTree lhs = tree.getVariable();
    ExpressionTree rhs = tree.getExpression();
    Element lhsElt = TreeUtils.elementFromTree(lhs);
    Element rhsElt = TreeUtils.elementFromTree(rhs);

    /*
     * if RHS is new array, put the array size variable into a datastructure, s.t. it may be used as
     * a loop bound when pattern-matching a loop.
     */
    if (tree.getExpression() instanceof NewArrayTree) {
      NewArrayTree nat = (NewArrayTree) tree.getExpression();
      if (tree.getVariable() instanceof IdentifierTree) {
        IdentifierTree identifier = (IdentifierTree) tree.getVariable();
        Element arrayElement = TreeUtils.elementFromTree(identifier);
        if (arrayElement.getAnnotation(OwningArray.class) != null) {
          arrayInitializationSize.remove(identifier.getName());
          if (nat.getDimensions().size() == 1) {
            ExpressionTree dim = nat.getDimensions().get(0);
            if (dim instanceof IdentifierTree) {
              IdentifierTree dimVariable = (IdentifierTree) dim;
              Element varElement = TreeUtils.elementFromTree(dimVariable);
              Name varName = dimVariable.getName();
              if (ElementUtils.isEffectivelyFinal(varElement)) {
                arrayInitializationSize.put(identifier.getName(), varName);
              }
            }
          }
        }
      }
    }

    // This code implements the following rule:
    //  * It is always safe to assign a MustCallAlias parameter of a constructor
    //    to an owning field of the enclosing class.
    // It is necessary to special case this because MustCallAlias is translated
    // into @PolyMustCall, so the common assignment check will fail when assigning
    // an @MustCallAlias parameter to an owning field: the parameter is polymorphic,
    // but the field is not.
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
          AnnotationUtils.containsSameByClass(rhsElt.getAnnotationMirrors(), MustCallAlias.class);
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
              && !AnnotationUtils.containsSameByClass(
                  elt.getAnnotationMirrors(), PolyMustCall.class);
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
    // It does not make sense for receivers to have must-call obligations. If the receiver of a
    // method were to have a non-empty must-call obligation, then actually this method should
    // be part of the must-call annotation on the class declaration! So skipping this check is
    // always sound.
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
}
