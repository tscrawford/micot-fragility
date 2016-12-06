package gov.lanl.micot.fragility;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for the Fragility application.
 */
public class FragilityTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FragilityTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FragilityTest.class );
    }

    /**
     * Test
     */
    public void testFragility()
    {
        assertTrue( true );
    }
}
