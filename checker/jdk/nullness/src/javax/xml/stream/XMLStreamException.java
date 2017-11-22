/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 */

package javax.xml.stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * The base exception for unexpected processing errors.  This Exception
 * class is used to report well-formedness errors as well as unexpected
 * processing conditions.
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @since 1.6
 */

public class XMLStreamException extends Exception {

  protected @Nullable Throwable nested;
  protected @Nullable Location location;

  /**
   * Default constructor
   */
  @SideEffectFree
  public XMLStreamException(){
    super();
  }

  /**
   * Construct an exception with the assocated message.
   *
   * @param msg the message to report
   */
  @SideEffectFree
  public XMLStreamException(@Nullable String msg) {
    super(msg);
  }

  /**
   * Construct an exception with the assocated exception
   *
   * @param th a nested exception
   */
  @SideEffectFree
  public XMLStreamException(@Nullable Throwable th) {
      super(th);
    nested = th;
  }

  /**
   * Construct an exception with the assocated message and exception
   *
   * @param th a nested exception
   * @param msg the message to report
   */
  @SideEffectFree
  public XMLStreamException(@Nullable String msg, @Nullable Throwable th) {
    super(msg, th);
    nested = th;
  }

  /**
   * Construct an exception with the assocated message, exception and location.
   *
   * @param th a nested exception
   * @param msg the message to report
   * @param location the location of the error
   */
  @SideEffectFree
  public XMLStreamException(@Nullable String msg, Location location, @Nullable Throwable th) {
    super("ParseError at [row,col]:["+location.getLineNumber()+","+
          location.getColumnNumber()+"]\n"+
          "Message: "+msg);
    nested = th;
    this.location = location;
  }

  /**
   * Construct an exception with the assocated message, exception and location.
   *
   * @param msg the message to report
   * @param location the location of the error
   */
  @SideEffectFree
  public XMLStreamException(@Nullable String msg,
                            Location location) {
    super("ParseError at [row,col]:["+location.getLineNumber()+","+
          location.getColumnNumber()+"]\n"+
          "Message: "+msg);
    this.location = location;
  }


  /**
   * Gets the nested exception.
   *
   * @return Nested exception
   */
  @Pure
  public @Nullable Throwable getNestedException() {
    return nested;
  }

  /**
   * Gets the location of the exception
   *
   * @return the location of the exception, may be null if none is available
   */
  @Pure public @Nullable Location getLocation() {
    return location;
  }

}
