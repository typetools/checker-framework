package checkers.interned;

import checkers.basetype.*;
import checkers.metaquals.TypeQualifiers;
import checkers.quals.Interned;
import checkers.source.*;
import checkers.types.*;
import checkers.util.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import com.sun.source.tree.*;

/**
 * A typechecker plug-in for the {@link checkers.quals.Interned} qualifier that
 * finds (and verifies the absence of) equality-testing and interning errors.
 * 
 * <p>
 * 
 * The {@link checkers.quals.Interned} annotation indicates that a variable
 * refers to the canonical instance of an object, meaning that it is safe to
 * compare that object using the "==" operator. This plugin suggests using "=="
 * instead of ".equals" where possible, and warns whenever "==" is used in cases
 * where one or both operands are not {@link checkers.quals.Interned}.
 * 
 * @manual #interned Interned checker
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedLintOptions({"dotequals", "flow"})
@SuppressWarningsKey("interned")
@TypeQualifiers({ Interned.class })
// TODO: move some of these options to annotations
public final class InternedChecker extends BaseTypeChecker {

    @Override
    protected Properties getMessages() {
        Properties msgs = new Properties(super.getMessages());
        msgs.setProperty("not.interned",
                "attempting to use a non-@Interned comparison operand" +
                "\nfound: %s");
        msgs.setProperty("unnecessary.equals",
                "use of .equals can be safely replaced by ==/!=");
        return msgs;
    }

    @Override
    public BaseTypeVisitor<?, ?> getSourceVisitor(CompilationUnitTree root) {
        return new InternedVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory createFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        boolean useFlow = this.getLintOption("flow", false);
        AnnotatedTypeFactory factory 
            = new InternedAnnotatedTypeFactory(env, root, useFlow);
        return factory;
    }
}

