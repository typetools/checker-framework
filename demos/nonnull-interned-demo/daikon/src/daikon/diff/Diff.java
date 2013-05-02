package daikon.diff;

import daikon.*;
import daikon.inv.*;
import java.io.*;
import java.util.*;
import utilMDE.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Diff is the main class for the invariant diff program.  The
 * invariant diff program outputs the differences between two sets of
 * invariants.
 *
 * The following is a high-level description of the program.  Each
 * input file contains a serialized PptMap or InvMap.  PptMap and
 * InvMap are similar structures, in that they both map program points
 * to invariants.  However, PptMaps are much more complicated than
 * InvMaps.  PptMaps are output by Daikon, and InvMaps are output by
 * this program.
 *
 * First, if either input is a PptMap, it is converted to an InvMap.
 * Next, the two InvMaps are combined to form a tree.  The tree is
 * exactly three levels deep.  The first level contains the root,
 * which holds no data.  Each node in the second level is a pair of
 * Ppts, and each node in the third level is a pair of Invariants.
 * The tree is constructed by pairing the corresponding Ppts and
 * Invariants in the two PptMaps.  Finally, the tree is traversed via
 * the Visitor pattern to produce output.  The Visitor pattern makes
 * it easy to extend the program, simply by writing a new Visitor.
 **/
public final class Diff {

  public static final Logger debug = Logger.getLogger ("daikon.diff.Diff");


  private static String usage =
    UtilMDE.joinLines(
      "Usage:",
      "    java daikon.diff.Diff [flags...] file1 [file2]",
      "  file1 and file2 are serialized invariants produced by Daikon.",
      "  If file2 is not specified, file1 is compared with the empty set.",
      "  For a list of flags, see the Daikon manual, which appears in the ",
      "  Daikon distribution and also at http://pag.csail.mit.edu/daikon/.");

  // added to disrupt the tree when bug hunting -LL
  private static boolean treeManip = false;

  // this is set only when the manip flag is set "-z"
  private static PptMap manip1 = null;
  private static PptMap manip2 = null;

  /** The long command line options. **/
  private static final String HELP_SWITCH =
    "help";
  private static final String INV_SORT_COMPARATOR1_SWITCH =
    "invSortComparator1";
  private static final String INV_SORT_COMPARATOR2_SWITCH =
    "invSortComparator2";
  private static final String INV_PAIR_COMPARATOR_SWITCH =
    "invPairComparator";
  private static final String IGNORE_UNJUSTIFIED_SWITCH =
    "ignore_unjustified";
  private static final String IGNORE_NUMBERED_EXITS_SWITCH =
    "ignore_exitNN";



  /** Determine which ppts should be paired together in the tree. **/
  private static final Comparator<PptTopLevel> PPT_COMPARATOR = new Ppt.NameComparator();

  /**
   * Comparators to sort the sets of invs, and to combine the two sets
   * into the pair tree.  Can be overriden by command-line options.
   **/
  private Comparator<Invariant> invSortComparator1;
  private Comparator<Invariant> invSortComparator2;
  private Comparator<Invariant> invPairComparator;

  private boolean examineAllPpts;
  private boolean ignoreNumberedExits;

  public Diff() {
    this(false, false);
    setAllInvComparators(new Invariant.ClassVarnameComparator());
  }

  public Diff(boolean examineAllPpts) {
    this (examineAllPpts, false);
  }

  public Diff (boolean examineAllPpts, boolean ignoreNumberedExits) {
    this.examineAllPpts = examineAllPpts;
    this.ignoreNumberedExits = ignoreNumberedExits;
  }

  /**
   * Read two PptMap or InvMap objects from their respective files.
   * Convert the PptMaps to InvMaps as necessary, and diff the
   * InvMaps.
   **/
  public static void main(String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException,
           InstantiationException, IllegalAccessException {
    try {
      mainHelper(args);
    } catch (Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException,
           InstantiationException, IllegalAccessException {
    daikon.LogHelper.setupLogs(daikon.LogHelper.INFO);

    boolean printDiff = false;
    boolean printUninteresting = false;
    boolean printAll = false;
    boolean includeUnjustified = true;
    boolean stats = false;
    boolean tabSeparatedStats = false;
    boolean minus = false;
    boolean xor = false;
    boolean union = false;
    boolean examineAllPpts = false;
    boolean ignoreNumberedExits = false;
    boolean printEmptyPpts = false;
    boolean verbose = false;
    boolean continuousJustification = false;
    boolean logging = false;
    File outputFile = null;
    String invSortComparator1Classname = null;
    String invSortComparator2Classname = null;
    String invPairComparatorClassname = null;

    boolean optionSelected = false;

    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
//     daikon.LogHelper.setLevel ("daikon.diff", daikon.LogHelper.FINE);

    LongOpt[] longOpts = new LongOpt[] {
      new LongOpt(Daikon.help_SWITCH,
                  LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(INV_SORT_COMPARATOR1_SWITCH,
                  LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(INV_SORT_COMPARATOR2_SWITCH,
                  LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(INV_PAIR_COMPARATOR_SWITCH,
                  LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(IGNORE_UNJUSTIFIED_SWITCH,
                  LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(IGNORE_NUMBERED_EXITS_SWITCH,
                  LongOpt.NO_ARGUMENT, null, 0),
    };

    Getopt g = new Getopt("daikon.diff.Diff", args,
                          "Hhyduastmxno:jzpevl", longOpts);
    int c;
    while ((c = g.getopt()) !=-1) {
      switch (c) {
      case 0:
        // got a long option
        String optionName = longOpts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(optionName)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (INV_SORT_COMPARATOR1_SWITCH.equals(optionName)) {
          if (invSortComparator1Classname != null) {
            throw new Error("multiple --" + INV_SORT_COMPARATOR1_SWITCH +
                            " classnames supplied on command line");
          }
          invSortComparator1Classname = g.getOptarg();
        } else if (INV_SORT_COMPARATOR2_SWITCH.equals(optionName)) {
          if (invSortComparator2Classname != null) {
            throw new Error("multiple --" + INV_SORT_COMPARATOR2_SWITCH +
                            " classnames supplied on command line");
          }
          invSortComparator2Classname = g.getOptarg();
        } else if (INV_PAIR_COMPARATOR_SWITCH.equals(optionName)) {
          if (invPairComparatorClassname != null) {
            throw new Error("multiple --" + INV_PAIR_COMPARATOR_SWITCH +
                            " classnames supplied on command line");
          }
          invPairComparatorClassname = g.getOptarg();
        } else if (IGNORE_UNJUSTIFIED_SWITCH.equals(optionName)) {
          optionSelected = true;
          includeUnjustified = false;
          break;
        } else if (IGNORE_NUMBERED_EXITS_SWITCH.equals(optionName)) {
          ignoreNumberedExits = true;
          break;
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     optionName);
        }
        break;
      case 'h':
        System.out.println(usage);
        throw new Daikon.TerminationMessage();
      case 'H':
        PrintAllVisitor.HUMAN_OUTPUT = true;
        break;
      case 'y':  // included for legacy code
        optionSelected = true;
        includeUnjustified = false;
        break;
      case 'd':
        optionSelected = true;
        printDiff = true;
        break;
      case 'u':
        printUninteresting = true;
        break;
      case 'a':
        optionSelected = true;
        printAll = true;
        break;
      case 's':
        optionSelected = true;
        stats = true;
        break;
      case 't':
        optionSelected = true;
        tabSeparatedStats = true;
        break;
      case 'm':
        optionSelected = true;
        minus = true;
        break;
      case 'x':
        optionSelected = true;
        xor = true;
        break;
      case 'n':
        optionSelected = true;
        union = true;
        break;
      case 'o':
        if (outputFile != null) {
          throw new Error
            ("multiple output files supplied on command line");
        }
        String outputFilename = g.getOptarg();
        outputFile = new File(outputFilename);
        if (! UtilMDE.canCreateAndWrite(outputFile)) {
          throw new Error("Cannot write to file " + outputFile);
        }
        break;
      case 'j':
        continuousJustification = true;
        break;
      case 'z':
        treeManip = true;
        // Only makes sense if -p is also on.
        examineAllPpts = true;
        break;
      case 'p':
        examineAllPpts = true;
        break;
      case 'e':
        printEmptyPpts = true;
        break;
      case 'v':
        verbose = true;
        break;
      case 'l':
        logging = true;
        break;
      case '?':
        // getopt() already printed an error
        System.out.println(usage);
        throw new Daikon.TerminationMessage("Bad argument");
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    // Turn on the defaults
    if (! optionSelected) {
      printDiff = true;
    }

    if (logging)
      System.err.println("Invariant Diff: Starting Log");

    if (logging)
      System.err.println("Invariant Diff: Creating Diff Object");

    Diff diff = new Diff(examineAllPpts, ignoreNumberedExits);

    // Set the comparators based on the command-line options

    Comparator<Invariant> defaultComparator;
    if (minus || xor || union) {
      defaultComparator = new Invariant.ClassVarnameFormulaComparator();
    } else {
      defaultComparator = new Invariant.ClassVarnameComparator();
    }
    diff.setInvSortComparator1
      (selectComparator(invSortComparator1Classname, defaultComparator));
    diff.setInvSortComparator2
      (selectComparator(invSortComparator2Classname, defaultComparator));
    diff.setInvPairComparator
      (selectComparator(invPairComparatorClassname, defaultComparator));

    if ((!(diff.invSortComparator1.getClass().toString().equals
           (diff.invSortComparator2.getClass().toString()))) ||
        (!(diff.invSortComparator1.getClass().toString().equals
           (diff.invPairComparator.getClass().toString())))) {
      System.out.println("You are using different comparators to sort or pair up invariants.");
      System.out.println("This may cause misalignment of invariants and may cause Diff to");
      System.out.println("work incorectly.  Make sure you know what you are doing!");
    }

    // The index of the first non-option argument -- the name of the
    // first file
    int firstFileIndex = g.getOptind();
    int numFiles = args.length - firstFileIndex;

    InvMap invMap1 = null;
    InvMap invMap2 = null;

    if (logging)
      System.err.println("Invariant Diff: Reading Files");

    if (numFiles == 1) {
      String filename1 = args[firstFileIndex];
      invMap1 = diff.readInvMap(new File(filename1));
      invMap2 = new InvMap();
    } else if (numFiles == 2) {
      String filename1 = args[firstFileIndex];
      String filename2 = args[firstFileIndex + 1];
      invMap1 = diff.readInvMap(new File(filename1));
      invMap2 = diff.readInvMap(new File(filename2));
    } else if (treeManip) {
      System.out.println ("Warning, the preSplit file must be second");
      if (numFiles < 3) {
        System.out.println
          ("Sorry, no manip file [postSplit] [preSplit] [manip]");
      }
      String filename1 = args[firstFileIndex];
      String filename2 = args[firstFileIndex + 1];
      String filename3 = args[firstFileIndex + 2];
      String filename4 = args[firstFileIndex + 3];
      PptMap map1 = FileIO.read_serialized_pptmap(new File(filename1),
                                                  false // use saved config
                                                  );
      PptMap map2 = FileIO.read_serialized_pptmap(new File(filename2),
                                                  false // use saved config
                                                  );
      manip1 = FileIO.read_serialized_pptmap(new File(filename3),
                                             false // use saved config
                                             );
      manip2 = FileIO.read_serialized_pptmap(new File(filename4),
                                             false // use saved config
                                             );

      // get the xor from these two manips
      treeManip = false;


      // RootNode pass_and_both = diff.diffPptMap (manip1, map2, includeUnjustified);
      // RootNode fail_and_both = diff.diffPptMap (manip2, map2, includeUnjustified);


      // get rid of the "both" invariants
      // MinusVisitor2 aMinusB = new MinusVisitor2();
      //      pass_and_both.accept (aMinusB);
      // fail_and_both.accept (aMinusB);


      RootNode pass_and_fail = diff.diffPptMap (manip1, manip2, includeUnjustified);



      XorInvariantsVisitor xiv = new XorInvariantsVisitor(System.out,
                                                          false,
                                                          false,
                                                          false);
      pass_and_fail.accept (xiv);

      // remove for the latest version
      treeManip = true;

      // form the root with tree manips
      RootNode root = diff.diffPptMap (map1, map2, includeUnjustified);


      // now run the stats visitor for checking matches
      //      MatchCountVisitor2 mcv = new MatchCountVisitor2
      //  (System.out, verbose, false);
      /*
      PptCountVisitor mcv = new PptCountVisitor
        (System.out, verbose, false);

      root.accept (mcv);
      mcv.printFinal();
      System.out.println ("Precison: " + mcv.calcPrecision());
      System.out.println ("Recall: " + mcv.calcRecall());
      System.out.println ("Success");
      //      System.exit(0);
      */




      MatchCountVisitor2 mcv2 = new MatchCountVisitor2
        (System.out, verbose, false);

      root.accept (mcv2);
      // print final is simply for debugging, remove
      // when experiments are over.
      mcv2.printFinal();

      // Most of the bug-experiments expect the final output
      // of the Diff to be these three lines.  It is best
      // not to change it.
      System.out.println ("Precison: " + mcv2.calcPrecision());
      System.out.println ("Recall: " + mcv2.calcRecall());
      System.out.println ("Success");
      throw new Daikon.TerminationMessage();

    } else if (numFiles > 2) {

      // The new stuff that allows multiple files -LL


      PptMap[] mapAr = new PptMap[numFiles];
      int j = 0;
      for (int i = firstFileIndex; i < args.length; i++) {
        String fileName = args[i];
        mapAr[j++] = FileIO.read_serialized_pptmap(new File (fileName),
                                                   false);
      }

      // Cascade a lot of the different invariants into one map,
      // and then put them into map1, map2

      // Initialize it all
      RootNode root = null;
      MultiDiffVisitor v1 = new MultiDiffVisitor (mapAr[0]);

      for (int i = 1; i < mapAr.length; i++) {
        root = diff.diffPptMap (mapAr[i], v1.currMap, includeUnjustified);
        root.accept (v1);
      }

      // now take the final result for the MultiDiffVisitor
      // and use it along side a null empty map
      PptMap map1 = v1.currMap;

      v1.printAll();
      return;
    } else {
      System.out.println (usage);
      throw new Daikon.TerminationMessage();
    }

    if (logging)
      System.err.println("Invariant Diff: Creating Tree");

    if (logging)
      System.err.println("Invariant Diff: Visiting Tree");

    RootNode root = diff.diffInvMap(invMap1, invMap2, includeUnjustified);

    if (stats) {
      DetailedStatisticsVisitor v =
        new DetailedStatisticsVisitor(continuousJustification);
      root.accept(v);
      System.out.print(v.format());
    }

    if (tabSeparatedStats) {
      DetailedStatisticsVisitor v =
        new DetailedStatisticsVisitor(continuousJustification);
      root.accept(v);
      System.out.print(v.repr());
    }

    if (printDiff) {
      PrintDifferingInvariantsVisitor v = new PrintDifferingInvariantsVisitor
        (System.out, verbose, printEmptyPpts, printUninteresting);
      root.accept(v);
    }

    if (printAll) {
      PrintAllVisitor v = new PrintAllVisitor
        (System.out, verbose, printEmptyPpts);
      root.accept(v);
    }

    if (minus) {
      if (outputFile != null) {
        MinusVisitor v = new MinusVisitor();
        root.accept(v);
        UtilMDE.writeObject(v.getResult(), outputFile);
        // System.out.println("Output written to: " + outputFile);
      } else {
        throw new Error("no output file specified on command line");
      }
    }

    if (xor) {
      if (outputFile != null) {
        XorVisitor v = new XorVisitor();
        root.accept(v);
        InvMap resultMap = v.getResult();
        UtilMDE.writeObject(resultMap, outputFile);
        if (debug.isLoggable(Level.FINE)) {
          debug.fine ("Result: " + resultMap.toString());
        }

        // System.out.println("Output written to: " + outputFile);
      } else {
        throw new Error("no output file specified on command line");
      }
    }

    if (union) {
      if (outputFile != null) {
        UnionVisitor v = new UnionVisitor();
        root.accept(v);
        UtilMDE.writeObject(v.getResult(), outputFile);
        // System.out.println("Output written to: " + outputFile);
      } else {
        throw new Error("no output file specified on command line");
      }
    }


    if (logging)
      System.err.println("Invariant Diff: Ending Log");

    // finished; return (and end program)
  }

  /**
   * Reads an InvMap from a file that contains a serialized InvMap or
   * PptMap.
   **/
  private InvMap readInvMap(File file) throws
  IOException, ClassNotFoundException {
    Object o = UtilMDE.readObject(file);
    if (o instanceof InvMap) {
      return (InvMap) o;
    } else {
      PptMap pptMap = FileIO.read_serialized_pptmap(file, false);
      return convertToInvMap(pptMap);
    }
  }

  /**
   * Extracts the PptTopLevel and Invariants out of a pptMap, and
   * places them into an InvMap.  Maps PptTopLevel to a List of
   * Invariants.  The InvMap is a cleaner representation than the
   * PptMap, and it allows us to more easily manipulate the contents.
   * The InvMap contains exactly the elements contained in the PptMap.
   * Conditional program points are also added as keys.  Filtering is
   * done when creating the pair tree.  The ppts in the InvMap must be
   * sorted, but the invariants need not be sorted.
   **/
  public InvMap convertToInvMap(PptMap pptMap) {
    InvMap map = new InvMap();

    // Created sorted set of top level ppts, possibly including
    // conditional ppts
    SortedSet<PptTopLevel> ppts = new TreeSet<PptTopLevel>(PPT_COMPARATOR);
    ppts.addAll(pptMap.asCollection());

    for (PptTopLevel ppt : ppts) {
      if (ignoreNumberedExits && ppt.ppt_name.isNumberedExitPoint())
        continue;

      // List<Invariant> invs = ppt.getInvariants();
      List<Invariant> invs = UtilMDE.sortList(ppt.getInvariants(), PptTopLevel.icfp);
      map.put(ppt, invs);
      if (examineAllPpts) {
        // Add conditional ppts
        for (Iterator<PptConditional> i2 = ppt.cond_iterator(); i2.hasNext(); ) {
          PptConditional pptCond = i2.next();
          List<Invariant> invsCond = UtilMDE.sortList (pptCond.getInvariants(),
                                          PptTopLevel.icfp);
          // List<Invariant> invsCond = pptCond.getInvariants();
          map.put(pptCond, invsCond);
        }
      }
    }
    return map;
  }

  /**
   * Returns a pair tree of corresponding program points, and
   * corresponding invariants at each program point.  This tree can be
   * walked to determine differences between the sets of invariants.
   * Calls diffInvMap and asks to include all justified invariants
   **/
  public RootNode diffInvMap(InvMap map1, InvMap map2) {
    return diffInvMap(map1, map2, true);
  }

  /**
   * Returns a pair tree of corresponding program points, and
   * corresponding invariants at each program point.  This tree can be
   * walked to determine differences between the sets of invariants.
   * The tree consists of the invariants in map1 and map2.  If
   * includeUnjustified is true, the unjustified invariants are included.
   **/
  public RootNode diffInvMap(InvMap map1, InvMap map2,
                             boolean includeUnjustified) {
    RootNode root = new RootNode();

    Iterator<Pair<PptTopLevel,PptTopLevel>> opi = new OrderedPairIterator<PptTopLevel>(map1.pptSortedIterator(PPT_COMPARATOR), map2.pptSortedIterator(PPT_COMPARATOR), PPT_COMPARATOR);
    while (opi.hasNext()) {
      Pair<PptTopLevel,PptTopLevel> ppts = opi.next();
      PptTopLevel ppt1 = ppts.a;
      PptTopLevel ppt2 = ppts.b;
      if (shouldAdd(ppt1) || shouldAdd(ppt2)) {
        PptNode node = diffPptTopLevel(ppt1, ppt2, map1, map2,
                                       includeUnjustified);
        root.add(node);
      }
    }

    return root;
  }


  /**
   * Diffs two PptMaps by converting them to InvMaps.  Provided for
   * compatibiliy with legacy code.
   * Calls diffPptMap and asks to include all invariants.
   **/
  public RootNode diffPptMap(PptMap pptMap1, PptMap pptMap2) {
    return diffPptMap(pptMap1, pptMap2, true);
  }

  /**
   * Diffs two PptMaps by converting them to InvMaps.  Provided for
   * compatibiliy with legacy code.
   * If includeUnjustified is true, the unjustified invariants are included.
   **/
  public RootNode diffPptMap(PptMap pptMap1, PptMap pptMap2,
                             boolean includeUnjustified) {
    InvMap map1 = convertToInvMap(pptMap1);
    InvMap map2 = convertToInvMap(pptMap2);
    return diffInvMap(map1, map2, includeUnjustified);
  }

  /**
   * Returns true if the program point should be added to the tree,
   * false otherwise.
   **/
  private boolean shouldAdd(PptTopLevel ppt) {
    if (examineAllPpts) {
      return true;
    } else {
      if (ppt == null) {
        return false;
      } else if (ppt.ppt_name.isEnterPoint()) {
        return true;
      } else if (ppt.ppt_name.isCombinedExitPoint()) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Takes a pair of corresponding top-level program points and maps,
   * and returns a tree of the corresponding invariants.  Either of
   * the program points may be null.
   * If includeUnjustied is true, the unjustified invariants are included.
   **/
  private PptNode diffPptTopLevel(PptTopLevel ppt1, PptTopLevel ppt2,
                                  InvMap map1, InvMap map2,
                                  boolean includeUnjustified) {
    PptNode pptNode = new PptNode(ppt1, ppt2);

    Assert.assertTrue(ppt1 == null || ppt2 == null ||
                      PPT_COMPARATOR.compare(ppt1, ppt2) == 0,
                      "Program points do not correspond");

    List<Invariant> invs1;
    if (ppt1 != null && !treeManip) {
      invs1 = map1.get(ppt1);
      Collections.sort(invs1, invSortComparator1);
    }

    else if (ppt1 != null && treeManip && !isCond(ppt1)) {
      HashSet<String> repeatFilter = new HashSet<String>();
      ArrayList<Invariant> ret = new ArrayList<Invariant> ();
      invs1 = map1.get(ppt1);
      for (Invariant inv : invs1) {
        if (/*inv.justified() && */inv instanceof Implication) {
          Implication imp = (Implication) inv;
          if (!repeatFilter.contains (imp.consequent().format_using(OutputFormat.JAVA))) {
            repeatFilter.add (imp.consequent().format_using(OutputFormat.JAVA));
            ret.add (imp.consequent());
          }
          // add both sides of a biimplication
          if (imp.iff == true) {
            if (!repeatFilter.contains(imp.predicate().format())) {
              repeatFilter.add (imp.predicate().format());
              ret.add (imp.predicate());
            }
          }
        }
        // Report invariants that are not part of implications
        // "as is".
        else {
          ret.add (inv);
        }
      }
      invs1 = ret;
      Collections.sort(invs1, invSortComparator1);
    }

    else {
      invs1 = new ArrayList<Invariant>();
    }

    List<Invariant> invs2;
    if (ppt2 != null && !treeManip) {
      invs2 = map2.get(ppt2);
      Collections.sort(invs2, invSortComparator2);
    } else {
      if ( false && treeManip && isCond (ppt1)) {
        // remember, only want to mess with the second list
        invs2 = findCondPpt (manip1, ppt1);
        List<Invariant> tmpList = findCondPpt (manip2, ppt1);

        invs2.addAll (tmpList);

        // This uses set difference model instead of XOR
        //        invs2 = tmpList;

        // must call sort or it won't work!
        Collections.sort(invs2, invSortComparator2);
      }
      else if (treeManip && ppt2 != null && !isCond(ppt2)) {

        invs2 = findNormalPpt (manip1, ppt2);
        invs2.addAll ( findNormalPpt (manip2, ppt2));
        Collections.sort (invs2, invSortComparator2);
      }
      else {
        invs2 = new ArrayList<Invariant>();
      }
    }

    Iterator<Pair<Invariant,Invariant>> opi = new OrderedPairIterator<Invariant>(invs1.iterator(), invs2.iterator(),
                                           invPairComparator);
    while (opi.hasNext()) {
      Pair invariants = opi.next();
      Invariant inv1 = (Invariant) invariants.a;
      Invariant inv2 = (Invariant) invariants.b;
      if (!includeUnjustified) {
        if ((inv1 != null) && !(inv1.justified())) {
          inv1 = null;
        }
        if ((inv2 != null) && !(inv2.justified())) {
          inv2 = null;
        }
      }
      if ((inv1 != null) || (inv2 != null)) {
        InvNode invNode = new InvNode(inv1, inv2);
        pptNode.add(invNode);
      }
    }


    return pptNode;
  }

  private boolean isCond (PptTopLevel ppt) {
    return (ppt instanceof PptConditional);
  }

  private List<Invariant> findCondPpt (PptMap manip, PptTopLevel ppt) {
    // targetName should look like this below
    // Contest.smallestRoom(II)I:::EXIT9;condition="max < num
    String targetName = ppt.name();

    String targ = targetName.substring (0, targetName.lastIndexOf(";condition"));

    for ( Iterator<String> i = manip.nameStringSet().iterator(); i.hasNext(); ) {
      String somePptName = i.next();
      // A conditional Ppt always contains the normal Ppt
      if (targ.equals (somePptName)) {
        PptTopLevel repl = manip.get (somePptName);
        return repl.getInvariants();
      }
    }
    //    System.out.println ("Could not find the left hand side of implication!!!");
    System.out.println ("LHS Missing: " + targ);
    return new ArrayList<Invariant>();
  }


  private List<Invariant> findNormalPpt (PptMap manip, PptTopLevel ppt) {
    // targetName should look like this below
    // Contest.smallestRoom(II)I:::EXIT9
    String targetName = ppt.name();

    //    String targ = targetName.substring (0, targetName.lastIndexOf(";condition"));

    for ( Iterator<String> i = manip.nameStringSet().iterator(); i.hasNext(); ) {
      String somePptName = i.next();
      // A conditional Ppt always contains the normal Ppt
      if (targetName.equals (somePptName)) {
        PptTopLevel repl = manip.get (somePptName);
        return UtilMDE.sortList(repl.getInvariants(), PptTopLevel.icfp);
      }
    }
    //    System.out.println ("Could not find the left hand side of implication!!!");
    System.out.println ("LHS Missing: " + targetName);
    return new ArrayList<Invariant>();
  }


  /**
   * Use the comparator for sorting both sets and creating the pair
   * tree.
   **/
  public void setAllInvComparators(Comparator<Invariant> c) {
    setInvSortComparator1(c);
    setInvSortComparator2(c);
    setInvPairComparator(c);
  }

  /**
   * If the classname is non-null, returns the comparator named by the
   * classname.  Else, returns the default.
   **/
  private static Comparator<Invariant> selectComparator
    (String classname, Comparator<Invariant> defaultComparator) throws
    ClassNotFoundException, InstantiationException, IllegalAccessException {

    if (classname != null) {
      Class cls = Class.forName(classname);
      @SuppressWarnings("unchecked")
      Comparator<Invariant> cmp = (Comparator<Invariant>) cls.newInstance();
      return cmp;
    } else {
      return defaultComparator;
    }
  }

  /** Use the comparator for sorting the first set. **/
  public void setInvSortComparator1(Comparator<Invariant> c) {
    invSortComparator1 = c;
  }

  /** Use the comparator for sorting the second set. **/
  public void setInvSortComparator2(Comparator<Invariant> c) {
    invSortComparator2 = c;
  }

  /** Use the comparator for creating the pair tree. **/
  public void setInvPairComparator(Comparator<Invariant> c) {
    invPairComparator = c;
  }

}
