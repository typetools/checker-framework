package daikon;

import daikon.split.*;
import daikon.inv.*;
import daikon.suppress.*;
import utilMDE.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;


/**
 * PptSplitter contains the splitter and its associated
 * PptConditional ppts.  Currently all splitters are binary and this
 * is presumed in the implementation.  However, this could easily
 * be extended by extending this class with specific other implementations.
 */
public class PptSplitter implements Serializable {

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20031031L;

  /**
   * Integer. A value of zero indicates that DummyInvariant objects should
   * not be created. A value of one indicates that dummy invariants
   * should be created only when no suitable condition was found in
   * the regular output. A value of two indicates that dummy
   * invariants should be created for each splitting condition.
   **/
  public static int dkconfig_dummy_invariant_level = 0;

  /**
   * Split bi-implications into two separate invariants.
   **/
  public static boolean dkconfig_split_bi_implications = false;

  /** General debug tracer. **/
  public static final Logger debug = Logger.getLogger ("daikon.PptSplitter");

  /** PptTopLevel that contains this split. */
  private PptTopLevel parent;

  /** Splitter that choses to which PptConditional a sample is applied. */
  public transient Splitter splitter;

  /**
   * PptConditionals for each splitter output.  ppts[0] is used
   * when the splitter is true, ppts[1] when the splitter is false.  The
   * contents are PptConditional objects if the splitter is valid, but are
   * PptTopLevel if the PptSplitter represents two exit points (for which
   * no splitter is required).
   **/
  public PptTopLevel[] ppts = new PptTopLevel[2];

  private static final Comparator<Invariant> icfp
                            = new Invariant.InvariantComparatorForPrinting();

  /**
   * Create a binary PptSplitter with the specied splitter for the specified
   * PptTopLevel parent.  The parent should be a leaf (i.e., a numbered
   * exit point)
   */
  public PptSplitter (PptTopLevel parent, Splitter splitter) {

    this.parent = parent;
    this.splitter = splitter;
    ppts[0] = new PptConditional (parent, splitter, false);
    ppts[1] = new PptConditional (parent, splitter, true);

    if (Debug.logDetail()) {
      debug.fine ("VarInfos for " + parent.name());
      for (int ii = 0; ii < parent.var_infos.length; ii++)
        debug.fine (parent.var_infos[ii].name() + " "
                            + ppts[0].var_infos[ii].name() + " "
                            + ppts[1].var_infos[ii].name());
    }
  }

  /**
   * Creates a PptSplitter over two exit points.  No splitter is required.
   */
  public PptSplitter (PptTopLevel parent, PptTopLevel exit1,
                      PptTopLevel exit2) {
    this.parent = parent;
    this.splitter = null;
    ppts[0] = exit1;
    ppts[1] = exit2;
  }


  /**
   * Returns true if the splitter is valid at this point, false otherwise.
   */
  public boolean splitter_valid() {

    Assert.assertTrue (((PptConditional)ppts[1]).splitter_valid()
                       == ((PptConditional) ppts[0]).splitter_valid());
    return ((PptConditional) ppts[0]).splitter_valid();
  }

  /** Adds the sample to each conditional ppt in the split. */
  public void add_bottom_up (ValueTuple vt, int count) {

    // Choose the appropriate conditional point based on the condition result
    PptConditional ppt_cond = choose_conditional (vt);
    if (ppt_cond == null)
      return;

/// ??? MDE
    // If any parent variables were missing out of bounds on this
    // sample, apply that to this conditional as well.  A more
    // efficient way to do this would be better.
    ppt_cond.get_missingOutOfBounds (parent, vt);

    // Add the point
    ppt_cond.add_bottom_up (vt, count);

    if (Debug.logDetail() && Debug.ppt_match (ppt_cond)) {
      System.out.println ("Adding sample to " + ppt_cond + " with vars "
                          + Debug.related_vars (ppt_cond, vt));
    }

  }

  /**
   * Chooses the correct conditional point based on the values in this sample.
   */
  public PptConditional choose_conditional (ValueTuple vt) {

    boolean splitter_test;
    try {
      splitter_test = ((PptConditional)ppts[0]).splitter.test(vt);
    } catch (Throwable e) {
      // If an exception is thrown, don't put the data on either side
      // of the split.
      if (false) {              // need to add a debugging switch
        System.out.println ("Exception thrown in "
          + "PptSplitter.choose_conditional() for " + ppts[0].name());
        System.out.println ("Vars = " + Debug.related_vars (ppts[0], vt));
      }
      return (null);
    }

    // Choose the appropriate conditional point based on the condition result
    return ((PptConditional) ppts[splitter_test ? 0 : 1]);
  }

  /**
   * Adds implication invariants based on the invariants found on each
   * side of the split
   */
  public void add_implications() {

    // Currently only binary implications are supported
    Assert.assertTrue (ppts.length == 2);

    // Create any NIS suppressed invariants in each conditional
    @SuppressWarnings("unchecked")
    List<Invariant> suppressed_invs[] = (ArrayList<Invariant>[]) new ArrayList[ppts.length];
    for (int i = 0; i < ppts.length; i++)
      suppressed_invs[i] = NIS.create_suppressed_invs (ppts[i]);

    add_implications_pair ();

    // Remove all of the NIS suppressed invariants that we previously created
    for (int i = 0; i < ppts.length; i++)
      ppts[i].remove_invs (suppressed_invs[i]);
  }

  /**
   * Given a pair of conditional program points, form implications from the
   * invariants true at each one.  The algorithm divides the invariants
   * into two groups:
   * <ol>
   *   <li>the "same" invariants are true at both program points, and
   *   <li>the "different" invariants are all other invariants.
   * </ol>
   * The "exclusive" invariants (a subset of the "different" inviariants)
   * are true at one program point, and their negation is true at the other
   * program point
   * At the first program point, for each exclusive invariant and each
   * different invariant, create a conditional of the form "exclusive =>
   * different".  Do the same at the second program point.
   * <p>
   *
   * This method is correct only if the two conditional program points
   * fully partition the input space (their conditions are negations of one
   * another).  For instance, suppose there is a three-way split with the
   * following invariants detected at each:
   * <pre>
   *   {A,B}  {!A,!B}  {A,!B}
   * </pre>
   * Examining just the first two would suggest that "A <=> B" is valid,
   * but in fact that is a false inference.  Note that this situation can
   * occur if the splitting condition uses variables that can ever be missing.
   */
  private void add_implications_pair () {

    for (PptTopLevel pchild : ppts) {
      // System.out.printf ("splitter child = %s%n", pchild.name());
      assert pchild.equality_view != null;
    }

    debug.fine ("Adding Implications for " + parent.name);

    // Maps permuted invariants to their original invariants
    Map<Invariant,Invariant> orig_invs = new LinkedHashMap<Invariant,Invariant>();

    Vector<Invariant> same_invs_vec = new Vector<Invariant>();

    Vector<Invariant[]> exclusive_invs_vec = new Vector<Invariant[]>();

    Vector<Invariant[]> different_invs_vec = new Vector<Invariant[]>();

/// ??? MDE
    // Loop through each possible parent slice
    List<VarInfo[]> slices = possible_slices();

    for (VarInfo[] vis : slices) {

      int num_children = ppts.length;
      // Each element is an invariant from the indexth child, permuted to
      // the parent (and with a parent slice as its ppt slot).
      Invariants[] invs = new Invariants[num_children];

      // find the parent slice
      PptSlice pslice = parent.get_or_instantiate_slice (vis);

      // Daikon.debugProgress.fine ("    slice: " + pslice.name());

      // Loop through each child ppt
      for (int childno = 0; childno < num_children; childno++) {
        PptTopLevel child_ppt = ppts[childno];

        assert child_ppt.equality_view != null : child_ppt.name();
        assert parent.equality_view != null : parent.name();

        invs[childno] = new Invariants(); // permuted to parent

        // Get the child vis in the correct order
        VarInfo[] cvis_non_canonical = new VarInfo[vis.length];
        VarInfo[] cvis = new VarInfo[vis.length];
        VarInfo[] cvis_sorted = new VarInfo[vis.length];
        for (int kk = 0; kk < vis.length; kk++) {
          cvis_non_canonical[kk] = matching_var (child_ppt, parent, vis[kk]);
          cvis[kk] = cvis_non_canonical[kk].canonicalRep();
          cvis_sorted[kk] = cvis[kk];
        }
        Arrays.sort (cvis_sorted, VarInfo.IndexComparator.getInstance());

        // Look for an equality invariant in the non-canonical slice (if any).
        // Note that only an equality invariant can exist in a non-canonical
        // slice.  If it does exist, we want it rather than the canonical
        // form (which for equality invariants will always be of the form
        // 'a == a').
        Invariant eq_inv = null;
        if (!Arrays.equals (cvis_non_canonical, cvis)) {
          PptSlice nc_slice = child_ppt.findSlice (cvis_non_canonical);
          if (nc_slice != null) {
            if (nc_slice.invs.size() != 1) {
              // Impossible: multiple invariants found
              System.out.println ("Found " + nc_slice.invs.size() +
                                  " invs at " + nc_slice);
              for (Invariant inv2 : nc_slice.invs)
                System.out.println (" -- inv = " + inv2);
              for (VarInfo cvi : cvis_non_canonical)
                System.out.println (" -- equality set = " +
                      cvi.equalitySet.shortString());
              throw new Error("nc_slice.invs.size() == " + nc_slice.invs.size());
            }
            eq_inv = nc_slice.invs.get (0);
            debug.fine ("Found eq inv " + eq_inv);
          }
        }

        // Find the corresponding slice
        PptSlice cslice = child_ppt.findSlice (cvis_sorted);
        if (cslice == null) {
          if (eq_inv != null) {
            for (int i = 0; i < cvis_sorted.length; i++)
              Fmt.pf ("con val = " + child_ppt.constants.all_vars[cvis_sorted[i].varinfo_index]);
            throw new RuntimeException("found eq_inv " + eq_inv + " @"
                                + eq_inv.ppt + " but can't find slice for "
                                + VarInfo.toString (cvis_sorted));
          }
          continue;
        }

        // Copy each invariant permuted to the parent.
        // This permits them to be directly compared to one another.
        int[] permute = PptTopLevel.build_permute (cvis_sorted, cvis);
        for (Invariant orig_inv : cslice.invs) {
          Invariant inv = orig_inv.clone_and_permute (permute);
          inv.ppt = pslice;
          invs[childno].add (inv);
          if ((eq_inv != null) && orig_inv.getClass().equals(eq_inv.getClass()))
            orig_inv = eq_inv;
          Assert.assertTrue (! orig_invs.containsKey (inv));
          orig_invs.put (inv, orig_inv);
        }
      } // children loop


      // If neither child slice has invariants there is nothing to do
      if ((invs[0].size() == 0) && (invs[1].size() == 0)) {
        if (pslice.invs.size() == 0)
          parent.removeSlice (pslice);
        continue;
      }


      if ((pslice.invs.size() == 0) && Debug.logDetail())
        debug.fine ("PptSplitter: created new slice " +
                            VarInfo.toString (vis) + " @" + parent.name);

      // Add any exclusive conditions for this slice to the list
      exclusive_invs_vec.addAll(exclusive_conditions(invs[0], invs[1]));

      // Add any invariants that are the same to the list
      same_invs_vec.addAll (same_invariants (invs[0], invs[1]));

      // Add any invariants that are different to the list
      different_invs_vec.addAll (different_invariants (invs[0], invs[1]));


    } // slices.iterator() loop

    if (Debug.logOn() || debug.isLoggable (Level.FINE)) {
      debug.fine ("Found " + exclusive_invs_vec.size()
                  + " exclusive conditions ");
      for (Invariant[] invs : exclusive_invs_vec) {
        invs[0].log ("exclusive condition with " + invs[1].format());
        invs[1].log ("exclusive condition with " + invs[0].format());
        debug.fine ("-- " + invs[0] + " -- " + invs[1]);
      }
      debug.fine ("Found " + different_invs_vec.size() + " different invariants ");
      for (Invariant[] invs : different_invs_vec) {
        if (invs[0] != null)
          invs[0].log (invs[0] + " differs from "  + invs[1]);
        if (invs[1] != null)
          invs[1].log (invs[0] + " differs from "  + invs[1]);
        debug.fine ("-- " + invs[0] + " -- " + invs[1]);
      }
    }

    PptTopLevel ppt1 = ppts[0];
    PptTopLevel ppt2 = ppts[1];

    // Add the splitting condition as an exclusive condition if requested
    if ((splitter != null) && dkconfig_dummy_invariant_level > 0) {
      if (exclusive_invs_vec.size() == 0
          || dkconfig_dummy_invariant_level >= 2) {
        // As a last resort, try using the user's supplied DummyInvariant
        debug.fine ("addImplications: resorting to dummy");
        PptConditional cond1 = (PptConditional)ppt1;
        PptConditional cond2 = (PptConditional)ppt2;
        cond1.splitter.instantiateDummy(ppt1);
        cond2.splitter.instantiateDummy(ppt2);
        DummyInvariant dummy1 = cond1.dummyInvariant();
        DummyInvariant dummy2 = cond2.dummyInvariant();
        if (dummy1 != null && dummy1.valid && dummy2 != null && dummy2.valid) {
          Assert.assertTrue(!cond1.splitter_inverse);
          Assert.assertTrue(cond2.splitter_inverse);
          dummy2.negate();
          Invariant[] dummy_pair = new Invariant[] {dummy1, dummy2};
          exclusive_invs_vec.add(dummy_pair);
          different_invs_vec.add(dummy_pair);
        }
      }
    }


    // If there are no exclusive conditions, we can do nothing here
    if (exclusive_invs_vec.size() == 0) {
      if (debug.isLoggable(Level.FINE)) {
        debug.fine ("addImplications: no exclusive conditions");
      }
      return;
    }

    // Remove exclusive invariants from the different invariants list
    // It would be better not to have added them in the first place,
    // but this is easier for now.
    for (Iterator<Invariant[]> ii = different_invs_vec.iterator(); ii.hasNext(); ) {
      Invariant[] diff_invs = ii.next();
      if (diff_invs[0] != null) {
        Assert.assertTrue (diff_invs[1] == null);
        // debug.fine ("Considering inv0 " + diff_invs[0]);
        for (Invariant[] ex_invs : exclusive_invs_vec) {
          if (ex_invs[0] == diff_invs[0]) {
            debug.fine ("removed exclusive invariant " + ex_invs[0]);
            ii.remove();
            break;
          }
        }
      } else {
        Assert.assertTrue (diff_invs[1] != null);
        // debug.fine ("Considering inv1 " + diff_invs[1]);
        for (Invariant[] ex_invs : exclusive_invs_vec) {
          if (ex_invs[1] == diff_invs[1]) {
            debug.fine ("removed exclusive invariant " + ex_invs[1]);
            ii.remove();
            break;
          }
        }
      }
    }

    // Get the canonical predicate invariants from the exclusive list.
    // We pick the first one that is neither obvious or suppressed.
    // If all are either obvious or suppressed, we just pick the first
    // one in the list
    Invariant[] con_invs = new Invariant[2];
    for (Invariant[] invs : exclusive_invs_vec) {
      for (int jj = 0; jj < con_invs.length; jj++) {
        if (con_invs[jj] == null) {
          Invariant orig = orig_invs.get (invs[jj]);
          if ((orig.isObvious() == null) && !orig.is_ni_suppressed())
            con_invs[jj] = invs[jj];
        }
      }
    }
    Invariant[] first = exclusive_invs_vec.get(0);
    for (int jj = 0; jj < con_invs.length; jj++) {
      if (con_invs[jj] == null) {
        System.out.println ("Warning: No non-obvious non-suppressed exclusive"
                            + " invariants found in " + parent.name);
        // Assert.assertTrue (false);
        con_invs[jj] = first[jj];
      }
    }

    // Create double-implications for each exclusive invariant
    for (Invariant[] invs : exclusive_invs_vec) {
      for (int jj = 0; jj < con_invs.length; jj++) {
        if (con_invs[jj] != invs[jj])
          add_implication (parent, con_invs[jj], invs[jj], true, orig_invs);
      }
    }

    // Create single implication for each different invariant
    for (Invariant[] invs : different_invs_vec) {
      for (int jj = 0; jj < con_invs.length; jj++) {
        if (invs[jj] != null)
          add_implication (parent, con_invs[jj], invs[jj], false, orig_invs);
      }
    }

  } // add_implications_pair


  /**
   * Returns a list of all possible slices that may appear at the parent.
   * The parent must have already been created by merging the invariants
   * from its child conditionals.
   *
   * This is different from the slices that actually exist at the parent
   * because there may be implications created from invariants in child
   * slices that only exist in one child (and thus don't exists in the parent)
   * because there may be implications created from invariants in child
   * slices that only exist in one child.
   **/
  private List<VarInfo[]> possible_slices() {

    List<VarInfo[]> result = new ArrayList<VarInfo[]>();

    // Get an array of leaders at the parent to build slices over
    VarInfo[] leaders = parent.equality_view.get_leaders_sorted();

    // Create unary views
    for (int i = 0; i < leaders.length; i++) {
      if (parent.is_slice_ok (leaders[i])) {
        result.add (new VarInfo[] {leaders[i]});
      }
    }

    // Create binary views
    for (int i = 0; i < leaders.length; i++) {
      for (int j = i; j < leaders.length; j++) {
        if (parent.is_slice_ok (leaders[i], leaders[j]))
          result.add (new VarInfo[] {leaders[i], leaders[j]});
      }
    }

/// Expensive!
/// ??? MDE
    // Create ternary views
    for (int i = 0; i < leaders.length; i++) {
      for (int j = i; j < leaders.length; j++) {
        for (int k = j; k < leaders.length; k++) {
          if (parent.is_slice_ok (leaders[i], leaders[j], leaders[k]))
            result.add (new VarInfo[] {leaders[i], leaders[j], leaders[k]});
        }
      }
    }

    return (result);
  }


  // Could be used in assertion that all invariants are at same point.
  private boolean at_same_ppt(Invariants invs1, Invariants invs2) {
    PptSlice ppt = null;
    Iterator<Invariant> itor = new UtilMDE.MergedIterator2<Invariant>(invs1.iterator(), invs2.iterator());
    for (; itor.hasNext(); ) {
      Invariant inv = itor.next();
      if (ppt == null) {
        ppt = inv.ppt;
      } else {
        if (inv.ppt != ppt)
          return false;
      }
    }
    return true;
  }


  /**
   * Determine which elements of invs1 are mutually exclusive with
   * elements of invs2.  Result elements are pairs of Invariants.
   * All the arguments should be over the same program point.
   */
  Vector<Invariant[]> exclusive_conditions (Invariants invs1,
                                                 Invariants invs2) {

    Vector<Invariant[]> result = new Vector<Invariant[]>();
    for (Invariant inv1 : invs1) {
      for (Invariant inv2 : invs2) {
        // // This is a debugging tool, to make sure that various versions
        // // of isExclusiveFormula remain coordinated.  (That's also one
        // // reason we don't break out of the loop early:  also, there will
        // // be few invariants in a slice, so breaking out is of minimal
        // // benefit.)
        // Assert.assertTrue(inv1.isExclusiveFormula(inv2)
        //                  == inv2.isExclusiveFormula(inv1),
        //              "Bad exclusivity: " + inv1.isExclusiveFormula(inv2)
        //               + " " + inv2.isExclusiveFormula(inv1)
        //               + "    " + inv1.format() + "    " + inv2.format());
        if (inv1.isExclusiveFormula(inv2)) {
          result.add(new Invariant[] { inv1, inv2 });
        }
      }
    }
    return result;
  }

  /**
   * Determine which elements of invs1 differ from elements of invs2.
   * Result elements are pairs of Invariants (with one or the other
   * possibly null).
   * All the arguments should be over the same program point.
   */
  Vector<Invariant[]> different_invariants (Invariants invs1,
                                            Invariants invs2) {
    SortedSet<Invariant> ss1 = new TreeSet<Invariant>(icfp);
    ss1.addAll(invs1);
    SortedSet<Invariant> ss2 = new TreeSet<Invariant>(icfp);
    ss2.addAll(invs2);
    Vector<Invariant[]> result = new Vector<Invariant[]>();
    for (OrderedPairIterator<Invariant> opi = new OrderedPairIterator<Invariant>(ss1.iterator(),
                                    ss2.iterator(), icfp);
         opi.hasNext(); ) {
      Pair<Invariant,Invariant> pair = opi.next();
      if ((pair.a == null) || (pair.b == null)
          // || (icfp.compare(pair.a, pair.b) != 0)
          ) {
        result.add(new Invariant[] { pair.a, pair.b });
      }
    }
    return result;
  }


  /**
   * Determine which elements of invs1 are the same as elements of invs2.
   * Result elements are Invariants (from the invs1 list)
   * All the arguments should be over the same program point.
   */
  Vector<Invariant> same_invariants(Invariants invs1, Invariants invs2) {

    SortedSet<Invariant> ss1 = new TreeSet<Invariant>(icfp);
    ss1.addAll(invs1);
    SortedSet<Invariant> ss2 = new TreeSet<Invariant>(icfp);
    ss2.addAll(invs2);
    Vector<Invariant> result = new Vector<Invariant>();
    for (OrderedPairIterator<Invariant> opi = new OrderedPairIterator<Invariant>(ss1.iterator(),
                                    ss2.iterator(), icfp);
         opi.hasNext(); ) {
      Pair pair = opi.next();
      if (pair.a != null && pair.b != null) {
        Invariant inv1 = (Invariant) pair.a;
        Invariant inv2 = (Invariant) pair.b;
        result.add(inv1);
      }
    }
    return result;
  }

  /**
   * If the implication specified by predicate and consequent
   * is a valid implication, adds it to the joiner view of
   * parent.
   * @param orig_invs Maps permuted invariants to their original invariants
   **/
  public void add_implication (PptTopLevel ppt, Invariant predicate,
                               Invariant consequent, boolean iff,
                               Map<Invariant,Invariant> orig_invs) {

    Assert.assertTrue (predicate != null);
    Assert.assertTrue (consequent != null);

    Invariant orig_pred = orig_invs.get (predicate);
    Invariant orig_cons = orig_invs.get (consequent);
    Assert.assertTrue (orig_pred != null);
    Assert.assertTrue (orig_cons != null);

    // Don't add consequents that are obvious or suppressed.
    // JHP: Jan 2005: It might be better to create them anyway and
    // only suppress them in printing.  Also, this could possibly be
    // better implemented by changing the way that we create the list
    // of invariants that is in one conditional and not in the other
    // to not include an invariant if it is suppressed on the other
    // side.  This would have the pleasant side effect of not forcing
    // all of the suppressed invariants to be created before
    // determining implications.
        if ((orig_cons.isObvious() != null) || orig_cons.is_ni_suppressed())
      return;


    // System.out.println("add_implication:");
    // System.out.println("  predicate = " + predicate.format());
    // System.out.println("  consequent= " + consequent.format());
    // System.out.println("  orig_pred = " + orig_pred.format());
    // System.out.println("  orig_cons = " + orig_cons.format());

    if (dkconfig_split_bi_implications && iff) {
      Implication imp = Implication.makeImplication (ppt, predicate, consequent,
                                                     false, orig_pred, orig_cons);
      if (imp != null)
        ppt.joiner_view.addInvariant (imp);
      imp = Implication.makeImplication (ppt, consequent, predicate,
                                                     false, orig_cons, orig_pred);
      if (imp != null)
        ppt.joiner_view.addInvariant (imp);

      return;
    }

    Implication imp = Implication.makeImplication (ppt, predicate, consequent,
                                                   iff, orig_pred, orig_cons);
    if (imp == null)
      // The predicate is the same as the consequent, or the implication
      // already exists.
      return;

    ppt.joiner_view.addInvariant (imp);
  }

  /**
   * Adds the specified relation from each conditional ppt in this
   * to the corresponding conditional ppt in ppt_split.  The relation
   * specified should be a relation from this.parent to ppt_split.parent.
   */
  public void add_relation (PptRelation rel, PptSplitter ppt_split) {

    for (int ii = 0; ii < ppts.length; ii++ ) {
      PptRelation cond_rel = rel.copy (ppts[ii], ppt_split.ppts[ii]);
      // System.out.println ("Added relation: " + cond_rel);
      // System.out.println ("with relations: "
      //                      + cond_rel.parent_to_child_var_string());
    }
  }

  /**
   * Returns the VarInfo in ppt1 that matches the specified VarInfo in ppt2.
   * The variables at each point must match exactly.  This is a reasonable
   * assumption for the ppts in PptSplitter and their parent.
   */
  private VarInfo matching_var (PptTopLevel ppt1, PptTopLevel ppt2,
                                VarInfo ppt2_var) {

    VarInfo v = ppt1.var_infos[ppt2_var.varinfo_index];
    Assert.assertTrue (v.name().equals (ppt2_var.name()));
    return (v);
  }

  public String toString() {

    return "Splitter " + splitter + ": ppt1 " + ppts[0].name() + ": ppt2 "
            + ppts[1].name;
  }

}
