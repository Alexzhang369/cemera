package bim.com.bar;

import org.junit.Test;

import bim.com.bar.my_test.Activiy_Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Activiy_Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Activiy_Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
}