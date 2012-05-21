// Copyright 2001-2005 freehep
package org.freehep.graphicsio.pdf;

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.freehep.graphics2d.font.CustomCharTable;

import org.freehep.graphicsio.font.FontEmbedderType1;

/**
 * Font embedder for type one fonts in pdf documents.
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio-pdf/src/main/java/org/freehep/graphicsio/pdf/PDFFontEmbedderType1.java f493ff6e61b2 2005/12/01 18:46:43 duns $
 */
public class PDFFontEmbedderType1 extends FontEmbedderType1 {

    private ByteArrayOutputStream byteBuffer;

    private String reference;

    private PDFWriter pdf;

    private PDFStream fontFile;

    private PDFRedundanceTracker redundanceTracker;

    public static PDFFontEmbedderType1 create(FontRenderContext context,
            PDFWriter pdf, String reference, PDFRedundanceTracker tracker) {
        return new PDFFontEmbedderType1(context, pdf, reference,
                new ByteArrayOutputStream(), tracker);
    }

    private PDFFontEmbedderType1(FontRenderContext context, PDFWriter pdf,
            String reference, ByteArrayOutputStream byteOut,
            PDFRedundanceTracker tracker) {
        super(context, byteOut, false);
        this.byteBuffer = byteOut;
        this.pdf = pdf;
        this.reference = reference;
        this.redundanceTracker = tracker;
    }

    private String getReference() {
        return reference;
    }
    
    private static int number = 0;
    private String getTwoLetters(int number) {
        int a = number / 25;
        int b = number - (a * 25);
        char chA = (char)((int)'A' + a);
        char chB = (char)((int)'B' + b);
        return "" + chA + chB;
    }
    

    // FIXME: The StemV entry is missing in the FontDescriptor dictionary, but
    // it
    // does not cause the Acrobat Reader to crash.
    @Override
    protected void openIncludeFont() throws IOException {
        super.openIncludeFont();
        String fontSubsetName = (getEncodingTable() instanceof  CustomCharTable ? "CUST" : "LATI") 
                + getTwoLetters(number++) + "+" + getFontPSName();
        
        PDFDictionary fontDict = pdf.openDictionary(reference);

        fontDict.entry("Type", pdf.name("Font"));
        fontDict.entry("Subtype", pdf.name("Type1"));
        fontDict.entry("Name", pdf.name(getFontName()));

        fontDict.entry("FirstChar", 0);
        fontDict.entry("LastChar", 255);
        fontDict.entry("Encoding", redundanceTracker.getReference(
                getEncodingTable(), PDFCharTableWriter.getInstance()));

        fontDict.entry("Widths", pdf.ref(reference + "Widths"));
        fontDict.entry("BaseFont", pdf.name(fontSubsetName)); // = FontName in font program
        fontDict.entry("FontDescriptor", pdf.ref(getReference() + "FontDescriptor"));
        if (getEncodingTable() instanceof CustomCharTable)
            fontDict.entry("ToUnicode", pdf.ref(reference + "ToUnicode"));

        pdf.close(fontDict);

        if (getEncodingTable() instanceof CustomCharTable)
            PDFFontEmbedder.writeToUnicode(pdf, reference + "ToUnicode", getEncodingTable());

        PDFDictionary fontDescriptor = pdf.openDictionary(getReference()
                + "FontDescriptor");
        fontDescriptor.entry("Type", pdf.name("FontDescriptor"));

        LineMetrics metrics = getFont().getLineMetrics("mM", getContext());
        fontDescriptor.entry("Ascent", metrics.getAscent());
        fontDescriptor.entry("Descent", metrics.getDescent());
        fontDescriptor.entry("FontName", pdf.name(fontSubsetName));
        fontDescriptor.entry("Flags", 32);
        fontDescriptor.entry("CapHeight", metrics.getAscent());
        fontDescriptor.entry("ItalicAngle", getFont().getItalicAngle());
        fontDescriptor.entry("StemV", 1);
        
        // Correct texy selection = bounding box Y coordinate inverce - Levantovsky, MagicPlot
        Rectangle2D boundingBox = getFontBBox();
        double llx = boundingBox.getX();
        double lly = -(boundingBox.getY() + boundingBox.getHeight());
        double urx = boundingBox.getX() + boundingBox.getWidth();
        double ury = -boundingBox.getY();
        fontDescriptor.entry("FontBBox", new double[] { llx, lly, urx, ury });

        fontDescriptor.entry("FontFile", pdf.ref(getReference() + "FontFile"));

        pdf.close(fontDescriptor);

    }

    protected void writeWidths(double[] widths) throws IOException {
        super.writeWidths(widths);
        Object[] widthsObj = new Object[256];
        for (int i = 0; i < widthsObj.length; i++)
            widthsObj[i] = new Integer((int) Math.round(widths[i]));
        pdf.object(reference + "Widths", widthsObj);
    }

    protected void openGlyphs() throws IOException {
        super.openGlyphs();
    }

    protected void closeEmbedFont() throws IOException {
        super.closeEmbedFont();

        fontFile = pdf.openStream(getReference() + "FontFile", new String[] {
                "Flate", "ASCII85" });
        fontFile.entry("Length1", getAsciiLength());
        fontFile.entry("Length2", getEncryptedLength());
        fontFile.entry("Length3", 0); // leave it to the viewer application to
                                        // add the 512 zeros

        String file = byteBuffer.toString("US-ASCII");
        fontFile.print(file);

        pdf.close(fontFile);
    }

}
