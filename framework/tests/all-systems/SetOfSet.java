package wildcards;

@SuppressWarnings("initialization")
public class SetOfSet<E> {

  public void addAll(SetOfSet<? extends E> s) {
    E[] svalues = s.values;
  }

  protected E[] values;
}
