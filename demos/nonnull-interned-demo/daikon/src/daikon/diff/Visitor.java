package daikon.diff;

/**
 * All visitors must implement this interface.
 **/
public interface Visitor {

  public void visit(RootNode node);
  public void visit(PptNode node);
  public void visit(InvNode node);

}
