package daikon.diff;

import java.io.*;
import java.util.*;
import daikon.*;
import daikon.inv.*;
import utilMDE.Assert;

/**
 * Maps ppts to lists of invariants.  Has an iterator to return the
 * ppts in the order they were inserted.
 * <p>
 * The ppts are used only as keys in this data structure.  Do not attempt
 * to look up invariants stored in the ppts; instead, obtain invariants via
 * the get() method.
 **/
public class InvMap implements Serializable {
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020301L;

  private Map<PptTopLevel,List<Invariant>> pptToInvs = new HashMap<PptTopLevel,List<Invariant>>();
  // The purpose of this field is apparently to permit the ppts to be
  // extracted in the same order in which they were inserted.
  private List<PptTopLevel> ppts = new ArrayList<PptTopLevel>();

  public InvMap() { }

  public void addPpt(PptTopLevel ppt) {
    put(ppt, new ArrayList<Invariant>());
  }

  public void put(PptTopLevel ppt, List<Invariant> invs) {
    if (ppts.contains(ppt)) {
      throw new Error("Tried to add duplicate PptTopLevel " + ppt.name());
    }
    ppts.add(ppt);
    pptToInvs.put(ppt, invs);
  }

  public void add(PptTopLevel ppt, Invariant inv) {
    if (! ppts.contains(ppt)) {
      throw new Error("ppt has not yet been added: " + ppt.name());
    }
    get(ppt).add(inv);
  }

  public List<Invariant> get(PptTopLevel ppt) {
    return pptToInvs.get(ppt);
  }

  /**
   * Returns an iterator over the ppts, in the order they were added to the
   * map.  Each element is a PptTopLevel.  These ppts are only used as
   * keys:  do not look in these Ppts to find the invariants associated
   * with them in the InvMap!  Use invariantIterator instead.
   * @see #invariantIterator()
   **/
  public Iterator<PptTopLevel> pptIterator() {
    return ppts.iterator();
  }

  // Returns a sorted iterator over the Ppts using c as the comparator
  public Iterator<PptTopLevel> pptSortedIterator(Comparator<PptTopLevel> c) {
    List<PptTopLevel> ppts_copy = new ArrayList<PptTopLevel>(ppts);
    Collections.sort(ppts_copy, c);
    return ppts_copy.iterator();
  }

  /**
   * Returns an iterator over the invariants in this.
   **/
  // The ppts are in the order added, and the invariants are in the order
  // added within each ppt, but the order of all invariants is not
  // necessarily that in which they were added, depending on calling
  // sequences.
  public Iterator<Invariant> invariantIterator() {
    ArrayList<Invariant> answer = new ArrayList<Invariant>();
    for (PptTopLevel ppt : ppts)
      answer.addAll(get(ppt));
    return answer.iterator();
  }

  public String toString() {
    String result = "";
    for (Iterator<PptTopLevel> i = pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      result += ppt.name() + Global.lineSep;
      List<Invariant> invs = get(ppt);
      for (Invariant inv : invs) {
        result += "  " + inv.format() + Global.lineSep;
      }
    }
    return result;
  }

  public int size() {
    int size1 = ppts.size();
    int size2 = pptToInvs.size();
    Assert.assertTrue(size1 == size2);
    return size1;
  }

}
