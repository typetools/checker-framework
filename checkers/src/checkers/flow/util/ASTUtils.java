package checkers.flow.util;

import javax.lang.model.element.Element;

import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

public class ASTUtils {
	/**
	 * Determine whether <code>tree</code> is a field access expressions, such
	 * as
	 * 
	 * <pre>
	 *   <em>f</em>
	 *   <em>obj</em> . <em>f</em>
	 * </pre>
	 * 
	 * @return true iff if tree is a field access expression (implicit or
	 *         explicit).
	 */
	public static boolean isFieldAccess(Tree tree) {
		if (tree.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
			// explicit field access
			MemberSelectTree memberSelect = (MemberSelectTree) tree;
			Element el = TreeUtils.elementFromUse(memberSelect);
			return el.getKind().isField();
		} else if (tree.getKind().equals(Tree.Kind.IDENTIFIER)) {
			// implicit field access
			IdentifierTree ident = (IdentifierTree) tree;
			Element el = TreeUtils.elementFromUse(ident);
			return el.getKind().isField()
					&& !ident.getName().contentEquals("this");
		}
		return false;
	}

	/**
	 * Compute the name of the field that the field access
	 * <code>expression</code> accesses. Requires <code>expression</code> to be
	 * a field access, as determined by <code>isFieldAccess</code>.
	 * 
	 * @return The name of the field accessed by <code>expression</code>.
	 */
	public static String getFieldName(Tree tree) {
		assert isFieldAccess(tree);
		if (tree.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
			MemberSelectTree mtree = (MemberSelectTree) tree;
			return mtree.getIdentifier().toString();
		} else {
			IdentifierTree itree = (IdentifierTree) tree;
			return itree.getName().toString();
		}
	}
}
