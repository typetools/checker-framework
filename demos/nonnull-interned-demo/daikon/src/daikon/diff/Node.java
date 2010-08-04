package daikon.diff;

import java.util.*;
import utilMDE.Pair;

/**
 * All nodes must subclass this class.
 * The type parameter CONTENT is (half of) the type of the objects stored in this
 * node:  they are Pair<CONTENT,CONTENT>.
 * The type parameter CHILD is the type of the children (and is ignored if
 * there are no children).
 **/
public abstract class Node<CONTENT,CHILD> {

  private List<CHILD> children = new ArrayList<CHILD>();
  private Pair<CONTENT,CONTENT> userObject = null;

  public Node() {
  }

  public Node(Pair<CONTENT,CONTENT> userObject) {
    this.userObject = userObject;
  }

  public Node(CONTENT left, CONTENT right) {
    this.userObject = new Pair<CONTENT,CONTENT>(left, right);
  }

  public void add(CHILD newChild) {
    children.add(newChild);
  }

  public Iterator<CHILD> children() {
    return children.iterator();
  }

  public Pair<CONTENT,CONTENT> getUserObject() {
    return userObject;
  }

  public CONTENT getUserLeft() {
    return userObject.a;
  }

  public CONTENT getUserRight() {
    return userObject.b;
  }

  public abstract void accept(Visitor v);

}
