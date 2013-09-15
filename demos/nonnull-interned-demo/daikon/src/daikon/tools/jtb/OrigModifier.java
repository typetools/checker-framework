package daikon.tools.jtb;

import java.util.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;

/**
 * OrigModifier is a visitor that places "orig()" around varible names
 * and correspondingly corrects positioning fields of all the tokens in
 * tree to accomodate the change.  For example, the expression get(this.x)
 * would be changed to get(orig(this.x)).
 **/

public class OrigModifier extends DepthFirstVisitor {

  private int columnshift = 0;
  private int columnshiftline = -1;

  // columnshifting only applies to a single line, then is turned off agian.
  // States for the variables:
  // columnshift == 0, columnshiftline == -1:
  //    no column shifting needed
  // columnshift != 0, columnshiftline != -1:
  //    column shifting being needed, applies only to specified line


  /**
   * Corrects column fields of n.
   * modifies n, this
   */
  public void visit(NodeToken n) {
    if (n.beginLine == columnshiftline) {
      n.beginColumn = n.beginColumn + columnshift;
      n.endColumn = n.endColumn + columnshift;
    }
    else {
      columnshift = 0;
      columnshiftline = -1;
    }
  }


  /**
   * Checks if n is a variable name.  If so adds "orig(" to the
   *          front of the name and ")" to the end.
   * modifies n, this
   */
  public void visit(PrimaryExpression n) {
    // let simple variables be varibles with out "."'s in their names
    // such as x or myList
    // let compound variables be varibles with "."'s in their names
    // such as this.x or myPackage.MyObject.myList

    // First checks for and handles simple variables.

    //test if optional list length is zero, if not, then it is not
    //the name of a simple variable
    if (n.f1.size() == 0) {
      //checks if the nodeChoice's choice is Name
      if (n.f0.f0.choice instanceof Name) {
        //checks if the Name is simple
        if (((Name) n.f0.f0.choice).f1.size() == 0) {
          NodeToken variableToken = ((Name) n.f0.f0.choice).f0;
          variableToken.tokenImage =  "orig(" + variableToken.tokenImage + ")";
          columnshift = columnshift + 6;
          columnshiftline = variableToken.endLine;
          super.visit(n);

          // Corrects for the fact that super.visit(n) incremented
          // variableToken.beginColumn by 6 too much since the addition of
          // "orig()" does not effect firstToken.beginColumn.
          variableToken.beginColumn = variableToken.beginColumn - 6;
          return;
        }
      }
    }

    if (n.f1.size() == 1) {
      // System.out.println("if1");
      if (n.f1.elementAt(0) instanceof PrimarySuffix) {
        // System.out.println("if2");
        if (((PrimarySuffix) n.f1.elementAt(0)).f0.choice instanceof NodeSequence) {
          if (n.f0.f0.choice instanceof NodeToken) {
            NodeToken firstToken = (NodeToken) n.f0.f0.choice;
            firstToken.tokenImage = "orig(" + firstToken.tokenImage;
            Enumeration nodeSequence =
              ((NodeSequence) ((PrimarySuffix) n.f1.elementAt(0)).f0.choice).elements();
            NodeToken lastToken = firstToken;
            while (nodeSequence.hasMoreElements()) {
              lastToken = (NodeToken) nodeSequence.nextElement();
            }
            lastToken.tokenImage = lastToken.tokenImage + ")";

            // Updates columnshift for the addition of "orig(", the
            // columnshift is updated for the addition of ")" after
            // super.visit(n)
            columnshift = columnshift + 5;
            columnshiftline = firstToken.beginLine;
            super.visit(n);

            // Corrects for the fact that super.visit(n) incremented
            // firstToken.beginColumn by 5 too much since the addition
            // of "orig(" does not effect firstToken.beginColumn.
            firstToken.beginColumn = firstToken.beginColumn - 5;

            // since lastToken is the last node in the visiting order
            // of n, all NodeToken effected by the addition of the
            // ")" at the end of lastToken are visited after all the children
            // of are visited.  Thus,super.visit(n) may be called before
            // all code that corrects the column fields for the addition of
            // ")".
            columnshiftline = lastToken.endLine;
            columnshift = columnshift + 1;
            lastToken.endColumn = lastToken.endColumn + 1;
            return;
          }
        }
      }
    }
    super.visit(n);
  }

}
