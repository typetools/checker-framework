package checkers.util;

import java.util.Map;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.Flow;
import checkers.source.SupportedLintOptions;
import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;

/**
 * Implements a checker for type qualifier systems that have only two qualified
 * types where one is a subtype of the other and the qualifiers have no special
 * (implicit) behavior.
 * 
 * <p>
 * 
 * The annotation(s) may be specified on the command line, using annotation
 * processor arguments. Two arguments are used:
 * 
 * <ul>
 * <li>{@code -Aqual}: specifies the annotation for the qualified subtype</li>
 * <li>{@code -Anqual}: (optional) specifies the annotation for the qualified
 * supertype, if one is needed</li>
 * </ul>
 * 
 * <p>
 * 
 * Only the annotation for the qualified subtype must be specified. An
 * annotation for the qualified supertype may be specified; it is used to
 * override a subtype default if the user changes the default.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedLintOptions( { "flow" })
@SupportedOptions( { "qual", "nqual" })
public class CustomChecker extends BaseTypeChecker {

    /** Whether flow is enabled if no lint option is provided. */
    private static final boolean FLOW_BY_DEFAULT = true;
    
    /** For creating annotations (e.g., for flow). */
    private AnnotationFactory annoFactory;
    
    /** The annotations that make up the custom type system. */
    private AnnotationMirror positive, negative;
    
    private String qualName;
    
    @Override
    public synchronized void init(ProcessingEnvironment p) {
        super.init(p);
        annoFactory = new AnnotationFactory(p);

        final Map<String, String> options = p.getOptions();

        if (!options.containsKey("qual"))
            throw new Error("missing required option: -Aqual");

        qualName = options.get("qual");
        if (!classExists(qualName))
            throw new Error("-Aqual class not found: " + qualName);

        final String nQual = options.get("nqual");
        if (nQual != null && !classExists(nQual))
            throw new Error("-Anqual class not found: " + nQual);
        
        positive = annoFactory.fromName(qualName);
        negative = (nQual != null ? annoFactory.fromName(nQual) : null);
    }

    /**
     * Checks to see if a class with the given name exists.
     * 
     * @param name the fully-qualified name of the class to check for
     * @return true if the class exists, false otherwise
     */
    private boolean classExists(final String name) {
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public AnnotatedTypeFactory createFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        AnnotatedTypeFactory factory = new UserTypeFactory(env, root, positive, 
                this.getLintOption("flow", FLOW_BY_DEFAULT));
        return factory;
    }

    @Override
    public Class<?>[] getSupportedTypeQualifiers() {
        try {
            Class<?> qual = Class.forName(this.env.getOptions().get("qual"));
            return new Class<?>[] { qual };
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Extends {@link AnnotatedTypeFactory} to optionally use flow-sensitive
     * qualifier inference.
     * 
     * @see {@link Flow}
     */
    private static class UserTypeFactory extends
            AnnotatedTypeFactory {

        private final AnnotationMirror pQual;
        private final Flow flow;
        private final SourcePositions srcPos;
        private final boolean useFlow;

        /**
         * Creates a type factory for checking the given compilation unit with
         * respect to the given annotation.
         * 
         * @param env the processing environment
         * @param root the compilation unit to scan
         * @param pQual the annotation for the subtype qualifier
         * @param useFlow whether or not flow-sensitive inference should be used
         */
        UserTypeFactory(ProcessingEnvironment env,
                CompilationUnitTree root, AnnotationMirror pQual,
                boolean useFlow) {
            super(env, root);

            this.pQual = pQual;
            this.srcPos = trees.getSourcePositions();
            this.flow = new Flow(env, root, pQual, this);
            
            this.useFlow = useFlow;

            // Apply flow-sensitive qualifier inference.
            if (useFlow) flow.scan(root, null);
        }

        @Override
        protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
            if (useFlow) {
                Boolean flowResult = null;
                Element elt = InternalUtils.symbol(tree);
                if (elt != null
                        && (tree instanceof IdentifierTree || tree instanceof MemberSelectTree)) {
                    long pos = srcPos.getStartPosition(root, tree);
                    flowResult = flow.test(pos);
                    if (flowResult == Boolean.TRUE)
                        type.addAnnotation(pQual);
                }
            }
        }
    }
}
