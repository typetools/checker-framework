package org.checkerframework.common.value;

import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;


public class ValueCheckerUtils {
    public static Class<?> getClassFromType(TypeMirror type) {

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
    
    /**
     * Returns the box primitive type if the passed type is an (unboxed)
     * primitive. Otherwise it returns the passed type
     * 
     * @param type
     * @return
     */
    public static Class<?> boxPrimatives(Class<?> type) {
        if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }

    public static <T extends Comparable<T>> List<T> removeDuplicates(List<T> values) {
        Set<T> set = new TreeSet<>(values);
        return new ArrayList<T>(set);

    }
}
