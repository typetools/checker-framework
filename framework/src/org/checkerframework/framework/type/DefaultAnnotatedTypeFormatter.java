package org.checkerframework.framework.type;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.framework.util.AnnotationFormatter;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * An AnnotatedTypeFormatter used by default by all AnnotatedTypeFactory (and therefore all
 * annotated types).
 * @see org.checkerframework.framework.type.AnnotatedTypeFormatter
 * @see org.checkerframework.framework.type.AnnotatedTypeMirror#toString
 */
public class DefaultAnnotatedTypeFormatter implements AnnotatedTypeFormatter {
    protected final AnnotationFormatter annoFormatter;
    protected final boolean defaultInvisiblesSetting;
    protected final FormattingVisitor formattingVisitor;

    public DefaultAnnotatedTypeFormatter() {
        this(new DefaultAnnotationFormatter(), false);
    }

    public DefaultAnnotatedTypeFormatter(boolean printInvisibleAnnos) {
        this(new DefaultAnnotationFormatter(), printInvisibleAnnos);
    }

    public DefaultAnnotatedTypeFormatter(AnnotationFormatter formatter, boolean printInvisibleAnnos) {
        this.annoFormatter = formatter;
        this.defaultInvisiblesSetting = printInvisibleAnnos;
        this.formattingVisitor = new FormattingVisitor();
    }

    /**
     * @inherit
     */
    public String format(final AnnotatedTypeMirror type) {
        formattingVisitor.printInvisibleAnnos = defaultInvisiblesSetting;
        return formattingVisitor.visit(type);
    }

    /**
     * @inherit
     */
    public String format(final AnnotatedTypeMirror type, final boolean printInvisibles) {
        formattingVisitor.printInvisibleAnnos = printInvisibles;
        return formattingVisitor.visit(type);
    }

    protected class FormattingVisitor implements AnnotatedTypeVisitor<String, Set<AnnotatedTypeMirror>> {

        protected boolean printInvisibleAnnos;

        public FormattingVisitor() {
            this.printInvisibleAnnos = false;
        }

        /** print to sb keyWord followed by field.  NULL types are substituted with
         * their annotations followed by " Void"
         */
        @SideEffectFree
        protected void printBound(final String keyWord, final AnnotatedTypeMirror field,
                                  final Set<AnnotatedTypeMirror> visiting, final StringBuilder sb) {
            sb.append(" ");
            sb.append(keyWord);
            sb.append(" ");

            if (field == null) {
                sb.append("<null>");
            } else if (field.getKind() != TypeKind.NULL) {
                sb.append(visit(field, visiting));
            } else {
                sb.append(annoFormatter.formatAnnotationString(field.getAnnotations(), printInvisibleAnnos));
                sb.append("Void");
            }
        }

        @SideEffectFree
        @Override
        public String visit(AnnotatedTypeMirror type) {
            return type.accept(this, Collections.newSetFromMap(new IdentityHashMap<AnnotatedTypeMirror, Boolean>()));
        }

        @Override
        public String visit(AnnotatedTypeMirror type, Set<AnnotatedTypeMirror> annotatedTypeVariables) {
            return type.accept(this, annotatedTypeVariables);
        }

        @Override
        public String visitDeclared(AnnotatedDeclaredType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();
            if (type.isDeclaration()) {
                sb.append("/*DECL*/ ");
            }
            final Element typeElt = type.getUnderlyingType().asElement();
            String smpl = typeElt.getSimpleName().toString();
            if (smpl.isEmpty()) {
                // For anonymous classes smpl is empty - toString
                // of the element is more useful.
                smpl = typeElt.toString();
            }
            sb.append(annoFormatter.formatAnnotationString(type.getAnnotations(), printInvisibleAnnos));
            sb.append(smpl);

            final List<AnnotatedTypeMirror> typeArgs = type.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                sb.append("<");

                boolean isFirst = true;
                for (AnnotatedTypeMirror typeArg : typeArgs) {
                    if (!isFirst) sb.append(", ");
                    sb.append(visit(typeArg, visiting));
                    isFirst = false;
                }
                sb.append(">");
            }
            return sb.toString();
        }

        @Override
        public String visitIntersection(AnnotatedIntersectionType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();

            boolean isFirst = true;
            for (AnnotatedDeclaredType adt : type.directSuperTypes()) {
                if (!isFirst) sb.append(" & ");
                sb.append(visit(adt, visiting));
                isFirst = false;
            }
            return sb.toString();
        }

        @Override
        public String visitUnion(AnnotatedUnionType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();

            boolean isFirst = true;
            for (AnnotatedDeclaredType adt : type.getAlternatives()) {
                if (!isFirst) sb.append(" | ");
                sb.append(visit(adt, visiting));
                isFirst = false;
            }
            return sb.toString();
        }

        @Override
        public String visitExecutable(AnnotatedExecutableType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();
            if (!type.getTypeVariables().isEmpty()) {
                sb.append('<');
                for (AnnotatedTypeVariable atv : type.getTypeVariables()) {
                    sb.append(visit(atv, visiting));
                }
                sb.append("> ");
            }
            if (type.getReturnType() != null) {
                sb.append(visit(type.getReturnType(), visiting));
            } else {
                sb.append("<UNKNOWNRETURN>");
            }
            sb.append(' ');
            if (type.getElement() != null) {
                sb.append(type.getElement().getSimpleName());
            } else {
                sb.append("METHOD");
            }
            sb.append('(');
            AnnotatedDeclaredType rcv = type.getReceiverType();
            if (rcv != null) {
                sb.append(visit(rcv, visiting));
                sb.append(" this");
            }
            if (!type.getParameterTypes().isEmpty()) {
                int p = 0;
                for (AnnotatedTypeMirror atm : type.getParameterTypes()) {
                    if (rcv != null ||
                            p > 0) {
                        sb.append(", ");
                    }
                    sb.append(visit(atm, visiting));
                    // Output some parameter names to make it look more like a method.
                    // TODO: go to the element and look up real parameter names, maybe.
                    sb.append(" p");
                    sb.append(p++);
                }
            }
            sb.append(')');
            if (!type.getThrownTypes().isEmpty()) {
                sb.append(" throws ");
                for (AnnotatedTypeMirror atm : type.getThrownTypes()) {
                    sb.append(visit(atm, visiting));
                }
            }
            return sb.toString();
        }

        @Override
        public String visitArray(AnnotatedArrayType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();

            AnnotatedArrayType array = type;
            AnnotatedTypeMirror component;
            while (true) {
                component = array.getComponentType();
                if (array.getAnnotations().size() > 0) {
                    sb.append(' ');
                    sb.append(annoFormatter.formatAnnotationString(array.getAnnotations(), printInvisibleAnnos));
                }
                sb.append("[]");
                if (!(component instanceof AnnotatedArrayType)) {
                    sb.insert(0, visit(component, visiting));
                    break;
                }
                array = (AnnotatedArrayType) component;
            }
            return sb.toString();
        }

        @Override
        public String visitTypeVariable(AnnotatedTypeVariable type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();
            if (type.isDeclaration()) {
                sb.append("/*DECL*/ ");
            }

            sb.append(type.actualType);
            if (!visiting.contains(type)) {
                try {
                    visiting.add(type);
                    sb.append("[");
                    printBound("extends", type.getUpperBoundField(), visiting, sb);
                    printBound("super", type.getLowerBoundField(), visiting, sb);
                    sb.append("]");
                } finally {
                    visiting.remove(type);
                }
            }
            return sb.toString();
        }

        @SideEffectFree
        @Override
        public String visitPrimitive(AnnotatedPrimitiveType type, Set<AnnotatedTypeMirror> visiting) {
            return formatFlatType(type);
        }

        @SideEffectFree
        @Override
        public String visitNoType(AnnotatedNoType type, Set<AnnotatedTypeMirror> visiting) {
            return formatFlatType(type);
        }

        @SideEffectFree
        @Override
        public String visitNull(AnnotatedNullType type, Set<AnnotatedTypeMirror> visiting) {
            //TODO: CODE REVIEW NOTE: THIS MATCHES OLD IMPLEMENTATION,
            //IT DOES NOT MAKE SENSE TO ME EXCEPT FOR PERHAPS IN THE NULLNESS TYPE SYSTEM,
            //ON TOP OF THAT, WHAT ABOUT LOWER BOUNDS?  THEY MAY HAVE NON-INVISIBLE ANNOTATIONS
            //FURTHERMORE, FOR LOWER BOUNDS IN THE NULLNESS TYPE SYSTEM WE MAY ENCOUNTER
            //ONE THING TO NOTE FOR THE NULLNESS TYPE SYSTEM:
            //  IF WE START PRINTING ANNOTATIONS ON NULL WE MAY END UP WITH WEIRD MESSAGES FOR THE
            //  LOWER BOUND LIKE: @NonNull null
            //  Because the lowerbound have a bottom Java type, and likely, a bottom qualifier
            if (!printInvisibleAnnos) {
                return "null";
            }
            return annoFormatter.formatAnnotationString(type.getAnnotations(), printInvisibleAnnos) + "null";
        }

        @Override
        public String visitWildcard(AnnotatedWildcardType type, Set<AnnotatedTypeMirror> visiting) {
            StringBuilder sb = new StringBuilder();
            sb.append(annoFormatter.formatAnnotationString(type.annotations, printInvisibleAnnos));
            sb.append("?");
            if (!visiting.contains(type)) {
                try {
                    visiting.add(type);
                    sb.append("[");
                    printBound("extends", type.getExtendsBoundField(), visiting, sb);
                    printBound("super", type.getSuperBoundField(), visiting, sb);
                    sb.append("]");
                } finally {
                    visiting.remove(type);
                }
            }
            return sb.toString();
        }

        @SideEffectFree
        protected String formatFlatType(final AnnotatedTypeMirror flatType) {
            return annoFormatter.formatAnnotationString(flatType.getAnnotations(), printInvisibleAnnos) + flatType.actualType;
        }
    }
}
