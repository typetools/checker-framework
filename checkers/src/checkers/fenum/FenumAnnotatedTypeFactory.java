package checkers.fenum;

import java.util.Collections;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;


public class FenumAnnotatedTypeFactory extends
		BasicAnnotatedTypeFactory<FenumChecker> {

	public FenumAnnotatedTypeFactory(FenumChecker checker,
			CompilationUnitTree root) {
		// Use the "flowinference" lint option to enable or disable flow inference.
		// Unfortunately, inference changes a field access that has the type
		// @Fenum("A") into an access @Fenum, ignoring the arguments.
		// This happens in BasicAnnotatedTypeFactory.annotateImplicit, where
		// all annotations are removed and the inferred annotation is added.
		// Inference apparently does not handle arguments yet.

		super(checker, root, checker.getLintOption("flowinference", false));
        
		if(checker.getLintOption("flowinference", false)) {
			defaults.setAbsoluteDefaults(
		 		this.annotations.fromClass(FenumUnqualified.class),
				Collections.singleton(DefaultLocation.ALL_EXCEPT_LOCALS));
			defaults.setLocalDefault(annotations.fromClass(FenumTop.class));

			// flow.setDebug(System.err);
			flow.scan(root, null);
		}
		// if "flowinference" is false, the checker uses the DefaultQualifierInHierarchy.
	}
    
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
		if (flow == null) {
			// If the flow field is null, the flow inference is turned off.
			// Just do what the superclass did.
			super.annotateImplicit(tree, type);
		} else {
			treeAnnotator.visit(tree, type);
			// typeAnnotator.visit(type);

			defaults.annotate(tree, type);

			final AnnotationMirror inferred = flow.test(tree);
			if (inferred != null) {
				type.clearAnnotations();
				type.addAnnotation(inferred);
				// System.out.println("Inferred: " + type);
			}
			// completer.visit(type);
		}
	}
}