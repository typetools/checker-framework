// MultiRandSelector.java
package utilMDE;
import java.util.*;

/**
 * <b>MultiRandSelector</b>, like <b>RandomSelector</b>, performs a
 * uniform random selection over an iteration of objects. However, the
 * objects in the iteration may be partitioned so that the random
 * selection chooses the same number from each group. For example,
 * given data about incomes by state, it may be more useful to select
 * 1000 people from each state rather than 50,000 from the
 * nation. Also, for selecting invocations in a Daikon trace file, it
 * may be more useful to select an equal number of samples per program
 * point.

 * <p>The performance is the equal to running a set of RandomSelector
 * Objects, one for each bucket, as well as some overhead for
 * determining which bucket to assign to each Object in the iteration.
 *
 * <p>To use this class, call this.accept() on every Object in the
 * iteration to be sampled. Then, call valuesIter() to receive an
 * iteration of all the values selected by the random selection.
 *
 *
 **/
public class MultiRandSelector<T> {

    private int num_elts = -1;
    private boolean coin_toss_mode;
    private double keep_probability = -1.0;
    private Random seed;
    private Partitioner<T,T> eq;

    private HashMap<T,RandomSelector<T>> map;

  /** @param num_elts the number of elements to select from each
   *  bucket
   *  @param eq partioner that determines how to partition the objects from
   *  the iteration.
   */
    public MultiRandSelector (int num_elts, Partitioner<T,T> eq) {
        this (num_elts, new Random(), eq);
    }

    public MultiRandSelector (double keep_prob, Partitioner<T,T> eq) {
        this (keep_prob, new Random(), eq);
    }

    public MultiRandSelector (int num_elts, Random r,
                              Partitioner<T,T> eq) {

        this.num_elts = num_elts;
        seed = r;
        this.eq = eq;
        map = new HashMap<T,RandomSelector<T>>();
    }

    public MultiRandSelector (double keep_prob, Random r,
                              Partitioner<T,T> eq) {
        this.keep_probability = keep_prob;
        coin_toss_mode = true;
        seed = r;
        this.eq = eq;
        map = new HashMap<T,RandomSelector<T>>();
    }

    public void acceptIter (Iterator<T> iter) {
        while (iter.hasNext()) {
            accept (iter.next());
        }
    }


    /**
     */
    public void accept (T next) {
        T equivClass = eq.assignToBucket (next);
        if (equivClass == null)
            return;
        RandomSelector<T> delegation = map.get (equivClass);
        if (delegation == null) {
            delegation = (coin_toss_mode) ?
                new RandomSelector<T> (keep_probability, seed) :
                new RandomSelector<T> (num_elts, seed);
            map.put (equivClass, delegation);
        }
        delegation.accept (next);
    }

    // TODO: is there any reason not to simply return a copy?
    /** NOT safe from concurrent modification. */
    public Map<T,RandomSelector<T>> values () {
        return map;
    }

    /** Returns an iterator of all objects selected. */
    public Iterator<T> valuesIter() {
        ArrayList<T> ret = new ArrayList<T>();
        for (RandomSelector<T> rs : map.values()) {
            ret.addAll (rs.getValues());
        }
        return ret.iterator();
    }

}
