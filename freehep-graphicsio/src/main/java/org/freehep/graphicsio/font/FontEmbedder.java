// Copyright 2001-2005 freehep
package org.freehep.graphicsio.font;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.freehep.graphics2d.font.CharTable;

/**
 * A FontIncluder that also embeds all glyphs. Subclasses must implement the
 * <tt>writeGlyph</tt> method which is called for all defined (up to 256)
 * characters and the notdefined character. These method calls are bracketed by
 * <tt>openGlyphs()</tt> and <tt>closeGlyph()</tt>. All invocations of
 * methods that are abstract in this class succeed the method calls of the
 * superclass <tt>FontIncluder</tt> (especially <tt>closeIncludeFont()</tt>!)
 * All of these calls are again succeeded by <tt>closeEmbedFont</tt>. <br>
 * The abstract methods are called in the following order:
 * <ul>
 * <li><tt>openIncludeFont</tt>
 * <li><tt>writeEncoding</tt>
 * <li><tt>closeIncludeFont</tt>
 * <li><tt>writeWidths</tt>
 * <li><tt>openGlyphs</tt>
 * <li>loop over all glyphs: <tt>openGlyphs</tt>
 * <li><tt>closeGlyphs</tt>
 * <li><tt>closeEmbedFont</tt>
 * </ul>
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version1 $Id: freehep-graphicsio/src/main/java/org/freehep/graphicsio/font/FontEmbedder.java d9a2ef8950b1 2006/03/03 19:08:18 duns $
 */
public abstract class FontEmbedder extends FontIncluder {

    public static final String NOTDEF = ".notdef";

    /**
     * Writes a single glyph to the file. A null value for <tt>glyphMetrics</tt>
     * indicates the undefined character. In this case the value of
     * <tt>unicodeName</tt> equals the value of
     * <tt>NOTDEF</TT> (=<tt>.notdef</tt>).
     *
     * @param unicodeName the character's name according to the unicode standard
     * @param glyph the shape that represents this glyph
     * @param glyphMetrics the metrics of this glyph
     */
    protected abstract void writeGlyph(String unicodeName, Shape glyph,
            GlyphMetrics glyphMetrics) throws IOException;

    /** Writes the character widths to the file. */
    protected abstract void writeWidths(double[] widths) throws IOException;

    /**
     * Called before the glyph loop starts. Does nothing by default but can be
     * implemented.
     */
    protected void openGlyphs() throws IOException {
    }

    /**
     * Called after the glyph loop ends. Does nothing by default but can be
     * implemented.
     */
    protected void closeGlyphs() throws IOException {
    }

    protected abstract void closeEmbedFont() throws IOException;

    public FontEmbedder(FontRenderContext context) {
        super(context);
    }
    
    @Override
    public void includeFont(Font font, CharTable charTable, String name, boolean[] codesUsed)
            throws IOException {

        super.includeFont(font, charTable, name, codesUsed);
        writeWidths(getAdvanceWidths());

        try {
            openGlyphs();

            // write the glyphs
            for (int i = 0; i < 256; i++) {
                if (isCodeUsed(i) && getCharName(i) != null) {
                    writeGlyph(getCharName(i), getGlyph(i), getGlyphMetrics(i));
                }
            }           
	    writeGlyph(NOTDEF, createUndefined(), null);

            closeGlyphs();
            closeEmbedFont();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Shape createUndefined() {
        GeneralPath ud = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 10);
        ud.append(new Rectangle2D.Double(3 * FONT_SIZE / 20, -16 * FONT_SIZE / 20, 14 * FONT_SIZE / 20, 18 * FONT_SIZE / 20), false);
        ud.append(new Rectangle2D.Double(4 * FONT_SIZE / 20, -15 * FONT_SIZE / 20,
                12 * FONT_SIZE / 20, 16 * FONT_SIZE / 20), false);
        return ud;
    }
}
