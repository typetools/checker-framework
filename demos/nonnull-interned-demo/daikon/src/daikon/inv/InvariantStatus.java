package daikon.inv;

import checkers.quals.*;

/**
 * This class is an enumerated type representing the possible results of
 * adding an sample to an invariant.
 */
public final /*@Interned*/ class InvariantStatus {

  private final String status;

  private InvariantStatus(String status) {
    this.status = status;
  }

  public String toString() { return status; }

  /**
   * The InvariantStatus that represents no change being made to the
   * invariant's validity.
   */
  public static final InvariantStatus NO_CHANGE = new InvariantStatus("no_change");

  /**
   * The InvariantStatus that represents an invariant being falsified.
   */
  public static final InvariantStatus FALSIFIED = new InvariantStatus("falsified");

  /**
   * The InvariantStatus that represents an invariant's condition being weakened.
   * For example OneOf{1,3} going to OneOf{1,3,10}.
   */
  public static final InvariantStatus WEAKENED = new InvariantStatus("weakened");

}
