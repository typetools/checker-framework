package utilMDE;


/** A Partitioner accepts Objects and assigns them to an equivalence
 * class represented by that Object. See MultiRandSelector.
 */

public interface Partitioner<ELEMENT,CLASS> {

  /** @param obj the Object to be assigned to a bucket
   *  @return A key representing the bucket containing obj
   */
    public CLASS assignToBucket (ELEMENT obj);

}
