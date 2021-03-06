// Copyright 2000-2009, FreeHEP
package org.freehep.xml.util.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

import org.freehep.util.export.test.Assert;
import org.freehep.xml.util.XMLWriter;

/**
 * Test class to test XMLWriter
 *
 * @author Mark Donszelmann
 * @version $Id: TestXMLWriter.java 8584 2006-08-10 23:06:37Z duns $
 */

public class TestXMLWriter extends TestCase {

    private File testFile = new File("TestXMLWriter.xml");
    private File refFile = new File("src/test/resources/org/freehep/util/export/test/TestXMLWriter.xml");
    
    // FIXME, rewrite in JUnit 4 in separate parts
    public void testXML() throws IOException {
        XMLWriter writer = new XMLWriter(new FileWriter(testFile),"    ","");
        writer.openDoc("1.0", "", true);

        writer.printComment("This is test file output generated by TestXMLWriter.java");

        try {
            writer.printComment("Illegal comment --");
        } catch (RuntimeException illegalCommentException) {
            writer.setAttribute("No", 1);
            writer.openTag("Chapter");

            writer.setAttribute("On", "Some Subject of a difficult nature & \nsomething else, <see next line>");
            writer.openTag("Section");

            writer.setAttribute("Useless", true);
            writer.setAttribute("Unicode", "\u45B0");
            writer.printTag("Paragraph");
            writer.closeTag();

            writer.println("Some Text in the middle of nowhere...");
            writer.println(" &amp; some extra lines,");
            writer.println(" \nwritten my <Mark> Donszelmann");
            writer.println(" and some unicode \u03A8 ");

            try {
                writer.printTag("Illegal Tag");
            } catch (RuntimeException invalidName) {
                try {
                    writer.closeDoc();
                } catch (RuntimeException openTagException) {
                    writer.close();
                    
                    Assert.assertEquals( refFile, testFile, false );
                    return;
                }
                Assert.assertTrue("Should have thrown a Exception for closing the document too early...", false);
            }
            Assert.assertTrue("Should have thrown an Exception for an illegal tag...", false);
        }
        Assert.assertTrue("Should have thrown a Exception for an illegal comment...", false);
    }
}
