package org.checkerframework.common.initializedfields;

import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.initializedfields.qual.EnsuresInitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsBottom;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsPredicate;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.framework.util.ContractsUtils;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.ElementUtils;

/** The annotated type factory for the Initialized Fields Checker. */
public class InitializedFieldsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {
    /**
     * Create a new accumulation checker's annotated type factory.
     *
     * @param checker the checker
     */
    public InitializedFieldsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(
                checker,
                InitializedFields.class,
                InitializedFieldsBottom.class,
                InitializedFieldsPredicate.class);
        this.postInit();
    }

    @Override
    public InitializedFieldsContractsUtils getContractsUtils() {
        return new InitializedFieldsContractsUtils(this);
    }

    private class InitializedFieldsContractsUtils extends ContractsUtils {
        /**
         * Creates an InitializedFieldsContractsUtils for the given factory.
         *
         * @param factory the type factory associated with the newly-created ContractsUtils
         */
        public InitializedFieldsContractsUtils(GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
            super(factory);
        }

        @Override
        public Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement) {
            Set<Contract.Postcondition> result = super.getPostconditions(executableElement);

            // Only process methods defined in source code being type-checked.
            if (declarationFromElement(executableElement) != null) {

                if (executableElement.getSimpleName().contentEquals("<init>")) {
                    // It's a constructor

                    String[] fieldsToInitialize =
                            fieldsToInitialize(
                                    (TypeElement) executableElement.getEnclosingElement());
                    if (fieldsToInitialize.length != 0) {

                        AnnotationMirror initializedFieldsAnno;
                        {
                            AnnotationBuilder builder =
                                    new AnnotationBuilder(
                                            processingEnv,
                                            InitializedFields.class.getCanonicalName());
                            builder.setValue("value", fieldsToInitialize);
                            initializedFieldsAnno = builder.build();
                        }
                        AnnotationMirror ensuresAnno;
                        {
                            AnnotationBuilder builder =
                                    new AnnotationBuilder(
                                            processingEnv,
                                            EnsuresInitializedFields.class.getCanonicalName());
                            builder.setValue("value", new String[] {"this"});
                            builder.setValue("fields", fieldsToInitialize);
                            ensuresAnno = builder.build();
                        }
                        Contract.Postcondition ensuresContract =
                                new Contract.Postcondition(
                                        "this", initializedFieldsAnno, ensuresAnno);

                        result.add(ensuresContract);
                    }
                }
            }

            return result;
        }
    }

    // It is a bit wasteful that this is recomputed for each constructor.
    /**
     * Returns the fields that the constructor must initialize. These are the fields F declared in
     * this class that satisfy all of the following conditions:
     *
     * <ul>
     *   <li>F is a non-final field (if final, Java will issue a warning, so we don't need to).
     *   <li>F's declaration has no initializer
     *   <li>No initialization block or static initialization block sets the field.
     *   <li>F's annotated type is not consistent with the default value (0, 0.0, false, or null)
     * </ul>
     */
    private String[] fieldsToInitialize(TypeElement type) {
        List<String> result = new ArrayList<String>();

        for (Element member : type.getEnclosedElements()) {

            if (member.getKind() != ElementKind.FIELD) {
                continue;
            }

            VariableElement field = (VariableElement) member;
            if (ElementUtils.isFinal(field)) {
                continue;
            }

            VariableTree fieldTree = (VariableTree) declarationFromElement(field);

            // TODO: Check whether an initialization block sets the field.
            if (fieldTree.getInitializer() != null) {
                continue;
            }

            AnnotatedTypeMirror fieldType = getAnnotatedType(field);
            AnnotatedTypeMirror defaultValueType =
                    getDefaultValueAnnotatedType(fieldType.getUnderlyingType());
            if (getTypeHierarchy().isSubtype(defaultValueType, fieldType)) {
                continue;
            }

            result.add(field.getSimpleName().toString());
        }

        return result.toArray(new String[result.size()]);
    }
}
