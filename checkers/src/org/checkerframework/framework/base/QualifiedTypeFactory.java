package org.checkerframework.framework.base;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

import javacutils.Pair;

import org.checkerframework.framework.base.QualifiedTypeMirror;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedTypeVariable;

interface QualifiedTypeFactory<Q> {
    // Get the qualified type from an Element or Tree.
    QualifiedTypeMirror<Q> getQualifiedType(Element element);
    QualifiedTypeMirror<Q> getQualifiedType(Tree tree);
    QualifiedTypeMirror<Q> getQualifiedTypeFromTypeTree(Tree typeTree);

    // Get the hierarchies for this type system.
    QualifierHierarchy<Q> getQualifierHierarchy();
    TypeHierarchy<Q> getTypeHierarchy();

    // Hooks for modifying certain typing rules.
    List<QualifiedTypeMirror<Q>> postDirectSuperTypes(QualifiedTypeMirror<Q> subtype, List<? extends QualifiedTypeMirror<Q>> supertypes);
    QualifiedTypeMirror<Q> postAsMemberOf(QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement);
    List<QualifiedTypeVariable<Q>> typeVariablesFromUse(QualifiedDeclaredType<Q> type, TypeElement element);
    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(MethodInvocationTree tree);
    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> constructorFromUse(NewClassTree tree);
}
