package org.checkerframework.framework.util.typeinference8.bound;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.CaptureVariable;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A bound of the form: {@code G<a1, ..., an> = capture(G<A1, ..., An>)}. The variables a1, ..., an
 * represent the result of capture conversion applied to {@code G<A1, ..., An>} (where A1, ..., An
 * may be types or wildcards and may mention inference variables).
 */
public class CaptureBound {
  /** {@code G<A1, ..., An>} sometimes called the right hand side */
  private final AbstractType capturedType;

  /**
   * The substitution [P1 := alpha1, ..., Pn := alphan] where P1, ..., Pn are the type parameters of
   * the underlying type, G.
   */
  private final Theta map;

  /** {@code G<a1, ..., an>} */
  private final InferenceType lhs;

  /** A list of {@link CaptureTuple}s. */
  private final List<CaptureTuple> tuples = new ArrayList<>();

  /**
   * All capture variables in this capture. For example, a1 in {@code G<a1, ..., an> = capture(G<A1,
   * ..., An>)}.
   */
  private final List<CaptureVariable> captureVariables = new ArrayList<>();

  public CaptureBound(
      AbstractType capturedType, ExpressionTree tree, Java8InferenceContext context) {
    this.capturedType = capturedType;
    DeclaredType underlying = (DeclaredType) capturedType.getJavaType();
    TypeElement ele = TypesUtils.getTypeElement(underlying);
    this.map = context.inferenceTypeFactory.createThetaForCapture(tree, capturedType);

    lhs = (InferenceType) context.inferenceTypeFactory.getTypeOfElement(ele, map);

    Iterator<Variable> alphas = this.map.values().iterator();
    Iterator<AbstractType> args = capturedType.getTypeArguments().iterator();
    for (TypeParameterElement pEle : ele.getTypeParameters()) {
      AbstractType Bi = context.inferenceTypeFactory.getTypeOfBound(pEle, map);
      AbstractType Ai = args.next();

      CaptureVariable alphai = (CaptureVariable) alphas.next();
      captureVariables.add(alphai);
      alphai.initialBounds(map);

      tuples.add(CaptureTuple.of(alphai, Ai, Bi));
    }
  }

  /**
   * Incorporate this capture bound. See JLS 18.3.1.
   *
   * <p>Also, reduces and incorporates the constraint {@code G<a1,...,an> -> target}. See JLS
   * 18.5.2.1.
   */
  public BoundSet incorporate(AbstractType target, Java8InferenceContext context) {
    // First add the non-wildcard bounds.
    for (CaptureTuple t : tuples) {
      if (t.capturedTypeArg.getTypeKind() != TypeKind.WILDCARD) {
        // If Ai is not a wildcard, then the bound alphai = Ai is implied.
        t.alpha.getBounds().addBound(VariableBounds.BoundKind.EQUAL, t.capturedTypeArg);
      }
    }

    ConstraintSet set = new ConstraintSet(new Typing(lhs, target, Kind.TYPE_COMPATIBILITY));
    // Reduce and incorporate so that the capture variables bounds are set.
    BoundSet b1 = set.reduce(context);
    b1.incorporateToFixedPoint(new BoundSet(context));

    // Then create constraints implied by captured type args that are wildcards.
    boolean containsFalse = false;
    for (CaptureTuple t : tuples) {
      if (t.capturedTypeArg.getTypeKind() == TypeKind.WILDCARD) {
        ConstraintSet newCon = t.alpha.getWildcardConstraints(t.capturedTypeArg, t.bound);
        if (newCon == null) {
          containsFalse = true;
        } else {
          set.addAll(newCon);
        }
      }
    }

    // Reduce and incorporate again.
    BoundSet b2 = set.reduce(context);
    b2.addCapture(this);
    if (containsFalse) {
      b2.addFalse();
    }
    b1.incorporateToFixedPoint(b2);
    return b1;
  }

  /**
   * Return all variables on the left-hand side of this capture.
   *
   * @return all variables on the left-hand side of this capture
   */
  public List<? extends CaptureVariable> getAllVariablesOnLHS() {
    return captureVariables;
  }

  /**
   * Return all variables on the right-hand side of this capture.
   *
   * @return all variables on the right-hand side of this capture
   */
  public LinkedHashSet<Variable> getAllVariablesOnRHS() {
    return new LinkedHashSet<>(capturedType.getInferenceVariables());
  }

  public boolean isCaptureMentionsAny(Collection<Variable> as) {
    for (Variable a : as) {
      if (map.containsValue(a)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For a capture of the form: {@code G<a1, ..., an> = capture(G<A1, ..., An>)}, a capture tuple
   * groups ai, Ai, and the upper bound of the corresponding type variable.
   */
  private static class CaptureTuple {

    /**
     * Fresh inference variable (in the left hand side of the capture). (Also referred to as beta in
     * the some places in the JLS.) For example {@code a1} in {@code G<a1, ..., an> = capture(G<A1,
     * ..., An>)}.
     */
    public final CaptureVariable alpha;

    /**
     * Type argument in the right hand side for the capture. For example {@code A1} in {@code G<a1,
     * ..., an> = capture(G<A1, ..., An>)}.
     */
    public final AbstractType capturedTypeArg;

    /**
     * Upper bound of one of the type parameters of G that has been substituted using the fresh
     * inference variables.
     */
    public final AbstractType bound;

    private CaptureTuple(CaptureVariable alpha, AbstractType capturedTypeArg, AbstractType bound) {
      this.alpha = alpha;
      this.capturedTypeArg = capturedTypeArg;
      this.bound = bound;
    }

    public static CaptureTuple of(
        CaptureVariable alpha, AbstractType capturedTypeArg, AbstractType bound) {
      return new CaptureTuple(alpha, capturedTypeArg, bound);
    }
  }
}
