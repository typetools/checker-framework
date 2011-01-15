package daikon.dcomp;

import utilMDE.*;
import daikon.chicory.DaikonVariableInfo;

import java.lang.ref.*;
import java.util.*;

/**
 * Union/Find datastructure for Objects without the ranking optimization.
 * All references to the Objects are weak so that they will be removed from
 * the sets when no longer referenced.
 */

/* TagEntry now implements tracing, which means that if A trace-points to B,
 * then we guarantee that A and B have directly interacted (stronger condition
 * than being in the same comparability set). Additionally, each trace-point
 * is tagged with a class name, method name, and line number indicating where
 * the relevant interaction occurred.
 *
 * TODO:
 *   Let A --> B and C --> D be in the same tree, where D is not a child of A
 *   and B is not a child of C. When union(A, C), consider x = rank(C) - rank(B)
 *   and y = rank(A) - rank(D). If either x or y is greater than 0, then A --> C
 *   if x > y, and C --> A otherwise.
 *
 *   Tracers should list line numbers and files where they are created.
 *
 *   Union-by-rank on normal and tracer trees.
 *
 * -charlest
 */

class TagEntry extends WeakReference<Object> {

  /** Maps each object to its entry in the Union-Find datastructure **/
  public static final WeakIdentityHashMap<Object,TagEntry> object_map
    = new WeakIdentityHashMap<Object,TagEntry>();

  private static SimpleLog debug = new SimpleLog(false);

  /**
   * Parent in the tree that represents the set for this element.  If this,
   * this entry is the representative one
   */
  private TagEntry parent;

  /**
   * Element in the tree that this element interacted with.
   * Important!: tracer is null if this has no tracer.
   *
   * if TRACING_ENABLED?
   */
  private TagEntry tracer;
  protected String trace_loc = "";

  /** Create an entry as a separate set **/
  public TagEntry (Object obj) {
    super(obj);
    this.parent = null;
    this.tracer = null;
    // System.out.printf("Make %s with parent %s%n", this, this.parent);
  }

  /** Create an entry and add it to an existing set **/
  public TagEntry (Object obj, TagEntry parent) {
    super (obj);
    this.parent = parent;
    this.tracer = parent; // if TRACING_ENABLED?
    // System.out.printf("Made %s with parent p%s%n", this, this.parent);
  }

  /**
   * Creates a set that only contains obj
   */
  public static TagEntry create (Object obj) {
    assert !object_map.containsKey (obj);
    TagEntry entry = new TagEntry(obj);
    object_map.put (obj, entry);
    return (entry);
  }

  /**
   * Merge the sets that contain the specified objects.  If this is the
   * first time either of the objects was seen, create an entry for it.
   */
  public static void union (Object obj1, Object obj2) {
    assert (obj1 != null) && (obj2 != null);
    debug.log ("union of '%s' and '%s'%n", obj1, obj2);

    TagEntry o1 = get_entry(obj1), o2 = get_entry(obj2);
    TagEntry r1 = o1.find(), r2 = o2.find();

    // if TRACING_ENABLED?

    if (r1 != r2) {
      r2.parent = r1;
      o1.rootMe(); o2.rootMe();
      o2.tracer = o1;
      o2.trace_loc = generateTraceString();
    }
  }

  public static String generateTraceString() {
    ArrayList<StackTraceElement> blarg =
      new ArrayList<StackTraceElement>(Arrays.asList(new Exception().getStackTrace()));
    if (blarg.get(blarg.size() - 1).getClassName()
				.equals("daikon.dcomp.Premain$ShutdownThread")) return "";
    do blarg.remove(0);
	while (blarg.get(0).getClassName().equals("daikon.dcomp.DCRuntime") ||
           blarg.get(0).getClassName().equals("daikon.dcomp.TagEntry"));

    if (daikon.DynComp.trace_line_depth == 1) {
      StackTraceElement first = blarg.get(0);
      return first.getClassName() + ":" +
             first.getMethodName() + "(), " +
             first.getLineNumber();
    } else {
			ArrayList<String> blarg2 = new ArrayList<String>(daikon.DynComp.trace_line_depth);
			try {
			  for (int i = 0; i < daikon.DynComp.trace_line_depth; i++) {
					StackTraceElement ste = blarg.get(i);
					String cname = ste.getClassName();
					cname = cname.substring(cname.lastIndexOf("."));
					String fname = ste.getMethodName();
					int lnum = ste.getLineNumber();
					blarg2.add(cname + ":" + fname + "(), " + lnum);
				}
			} catch (IndexOutOfBoundsException e) {}
			String result = "";
			for (String s : blarg2) result += s + " <- ";
			return result.substring(0, result.length() - 4);
    }
	}

  /**
   * Find the entry associated with obj.  If an entry does not currently
   * exist, create it
   */
  public static TagEntry get_entry (Object obj) {

    assert obj != null;
    TagEntry entry = object_map.get(obj);
    if (entry == null)
      entry = create (obj);
    return (entry);
  }

  public static String get_line_trace (Object obj) {
    return get_entry(obj).trace_loc;
  }

  /**
   * Find the TagEntry that is the representative of this set.
   * As part of finding the representative, the path from the specified entry
   * to the representative is compressed.
   */
  public TagEntry find() {

    if (parent == null)
      return this;

    // Find the tag at the top of the list
    TagEntry tag = this;
    while (tag.parent != null)
      tag = tag.parent;
    TagEntry top = tag;

    // Set everyone to point to the top
    tag = this;
    while (tag.parent != null) {
      TagEntry next = tag.parent;
      tag.parent = top;
      tag = next;
    }

    return top;
  }

  /**
   * Given an Object returns the Object that is the representative for
   * its set.  If there is no entry for Object in the map, it must be
   * in a set by itself.
   */
  public static Object find (Object obj) {
    assert obj != null;
    TagEntry entry = object_map.get (obj);
    if (entry == null)
      return obj;
    TagEntry root = entry.find();

    // It shouldn't matter that this isn't a member of the set, only that
    // it is unique.
    // return (root);

    // If the root object has been garbage collected, just return
    // a reference to the tag instead.  Since the caller has no references
    // to the root, it can't matter what we returned.
    Object root_ref = root.get();
    if (root_ref == null)
      root_ref = root;
    return (root_ref);
  }

  public static Object tracer_find (Object obj) {
    TagEntry entry = object_map.get(obj);
    if (entry == null) { return obj; }
    TagEntry tracer = entry.getTracer();
    if (tracer == null) { return null; }
    Object tr_ref = tracer.get();
    if (tr_ref == null) tr_ref = tracer;
    return (tr_ref);
  }

  public static Object troot_find (Object obj) {
    TagEntry entry = object_map.get(obj);
    if (entry == null) { return obj; }
    TagEntry troot = entry.getTraceRoot();
    Object tr_ref = troot.get();
    if (tr_ref == null) tr_ref = troot;
    return (tr_ref);
  }

  /**
   * Recursively traces from this object to the root of its tracer tree,
   * and reverses the direction of every pointer on the path, such that
   * this object is now the root of its tracer tree. (Imprecise wording, I know)
   */
  public void reroute(TagEntry newTracer, String tloc) {
		try { this.tracer.reroute(this, trace_loc); }
    catch (NullPointerException e) { }
    finally { this.tracer = newTracer; this.trace_loc = tloc; }
//    if(this.tracer != null) { System.out.println("Tracer not null"); this.tracer.reroute(this); }
//    this.tracer = newTracer;
  }

  public void rootMe() { this.reroute(null, ""); }

  /**
   * Returns each of the sets with elements in each set on a separate
   * line.
   */
  public static String dump() {

    LinkedHashMap<Object, List<Object>> sets
      = new LinkedHashMap<Object,List<Object>>();

    /* Fill sets from object_map by placing every object in an ArrayList
     * whose key is its root. */
    for (Object obj : object_map.keySet()) {
      Object rep = find (obj);
      List<Object> set = sets.get (rep);
      if (set == null) {
        set = new ArrayList<Object>();
        sets.put (rep, set);
      }
      set.add (obj);
    }

    String out = String.format ("%d objects in object_map%n",
                                object_map.size());
    for (Object rep : sets.keySet()) {
      List<Object> set = sets.get (rep);
      String line = "";
      for (Object entry : set) {
        if (line != "")
          line += ", ";
        if (entry instanceof DaikonVariableInfo)
          line += String.format ("%s ", ((DaikonVariableInfo)entry).getName());
        else
          line += String.format ("%s [%s]", entry.getClass(), entry);
      }
      out += String.format ("%s%n", line);
    }
    return (out);
  }

  /** Returns the tracer of this node **/
  public TagEntry getTracer() { return tracer; }

  public TagEntry getTraceRoot() {
    if (tracer == null) return this;
    else return tracer.getTraceRoot();
  }
}
