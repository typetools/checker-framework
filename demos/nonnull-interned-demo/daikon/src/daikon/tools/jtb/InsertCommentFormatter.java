package daikon.tools.jtb;

import java.util.*;
import utilMDE.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;

/**
 * InsertCommentFormatter is a visitor that does not actually insert
 * comments, but instead corrects positioning fields of all the tokens
 * in the tree to accomodate already-inserted comments, while
 * modifying the formatting as little as possible.  (It edits the
 * {begin,end}{Line,Column} fields.)
 * <p>
 *
 * Each inserted comment either affects only the rest of its line
 * -- by shifting all subsequent characters rightward -- or only
 * subsequent lines -- by shifting lines downward.
 * <p>
 *
 * The caller must supply the collection of inserted comments for
 * recognition by this visitor.
 **/
public class InsertCommentFormatter
  extends DepthFirstVisitor
{
  private boolean debug = false;

  private Vector<NodeToken> comments;
  private int columnshift = 0;
  private int lineshift = 0;
  private int columnshiftline = -1; // the line currently being column-shifted.

  // Column shifting only applies to a single line, then is turned off again.
  // States for the variables:
  // columnshift == 0, columnshiftline == -1:
  //    no column shifting being done
  // columnshift != 0, columnshiftline == -1:
  //    column shifting being done, but first real token not yet found
  // columnshift != 0, columnshiftline != -1:
  //    column shifting being done, applies only to specified line


  private static final String lineSep = System.getProperty("line.separator");

  public InsertCommentFormatter(Vector<NodeToken> comments) {
    this.comments = comments;
  }

  private static int numLines(NodeToken n) {
    String image = n.tokenImage;
    return UtilMDE.count(image, lineSep);
  }

  private static int numColumns(NodeToken n) {
    if (numLines(n) > 0) {
      return 0;
    } else {
      return n.tokenImage.length();
    }
  }

  public void visit(NodeToken n) {
    if (debug) { System.out.println("Visit (at " + n.beginLine + "," + n.beginColumn + ") (in comments = " + comments.contains(n) + ") " + n.tokenImage); }

    // See comment at use of this variable below
    boolean prev_is_double_slash_comment = false;

    // Handle special tokens first
    if ( n.numSpecials() > 0 )  // handles case when n.specialTokens is null
      for ( NodeToken s : n.specialTokens ) {
        visit(s);
        prev_is_double_slash_comment = s.tokenImage.startsWith("//");
      }

    if ((columnshift == 0) && (lineshift == 0)) {
      // nothing to do
    } else {
      if (columnshift != 0) {
        if (columnshiftline == -1) {
          columnshiftline = n.beginLine;
        }
        if (columnshiftline != n.beginLine) {
          columnshift = 0;
          columnshiftline = -1;
        }
      }
      n.beginLine += lineshift;
      n.endLine += lineshift;
      n.beginColumn += columnshift;
      n.endColumn += columnshift;
      if (debug) { System.out.println("Shifted by " + lineshift + "," + columnshift + ": <<<" + n.tokenImage.trim() + ">>>"); }
    }
    // Special-case the situation of ending a file with a "//"-style
    // comment that does not start at the beginning of its line; in that
    // case, we need to increment the "" token for EOF to start at the next
    // line.  Otherwise the "" EOF token is marked as starting at the end
    // of the previous line, though the "//"-style comment doesn't end
    // until the start of the next line.  Without this code,
    // jtb.visitor.TreeDumper.visit throws an error.
    if (n.tokenImage.equals("") && prev_is_double_slash_comment) {
      Assert.assertTrue(n.beginLine == n.endLine && n.beginColumn == n.endColumn);
      n.beginLine += 1;
      n.beginColumn = 1;
      n.endLine += 1;
      n.endColumn = 1;
    }
    if (comments.contains(n)) {
      columnshift += numColumns(n);
      lineshift += numLines(n);
    }
    if (debug) { System.out.println("End visit (at " + n.beginLine + "," + n.beginColumn + ") " + n.tokenImage); }

  }
}
