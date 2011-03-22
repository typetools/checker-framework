package checkers.util.test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

import checkers.flow.*;
import checkers.types.*;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;


@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class FlowTestChecker extends TestChecker {

    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new FlowTypeFactory(env, root);
    }

    class FlowTypeFactory extends AnnotatedTypeFactory {

        final Flow flow;
        final SourcePositions srcPos;

        FlowTypeFactory(ProcessingEnvironment env, CompilationUnitTree root) {
            super(env, root);
            srcPos = trees.getSourcePositions();
            flow = new Flow(env, root, ODD, this);
            //flow.debug = System.out;
            flow.scan(root, null);
        }

        @Override
        public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
            Tree t = TreeUtils.skipParens(tree);
            AnnotatedTypeMirror result = super.getAnnotatedType(t);
            if (tree instanceof IdentifierTree || tree instanceof MemberSelectTree) {
                long pos = srcPos.getStartPosition(root, tree);
                Boolean r = flow.test(pos);
                if (r == Boolean.TRUE)
                    result.addAnnotation(ODD);
            }
            return result;
        }
    }

}
