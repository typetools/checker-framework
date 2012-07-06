package checkers.fenum;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.Fenum;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.Bottom;
import checkers.types.QualifierHierarchy;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.basetype.BaseTypeChecker;

/**
 * The main checker class for the fake enum checker.
 *
 * There are two options to distinguish different enumerators:
 *
 * <ol>
 * <li> {@code @Fenum("Name")}: introduces a fake enumerator with the name
 * "Name". Enumerators with different names are distinct. The default name is
 * empty, but you are encouraged to use a unique name for your purpose.
 * </li>
 *
 * <li> Alternatively, you can specify the annotation to use with the
 * {@code -Aqual} command line argument.
 * </li>
 * </ul>
 *
 * @author wmdietl
 */
@SupportedOptions( { "quals" } )
public class FenumChecker extends BaseTypeChecker {
    protected AnnotationMirror FENUM, BOTTOM;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        AnnotationUtils utils = AnnotationUtils.getInstance(env);
        BOTTOM = utils.fromClass(Bottom.class);
        FENUM = utils.fromClass(Fenum.class);
        super.initChecker(env);
    }

    /** Copied from BasicChecker.
     * Instead of returning an empty set if no "quals" option is given,
     * we return Fenum as the only qualifier.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
            new HashSet<Class<? extends Annotation>>();

        String qualNames = env.getOptions().get("quals");
        if (qualNames == null) {
          // maybe issue a warning?
        } else {
          for (String qualName : qualNames.split(",")) {
            try {
              final Class<? extends Annotation> q =
                (Class<? extends Annotation>) Class.forName(qualName);
              qualSet.add(q);
            } catch (ClassNotFoundException e) {
              errorAbort("FenumChecker: could not load class for qualifier: " + qualName + "; ensure that your classpath is correct.");
            }
          }
        }
        qualSet.add(FenumTop.class);
        qualSet.add(Fenum.class);
        qualSet.add(FenumUnqualified.class);
        qualSet.add(Bottom.class);

        // Also call super. This way a subclass can use the
        // @TypeQualifiers annotation again.
        qualSet.addAll(super.createSupportedTypeQualifiers());

        // TODO: warn if no qualifiers given?
        // Just Fenum("..") is still valid, though...
        return Collections.unmodifiableSet(qualSet);
    }

    /** Copied from BasicChecker; cannot reuse it, because BasicChecker is final.
     */
    @Override
    public Collection<String> getSuppressWarningsKey() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKey();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }

    /* The user is expected to introduce additional fenum annotations.
     * These annotations are declared to be subtypes of FenumTop, using the
     * @SubtypeOf annotation.
     * However, there is no way to declare that it is a supertype of Bottom.
     * Therefore, we use the constructor of GraphQualifierHierarchy that allows
     * us to set a dedicated bottom qualifier.
     */
    @Override
    protected GraphQualifierHierarchy.GraphFactory createQualifierHierarchyFactory() {
        return new GraphQualifierHierarchy.GraphFactory(this, BOTTOM);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new FenumQualifierHierarchy((GraphQualifierHierarchy)super.createQualifierHierarchy());
    }

    protected class FenumQualifierHierarchy extends GraphQualifierHierarchy {

        public FenumQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, FENUM) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, FENUM)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, FENUM)) {
                lhs = FENUM;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, FENUM)) {
                rhs = FENUM;
            }
            return super.isSubtype(rhs, lhs);
        }
    }
}
