package org.checkerframework.framework.base;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotationBuilder;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import org.checkerframework.framework.base.QualifiedTypeMirror.*;

class TypeMirrorConverter<Q> {
    private ProcessingEnvironment processingEnv;
    private ExecutableElement indexElement;
    private AnnotationMirror blankKey;

    private int nextIndex = 0;

    private HashMap<Q, AnnotationMirror> qualToAnno;
    private HashMap<Integer, Q> indexToQual;
    private HashMap<QualifiedTypeMirror<Q>, AnnotationMirror> qualTypeToAnno;
    private HashMap<QualifiedTypeMirror<Q>, AnnotatedTypeMirror> qualTypeToAnnoType;
    private HashMap<Integer, QualifiedTypeMirror<Q>> indexToQualType;

    @TypeQualifier
    @SubtypeOf({})
    public static @interface Key {
        int index() default -1;
        String desc() default "";
    }


    public TypeMirrorConverter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.indexElement = TreeUtils.getMethod(Key.class.getCanonicalName(), "index", 0, processingEnv);
        this.blankKey = AnnotationUtils.fromClass(processingEnv.getElementUtils(), Key.class);

        this.qualToAnno = new HashMap<>();
        this.indexToQual = new HashMap<>();
        this.qualTypeToAnno = new HashMap<>();
        this.qualTypeToAnnoType = new HashMap<>();
        this.indexToQualType = new HashMap<>();
    }


    private AnnotationMirror createKey(Object desc) {
        int index = nextIndex++;

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Key.class.getCanonicalName());
        builder.setValue("index", index);
        builder.setValue("desc", "" + desc);
        return builder.build();
    }

    private int getIndex(AnnotationMirror anno) {
        AnnotationValue value = anno.getElementValues().get(indexElement);
        if (value == null) {
            throw new IllegalArgumentException("@Key annotation contains no index");
        }
        assert(value.getValue() instanceof Integer);
        Integer index = (Integer)value.getValue();
        return index;
    }


    private void insert(Q qual) {
        AnnotationMirror key = createKey(qual);
        int index = getIndex(key);

        qualToAnno.put(qual, key);
        indexToQual.put(index, qual);
    }

    private void insertType(QualifiedTypeMirror<Q> qtm) {
        AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(
                qtm.getUnderlyingType(), null);
        bindTypes(qtm, atm);
    }

    public void bindTypes(QualifiedTypeMirror<Q> qtm, AnnotatedTypeMirror atm) {
        if (qtm == null && atm == null) {
            return;
        }
        if (qtm != null) {
            // Recursively create entries for all component QTM-ATM pairs.
            BIND_TYPE_COMPONENTS_VISITOR.visit(qtm, atm);
        }

        // Create entries for this QTM-ATM pair.
        AnnotationMirror key = createKey(qtm);
        int index = getIndex(key);

        atm.clearAnnotations();
        atm.addAnnotation(key);

        if (qtm != null) {
            indexToQual.put(index, qtm.getQualifier());
        }
        qualTypeToAnno.put(qtm, key);
        qualTypeToAnnoType.put(qtm, atm);
        indexToQualType.put(index, qtm);
    }

    private void bindTypeLists(List<? extends QualifiedTypeMirror<Q>> qtms, List<? extends AnnotatedTypeMirror> atms) {
        assert(qtms.size() == atms.size());
        for (int i = 0; i < qtms.size(); ++i) {
            bindTypes(qtms.get(i), atms.get(i));
        }
    }

    private SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror> BIND_TYPE_COMPONENTS_VISITOR =
        new SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror>() {
            public Void visitArray(QualifiedArrayType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedArrayType atm = (AnnotatedArrayType)rawAtm;
                bindTypes(qtm.getComponentType(), atm.getComponentType());
                return null;
            }

            public Void visitDeclared(QualifiedDeclaredType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedDeclaredType atm = (AnnotatedDeclaredType)rawAtm;
                bindTypeLists(qtm.getTypeArguments(), atm.getTypeArguments());  
                return null;
            }

            public Void visitExecutable(QualifiedExecutableType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedExecutableType atm = (AnnotatedExecutableType)rawAtm;
                bindTypeLists(qtm.getParameterTypes(), atm.getParameterTypes());
                bindTypes(qtm.getReceiverType(), atm.getReceiverType());
                bindTypes(qtm.getReturnType(), atm.getReturnType());
                bindTypeLists(qtm.getThrownTypes(), atm.getThrownTypes());
                bindTypeLists(qtm.getTypeVariables(), atm.getTypeVariables());
                return null;
            }

            public Void visitIntersection(QualifiedIntersectionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedIntersectionType atm = (AnnotatedIntersectionType)rawAtm;
                bindTypeLists(qtm.getBounds(), atm.directSuperTypes());
                return null;
            }

            public Void visitNoType(QualifiedNoType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NoType has no components.
                return null;
            }

            public Void visitNull(QualifiedNullType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NullType has no components.
                return null;
            }

            public Void visitPrimitive(QualifiedPrimitiveType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // PrimitiveType has no components.
                return null;
            }

            public Void visitTypeVariable(QualifiedTypeVariable<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedTypeVariable atm = (AnnotatedTypeVariable)rawAtm;
                bindTypes(qtm.getLowerBound(), atm.getLowerBound());
                bindTypes(qtm.getUpperBound(), atm.getUpperBound());
                return null;
            }

            public Void visitUnion(QualifiedUnionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedUnionType atm = (AnnotatedUnionType)rawAtm;
                bindTypeLists(qtm.getAlternatives(), atm.getAlternatives());
                return null;
            }

            public Void visitWildcard(QualifiedWildcardType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedWildcardType atm = (AnnotatedWildcardType)rawAtm;
                bindTypes(qtm.getExtendsBound(), atm.getExtendsBound());
                bindTypes(qtm.getSuperBound(), atm.getSuperBound());
                return null;
            }
        };


    public Q getQualifier(AnnotationMirror anno) {
        int index = getIndex(anno);
        return indexToQual.get(index);
    }

    public Q getQualifier(AnnotatedTypeMirror atm) {
        return getQualifier(atm.getAnnotation(Key.class));
    }

    public QualifiedTypeMirror<Q> getQualifiedType(AnnotatedTypeMirror atm) {
        AnnotationMirror key = atm.getAnnotation(Key.class);
        int index = getIndex(key);
        return indexToQualType.get(index);
    }

    public AnnotationMirror getAnnotation(Q qual) {
        if (!qualToAnno.containsKey(qual)) {
            insert(qual);
        }
        return qualToAnno.get(qual);
    }

    public AnnotationMirror getAnnotation(QualifiedTypeMirror<Q> qtm) {
        if (!qualTypeToAnno.containsKey(qtm)) {
            insertType(qtm);
        }
        return qualTypeToAnno.get(qtm);
    }

    public AnnotatedTypeMirror getAnnotatedType(QualifiedTypeMirror<Q> qtm) {
        if (!qualTypeToAnnoType.containsKey(qtm)) {
            insertType(qtm);
        }
        return qualTypeToAnnoType.get(qtm);
    }


    public boolean isKey(AnnotationMirror anno) {
        return anno != null && blankKey.getAnnotationType().equals(anno.getAnnotationType());
    }

    public AnnotationMirror getBlankKeyAnnotation() {
        return blankKey;
    }
}
