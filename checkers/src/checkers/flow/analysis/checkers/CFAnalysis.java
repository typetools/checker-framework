package checkers.flow.analysis.checkers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import checkers.flow.analysis.AbstractValue;
import checkers.flow.analysis.Analysis;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.Store;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.CFGDOTVisualizer;
import checkers.flow.cfg.node.AbstractNodeVisitor;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * DefaultTypeAnalysis characterizes a kind of abstract value that is computed
 * by the checker framework's default flow-sensitive analysis. It is
 * parameterized by a QualifierHierarchy, defining the relationship among type
 * annotations and an AnnotatedTypeFactory, providing the static type
 * annotations for AST Trees.
 * 
 * The inner class DefaultTypeAnalysis.Value represents a single abstract value
 * computed by the checker framework's default flow-sensitive analysis. A Value
 * is a set of type annotations from the QualifierHierarchy.
 * 
 * The inner class DefaultTypeAnalysis.NodeInfo represents the set of dataflow
 * facts known at a program point, which is a mapping from values, represented
 * by CFG Nodes, to sets of type annotations, represented by
 * DefaultTypeAnalysis.Values.
 * 
 * TODO: Since statically known annotations provided by the AnnotatedTypeFactory
 * are upper bounds, avoid storing NodeInfos explicitly unless they are more
 * precise than the static annotations.
 * 
 * The inner class DefaultTypeAnalysis.Transfer is the transfer function mapping
 * input dataflow facts to output facts. For the default analysis, it merely
 * tracks type annotations through assignments to local variables. Improvements
 * in the precision of type annotations arise from assignments whose RHS has a
 * more precise static type than their LHS.
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 * 
 */
public class CFAnalysis
		extends
		Analysis<CFAnalysis.CFValue, CFAnalysis.CFStore, CFAnalysis.CFTransfer> {
	/**
	 * The qualifier hierarchy for which to track annotations.
	 */
	protected final QualifierHierarchy typeHierarchy;

	/**
	 * A type factory that can provide static type annotations for AST Trees.
	 */
	protected final AnnotatedTypeFactory factory;

	/**
	 * The full set of annotations allowed for this type hierarchy.
	 */
	protected final Set<AnnotationMirror> legalAnnotations;

	public CFAnalysis(QualifierHierarchy typeHierarchy,
			AnnotatedTypeFactory factory) {
		super(new CFTransfer());
		this.typeHierarchy = typeHierarchy;
		this.legalAnnotations = typeHierarchy.getAnnotations();
		this.factory = factory;
		this.transferFunction.setAnalysis(this);
	}

	/**
	 * An abstact value for the default analysis is a set of annotations from
	 * the QualifierHierarchy.
	 */
	public class CFValue implements AbstractValue<CFValue> {
		private Set<AnnotationMirror> annotations;

		private CFValue() {
			annotations = new HashSet<AnnotationMirror>();
		}

		private CFValue(Set<AnnotationMirror> annotations) {
			this.annotations = annotations;
		}

		public Set<AnnotationMirror> getAnnotations() {
			return annotations;
		}

		/**
		 * Computes and returns the least upper bound of two sets of type
		 * annotations. The return value is always of type
		 * DefaultTypeAnalysis.Value.
		 */
		@Override
		public CFValue leastUpperBound(CFValue other) {
			Set<AnnotationMirror> lub = typeHierarchy.leastUpperBound(
					annotations, other.annotations);
			return new CFValue(lub);
		}

		/**
		 * Return whether this Value is a proper subtype of the argument Value.
		 */
		boolean isSubtypeOf(CFValue other) {
			return typeHierarchy.isSubtype(annotations, other.annotations);
		}
	}

	/**
	 * Create a new dataflow value with no type annotations.
	 */
	public CFValue createValue() {
		return new CFValue();
	}

	/**
	 * Create a new dataflow value with the given type annotations, which must
	 * belong to the QualifierHierarchy for which this DefaultTypeAnalysis was
	 * created.
	 */
	public CFValue createValue(Set<AnnotationMirror> annotations)
			throws IllegalArgumentException {
		for (AnnotationMirror anno : annotations) {
			if (!legalAnnotations.contains(anno)) {
				throw new IllegalArgumentException();
			}
		}
		return new CFValue(annotations);
	}

	/**
	 * A store for the default analysis is a mapping from Nodes to Values. If no
	 * Value is explicitly stored for a Node, we fall back on the statically
	 * known annotations.
	 * 
	 * Only Nodes representing mutable values, such as VariableDeclarationNodes
	 * are tracked. If we compute a more precise type annotation for a variable
	 * than its static annotation, then it is entered into the {@link CFStore}
	 * and stays there.
	 * 
	 * TODO: Extend {@link CFStore} to track class member fields in the same way
	 * as variables.
	 */
	public class CFStore implements Store<CFStore> {

		/**
		 * Information collected about local variables, which are identified by
		 * the corresponding element.
		 */
		protected Map<Element, CFValue> localVariableValues;

		public CFStore() {
			localVariableValues = new HashMap<>();
		}

		/** Copy constructor. */
		protected CFStore(CFStore other) {
			localVariableValues = new HashMap<>(other.localVariableValues);
		}

		/**
		 * Current abstract value of a local variable.
		 */
		public CFValue getValue(LocalVariableNode n) {
			Element el = n.getElement();
			assert localVariableValues.containsKey(el);
			return localVariableValues.get(el);
		}

		/**
		 * Set the abstract value of a local variable in the store. Overwrites
		 * any value that might have been available previously.
		 */
		public void setValue(LocalVariableNode n, CFValue val) {
			localVariableValues.put(n.getElement(), val);
		}

		/**
		 * Merge in an abstract value of a local variable in the store by taking
		 * the least upper bound of the previous value and {@code val}. Previous
		 * information needs to be available.
		 */
		public void mergeValue(LocalVariableNode n, CFValue val) {
			Element el = n.getElement();
			assert localVariableValues.containsKey(el);
			CFValue newVal = val.leastUpperBound(localVariableValues.get(el));
			localVariableValues.put(el, newVal);
		}

		@Override
		public CFStore copy() {
			return new CFStore(this);
		}

		@Override
		public CFStore leastUpperBound(CFStore other) {
			CFStore newStore = new CFStore();

			for (Entry<Element, CFValue> e : other.localVariableValues.entrySet()) {
				// local variables that are only part of one store, but not the
				// other are discarded. They are assumed to not be in scope any
				// more.
				Element el = e.getKey();
				if (localVariableValues.containsKey(el)) {
					CFValue otherVal = e.getValue();
					CFValue thisVal = localVariableValues.get(el);
					CFValue mergedVal = thisVal.leastUpperBound(otherVal);
					newStore.localVariableValues.put(el, mergedVal);
				}
			}

			return newStore;
		}

		/**
		 * Returns true iff this {@link CFStore} contains a superset of the map
		 * entries of the argument {@link CFStore}. Note that we test the entry
		 * keys and values by Java equality, not by any subtype relationship.
		 * This method is used primarily to simplify the equals predicate.
		 */
		protected boolean supersetOf(CFStore other) {
			for (Entry<Element, CFValue> e : other.localVariableValues.entrySet()) {
				Element key = e.getKey();
				if (!localVariableValues.containsKey(key)
						|| !localVariableValues.get(key).equals(e.getValue())) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof CFStore) {
				CFStore other = (CFStore) o;
				return this.supersetOf(other) && other.supersetOf(this);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder("CFStore (\\n");
			for (Map.Entry<Element, CFValue> entry : localVariableValues
					.entrySet()) {
				result.append(entry.getKey() + "->"
						+ entry.getValue().getAnnotations() + "\\n");
			}
			result.append(")");
			return result.toString();
		}
	}

	/**
	 * The default analysis transfer function propagates information through
	 * assignments to local variables.
	 */
	public static class CFTransfer
			extends
			AbstractNodeVisitor<TransferResult<CFValue, CFStore>, TransferInput<CFValue, CFStore>>
			implements TransferFunction<CFValue, CFStore> {

		private/* @LazyNonNull */CFAnalysis analysis;

		public void setAnalysis(CFAnalysis analysis) {
			this.analysis = analysis;
		}

		/**
		 * The initial store maps method formal parameters to their currently
		 * most refined type.
		 */
		@Override
		public CFStore initialStore(MethodTree tree,
				List<LocalVariableNode> parameters) {
			CFStore info = analysis.new CFStore();

			for (LocalVariableNode p : parameters) {
				CFValue flowInsensitive = null; // TODO
				assert flowInsensitive != null : "Missing initial type information for method parameter";
				info.mergeValue(p, flowInsensitive);
			}

			return info;
		}

		// TODO: We could use an intermediate classes such as ExpressionNode
		// to refactor visitors. Propagation is appropriate for all expressions.

		/**
		 * The default visitor returns the input information unchanged, or in
		 * the case of conditional input information, merged.
		 */
		@Override
		public TransferResult<CFValue, CFStore> visitNode(Node n,
				TransferInput<CFValue, CFStore> in) {
			// TODO: Perform type propagation separately with a thenStore and an
			// elseStore.
			CFStore info = in.getRegularStore();
			CFValue value = null;

			if (n.hasResult()) {
				Tree tree = n.getTree();
				assert tree != null : "Node has a result, but no Tree";

				Set<AnnotationMirror> annotations = null; // TODO
				value = analysis.createValue(annotations);
			}

			return new RegularTransferResult<>(value, info);
		}

		/**
		 * Map local variable uses to their declarations and extract the most
		 * precise information available for the declaration.
		 */
		@Override
		public TransferResult<CFValue, CFStore> visitLocalVariable(
				LocalVariableNode n, TransferInput<CFValue, CFStore> in) {
			CFStore store = in.getRegularStore();
			CFValue value = store.getValue(n);
			return new RegularTransferResult<>(value, store);
		}

		/**
		 * Propagate information from the assignment's RHS to a variable on the
		 * LHS, if the RHS has more precise information available.
		 */
		@Override
		public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n,
				TransferInput<CFValue, CFStore> in) {
			Node lhs = n.getTarget();
			Node rhs = n.getExpression();

			CFStore info = in.getRegularStore();
			CFValue rhsValue = in.getValueOfSubNode(rhs);

			// assignment to a local variable
			if (lhs instanceof LocalVariableNode) {
				LocalVariableNode var = (LocalVariableNode) lhs;
				info.setValue(var, rhsValue);
			}

			return new RegularTransferResult<>(rhsValue, info);
		}
	}

	/**
	 * Print a DOT graph of the CFG and analysis info for inspection.
	 */
	public void outputToDotFile(String outputFile) {
		String s = CFGDOTVisualizer.visualize(cfg.getEntryBlock(), this);

		try {
			FileWriter fstream = new FileWriter(outputFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
