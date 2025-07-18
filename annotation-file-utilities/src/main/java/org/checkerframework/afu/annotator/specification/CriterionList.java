package org.checkerframework.afu.annotator.specification;

import org.checkerframework.afu.annotator.find.Criteria;
import org.checkerframework.afu.annotator.find.Criterion;

// The notion of a CriterionList as a list of independent elements (with the
// list being satisfied if each of its elements is) is broken.  For example, a
// class may have an inner class of the same name, and the list would be
// satisfied for either one.  The same goes for method names, and (most
// problematically, because it comes up most often) for relative location in
// GenericArrayLocationCriterion, where the criterion [1] would also match [2
// 1] because it is a suffix of it, or [2 1] would also match [3 2 1].  A
// related problem is the idea of checking from the bottom to the top of a
// TreePath whether it satisfies a criterion.  I have put into place piecemeal
// fixes for some of these, but really the CriterionList needs to be turned
// into a path that is followed, and checking needs to start at the top of the
// tree (at the CompilationUnit). -MDE 9/2009

/**
 * A CriterionList is a singly-linked list of Criterion meant to be treated as a stack. It is useful
 * for creating base criteria and passing independent copies to different parts of a specification
 * that creates all the criterion. A CriterionList is immutable, and so copies created by the add()
 * function can safely be passed anywhere. It is supposed to be easier to manipulate than a
 * Criteria.
 */
public class CriterionList {
  // This really is a simple data structure to facilitate creation
  //  of specifications.  TODO: make it a private class?
  private Criterion current;
  private CriterionList next;

  /** Creates a new CriterionList with no criterion. */
  public CriterionList() {
    next = null;
    current = null;
  }

  /**
   * Creates a new CriterionList containing just the given Criterion.
   *
   * @param c the sole criterion the list contains at the moment
   */
  public CriterionList(Criterion c) {
    current = c;
    next = null;
  }

  private CriterionList(Criterion c, CriterionList n) {
    current = c;
    next = n;
  }

  /**
   * Adds the given criterion to the present list and returns a newly-allocated list containing the
   * result. Does not modify its argument.
   *
   * @param c the criterion to add
   * @return a new list containing the given criterion and the rest of the criterion already in this
   *     list
   */
  public CriterionList add(Criterion c) {
    return new CriterionList(c, this);
  }

  /**
   * Creates a Criteria object representing all the criterion in this list.
   *
   * @return a Criteria that contains all the criterion in this list
   */
  public Criteria criteria() {
    Criteria criteria = new Criteria();

    CriterionList c = this;
    while (c != null && c.current != null) {
      criteria.add(c.current);
      c = c.next;
    }

    return criteria;
  }

  @Override
  public String toString() {
    if (current == null) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[").append(current);
    for (CriterionList n = next; n.next != null; n = n.next) {
      sb.append(", ").append(n.current);
    }
    return sb.append("]").toString();
  }
}
