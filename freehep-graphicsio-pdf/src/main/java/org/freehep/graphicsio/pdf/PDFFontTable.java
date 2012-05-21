// Copyright 2001-2003, FreeHEP.
package org.freehep.graphicsio.pdf;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.freehep.graphics2d.font.CharTable;
import org.freehep.graphics2d.font.CustomCharTable;
import org.freehep.graphics2d.font.Lookup;
import org.freehep.graphicsio.FontConstants;
import org.freehep.graphicsio.font.FontIncluder;
import org.freehep.graphicsio.font.FontTable;

/**
 * A table to remember which fonts were used while writing a pdf document.
 * Entries to resource dictionaries and embedding of fonts can be done when the
 * drawing is finished by calling <tt>addAll()</tt>.
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio-pdf/src/main/java/org/freehep/graphicsio/pdf/PDFFontTable.java 7cb75bc60b0e 2006/11/14 22:29:00 duns $
 */
public class PDFFontTable extends FontTable {
    private PDFWriter pdf;

    private PDFRedundanceTracker tracker;

    public PDFFontTable(PDFWriter pdf) {
        super();
        this.pdf = pdf;
        this.tracker = new PDFRedundanceTracker(pdf);
    }

    /** Adds all fonts to a dictionary named "FontList". */
    public int addFontDictionary() throws IOException {
        Collection<?> fonts = getEntries();
        if (fonts.size() > 0) {
            PDFDictionary fontList = pdf.openDictionary("FontList");
            for (Iterator<?> i = fonts.iterator(); i.hasNext();) {
                Entry e = (Entry) i.next();
                fontList.entry(e.getReference(), pdf.ref(e.getReference()));
            }
            pdf.close(fontList);
        }
        return fonts.size();
    }

    /** Embeds all not yet embedded fonts to the file. */
    public void embedAll(FontRenderContext context, final boolean embed, 
            boolean embedStandard, String embedAs) throws IOException {
        Collection<?> col = getEntries();
        Iterator<?> i = col.iterator();
        while (i.hasNext()) {
            Entry e = (Entry) i.next();
            if (!e.isWritten()) {
                e.setWritten(true);
                
                FontIncluder fontIncluder = null;
                boolean embedThis = embed && (embedStandard || !FontIncluder.isStandardFont(e.getFont()) || e.getEncoding() instanceof CustomCharTable);
                String name = FontIncluder.getFontPSName(e.getFont());
                
                if (embedThis) {
                    if (embedAs.equals(FontConstants.EMBED_FONTS_TYPE3)) {
                        fontIncluder = new PDFFontEmbedderType3(context, pdf, e.getReference(), tracker);
                    } else if (embedAs.equals(FontConstants.EMBED_FONTS_TYPE1)) {
                        fontIncluder = PDFFontEmbedderType1.create(context, pdf, e.getReference(), tracker);
                    } else {
                        System.out.println("PDFFontTable: invalid value for embedAs: " + embedAs);
                    }
                } else {
                    fontIncluder = new PDFFontIncluder(context, pdf, e.getReference(), tracker);
                }
                
                fontIncluder.includeFont(e.getFont(), e.getEncoding(), name, e.getCodesUsed());
            }
        }
        tracker.writeAll();
    }

    public CharTable getEncodingTable() {
        /*
         * Why PDFLatin? From PDF Reference:
         * "PDFDocEncoding Encoding for text strings in a PDF document outside the document’s
         * content streams. This is one of two encodings (the other
         * being Unicode) that can be used to represent text strings; see Section
         * 3.8.1, 'Text Strings.'"
         */
        return Lookup.getInstance().getTable("PDFLatin");
    }

    public void firstRequest(Entry e, boolean embed, String embedAs) {
    }


    protected Font substituteFont(Font font) {
        return font;
    }

    /**
     * Creates the reference by numbering them.
     * This title is shown in Acrobt file properties, it must be readable - Levantovsky, MagicPlot
     */
    protected String createFontReference(Font font, boolean customCharTable, boolean embed, boolean embedStandard) {
      boolean useRealFontName = embed && (embedStandard || !FontIncluder.isStandardFont(font));
      String psName = useRealFontName
              ? FontIncluder.getFontPSName(font) 
              : FontIncluder.getFontRemappedPSName(font);
        if (customCharTable)
          return psName + "-Custom";
        return psName;
    }
}
