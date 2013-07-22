/*
 * Created on 11/01/2009
 */
package cfjapa.parser.ast.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Julio Vilmar Gesser
 */
@RunWith(value = Suite.class)
@SuiteClasses(value = {//
TestAdapters.class, //
        TestNodePositions.class, //
        TestDumper.class, //
        TestHashCodeEquals.class })
public class AllTests {

}
