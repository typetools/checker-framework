package org.checkerframework.framework.util;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A utility for converting AnnotationMirrors to Strings.
 */
public class DefaultAnnotationFormatter implements AnnotationFormatter {

    /**
     * Returns true if, by default, anno should not be printed
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return True if anno's declaration was qualified by InvisibleQualifier.
     */
    public static boolean isInvisibleQualified(AnnotationMirror anno) {
        return ((TypeElement)anno.getAnnotationType().asElement()).getAnnotation(InvisibleQualifier.class) != null;
    }

    /**
     * Creates a space String of each annotation in annos separated by a single space character,
     * obeying the printInvisible parameter.
     * @param annos A collection of annotations to print
     * @param printInvisible Whether or not to print "invisible" annotation mirrors
     * @return The list of annotations converted to a String
     */
    @SideEffectFree
    public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationMirror obj : annos) {
            if (obj == null) {
                ErrorReporter.errorAbort("AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror!");
            }
            if (isInvisibleQualified(obj) && !printInvisible) {
                continue;
            }
            formatAnnotationMirror(obj, sb);
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     *
     * @param anno The annotation mirror to convert
     * @return the string representation of a single AnnotationMirror, without showing full package names
     */
    @SideEffectFree
    public String formatAnnotationMirror(AnnotationMirror anno) {
        StringBuilder sb = new StringBuilder();
        formatAnnotationMirror(anno, sb);
        return sb.toString();
    }

    // A helper method to output a single AnnotationMirror, without showing full package names.
    protected void formatAnnotationMirror(AnnotationMirror am, StringBuilder sb) {
        sb.append("@");
        sb.append(am.getAnnotationType().asElement().getSimpleName());
        Map<? extends ExecutableElement, ? extends AnnotationValue> args = am.getElementValues();
        if (!args.isEmpty()) {
            sb.append("(");
            boolean oneValue = false;
            if (args.size() == 1) {
                Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> first = args.entrySet().iterator().next();
                if (first.getKey().getSimpleName().contentEquals("value")) {
                    formatAnnotationMirrorArg(first.getValue(), sb);
                    oneValue = true;
                }
            }
            if (!oneValue) {
                boolean notfirst = false;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> arg : args.entrySet()) {
                    if (notfirst) {
                        sb.append(", ");
                    }
                    notfirst = true;
                    sb.append(arg.getKey().getSimpleName() + "=");
                    formatAnnotationMirrorArg(arg.getValue(), sb);
                }
            }
            sb.append(")");
        }
    }

    // A helper method to print AnnotationValues, without showing full package names.
    @SuppressWarnings("unchecked")
    protected void formatAnnotationMirrorArg(AnnotationValue av, StringBuilder sb) {
        Object val = av.getValue();
        if (List.class.isAssignableFrom(val.getClass())) {
            List<AnnotationValue> vallist = (List<AnnotationValue>) val;
            if (vallist.size() == 1) {
                formatAnnotationMirrorArg(vallist.get(0), sb);
            } else {
                sb.append('{');
                boolean notfirst = false;
                for (AnnotationValue nav : vallist) {
                    if (notfirst) {
                        sb.append(", ");
                    }
                    notfirst = true;
                    formatAnnotationMirrorArg(nav, sb);
                }
                sb.append('}');
            }
        } else if (VariableElement.class.isAssignableFrom(val.getClass())) {
            VariableElement ve = (VariableElement) val;
            sb.append(ve.getEnclosingElement().getSimpleName() + "." + ve.getSimpleName());
        } else {
            sb.append(av.toString());
        }
    }
}
