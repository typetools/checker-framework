package daikon;

import daikon.inv.*;
import daikon.suppress.*;
import daikon.inv.ternary.threeScalar.*;

import utilMDE.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * Holds Equality invariants.
 **/
public class PptSliceEquality
  extends PptSlice
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20021231L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.

  /**
   * If true, create one equality set for each variable.
   * This has the effect of turning
   * the equality optimization off, without actually removing the sets
   * themselves (which are presumed to exist in many parts of the code).
   */
  public static boolean dkconfig_set_per_var = false;

  public static final Logger debug =
    Logger.getLogger ("daikon.PptSliceEquality");

  public static final Logger debugGlobal
    = Logger.getLogger ("daikon.PptSliceEquality.Global");

  PptSliceEquality(PptTopLevel parent) {
     super(parent, parent.var_infos);
  }

  public final int arity() {
    throw new Error("Don't call arity on PptSliceEquality");
  }


  void init_po() {
    throw new Error("Shouldn't get called");
  }

  public void addInvariant(Invariant inv) {
    Assert.assertTrue(inv instanceof Equality);
    invs.add(inv);
  }

  // Not valid for this type of slice.  Always pretend there are enough.
  public int num_samples() { if (true) throw new Error(); return Integer.MAX_VALUE; }
  public int num_mod_samples() { if (true) throw new Error(); return Integer.MAX_VALUE; }
  public int num_values() { if (true) throw new Error(); return Integer.MAX_VALUE; }

  /**
   * Encapsulates a VarInfo and its Comparability so that the two can
   * be used to create sets of VarInfos that are initially equal. Two
   * VarInfoAndComparability's are true iff they are
   * VarComparability.comparable() to each other.
   **/
  private static class VarInfoAndComparability {
    public VarInfo vi;

    public int hashCode() {
      // This is about as good as we can do it.  Can't do hashcode of
      // the comparability because two comparabilities may be
      // comparable and yet be not the same
      // (e.g. VarComparabilityExplicit).
      return vi.file_rep_type.hashCode();
    }

    public boolean equals (Object o) {
      if (!(o instanceof VarInfoAndComparability)) return false;
      return equals ((VarInfoAndComparability) o);
    }

    /**
     * Whether two VarInfos can be set to be equal to each other is
     * whether they are comparableNWay.  Since we do not yet handle
     * inheritance, we require that the comptability go both ways.
     **/
    public boolean equals (VarInfoAndComparability o) {

      return (vi.comparableNWay (o.vi)
              && (vi.comparability.equality_set_ok (o.vi.comparability)));
    }

    public VarInfoAndComparability (VarInfo vi) {
      this.vi = vi;
    }

  }

  /**
   * Actually instantiate the equality sets.
   **/
  void instantiate_invariants() {

    // If each variable gets its own set, create those sets and return
    if (dkconfig_set_per_var) {
      // Debug.debugTrack.fine ("Vars for " + parent.name());
      for (int i = 0; i < var_infos.length; i++) {
        VarInfo vi = var_infos[i];
        List<VarInfo> vi_list = new ArrayList<VarInfo>(1);
        vi_list.add (vi);
        Equality eq = new Equality (vi_list, this);
        invs.add (eq);
        // System.out.println ("  eq set = " + eq.shortString());
      }
      return;
    }

    // Start with everything comparable being equal.
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("InstantiateInvariants: " + parent.name() + " vars:") ;
    }
    LinkedHashMap<VarInfoAndComparability,List<VarInfo>> multiMap = new LinkedHashMap<VarInfoAndComparability,List<VarInfo>>();
    for (int i = 0; i < var_infos.length; i++) {
      VarInfo vi = var_infos[i];
      VarInfoAndComparability viac = new VarInfoAndComparability(vi);
      addToBindingList (multiMap, viac, vi);
      if (debug.isLoggable(Level.FINE)) {
        debug.fine ("  " + vi.name() + ": " + vi.comparability);
      }
    }
    if (debug.isLoggable(Level.FINE)) {
      debug.fine (Integer.toString(multiMap.keySet().size()));
    }
    Equality[] newInvs = new Equality[multiMap.keySet().size()];
    int varCount = 0;
    int invCount = 0;
    for (List<VarInfo> list : multiMap.values()) {
      varCount += list.size();

      Equality eq = new Equality (list, this);
      newInvs[invCount] = eq;
      if (debug.isLoggable(Level.FINE)) {
        debug.fine (" Created: " + eq);
      }
      if (Debug.logOn())
        Debug.log (getClass(), parent, Debug.vis (eq.leader()), "Created");
      invCount ++;
    }
    // Ensure determinism
    Arrays.sort (newInvs, EqualityComparator.theInstance);
    invs.addAll (Arrays.asList (newInvs));
    Assert.assertTrue (varCount == var_infos.length); // Check that we get all vis
  }

  /**
   * Instantiate the full equality sets from a set of variable pairs where
   * each member of a pair is equal to the other.
   */

  public void instantiate_from_pairs (Set<VarInfo.Pair> eset) {

    // Build a map from each variable to all those that are equal to it
    Map<VarInfo,List<VarInfo>> varmap = new LinkedHashMap<VarInfo,List<VarInfo>>();
    Map<VarInfo,Integer> sample_cnt_map = new LinkedHashMap<VarInfo,Integer>();
    for (VarInfo.Pair cp : eset) {
      List<VarInfo> vlist =  varmap.get (cp.v1);
      if (vlist == null) {
        vlist = new ArrayList<VarInfo>();
        vlist.add (cp.v1);
        varmap.put (cp.v1, vlist);
        sample_cnt_map.put (cp.v1, new Integer(cp.samples));
      }
      vlist.add (cp.v2);
      vlist = varmap.get (cp.v2);
      if (vlist == null) {
        vlist = new ArrayList<VarInfo>();
        vlist.add (cp.v2);
        varmap.put (cp.v2, vlist);
        sample_cnt_map.put (cp.v2, new Integer(cp.samples));
      }
      vlist.add (cp.v1);
    }

    // Loop through each variable, building the appropriate equality set
    // for each.  Note that variables that are distinct still have an
    // equality set (albeit with only the one variable)
    ArrayList<Invariant> newInvs = new ArrayList<Invariant>();
    for (int i = 0; i < var_infos.length; i++) {
      VarInfo v = var_infos[i];
      if (v.equalitySet != null)
        continue;
      List<VarInfo> vlist = varmap.get (v);
      if (vlist == null) {
        vlist = new ArrayList<VarInfo>(1);
        vlist.add (v);
      }
      Equality eq = new Equality (vlist, this);
      Integer sample_cnt = sample_cnt_map.get (v);
      if (sample_cnt != null)
        eq.setSamples (sample_cnt.intValue());
      v.equalitySet = eq;
      newInvs.add (eq);
    }
    invs.addAll (newInvs);
  }

  /**
   * Returns a List of Invariants that have been weakened/destroyed.
   * However, this handles the creation of new Equality invariants and
   * the instantiation of other invariants.
   * @return a List of invariants that have been weakened
   **/
  // The basic approach is as follows:
  //    - Loop through each equality set
  //        - look for any variables that are no longer equal
  //        - Create new equality sets (call createEqualityInvs)
  //        - Get the new leaders
  //        - Create new slices and invariants (call CopyInvsFromLeader)
  //
  public List<Invariant> add(ValueTuple vt, int count) {

    LinkedList<Equality> allNewInvs = new LinkedList<Equality>();
    LinkedList<Invariant> weakenedInvs = new LinkedList<Invariant>();

    // Loop through each existing equality invariant
    for (Invariant invar : invs) {
      Equality inv = (Equality) invar;

      // Add this sample to the invariant and track any vars that fall
      // out of the set.
      List<VarInfo> nonEqualVis = inv.add (vt, count);

      // If some vars fell out
      if (nonEqualVis.size() > 0) {

        // Create new equality sets for all of the non-equal vars
        List<Equality> newInvs =
          createEqualityInvs (nonEqualVis, vt, inv, count);

        // Get a list of all of the new non-missing leaders
        List<VarInfo> newInvsLeaders = new ArrayList<VarInfo> (newInvs.size());
        for (Equality eq : newInvs) {
          if ((parent.constants == null) || !parent.constants.is_missing (eq.leader()))
            newInvsLeaders.add (eq.leader());
        }

        //Debug print the new leaders
        if (Debug.logOn()) {
          for (VarInfo nileader : newInvsLeaders) {
            Debug.log (getClass(), parent,
                       Debug.vis (nileader),
              "Split off from previous leader " + inv.leader().name()
              + ": new set = " + nileader.equalitySet
              + ": old set = " + inv);
          }
        }

        // Create new slices and invariants for each new leader
        weakenedInvs.addAll (copyInvsFromLeader (inv.leader(),newInvsLeaders));

        // Keep track of all of the new invariants created.
        allNewInvs.addAll (newInvs);
      }
    }

    // Add all of the new equality sets to our list
    invs.addAll (allNewInvs);

    return weakenedInvs;
  }

  /**
   * Dummy value that's incomparable to everything else to indicate
   * missings in createEqualityInvs.
   **/
  private static final Object dummyMissing = new StringBuffer("Dummy missing");

  /**
   * Create a List of Equality invariants based on the values given
   * by vt for the VarInfos in vis.  Any variables that are out
   * of bounds are forced into a separate equality set (since they
   * no longer make sense and certainly shouldn't be equal to anything
   * else)
   * @param vis The VarInfos that were different from leader
   * @param vt The ValueTuple associated with the VarInfos now
   * @param leader The original leader of VarInfos
   * @param count The number of samples seen (needed to set the number
   * of samples for the new Equality invariants)
   * @return a List of Equality invariants bundling together same
   * values from vis, and if needed, another representing all the
   * missing values.
   * pre vis.size() > 0
   * post result.size() > 0
   **/
  private List<Equality> createEqualityInvs (List<VarInfo> vis, ValueTuple vt,
                                                 Equality leader, int count
                                                 ) {
    Assert.assertTrue (vis.size() > 0);
    HashMap<Object,List<VarInfo>> multiMap = new HashMap<Object,List<VarInfo>>(); /* key is a value */
    List<VarInfo> out_of_bounds = new ArrayList<VarInfo>();
    for (VarInfo vi : vis) {
      if (vi.missingOutOfBounds())
        out_of_bounds.add (vi);
      else if (vt.isMissing (vi)) {
        addToBindingList (multiMap, dummyMissing, vi);
      } else {
        if (vi.getValue(vt) == null) {
          Fmt.pf ("null value for variable %s, mod=%s at ppt %s",
                vi.name(), "" + vt.getModified(vi), parent.name());
          VarInfo rv = parent.find_var_by_name ("return");
          Fmt.pf ("return value = " + Debug.toString (rv.getValue(vt)));
          Fmt.pf("At line number "
		 + FileIO.data_trace_state.reader.getLineNumber());
        }
        addToBindingList (multiMap, vi.getValue(vt), vi);
      }
    }
    // Why use an array?  Because we'll be sorting shortly
    Equality[] resultArray = new Equality[multiMap.values().size()
                                          + out_of_bounds.size()];
    int resultCount = 0;
    for (Map.Entry<Object,List<VarInfo>> entry : multiMap.entrySet()) {
      Object key = entry.getKey();
      List<VarInfo> list = entry.getValue();
      Assert.assertTrue (list.size() > 0);
      Equality eq = new Equality (list, this);
      @SuppressWarnings("interned") // Special value
      boolean isMissing = (key == dummyMissing);
      if (isMissing) {
        eq.setSamples (leader.numSamples() - count);
      } else {
        eq.setSamples (leader.numSamples());
      }
      if (debug.isLoggable(Level.FINE)) {
        debug.fine ("  created new inv: " + eq + " samples: " + eq.numSamples());
      }
      resultArray[resultCount] = eq;
      resultCount++;
    }
    for (VarInfo oob : out_of_bounds) {
      List<VarInfo> list = new LinkedList<VarInfo>();
      list.add (oob);
      resultArray[resultCount] = new Equality (list, this);
      resultCount++;
    }

    // Sort for determinism
    Arrays.sort (resultArray, EqualityComparator.theInstance);
    List<Equality> result = Arrays.asList (resultArray);
    Assert.assertTrue (result.size() > 0);
    return result;
  }

  /**
      * Create a List of Equality invariants based on the VarInfos in vis.
      * Assumes that the VarInfos in vis are not missing.  The method is used
      * exclusively for reversing optimizations in Daikon.
      * @param vis The VarInfos that were different from leader
      * @param leader The original leader of VarInfos
      * @return a List of Equality invariants bundling together same
      * values from vis.
      * pre vis.size() > 0
      * post result.size() > 0
      */
     public List<Equality> createEqualityInvs(List<VarInfo> vis, Equality leader) {
       Assert.assertTrue(vis.size() > 0);

       // Why use an array?  Because we'll be sorting shortly
       Equality[] resultArray = new Equality[vis.size()];
       int resultCount = 0;
       for (VarInfo vi : vis) {
         List<VarInfo> list = new ArrayList<VarInfo>();
         list.add(vi);
         Equality eq = new Equality(list, this);
         eq.setSamples(leader.numSamples());
         resultArray[resultCount] = eq;
         resultCount++;
       }

       // Sort for determinism
       Arrays.sort(
         resultArray,
         PptSliceEquality.EqualityComparator.theInstance);
       List<Equality> result = Arrays.asList(resultArray);
       Assert.assertTrue(result.size() > 0);
       return result;
     }

  /**
   * Map maps keys to non-empty lists of elements.
   * This method adds var to the list mapped by key,
   * creating a new list for key if one doesn't already exist.
   * @param map The map to add the bindings to
   * @param key If there is already a List associated with key, then
   * add value to key.  Otherwise create a new List associated with
   * key and insert value.
   * @param value The value to insert into the List mapped to key.
   * pre: Each value in map is a list of size 1 or greater
   * post: Each value in map is a list of size 1 or greater
   **/
  private <T> void addToBindingList (Map<T,List<VarInfo>> map, T key, VarInfo value) {
    Assert.assertTrue (key != null);
    List<VarInfo> elements = map.get(key);
    if (elements == null) {
      elements = new LinkedList<VarInfo>();
      map.put (key, elements);
    }
    elements.add (value);
  }


  /**
   * Instantiate invariants from each inv's leader.  This is like
   * instantiate_invariants at the start of reading the trace file,
   * where we create new PptSliceNs.  This is called when newVis have
   * just split off from leader, and we want the leaders of newVis to
   * have the same invariants as leader.
   * @param leader the old leader
   * @param newVis a List of new VarInfos that used to be equal to
   * leader.  Actually, it's the list of canonical that were equal to
   * leader, representing their own newly-created equality sets.
   * post: Adds the newly instantiated invariants and slices to
   * this.parent.
   **/
  public List<Invariant> copyInvsFromLeader (VarInfo leader, List<VarInfo> newVis) {


    List<Invariant> falsified_invs = new ArrayList<Invariant>();
    List<PptSlice> newSlices = new LinkedList<PptSlice>();
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("copyInvsFromLeader: " + parent.name() + ": leader "
                  + leader.name()
                  + ": new leaders = " + VarInfo.toString (newVis));
      debug.fine ("  orig slices count:" + parent.numViews());
    }

    // Copy all possible combinations from the current ppt (with repetition)
    // of replacing leader with different members of newVis.

    // Loop through each slice
    for (Iterator<PptSlice> i = parent.views_iterator(); i.hasNext(); ) {
      PptSlice slice = i.next();

      if (debug.isLoggable(Level.FINE)) {
        debug.fine ("  Slice is: " + slice.toString());
        debug.fine ("  With invs: " + slice.invs);
      }

      // If this slice contains the old leader
      if (slice.containsVar(leader)) {

        // Substitute new leader for old leader and create new slices/invs
        VarInfo[] toFill = new VarInfo[slice.var_infos.length];
        copyInvsFromLeaderHelper (leader, newVis, slice, newSlices,
                                  0, -1, toFill);

        // Remove any statically obvious invariants in the old slice.
        // This is called here because breaking up the equality set may
        // cause some invariants to become statically obvious (because
        // they will now be the only item in their set)
        for (Invariant inv : slice.invs) {
          if (!Daikon.dkconfig_undo_opts) {
            if (inv.isObviousStatically_AllInEquality()) {
              inv.falsify();
              falsified_invs.add (inv);
            }
          }
        }
        if (slice.invs.size() == 0) i.remove();
      }
    }

    // Add each new slice with invariants
    for (PptSlice slice : newSlices) {
      if (slice.invs.size() == 0) {
        continue;
      }
      assert (parent.findSlice (slice.var_infos) == null)
        : parent.findSlice (slice.var_infos);
      slice.repCheck();
      parent.addSlice (slice);
    }

    parent.repCheck();

    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("  new slices count:" + parent.numViews());
    }
    return (falsified_invs);
  }

  /**
   * Clones slice (zero or more times) such that instances of leader
   * are replaced by members of newVis; places new slices in
   * newSlices.  The replacement is such that we get all combinations,
   * with repetition of newVis and leader in every slot in slice where
   * there used to be leader.  For example, if slice contained (A1,
   * A1, B) and A1 is leader and newVis contains A2 and A3, then the
   * slices we produce would be: (A1, A2, B), (A1, A3, B), (A2, A2, B)
   * (A2, A3, B), (A3, A3, B).  We do not produce (A1, A1, B) because
   * it is already there.  We do not produce (A2, A1, B) because it is
   * the same as (A1, A2, B) wrt combinations.  This method does the
   * main work of copyInvsFromLeader so that each new equality set
   * that spawned off leader has the correct slices.  It works as a
   * nested series of for loops, whose depth is equal to the length of
   * slice.var_infos.  The position and loop arguments along with the
   * call stack keep track of the loop nesting.  When position reaches
   * the end of slice.var_infos, this method attempts to instantiate
   * the slice that has been produced.  The standard start for
   * position is 0, and for loop is -1.
   * @param leader The variable to replace in slice
   * @param newVis of VarInfos that will replace leader in combination in slice
   * @param slice The slice to clone
   * @param newSlices Where to put the cloned slices
   * @param position The position currently being replaced in source.  Starts at 0.
   * @param loop The iteration of the loop for this position.  If -1,
   * means the previous replacement is leader.
   * @param soFar Buffer to which assignments temporarily go before
   * becoming instantiated.  Has to equal slice.var_infos in length.
   **/
  private void copyInvsFromLeaderHelper (VarInfo leader, List<VarInfo> newVis,
                                         PptSlice slice, List<PptSlice> newSlices,
                                         int position, int loop,
                                         VarInfo[] soFar) {

    // Track debug if any variables are in newVis
    Debug dlog = null;
    if (Debug.logOn())
      dlog = new Debug (getClass(), parent, newVis);

    if (position >= slice.var_infos.length) {
      // Done with assigning positions and recursion
      if (parent.findSlice_unordered (soFar) == null) {
        // If slice is already there, no need to clone.

        if (parent.is_slice_ok (soFar, slice.arity())) {
          PptSlice newSlice = slice.cloneAndPivot(soFar);
          // Debug.debugTrack.fine ("LeaderHelper: Created Slice " + newSlice);
          if (Debug.logOn()) {
            dlog.log ("Created slice " + newSlice + " Leader equality set = "
                      + soFar[0].equalitySet);
            Debug.log (getClass(), newSlice, "Created this slice");
          }
          List<Invariant> invs = newSlice.invs;
          for (Iterator<Invariant> iInvs = invs.iterator(); iInvs.hasNext(); ) {
            Invariant inv = iInvs.next();
            if (!Daikon.dkconfig_undo_opts) {
              if (inv.isObviousStatically_AllInEquality()) {
                iInvs.remove();
              }
            }
          }
          if (newSlice.invs.size() == 0) {
            Debug.log (debug, getClass(), newSlice, soFar,
                       "slice not added because 0 invs");
          } else {
            newSlices.add (newSlice);
          }
        }
      } else {
        if (Debug.logOn())
          dlog.log ("Slice already existed " +
                    parent.findSlice_unordered (soFar));
      }
      return;
    } else {
      // Not yet done with recursion, keep assigning to soFar
      if (slice.var_infos[position] == leader) {
        // If leader does need replacing
        // newLoop starts at loop so that we don't have repeats
        for (int newLoop = loop; newLoop < newVis.size(); newLoop++) {
          VarInfo vi = newLoop == -1 ? leader : newVis.get(newLoop);
          soFar[position] = vi;
          // Advance position to next step, let next loop variable be
          // this loop's counter.
          copyInvsFromLeaderHelper (leader, newVis, slice, newSlices,
                                    position + 1, newLoop, soFar);
        }
      } else {
        // Non leader position, just keep going after assigning soFar
        soFar[position] = slice.var_infos[position];
          copyInvsFromLeaderHelper (leader, newVis, slice, newSlices,
                                    position + 1, loop, soFar);
      }
    }
  }

  @SuppressWarnings("interned") // PptTopLevel
  public void repCheck() {
    for (Invariant inv : invs) {
      inv.repCheck();
      Assert.assertTrue (inv.ppt == this);
    }
  }

  public String toString() {
    StringBuffer result = new StringBuffer("PptSliceEquality: [");
    for (Invariant inv : invs) {
      result.append (inv.repr());
      result.append (lineSep);
    }
    result.append ("  ]");
    return result.toString();
  }

  /**
   * Order Equality invariants by the indices of leaders.
   **/
  public static final class EqualityComparator implements Comparator<Equality> {
    public static final EqualityComparator theInstance = new EqualityComparator();
    private EqualityComparator() {

    }
    public int compare(Equality eq1, Equality eq2) {
      return VarInfo.IndexComparator.theInstance.compare (eq1.leader(), eq2.leader());
    }

  }

  /**
   * Returns an array of all of the leaders sorted by varinfo_index
   * for this equality view.
   */
  public VarInfo[] get_leaders_sorted() {
    VarInfo[] leaders = new VarInfo[invs.size()];
    for (int i = 0; i < invs.size(); i++) {
      leaders[i] = ((Equality) invs.get(i)).leader();
      Assert.assertTrue (leaders[i] != null);
    }
    Arrays.sort (leaders, VarInfo.IndexComparator.getInstance());
    return (leaders);
  }

}
