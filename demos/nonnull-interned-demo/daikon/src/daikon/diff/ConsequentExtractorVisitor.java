package daikon.diff;

import daikon.inv.Invariant;
import daikon.inv.Implication;
import daikon.*;
import java.util.*;

/** <B>ConsequentExtractorVisitor</B> is a visitor that
 *  takes in RootNode tree used by the other visitors in Diff
 *  and only modifies the first inv tree out of the pair of two
 *  inv trees (the second tree is never read or modified).
 *
 *  <P>The goal is to take the right hand side of any implication
 *  and extract it for later use.  The implementation completely replaces
 *  the previous inv tree with the a new inv tree.  The new inv tree
 *  contains only the extracted consequents of the original inv tree.
 **/
public class ConsequentExtractorVisitor extends DepthFirstVisitor  {


    private int nonce;

    // Gets rid of repeated reports
    private HashSet<String> repeatFilter = new HashSet<String>();

    // Accumulation of extracted consequents
    private Vector<Invariant> accum = new Vector<Invariant>();


    public ConsequentExtractorVisitor () {
        nonce = 0;
    }

    public void visit (PptNode node) {
        if (node.getPpt1() instanceof PptConditional) {
            return;
        }
        System.out.println (node.getPpt1().name);
        repeatFilter.clear();
        accum.clear();
        super.visit(node);
        // clear all of the old ppts

        for (Iterator<InvNode> i = node.children(); i.hasNext(); ) {
            InvNode child = i.next();
            if (child.getInv1() != null) {
                child.getInv1().ppt.invs.clear();
            }
        }
        /*
        for (Invariant inv : accum) {
            inv.ppt.invs.clear();
        }
        */
        // Now add back everything in accum
        for (Invariant inv : accum) {
            inv.ppt.addInvariant (inv);
        }
        System.out.println ("NONCE: " + nonce);
    }


  /** The idea is to check if the node is an Implication Invariant.If not,
   *  immediately remove the invariant.  Otherwise, extract the Consequent,
   *  remove the Implication, and then add the consequent to the list. */
     public void visit (InvNode node) {
       Invariant inv1 = node.getInv1();
       // do nothing if the invariant does not exist
       if (inv1 != null) {
         if (inv1.justified() && (inv1 instanceof Implication)) {
             nonce++;
             Implication imp = (Implication) inv1;
             if (!repeatFilter.contains((imp.consequent().format()))) {
                 repeatFilter.add (imp.consequent().format());
                 // inv1.ppt.invs.add (imp.consequent());
                 accum.add (imp.consequent());
             }
           // add both sides of a biimplication
           if (imp.iff == true) {
               if (!repeatFilter.contains(imp.predicate().format())) {
                   repeatFilter.add (imp.predicate().format());
                   // inv1.ppt.invs.add (imp.predicate());
                   accum.add (imp.predicate());
               }
           }
         }
         inv1.ppt.removeInvariant (inv1);
         System.out.println (inv1.ppt.invs.size() + " " + repeatFilter.size());
       }
       else {

       }
     }

  /**
   * Returns true if the pair of invariants should be printed,
   * depending on their type, relationship, and printability.
   **/
  protected boolean shouldPrint(Invariant inv1, Invariant inv2) {
      int type = DetailedStatisticsVisitor.determineType(inv1, inv2);
      if (type == DetailedStatisticsVisitor.TYPE_NULLARY_UNINTERESTING ||
          type == DetailedStatisticsVisitor.TYPE_UNARY_UNINTERESTING) {
          return false;
      }

      int rel = DetailedStatisticsVisitor.determineRelationship(inv1, inv2);
      if (rel == DetailedStatisticsVisitor.REL_SAME_JUST1_JUST2 ||
          rel == DetailedStatisticsVisitor.REL_SAME_UNJUST1_UNJUST2 ||
          rel == DetailedStatisticsVisitor.REL_DIFF_UNJUST1_UNJUST2 ||
          rel == DetailedStatisticsVisitor.REL_MISS_UNJUST1 ||
          rel == DetailedStatisticsVisitor.REL_MISS_UNJUST2) {
          return false;
      }

      if ((inv1 == null || !inv1.isWorthPrinting()) &&
          (inv2 == null || !inv2.isWorthPrinting())) {
          return false;
      }

      return true;
  }
}
