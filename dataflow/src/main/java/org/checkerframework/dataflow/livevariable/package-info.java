/**
 * Classes using for live variable analysis. Live variable analysis is a backward analysis to
 * calculate the variables that are live at each point in the program. To run live variable analysis
 * on a file and create a PDF of the CFG, run {@code java
 * org.checkerframework.dataflow.cfg.playground.LiveVariablePdf MyFile.java}.
 *
 * @see <a
 *     href="https://en.wikipedia.org/wiki/Live_variable_analysis">https://en.wikipedia.org/wiki/Live_variable_analysis</a>
 */
package org.checkerframework.dataflow.livevariable;
