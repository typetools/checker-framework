package org.checkerframework.checker.lock;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.Kind;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.SynchronizedNode;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

/**
 * LockTransfer handles constructors, initializers, synchronized methods, and synchronized blocks.
 */
public class LockTransfer extends CFAbstractTransfer<CFValue, LockStore, LockTransfer> {
  /** The type factory associated with this transfer function. */
  private final LockAnnotatedTypeFactory atypeFactory;

  /**
   * Create a transfer function for the Lock Checker.
   *
   * @param analysis the analysis this transfer function belongs to
   * @param checker the type-checker this transfer function belongs to
   */
  public LockTransfer(LockAnalysis analysis, LockChecker checker) {
    // Always run the Lock Checker with -AconcurrentSemantics turned on.
    super(analysis, /*useConcurrentSemantics=*/ true);
    this.atypeFactory = (LockAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  /**
   * Sets a given {@link Node} to @LockHeld in the given {@code store}.
   *
   * @param store the store to update
   * @param node the node that should be @LockHeld
   */
  protected void makeLockHeld(LockStore store, Node node) {
    JavaExpression internalRepr = JavaExpression.fromNode(node);
    store.insertValue(internalRepr, atypeFactory.LOCKHELD);
  }

  /**
   * Sets a given {@link Node} to @LockPossiblyHeld in the given {@code store}.
   *
   * @param store the store to update
   * @param node the node that should be @LockPossiblyHeld
   */
  protected void makeLockPossiblyHeld(LockStore store, Node node) {
    JavaExpression internalRepr = JavaExpression.fromNode(node);

    // insertValue cannot change an annotation to a less
    // specific type (e.g. LockHeld to LockPossiblyHeld),
    // so insertLockPossiblyHeld is called.
    store.insertLockPossiblyHeld(internalRepr);
  }

  /** Sets a given {@link Node} {@code node} to LockHeld in the given {@link TransferResult}. */
  protected void makeLockHeld(TransferResult<CFValue, LockStore> result, Node node) {
    if (result.containsTwoStores()) {
      makeLockHeld(result.getThenStore(), node);
      makeLockHeld(result.getElseStore(), node);
    } else {
      makeLockHeld(result.getRegularStore(), node);
    }
  }

  /**
   * Sets a given {@link Node} {@code node} to LockPossiblyHeld in the given {@link TransferResult}.
   */
  protected void makeLockPossiblyHeld(TransferResult<CFValue, LockStore> result, Node node) {
    if (result.containsTwoStores()) {
      makeLockPossiblyHeld(result.getThenStore(), node);
      makeLockPossiblyHeld(result.getElseStore(), node);
    } else {
      makeLockPossiblyHeld(result.getRegularStore(), node);
    }
  }

  @Override
  public LockStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {

    LockStore store = super.initialStore(underlyingAST, parameters);

    Kind astKind = underlyingAST.getKind();

    // Methods with the 'synchronized' modifier are
    // holding the 'this' lock.

    // There is a subtle difference between synchronized methods
    // and constructors/initializers. A synchronized method is only
    // taking the intrinsic lock of the current object. It says nothing
    // about any fields of the current object.

    // Furthermore, since the current object already exists,
    // other objects may be guarded by the current object. So
    // a synchronized method can affect the locking behavior of other
    // objects.

    // A constructor/initializer behaves as if the current object
    // and all its non-static fields were held as locks. But in
    // reality no locks are held.

    // Furthermore, since the current object is being constructed,
    // no other object can be guarded by it or any of its non-static
    // fields.

    // Handle synchronized methods and constructors.
    if (astKind == Kind.METHOD) {
      CFGMethod method = (CFGMethod) underlyingAST;
      MethodTree methodTree = method.getMethod();

      ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodTree);

      if (methodElement.getModifiers().contains(Modifier.SYNCHRONIZED)) {
        final ClassTree classTree = method.getClassTree();
        TypeMirror classType = TreeUtils.typeOf(classTree);

        if (methodElement.getModifiers().contains(Modifier.STATIC)) {
          store.insertValue(new ClassName(classType), atypeFactory.LOCKHELD);
        } else {
          store.insertThisValue(atypeFactory.LOCKHELD, classType);
        }
      } else if (methodElement.getKind() == ElementKind.CONSTRUCTOR) {
        store.setInConstructorOrInitializer();
      }
    } else if (astKind == Kind.ARBITRARY_CODE) { // Handle initializers
      store.setInConstructorOrInitializer();
    }

    return store;
  }

  @Override
  public TransferResult<CFValue, LockStore> visitSynchronized(
      SynchronizedNode n, TransferInput<CFValue, LockStore> p) {

    TransferResult<CFValue, LockStore> result = super.visitSynchronized(n, p);

    // Handle the entering and leaving of the synchronized block
    if (n.getIsStartOfBlock()) {
      makeLockHeld(result, n.getExpression());
    } else {
      makeLockPossiblyHeld(result, n.getExpression());
    }

    return result;
  }
}
