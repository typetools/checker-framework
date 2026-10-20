package wildcards;

@SuppressWarnings({
  "initialization",
  "initializedfields:contracts.postcondition",
  "unneeded.suppression" // The suppression is needed when the InitializedFields Checker is run
  // without the Value Checker.
}) // field isn't set
public class SetOfSet<E> {

  public void addAll(SetOfSet<? extends E> s) {
    E[] svalues = s.values;
  }

  protected E[] values;
}
