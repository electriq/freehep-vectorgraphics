// Copyright 2001-2005 freehep
package org.freehep.graphicsio.pdf;

import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio-pdf/src/main/java/org/freehep/graphicsio/pdf/PDFFontEmbedderType3.java f493ff6e61b2 2005/12/01 18:46:43 duns $
 */
public class PDFFontEmbedderType3 extends PDFFontEmbedder {

    public PDFFontEmbedderType3(FontRenderContext context, PDFWriter pdf,
            String reference, PDFRedundanceTracker tracker) {
        super(context, pdf, reference, tracker);
    }

    protected String getSubtype() {
        return "Type3";
    }

    protected void addAdditionalEntries(PDFDictionary fontDict)
            throws IOException {
        Rectangle2D boundingBox = getFontBBox();
        double llx = boundingBox.getX();
        double lly = -(boundingBox.getY() + boundingBox.getHeight());
        double urx = boundingBox.getX() + boundingBox.getWidth();
        double ury = -boundingBox.getY();
        fontDict.entry("FontBBox", new double[] { llx, lly, urx, ury });

        fontDict.entry("FontMatrix", new double[] { 1 / FONT_SIZE, 0, 0,
                1 / FONT_SIZE, 0, 0 });

        fontDict.entry("CharProcs", pdf.ref(getReference() + "CharProcs"));

        PDFDictionary resources = fontDict.openDictionary("Resources");
        resources.entry("ProcSet", new Object[] { pdf.name("PDF") });
        fontDict.close(resources);
    }

    protected void addAdditionalInitDicts() throws IOException {
        // CharProcs
        PDFDictionary charProcs = pdf.openDictionary(getReference()
                + "CharProcs");
        // boolean undefined = false;
        for (int i = 0; i < 256; i++) {
            String charName = getEncodingTable().toName(i);
            if (isCodeUsed(i) &&  charName != null) {
                charProcs.entry(charName, pdf
                        .ref(createCharacterReference(charName)));
            } else {
                // undefined = true;
            }
        }
        // if (undefined)
        charProcs.entry(NOTDEF, pdf.ref(createCharacterReference(NOTDEF)));
        pdf.close(charProcs);
    }

    private static final AffineTransform transform = AffineTransform.getScaleInstance(1.0, -1.0);
    
    protected void writeGlyph(String characterName, Shape glyph,
            GlyphMetrics glyphMetrics) throws IOException {

        // Value 0 seems to be right here - Levantovsky
        double sidebearing = 0; // glyphMetrics != null ? glyphMetrics.getLSB() : 0;
      
        PDFStream glyphStream = pdf.openStream(
                createCharacterReference(characterName), new String[] {
                        "Flate", "ASCII85" });

        Rectangle2D bounds = glyphMetrics != null ? glyphMetrics.getBounds2D()
                : glyph.getBounds2D();
        double advance = glyphMetrics != null ? glyphMetrics.getAdvance()
                : getUndefinedWidth();
        // Invert Y coordinates - Levantovsky, MagicPlot
        glyphStream.glyph(advance, 0, bounds.getX() - sidebearing, -bounds.getY(), 
                bounds.getX() - sidebearing + bounds.getWidth(), -(bounds.getY() + bounds.getHeight()));

        // Invert Y coordinates - Levantovsky, MagicPlot
        glyph = transform.createTransformedShape(glyph);

        boolean windingRule = glyphStream.drawPath(glyph);
        if (windingRule) {
            glyphStream.fillEvenOdd();
        } else {
            glyphStream.fill();
        }
        pdf.close(glyphStream);
    }
}
