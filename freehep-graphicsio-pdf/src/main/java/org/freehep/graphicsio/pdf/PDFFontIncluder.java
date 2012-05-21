// Copyright 2001-2005 freehep
package org.freehep.graphicsio.pdf;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.freehep.graphics2d.font.CharTable;
import org.freehep.graphicsio.font.FontIncluder;

/**
 * Includes one of the 14 Type1 fonts in PDF documents
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $id$
 */
public class PDFFontIncluder extends FontIncluder {
    private static final Set<String> STD_PDF_FONTS = new HashSet<String>();
    //
    static {
      STD_PDF_FONTS.add("Courier");
      STD_PDF_FONTS.add("Courier-Bold");
      STD_PDF_FONTS.add("Courier-Oblique");
      STD_PDF_FONTS.add("Courier-BoldOblique");
      
      STD_PDF_FONTS.add("Helvetica");
      STD_PDF_FONTS.add("Helvetica-Bold");
      STD_PDF_FONTS.add("Helvetica-Oblique");
      STD_PDF_FONTS.add("Helvetica-BoldOblique");
      
      STD_PDF_FONTS.add("Times-Roman");
      STD_PDF_FONTS.add("Times-Bold");
      STD_PDF_FONTS.add("Times-Italic");
      STD_PDF_FONTS.add("Times-BoldItalic");
            
      STD_PDF_FONTS.add("Symbol");
      STD_PDF_FONTS.add("ZapfDingbats");
    }

    private PDFWriter pdf;

    private String reference;

    private PDFRedundanceTracker redundanceTracker;

    public PDFFontIncluder(FontRenderContext context, PDFWriter pdf,
            String reference, PDFRedundanceTracker redundanceTracker) {
        super(context);
        this.pdf = pdf;
        this.reference = reference;
        this.redundanceTracker = redundanceTracker;
    }
    
    protected boolean isStandardFont(String psName) {
        return STD_PDF_FONTS.contains(psName);
    }
    
    protected void openIncludeFont() throws IOException {
        String outputPSName = getFontRemappedPSName(getFont());
        boolean standardFont = isStandardFont(outputPSName);

        PDFDictionary font = pdf.openDictionary(reference);
        font.entry("Type", pdf.name("Font"));
        font.entry("Subtype", pdf.name(standardFont ? "Type1" : "TrueType"));
        font.entry("Name", pdf.name(reference));
        font.entry("BaseFont", pdf.name(outputPSName));
        font.entry("Encoding", redundanceTracker.getReference(
                getEncodingTable(), PDFCharTableWriter.getInstance()));
        
        if (!standardFont)
        {
          font.entry("FontDescriptor", pdf.ref(reference + "FontDescriptor"));
          font.entry("FirstChar", 0);
          font.entry("LastChar", 255);
          font.entry("Widths", pdf.ref(reference + "Widths"));
        }
       
        pdf.close(font);
        
        if (!standardFont) {
            PDFDictionary fontDescriptor = pdf.openDictionary(reference + "FontDescriptor");
            fontDescriptor.entry("Type", pdf.name("FontDescriptor"));

            LineMetrics metrics = getFont().getLineMetrics("mM", getContext());
            fontDescriptor.entry("Ascent", metrics.getAscent());
            fontDescriptor.entry("Descent", metrics.getDescent());
            fontDescriptor.entry("FontName", pdf.name(outputPSName));
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

            pdf.close(fontDescriptor);
            
            writeWidths(getAdvanceWidths());
        }
    }
    
    protected void writeWidths(double[] widths) throws IOException {
        Object[] widthsObj = new Object[256];
        for (int i = 0; i < widthsObj.length; i++)
            widthsObj[i] = new Integer((int) Math.round(widths[i]));
        pdf.object(reference + "Widths", widthsObj);
    }

    protected void writeEncoding(CharTable charTable) throws IOException {
    }
}
