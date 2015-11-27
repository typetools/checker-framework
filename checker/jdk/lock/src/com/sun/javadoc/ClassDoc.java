package com.sun.javadoc;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public interface ClassDoc extends ProgramElementDoc, Type {
    @Pure boolean isAbstract(@GuardSatisfied ClassDoc this);
    @Pure boolean isSerializable(@GuardSatisfied ClassDoc this);
    @Pure boolean isExternalizable(@GuardSatisfied ClassDoc this);
    MethodDoc[] serializationMethods();
    FieldDoc[] serializableFields();
    boolean definesSerializableFields();
    ClassDoc superclass();
    Type superclassType();
    boolean subclassOf(ClassDoc cd);
    ClassDoc[] interfaces();
    Type[] interfaceTypes();
    TypeVariable[] typeParameters();
    ParamTag[] typeParamTags();
    FieldDoc [] fields();
    FieldDoc[] fields(boolean filter);
    FieldDoc[] enumConstants();
    MethodDoc[] methods();
    MethodDoc[] methods(boolean filter);
    ConstructorDoc[] constructors();
    ConstructorDoc[] constructors(boolean filter);
    ClassDoc[] innerClasses();
    ClassDoc[] innerClasses(boolean filter);
    ClassDoc findClass(String className);
    @Deprecated ClassDoc[] importedClasses();
    @Deprecated PackageDoc[] importedPackages();
}
