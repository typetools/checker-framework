package org.checkerframework.afu.scenelib.el;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.TypePath;

/**
 * A {@link TypeASTMapper} traverses a client-maintained abstract syntax tree representing a type in
 * parallel with an {@link ATypeElement} from the annotation scene library, indicating corresponding
 * pairs of AST nodes and {@link AElement}s to the client so the client can process them in some
 * fashion.
 *
 * <p>To use {@link TypeASTMapper}, write a subclass for your particular AST. Implement {@link
 * #getElementType}, {@link #numTypeArguments}, and {@link #getTypeArgument} so that the mapper
 * knows how to traverse your AST; implement {@link #map} to perform whatever processing you desire
 * on each AST node and its corresponding {@link AElement}. Then, pass the root of your AST and the
 * corresponding {@link ATypeElement} from your annotation scene to {@link #traverse}.
 *
 * <p>{@link TypeASTMapper} itself saves no state, but subclasses may save state if they wish.
 *
 * <p>{@link ATypeElement} objects that will be traversed
 *
 * @param <N> common supertype of the AST nodes
 */
@SuppressWarnings("resourceleak:required.method.not.known") // Not relevant to resources
public abstract class TypeASTMapper<N> {
  /** Constructs a {@link TypeASTMapper}. A {@link TypeASTMapper} stores no state. */
  protected TypeASTMapper() {}

  private static ATypeElement getInnerType(ATypeElement te, List<TypePathEntry> ls) {
    if (ls.isEmpty()) {
      return te;
    } else {
      return te.innerTypes.getVivify(ls);
    }
  }

  /**
   * Traverses the type AST rooted at <code>tastRoot</code>, calling {@link #map} with each AST node
   * and the corresponding {@link AElement} from <code>aslRoot</code>.
   *
   * <p>If a node of the AST has no corresponding inner type in <code>aslRoot</code>, an inner type
   * {@link AElement} is vivified to hold any annotations that {@link #map} might wish to store in
   * it. Thus, a call to {@link #traverse} may write to <code>aslRoot</code> even if {@link #map}
   * does not write to its {@link AElement} argument. You may wish to {@linkplain AElement#prune
   * prune} <code>aslRoot</code> after traversal.
   */
  public void traverse(N tastRoot, ATypeElement aslRoot) {
    // Elements are added and removed from the end of this sole mutable
    // list during the traversal.
    List<TypePathEntry> ls = new ArrayList<>();
    traverse1(tastRoot, aslRoot, ls);
  }

  // "Sane": top-level or type argument
  private void traverse1(N n, ATypeElement te, List<TypePathEntry> ls) {
    N elType = getElementType(n);
    if (elType == null) {
      // no array, so the prefix corresponds to the type right here
      // System.out.printf("non-array: map(%s, getInnerType(%s, %s)=%s)%n",
      //                   n, te, ls, getInnerType(te, ls));
      map(n, getInnerType(te, ls));
      int nta = numTypeArguments(n);
      for (int tai = 0; tai < nta; tai++) {
        ls.add(TypePathEntry.create(TypePath.TYPE_ARGUMENT, tai));
        traverse1(getTypeArgument(n, tai), te, new ArrayList<>(ls));
        ls.remove(ls.size() - 1);
      }
    } else {
      // System.out.printf("array top-level: map(%s, getInnerType(%s, %s)=%s)%n",
      //                   n, te, ls, getInnerType(te, ls));
      map(n, getInnerType(te, ls));

      // at least one array layer to confuse us
      int layers = 0;
      while ((elType = getElementType(n)) != null) {
        ls.add(TypePathEntry.ARRAY_ELEMENT);
        // System.out.printf("layers=%d, map(%s, getInnerType(%s, %s)=%s)%n",
        //                   layers, elType, te, ls, getInnerType(te, ls));
        map(elType, getInnerType(te, ls));
        n = elType;
        layers++;
      }
      // // n is now the innermost element type
      // // map it to the prefix
      // map(n, getInnerType(te, ls));

      int nta = numTypeArguments(n);
      for (int tai = 0; tai < nta; tai++) {
        ls.add(TypePathEntry.create(TypePath.TYPE_ARGUMENT, tai));
        traverse1(getTypeArgument(n, tai), te, ls);
        ls.remove(ls.size() - 1);
      }

      for (int i = 0; i < layers; i++) {
        ls.remove(ls.size() - 1);
      }
    }
  }

  /**
   * If <code>n</code> represents an array type, {@link #getElementType} returns the node for the
   * element type of the array; otherwise it returns <code>null</code>.
   */
  protected abstract N getElementType(N n);

  /**
   * If <code>n</code> represents a parameterized type, {@link #numTypeArguments} returns the number
   * of type arguments; otherwise it returns 0.
   */
  protected abstract int numTypeArguments(N n);

  /**
   * Returns the node corresponding to the type argument of <code>n</code> (which must be a
   * parameterized type) at the given index. The caller must ensure that <code>
   * 0 &lt;= index &lt; {@link #numTypeArguments}(n)</code>.
   */
  protected abstract N getTypeArgument(N n, int index);

  /**
   * Signals to the client that <code>n</code> corresponds to <code>e</code>. The client may, for
   * example, set flags in <code>n</code> based on the annotations in <code>
   * e.{@link AElement#tlAnnotationsHere tlAnnotationsHere}</code>. The {@link TypeASTMapper} calls
   * {@link #map} on <code>n</code> before it calls {@link #map} on sub-nodes of <code>n</code> but
   * not necessarily before it explores the structure of <code>n</code>'s subtree.
   */
  protected abstract void map(N n, ATypeElement e);
}
