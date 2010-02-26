// RandomSelector.java
package utilMDE;

import checkers.quals.*;
import java.util.*;


/**
 *
 * <b>RandomSelector</b> selects k elements uniformly at random from
 * an iteration over n elements using only O(k) space instead of O(n)
 * space.  For example, selecting 1 element from a FileStream
 * containing 1000 elements will take O(1) space. The class takes as
 * input the number k during initialization and then can accept() any
 * number of Objects in the future.  At any point in time, getValues()
 * will either return k randomly selected elements from the elements
 * previous accepted or if accept() was called fewer than k times, will
 * return all elements previously accepted.
 *
 * <p>The random selection is independent between every constructed
 * instance of RandomSelector objects, but for the same instance,
 * multiple calls to getValues() are not independent. Making two calls
 * to consecutive getValues() without an accept() in between will
 * return two new Lists containing the same elements.
 *
 * <p>A second mode allows for a fixed probability of randomly keeping
 *  each item as opposed to a fixed number of samples.
 *
 * <P>SPECFIELDS:
 * <BR>current_values  : Set : The values chosen based on the Objects observed
 * <BR>number_observed : int : The number of Objects observed
 * <BR>number_to_take  : int : The number of elements to choose ('k' above)
 * <BR>keep_probability: double :  The percentage of elements to keep
 * <BR>selector_mode :
 *       {FIXED,PERCENT}  : either fixed amount of samples or fixed percent.
 *
 * <P>Example use:
 * <BR> // randomly selects 100 lines of text from a file
 * <pre>
 *  List selectedLines = null;
 *  try {
 *     BufferedReader br = new BufferedReader
 *       (new FileReader ("myfile.txt"));
 *     RandomSelector selector = new RandomSelector (100);
 *     while (br.ready()) {
 *       selector.accept (br.readLine());
 *     }
 *     selectedLines = selector.getValues();
 *   }
 *   catch (IOException e2) { e2.printStackTrace(); }
 * </pre>
 *
 **/

@DefaultQualifier("checkers.nullness.quals.NonNull")
public class RandomSelector<T> {

    // Rep Invariant: values != null && values.size() <= num_elts &&
    //                ((num_elts == -1 && coin_toss_mode == true) ||
    //                 (keep_probability == -1.0 && coin_toss_mode == false))

    // Abstraction Function:
    // 1. for all elements, 'val' of AF(current_values),
    //    this.values.indexOf (val) != -1
    // 2. AF(number_observed) = this.observed
    // 3. AF(number_to_take) = this.num_elts
    // 4. AF(keep_probability) = this.keep_probability
    // 5. AF(selector_mode) = fixed amount if coin_toss_mode == true
    //                        fixed percentage if coin_toss_mode == false

    private int num_elts = -1;
    private int observed;
    private Random generator;
    private ArrayList<T> values;
    private boolean coin_toss_mode = false;
    private double keep_probability = -1.0;


    /** @param num_elts The number of elements intended to be selected
     * from the input elements
     *
     * Sets 'number_to_take' = num_elts
     **/
    public RandomSelector (int num_elts) {
        this (num_elts, new Random());
    }


    /** @param num_elts The number of elements intended to be selected
     * from the input elements.
     * @param r The seed to give for random number generation.
     *
     * Sets 'number_to_take' = num_elts
     **/
    public RandomSelector (int num_elts, Random r) {
        values = new ArrayList<T>();
        this.num_elts = num_elts;
        observed = 0;
        generator = r;
    }

    /** @param keep_probability The probability that each element is
     * selected from the oncoming Iteration.
     * @param r The seed to give for random number generation.
     **/
    public RandomSelector (double keep_probability, Random r) {
        values = new ArrayList<T>();
        this.keep_probability = keep_probability;
        coin_toss_mode = true;
        observed = 0;
        generator = r;
    }

    /** <P>When in fixed sample mode, increments the number of
     * observed elements i by 1, then with probability k / i, the
     * Object 'next' will be added to the currently selected values
     * 'current_values' where k is equal to 'number_to_take'. If the
     * size of current_values exceeds number_to_take, then one of the
     * existing elements in current_values will be removed at random.
     *
     *
     * <P>When in probability mode, adds next to 'current_values' with
     * probability equal to 'keep_probability'.
     *
     **/
    public void accept (T next) {

        // if we are in coin toss mode, then we want to keep
        // with probability == keep_probability.
        if (coin_toss_mode) {
            if (generator.nextDouble() < keep_probability) {
                values.add (next);
                // System.out.println ("ACCEPTED " + keep_probability );
            }
            else {
                // System.out.println ("didn't accept " + keep_probability );
            }
            return;
        }

        // in fixed sample mode, the i-th element has a k/i chance
        // of being accepted where k is number_to_take.
        if (generator.nextDouble() < ((double) num_elts / (++observed))) {
            if (values.size() < num_elts) {
                values.add (next);
            }
            else {
                int rem = (int) (values.size() * generator.nextDouble());
                values.set (rem, next);
            }
        }
        // do nothing if the probability condition is not met
    }

    /** Returns current_values, modifies none.  **/
    public List<T> getValues() {
        // avoid concurrent mod errors and rep exposure
        ArrayList<T> ret = new ArrayList<T>();
        ret.addAll (values);
        return ret;
    }

}
