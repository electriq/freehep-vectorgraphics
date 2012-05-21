// Copyright 2001-2005, FreeHEP.
package org.freehep.graphicsio.ps;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.freehep.graphics2d.font.*;
import org.freehep.graphicsio.FontConstants;
import org.freehep.graphicsio.font.FontEmbedderType1;
import org.freehep.graphicsio.font.FontIncluder;
import org.freehep.graphicsio.font.FontTable;

/**
 * FontTable for PS files. The fonts name is used as a reference for the font.
 * When the font is first used, it is embedded to the file if it is not a
 * standard font. If it is unknown it is not substituted.
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio-ps/src/main/java/org/freehep/graphicsio/ps/PSFontTable.java 59372df5e0d9 2007/02/06 21:11:19 duns $
 */
public class PSFontTable extends FontTable {

    private OutputStream out;

    private FontRenderContext context;

    public PSFontTable(OutputStream out, FontRenderContext context) {
        super();
        this.out = out;
        this.context = context;
    }

    public CharTable getEncodingTable() {
        return Lookup.getInstance().getTable("STDLatin");
    }

    protected void firstRequest(Entry e, boolean embed, String embedAs)
            throws IOException {
    }
     
    protected void embedFont(Entry e, boolean embed, boolean embedStandard, String embedAs)
            throws IOException {
        FontIncluder fontIncluder = null;
        e.setWritten(true);

        out.flush();

        if (embed && (embedStandard || !FontIncluder.isStandardFont(e.getFont()) || e.getEncoding() instanceof CustomCharTable)) {
            if (embedAs.equals(FontConstants.EMBED_FONTS_TYPE3)) {
                fontIncluder = new PSFontEmbedder(context, new PrintStream(out));
            } else if (embedAs.equals(FontConstants.EMBED_FONTS_TYPE1)) {
                fontIncluder = new FontEmbedderType1(context, out, true);
            } else {
                System.err.println("PSFontTable: not a valid value for embedAs: " + embedAs);
            }
        } else {
            return;
        }
        
        fontIncluder.includeFont(e.getFont(), e.getEncoding(), e.getReference(), e.getCodesUsed());
        out.flush();
    }
    
    public void embedAll(boolean embed, boolean embedStandard, String embedAs) throws IOException {
      for (Entry e : getEntries())
          embedFont(e, embed, embedStandard, embedAs);
    }

    /**
     * removes any transformation and superscript, changes the names
     * to PS font name
     *
     * @param font
     * @return derived font
     */
    @SuppressWarnings( "unchecked" )
    protected Font substituteFont(Font font) {
        Map<Attribute, Object> attributes = FontUtilities.getAttributes(font);
        // change names
        // normalize(attributes);
        // remove transformations
        attributes.remove(TextAttribute.TRANSFORM);
        attributes.remove(TextAttribute.SUPERSCRIPT);
        return FontMap.getFont(attributes);
    }

    /**
     * Uses the font name as a reference. Whitespace is stripped. The font style
     * (italic/bold) is added as a suffix delimited by a dash.
     * Uses {@link #normalize(java.util.Map)}
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
