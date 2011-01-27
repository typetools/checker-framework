package daikon.inv.filter;

import daikon.inv.*;

// The template for an invariant filter.
// Groups of invariant filters are managed by InvariantFilters.
public abstract class InvariantFilter {
  boolean isOn;

  public InvariantFilter( boolean isOn ) {
    this.isOn = isOn;
  }

  // TODO:  This is a hack.  Should add constructors that take a boolean
  // for every subclass.
  public InvariantFilter() {
    this( true );
  }

  public abstract String getDescription();

  public void turnOn()  { isOn = true; }
  public void turnOff() { isOn = false; }

  public boolean getSetting() {
    return isOn;
  }

  public boolean shouldDiscard( Invariant invariant ) {
    if (! isOn)
      return false;
    else
      return shouldDiscardInvariant( invariant );
  }

  abstract boolean shouldDiscardInvariant( Invariant invariant );
}
