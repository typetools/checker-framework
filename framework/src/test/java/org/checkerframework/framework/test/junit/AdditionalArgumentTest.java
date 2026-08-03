package org.checkerframework.framework.test.junit;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.framework.util.typeinference8.constraint.AdditionalArgument;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.trees.TreeParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link AdditionalArgument} implements {@code equals} and {@code hashCode} in terms of
 * the method or constructor invocation it wraps, and that {@link ConstraintSet} therefore maintains
 * the invariant documented on its {@code list} field: "It does not contain constraints that are
 * equal."
 */
public class AdditionalArgumentTest {

  /** Parses expressions into trees. */
  private final TreeParser parser;

  /** Creates an {@code AdditionalArgumentTest}. */
  public AdditionalArgumentTest() {
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(new Context());
    parser = new TreeParser(env);
  }

  /**
   * Returns a new tree for the method invocation {@code m()}. Each call returns a distinct tree.
   *
   * @return a new tree for the method invocation {@code m()}
   */
  private ExpressionTree newInvocation() {
    return parser.parseTree("m()");
  }

  /** Two constraints for the same invocation are equal and have the same hash code. */
  @Test
  public void sameInvocationIsEqual() {
    ExpressionTree invocation = newInvocation();
    AdditionalArgument c1 = new AdditionalArgument(invocation);
    AdditionalArgument c2 = new AdditionalArgument(invocation);

    Assert.assertEquals(c1, c2);
    Assert.assertEquals(c2, c1);
    Assert.assertEquals(c1.hashCode(), c2.hashCode());
  }

  /** Two constraints for different invocations are not equal. */
  @Test
  public void differentInvocationIsNotEqual() {
    AdditionalArgument c1 = new AdditionalArgument(newInvocation());
    AdditionalArgument c2 = new AdditionalArgument(newInvocation());

    Assert.assertNotEquals(c1, c2);
  }

  /** {@link ConstraintSet#add} does not add a constraint that is equal to one already present. */
  @Test
  public void addDeduplicates() {
    ExpressionTree invocation = newInvocation();
    ConstraintSet set = new ConstraintSet();
    set.add(new AdditionalArgument(invocation));
    set.add(new AdditionalArgument(invocation));

    set.pop();
    Assert.assertTrue("ConstraintSet.add did not deduplicate equal constraints", set.isEmpty());
  }

  /** {@link ConstraintSet#push} does not add a constraint that is equal to one already present. */
  @Test
  public void pushDeduplicates() {
    ExpressionTree invocation = newInvocation();
    ConstraintSet set = new ConstraintSet();
    set.push(new AdditionalArgument(invocation));
    set.push(new AdditionalArgument(invocation));

    set.pop();
    Assert.assertTrue("ConstraintSet.push did not deduplicate equal constraints", set.isEmpty());
  }

  /**
   * {@link ConstraintSet#addAll(ConstraintSet)} does not add a constraint that is equal to one
   * already present.
   */
  @Test
  public void addAllDeduplicates() {
    ExpressionTree invocation = newInvocation();
    ConstraintSet set = new ConstraintSet(new AdditionalArgument(invocation));
    set.addAll(new ConstraintSet(new AdditionalArgument(invocation)));

    set.pop();
    Assert.assertTrue("ConstraintSet.addAll did not deduplicate equal constraints", set.isEmpty());
  }

  /** {@link ConstraintSet#remove} removes a constraint that is equal to one in the argument. */
  @Test
  public void removeUsesEquals() {
    ExpressionTree invocation = newInvocation();
    ConstraintSet set = new ConstraintSet(new AdditionalArgument(invocation));
    set.remove(new ConstraintSet(new AdditionalArgument(invocation)));

    Assert.assertTrue("ConstraintSet.remove did not remove an equal constraint", set.isEmpty());
  }
}
