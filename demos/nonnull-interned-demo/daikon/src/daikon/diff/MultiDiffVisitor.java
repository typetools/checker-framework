// MultiDiffVisitor.java

package daikon.diff;

import daikon.*;
import daikon.inv.*;
import java.io.*;
import java.util.*;

/**
 * <B>MultiDiffVisitor</B> is a state-storing NodeVisitor that works
 * across multiple files regardless of the current two-file infrastructure.
 * This allows the selection of very unique invariants that occur once over
 * an entire set of trace files
 **/

public class MultiDiffVisitor extends PrintNullDiffVisitor {

    protected PptMap currMap;
    private HashSet<String> programPointsList;
    private HashMap<String,Integer> freqList;
    private HashSet<String> justifiedList;
    private int total = 0;
    private static boolean spinfoMode = false;
    private static PrintStream out = System.out;

    public MultiDiffVisitor (PptMap firstMap) {
        // I'll always want System.out, and never verbose!
        super (System.out, false);
        currMap = firstMap;
        programPointsList = new HashSet<String>();
        freqList = new HashMap<String,Integer>();
        justifiedList = new HashSet<String>();
    }

    public static void setForSpinfoOut (OutputStream out_os) {
        MultiDiffVisitor.out = new PrintStream (out_os, true);
        spinfoMode = true;
    }

    public void visit (RootNode node) {

        total++;
        super.visit (node);

    }

    public void visit (InvNode node) {
        Invariant inv1 = node.getInv1();
        Invariant inv2 = node.getInv2();

        // Use the histogram map
        if (inv1 != null && shouldPrint (inv1, inv2)) {
            String tmpStr = inv1.ppt.name();
            // example:
            // tmpStr == FeedTheCat.measure(III)I:::ENTER(b, this.bCap
            String thisPptName = tmpStr.substring (0,
                                                   tmpStr.lastIndexOf ('('));

            programPointsList.add (thisPptName);
            String key = thisPptName + "$" + inv1.format_using(OutputFormat.JAVA);
            Integer val = freqList.get (key);
            if (val == null) {
                // Use one as default, obviously
                freqList.put (key, new Integer (1));
            }
            // increment if it's already there
            else {
                freqList.put ( key,
                               new Integer (val.intValue() + 1));
            }

            // add to justified list if this was justified once
            //    if (inv1.justified()) {
                justifiedList.add (key);
                // }
        }

    }

    /** Prints everything in the goodList. */
    public void printAll () {

        if (spinfoMode) {
            printAllSpinfo();
            return;
        }

        // keeps track of suppressed invariants due to appearing in
        // every sample of the MultiDiff
        int kill = 0;
        int unjustifiedKill = 0;
        ArrayList<String> bigList = new ArrayList<String>();

        // New historgram stuff
        System.out.println ("Histogram**************");

        // This gets all of the output in the format:
        // inv.ppt.name() + "$" + inv1.format_java() + " Count = " + freq
        for (String str : freqList.keySet()) {
            int freq = freqList.get(str).intValue();
            if (freq < total && justifiedList.contains (str)) {
                bigList.add (str + " Count =  " + freq);
                // System.out.println (str + " Count =  " + freq);

            }
            // don't print something true in EVERY set
            else if (freq == total) { kill++; }
            // don't print something that was never justified
            else unjustifiedKill++;
        }
        System.out.println ("Invariants appearing in all: " + kill);
        System.out.println ("Invariants never justified: " + unjustifiedKill);

        // Now build the final HashMap that will have the following
        // mapping:  program point names ->
        //                      ArrayList of inv.format_java() with frequency

        HashMap<String,ArrayList<String>> lastMap = new HashMap<String,ArrayList<String>>();
        // One pass to fill each mapping with an empty ArrayList
        for (String key : programPointsList) {
            lastMap.put (key, new ArrayList<String>());
        }

        // Now to populate those ArrayLists
        for (String str : bigList) {
            StringTokenizer st = new StringTokenizer (str, "$");
            String key = st.nextToken();
            String data = st.nextToken();
            try {
                lastMap.get(key).add (data);
            } catch (Exception e) {System.out.println (key + " error in MultiDiffVisitor");}
        }

        // print it all
        for (Map.Entry<String,ArrayList<String>> entry : lastMap.entrySet()) {
            String key = entry.getKey();
            ArrayList al = entry.getValue();
            // don't print anything if there are no selective invariants
            if (al.size() == 0) continue;
            System.out.println ();
            System.out.println (key + "*****************");
            System.out.println ();
            for (Object toPrint : al) {
                System.out.println (toPrint);
            }
        }
        System.out.println ();
        System.out.println ();
    }

     /** Prints everything in the goodList, outputs as spinfo. */
    public void printAllSpinfo() {

        // keeps track of suppressed invariants due to appearing in
        // every sample of the MultiDiff
        int kill = 0;
        ArrayList<String> bigList = new ArrayList<String>();


        // This gets all of the output in the format:
        // inv.ppt.name() + "$" + inv1.format_java()
        for (String str : freqList.keySet()) {
            int freq = freqList.get(str).intValue();
            if (freq < total && justifiedList.contains (str)) {
                // just want the String on its own line
                bigList.add (str);
            }

        }


        // Now build the final HashMap that will have the following
        // mapping:  program point names ->
        //                      ArrayList of inv.format_java() with frequency

        HashMap<String,ArrayList<String>> lastMap = new HashMap<String,ArrayList<String>>();
        // One pass to fill each mapping with an empty ArrayList
        for (String key : programPointsList) {
            lastMap.put (key, new ArrayList<String>());
        }

        // Now to populate those ArrayLists
        for (String str : bigList) {
            StringTokenizer st = new StringTokenizer (str, "$");
            String key = st.nextToken();
            String data = st.nextToken();
            try {
                lastMap.get(key).add (data);
            } catch (Exception e) { out.println (key + " error in MultiDiffVisitor");}
        }

        // print it all
        ArrayList<String> theKeys = new ArrayList<String> (lastMap.keySet());
        // sort them so that multiple exits will end up being adjacent
        // to each other when they are from the same method
        Collections.sort (theKeys);
        String lastPpt = "";
        for (String key : theKeys) {
            ArrayList al = lastMap.get(key);
            // don't print anything if there are no selective invariants

            if (al.size() == 0) continue;

            // Get rid of the extra stuff like (III)I:::ENTER
            // at the end of each of the program points

            // use the fact that we only want the stuff before the first '('
            StringTokenizer pToke = new StringTokenizer (key, "(");

            // sadly we only want EXIT values, so throw out any ENTERs
            // because the spinfo won't deal well with them anyway

            //            if (key.indexOf ("ENTER") != -1) continue;


            // Now we don't want to reprint the program point name
            // again in the spinfo file if it has been printed from
            // the previous Ppt that exited at a different point -LL

            String thisPpt = pToke.nextToken();

            if (! lastPpt.equals (thisPpt)) {
                out.println ();
                out.println ("PPT_NAME " + thisPpt);

                lastPpt = thisPpt;
            }
            for (Object toPrint : al) {
                out.println (toPrint);

            }
        }

    }

    protected boolean shouldPrint (Invariant inv1, Invariant inv2) {
        return true; // super.shouldPrint (inv1, inv2) &&
            //    inv1.format().toString().indexOf(">") == -1 &&
            // inv1.format().toString().indexOf("orig") == -1;
    }

}
