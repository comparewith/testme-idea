package com.weirddev.testme.intellij.generator;

import com.weirddev.testme.intellij.template.TemplateRegistry;

/**
 * Date: 24/02/2017
 *
 * @author Yaron Yamin
 */
public class TestMeGeneratorSpockTest extends TestMeGeneratorTestBase {
    public TestMeGeneratorSpockTest() {
        super(TemplateRegistry.SPOCK_GROOVY_MOCKITO_JAVA_TEMPLATE, "testSpock");
        expectedTestClassExtension = "groovy";
    }
    public void testBean() throws Exception{
        doTest();
    }
    public void testCtorOverProps() throws Exception{
        doTest(true,true,true);
    }

    public void testGenerics() throws Exception{
        doTest(true,true,true);
    }
}