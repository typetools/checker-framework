package org.checkerframework.common.value;

import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.Tree;

public class ValueCheckerUtils {
    public static Class<?> getClassFromType(TypeMirror type, Tree tree) {

        switch (type.getKind()) {
        case INT:
            return int.class;
        case LONG:
            return long.class;
        case SHORT:
            return short.class;
        case BYTE:
            return byte.class;
        case CHAR:
            return char.class;
        case DOUBLE:
            return double.class;
        case FLOAT:
            return float.class;
        case BOOLEAN:
            return boolean.class;
        case ARRAY:
            return getArrayType(((ArrayType) type).getComponentType());
        case DECLARED:
            String stringType = TypesUtils.getQualifiedName(
                    (DeclaredType) type).toString();
            if(stringType.equals("<nulltype>")){
                return Object.class;
            }

            try {
                return Class.forName(stringType);
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                return Object.class;
            }
            
        default:
            return Object.class;
        }
    }

    public static Class<?> getArrayType(TypeMirror componentType) {
        switch (componentType.getKind()) {
        case INT:
            return int[].class;
        case LONG:
            return long[].class;
        case SHORT:
            return short[].class;
        case BYTE:
            return byte[].class;
        case CHAR:
            return char[].class;
        case DOUBLE:
            return double[].class;
        case FLOAT:
            return float[].class;
        case BOOLEAN:
            return boolean[].class;
        default:
            return Object[].class;
        }
    }
}
