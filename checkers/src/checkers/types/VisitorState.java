package checkers.types;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

/**
 * Represents the state of a visitor.  Stores the relevant information to find
 * the type of 'this' in the visitor.
 */
public class VisitorState {
    /** The type of the enclosing class tree**/
    private AnnotatedDeclaredType act;
    /** The enclosing class tree **/
    private ClassTree ct;

    /** The receiver type of the enclosing class tree **/
    private AnnotatedDeclaredType mrt;
    /** The enclosing method tree **/
    private MethodTree mt;

    private Tree assignmentcontext;

    /** The visitor's current tree path. */
    private TreePath path;

    /**
     * Updates the type of the current class currently visited
     */
    public void setClassType(AnnotatedDeclaredType act) {
        this.act = act;
    }

    /**
     * Updates the tree of the current class currently visited
     */
    public void setClassTree(ClassTree ct) {
        this.ct = ct;
    }

    /**
     * Updates the method receiver type currently visited
     */
    public void setMethodReceiver(AnnotatedDeclaredType mrt) {
        this.mrt = mrt;
    }

    /**
     * Updates the method currently visited
     */
    public void setMethodTree(MethodTree mt) {
        this.mt = mt;
    }

    public void setAssignmentContextTree(Tree assCtxt) {
        this.assignmentcontext = assCtxt;
    }

    /**
     * Sets the current path for the visitor.
     *
     * @param path
     */
    public void setPath(TreePath path) {
        this.path = path;
    }

    /**
     * @return the type of the enclosing class
     */
    public AnnotatedDeclaredType getClassType() {
        if (act == null) return null;
        return AnnotatedTypes.deepCopy(act);
    }

    /**
     * @return the class tree currently visiting
     */
    public ClassTree getClassTree() {
        return this.ct;
    }

    /**
     * @return the method receiver type of the enclosing method
     */
    public AnnotatedDeclaredType getMethodReceiver() {
        if (mrt == null) return null;
        return AnnotatedTypes.deepCopy(mrt);
    }

    /**
     * @return the method tree currently visiting
     */
    public MethodTree getMethodTree() {
        return this.mt;
    }

    public Tree getAssignmentContextTree() {
        return assignmentcontext;
    }

    /**
     * @return the current path for the visitor
     */
    public TreePath getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return String.format("method %s (%s) / class %s (%s)",
                (mt != null ? mt.getName() : "null"),
                mrt,
                (ct != null ? ct.getSimpleName() : "null"),
                act);
    }
}
