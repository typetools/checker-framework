package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.Pair;

/**
 * Represents the state of a visitor. Stores the relevant information to find the type of 'this' in
 * the visitor.
 */
public class VisitorState {
    /** The type of the enclosing class tree. */
    private AnnotatedDeclaredType act;
    /** The enclosing class tree. */
    private ClassTree ct;

    /** The receiver type of the enclosing method tree. */
    private AnnotatedDeclaredType mrt;
    /** The enclosing method tree. */
    private MethodTree mt;

    /** The assignment context is a tree as well as its type. */
    private Pair<Tree, AnnotatedTypeMirror> assignmentcontext;

    /** The visitor's current tree path. */
    private TreePath path;

    /** Updates the type of the class currently visited. */
    public void setClassType(AnnotatedDeclaredType act) {
        this.act = act;
    }

    /** Updates the tree of the class currently visited. */
    public void setClassTree(ClassTree ct) {
        this.ct = ct;
    }

    /** Updates the method receiver type currently visited. */
    public void setMethodReceiver(AnnotatedDeclaredType mrt) {
        this.mrt = mrt;
    }

    /** Updates the method currently visited. */
    public void setMethodTree(MethodTree mt) {
        this.mt = mt;
    }

    public void setAssignmentContext(Pair<Tree, AnnotatedTypeMirror> assCtxt) {
        this.assignmentcontext = assCtxt;
    }

    /** Sets the current path for the visitor. */
    public void setPath(TreePath path) {
        this.path = path;
    }

    /** @return the type of the enclosing class */
    public AnnotatedDeclaredType getClassType() {
        if (act == null) {
            return null;
        }
        return act.deepCopy();
    }

    /** @return the class tree currently visiting */
    public ClassTree getClassTree() {
        return this.ct;
    }

    /** @return the method receiver type of the enclosing method */
    public AnnotatedDeclaredType getMethodReceiver() {
        if (mrt == null) {
            return null;
        }
        return mrt.deepCopy();
    }

    /** @return the method tree currently visiting */
    public MethodTree getMethodTree() {
        return this.mt;
    }

    public Pair<Tree, AnnotatedTypeMirror> getAssignmentContext() {
        return assignmentcontext;
    }

    /** @return the current path for the visitor */
    public TreePath getPath() {
        return this.path;
    }

    @SideEffectFree
    @Override
    public String toString() {
        return String.format(
                "VisitorState: method %s (%s) / class %s (%s)\n"
                        + "    assignment context %s (%s)\n"
                        + "    path is non-null: %s",
                (mt != null ? mt.getName() : "null"),
                mrt,
                (ct != null ? ct.getSimpleName() : "null"),
                act,
                (assignmentcontext != null ? assignmentcontext.first : "null"),
                (assignmentcontext != null ? assignmentcontext.second : "null"),
                path != null);
    }
}
