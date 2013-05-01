package daikon;

import java.io.*;
import java.util.*;
import utilMDE.*;

/** Maps from a name (a String) to a PptTopLevel. */
public class PptMap
  implements Serializable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20040921L;

  private final Map<String,PptTopLevel> nameToPpt
    = new LinkedHashMap<String,PptTopLevel>();

  public void add(PptTopLevel ppt) {
    nameToPpt.put(ppt.name(), ppt);
  }

  public void addAll(List<PptTopLevel> ppts) {
    for (PptTopLevel ppt : ppts) {
      add (ppt);
    }
  }

  public PptTopLevel get(String name) {
    return nameToPpt.get(name);
  }

  public PptTopLevel get(PptName name) {
    return get(name.toString());
  }

  public boolean containsName(String name) {
    return nameToPpt.containsKey(name);
  }

  /** Returns all of the program points in the map **/
  public Collection<PptTopLevel> all_ppts() {
    return (nameToPpt.values());
  }

  /**
   * @return unstably-ordered collection of PptTopLevels
   * @see #pptIterator()
   **/
  public Collection<PptTopLevel> asCollection() {
    return Collections.unmodifiableCollection(nameToPpt.values());
  }

  public Collection<String> nameStringSet() {
    return Collections.unmodifiableSet(nameToPpt.keySet());
  }

  /**
   * @return an iterator over the PptTopLevels in this, sorted by
   * Ppt.NameComparator on their names.  This is good for consistency.
   **/
  public Iterator<PptTopLevel> pptIterator() {
    TreeSet<PptTopLevel> sorted = new TreeSet<PptTopLevel>(new Ppt.NameComparator());
    sorted.addAll(nameToPpt.values());
    // Use a (live) view iterator to get concurrent modification
    // exceptions, and an iterator over sorted to get consistency.
    final Iterator<PptTopLevel> iter_view = nameToPpt.values().iterator();
    final Iterator<PptTopLevel> iter_sort = sorted.iterator();
    return new Iterator<PptTopLevel>() {
        public boolean hasNext() {
          boolean result = iter_view.hasNext();
          Assert.assertTrue(result == iter_sort.hasNext());
          return result;
        }
        public PptTopLevel next() {
          iter_view.next(); // to check for concurrent modifications
          return iter_sort.next();
        }
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
  }

  /**
   * @return an iterator over the PptTopLevels in this, sorted by
   * Ppt.NameComparator on their names.  This differs from pptIterator()
   * in that it includes all ppts (including conditional ppts).
   **/
  public Iterator<PptTopLevel> ppt_all_iterator() {
    TreeSet<PptTopLevel> sorted = new TreeSet<PptTopLevel>(new Ppt.NameComparator());
    sorted.addAll(nameToPpt.values());
    // Use a (live) view iterator to get concurrent modification
    // exceptions, and an iterator over sorted to get consistency.
    final Iterator<PptTopLevel> iter_view = nameToPpt.values().iterator();
    final Iterator<PptTopLevel> iter_sort = sorted.iterator();
    return new Iterator<PptTopLevel>() {
        Iterator<PptConditional> cond_iterator = null;
        public boolean hasNext() {
          if ((cond_iterator != null) && cond_iterator.hasNext())
            return (true);
          boolean result = iter_view.hasNext();
          Assert.assertTrue(result == iter_sort.hasNext());
          return result;
        }
        public PptTopLevel next() {
          if ((cond_iterator != null) && cond_iterator.hasNext())
            return (cond_iterator.next());
          iter_view.next(); // to check for concurrent modifications
          PptTopLevel ppt = iter_sort.next();
          if ((ppt != null) && ppt.has_splitters())
            cond_iterator = ppt.cond_iterator();
          return (ppt);
        }
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
  }

  /** Iterate over the PptTopLevels and trim them. */
  public void trimToSize() {
    for (PptTopLevel ppt : nameToPpt.values()) {
      ppt.trimToSize();
    }
  }

  /**
   * Check the rep invariant of this.  Throws an Error if incorrect.
   **/
  public void repCheck() {
    for (Iterator<PptTopLevel> i = this.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      ppt.repCheck();
    }
  }

  /**
   * Return the number of active PptSlices.
   **/
  public int countSlices() {
    int result = 0;
    for (Iterator<PptTopLevel> i = this.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      result += ppt.numViews();
    }
    return result;
  }

  public int size() {
    return nameToPpt.size();
  }

  public String toString() {
    return "PptMap: " + nameToPpt.toString();
  }

  /**
   * Blow away any PptTopLevels that never saw any samples (to reclaim space).
   **/
  public void removeUnsampled() {
    Iterator<PptTopLevel> iter = nameToPpt.values().iterator();
    while (iter.hasNext()) {
      PptTopLevel ppt = iter.next();
      if ((ppt.num_samples() == 0)
          && ! FileIO.has_unmatched_procedure_entry(ppt))
        iter.remove();
    }
  }
}
