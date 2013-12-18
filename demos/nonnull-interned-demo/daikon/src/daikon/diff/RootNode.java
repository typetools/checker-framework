package daikon.diff;

import utilMDE.NoType;

/**
 * The root of the tree.  All its children are PptNodes.
 **/
public class RootNode extends Node<NoType,PptNode> {

  public RootNode() {
    super();
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
