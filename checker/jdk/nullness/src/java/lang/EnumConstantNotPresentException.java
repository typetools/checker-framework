/*
 * Copyright 2004-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Thrown when an application tries to access an enum constant by name
 * and the enum type contains no constant with the specified name.
 *
 * @author  Josh Bloch
 * @since   1.5
 */
public class EnumConstantNotPresentException extends RuntimeException {
    private static final long serialVersionUID = -6046998521960521108L;

    /**
     * The type of the missing enum constant.
     */
    private final Class<? extends Enum<?>> enumType;

    /**
     * The name of the missing enum constant.
     */
    private final String constantName;

    /**
     * Constructs an <tt>EnumConstantNotPresentException</tt> for the
     * specified constant.
     *
     * @param enumType the type of the missing enum constant
     * @param constantName the name of the missing enum constant
     */
    @SideEffectFree
    public EnumConstantNotPresentException(Class<? extends Enum<?>> enumType,
                                           String constantName) {
        super(enumType.getName() + "." + constantName);
        this.enumType = enumType;
        this.constantName  = constantName;
    }

    /**
     * Returns the type of the missing enum constant.
     *
     * @return the type of the missing enum constant
     */
    public Class<? extends Enum<?>> enumType() { return enumType; }

    /**
     * Returns the name of the missing enum constant.
     *
     * @return the name of the missing enum constant
     */
    public String constantName() { return constantName; }
}
