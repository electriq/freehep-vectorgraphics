// Copyright 2001-2005 freehep
package org.freehep.graphicsio.font;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.*;

import org.freehep.graphics2d.font.CharTable;
import org.freehep.graphics2d.font.FontMap;
import org.freehep.graphics2d.font.FontUtilities;

/**
 * Instances of this class write the information into documents (ps or pdf) that
 * is necessary in order to include or embed fonts. In order to guarantee a
 * time-invariant interface the main methods to implement by subclasses
 * <tt>includeFont</tt> takes no arguments. All necessary data should be
 * available by getter methods which can easily be added. <br>
 * The abstract methods are called in the following order:
 * <ul>
 * <li><tt>openIncludeFont</tt>
 * <li><tt>writeEncoding</tt>
 * <li><tt>closeIncludeFont</tt>
 * </ul>
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio/src/main/java/org/freehep/graphicsio/font/FontIncluder.java 5641ca92a537 2005/11/26 00:15:35 duns $
 */
public abstract class FontIncluder {

    public static final double FONT_SIZE = 1000;

    // -------------------- abstract methods --------------------

    /**
     * Writes the given information about the font into the file. When this
     * method is called all <tt>getXXX()</tt> are guaranteed to return
     * reasonable values.
     */
    protected abstract void openIncludeFont() throws IOException;

    /** Writes the encoding table to the file. */
    protected abstract void writeEncoding(CharTable charTable)
            throws IOException;

    /** Does nothing, but can be implemented by subclasses if necessary. */
    protected void closeIncludeFont() throws IOException {
    }

    // -----------------------------------------------------------

    private FontRenderContext context;

    private Rectangle2D fontBBox;

    private Font font;

    private String fontName;

    private CharTable charTable;

    private char[] unicode;

    private String[] charName;

    private boolean[] codeUsed;
    
    private int noDefinedChars;
    
    private Shape[] glyphShapes;
    private GlyphMetrics[] glyphMetrics;
    private double[] glyphWidths;

    public FontIncluder(FontRenderContext context) {
        this.context = context;
        this.noDefinedChars = -1;
    }

    // -----------------------------------------------------------

    protected FontRenderContext getContext() {
        return context;
    }

    protected String getFontName() {
        return fontName;
    }

    protected Font getFont() {
        return font;
    }

    protected CharTable getEncodingTable() {
        return charTable;
    }

    protected String getCharName(int i) {
        return charName[i];
    }

    protected char getUnicode(int i) {
        return unicode[i];
    }

    protected char[] getUnicode() {
        return unicode;
    }

    public boolean isCodeUsed(int i) {
        return codeUsed[i];
    }

    protected int getNODefinedChars() {
        return noDefinedChars;
    }

    // -----------------------------------------------------------

    public void includeFont(Font font, CharTable charTable, String name) throws IOException {
        boolean[] codesUsed = new boolean[256];
        Arrays.fill(codesUsed, true);
        includeFont(font, charTable, name, codesUsed);
    }
   
    /**
     * Creates Glyph shapes, metrics and width. Invoked on first demand.
     */
    private void createGlyphs() {
        glyphWidths = new double[256];
        glyphMetrics = new GlyphMetrics[256];
        glyphShapes = new Shape[256];
        
        FontRenderContext orig = getContext();
        FontRenderContext frc = new FontRenderContext(null, orig
                .isAntiAliased(), orig.usesFractionalMetrics());

        for (int i = 0; i < 256; i++) {
            char code = getUnicode(i);
            if (code != 0) {
                GlyphVector glyph = font.createGlyphVector(frc, new char[] { code });
                glyphWidths[i] = glyph.getGlyphMetrics(0).getAdvance();
                glyphShapes[i] = glyph.getGlyphOutline(0);
                glyphMetrics[i] = glyph.getGlyphMetrics(0);
            } else {
                // in case of undefined character set to width of undefined symbol
                glyphWidths[i] = getUndefinedWidth();
            }
        }
    }
      
    protected Rectangle2D getFontBBox() {
        if (fontBBox == null) {
            fontBBox = font.getMaxCharBounds(context);
            for (int i = 0; i < 256; i++) {
                if (isCodeUsed(i) && getEncodingTable().toName(i) != null) {
                    fontBBox = fontBBox.createUnion(getGlyphMetrics(i).getBounds2D());
                }
            }
        }
        return fontBBox;
    }
     
    protected double[] getAdvanceWidths() {
        if (glyphShapes == null)
          createGlyphs();
        return glyphWidths;
    }

    protected double getAdvanceWidth(int character) {
        return glyphWidths[character];
    }

    protected Shape getGlyph(int i) {
        if (glyphShapes == null)
          createGlyphs();
        return glyphShapes[i];
    }

    protected GlyphMetrics getGlyphMetrics(int i) {
        if (glyphShapes == null)
          createGlyphs();
        return glyphMetrics[i];
    }
    
    /**
     * Embed this font to the file.
     * 
     * @param font The font to include
     * @param name The name under which this font is addressed within the
     *        document (can be retrieved by <tt>getFontName()</tt>)
     */
    public void includeFont(Font font, CharTable charTable, String name, boolean[] codesUsed)
            throws IOException {
        
        // Evaluated on first demand - reset to recalculate for new font
        glyphShapes = null;
        glyphWidths = null;
        glyphMetrics = null;
        fontBBox = null; 

        this.font = font;
        this.charTable = charTable;
        this.fontName = name;
        this.codeUsed = codesUsed;

        noDefinedChars = 0;
        unicode = new char[256];
        charName = new String[256];
        
        for (int i = 0; i < unicode.length; i++) {
            charName[i] = charTable.toName(i);
            if (codeUsed[i] && charName[i] != null) {
                unicode[i] = charTable.toUnicode(charName[i]);
                noDefinedChars++;
            } else {
                unicode[i] = 0;
            }
        }

        openIncludeFont();
        writeEncoding(charTable);
        closeIncludeFont();
    }

    protected double getUndefinedWidth() {
        return FONT_SIZE;
    }
    
    
    private static final Map<String, Map<Integer, String>> logicalFontsReplacement = new HashMap<String, Map<Integer, String>>();
    private static final Map<String, Map<Integer, String>> standardFontsReplacement = new HashMap<String, Map<Integer, String>>();
    //
    static {
        Map<Integer, String> timesMap = new HashMap<Integer, String>();
        timesMap.put(Font.PLAIN, "Times-Roman");
        timesMap.put(Font.ITALIC, "Times-Italic");
        timesMap.put(Font.BOLD, "Times-Bold");
        timesMap.put(Font.ITALIC | Font.BOLD, "Times-BoldItalic");

        Map<Integer, String> helveticaMap = new HashMap<Integer, String>();
        helveticaMap.put(Font.PLAIN, "Helvetica");
        helveticaMap.put(Font.ITALIC, "Helvetica-Oblique");
        helveticaMap.put(Font.BOLD, "Helvetica-Bold");
        helveticaMap.put(Font.ITALIC | Font.BOLD, "Helvetica-BoldOblique");

        Map<Integer, String> courierMap = new HashMap<Integer, String>();
        courierMap.put(Font.PLAIN, "Courier");
        courierMap.put(Font.ITALIC, "Courier-Oblique");
        courierMap.put(Font.BOLD, "Courier-Bold");
        courierMap.put(Font.ITALIC | Font.BOLD, "Courier-BoldOblique");
      
        Map<Integer, String> symbolMap = new HashMap<Integer, String>(4);
        symbolMap.put(Font.PLAIN, "Symbol");
        symbolMap.put(Font.BOLD, "Symbol");
        symbolMap.put(Font.ITALIC, "Symbol");
        symbolMap.put(Font.BOLD | Font.ITALIC, "Symbol");

        Map<Integer, String> zapfDingbatsMap = new HashMap<Integer, String>(4);
        zapfDingbatsMap.put(Font.PLAIN, "ZapfDingbats");
        zapfDingbatsMap.put(Font.BOLD, "ZapfDingbats");
        zapfDingbatsMap.put(Font.ITALIC, "ZapfDingbats");
        zapfDingbatsMap.put(Font.BOLD | Font.ITALIC, "ZapfDingbats");

        
        logicalFontsReplacement.put(Font.DIALOG, helveticaMap);
        logicalFontsReplacement.put(Font.DIALOG_INPUT, helveticaMap);
        logicalFontsReplacement.put(Font.SERIF, timesMap);
        logicalFontsReplacement.put(Font.SANS_SERIF, helveticaMap);
        logicalFontsReplacement.put(Font.MONOSPACED, courierMap);
        
        
        standardFontsReplacement.put("Times", timesMap);
        standardFontsReplacement.put("Times New Roman", timesMap);
        standardFontsReplacement.put("Nimbus Roman No9 L", timesMap);
        
        standardFontsReplacement.put("Helvetica", helveticaMap);
        standardFontsReplacement.put("Arial", helveticaMap);
        standardFontsReplacement.put("Nimbus Sans L", helveticaMap);
        
        standardFontsReplacement.put("Courier", courierMap);
        standardFontsReplacement.put("Courier New", courierMap);
        standardFontsReplacement.put("Nimbus Mono L", courierMap);
        
        standardFontsReplacement.put("Symbol", symbolMap);
        standardFontsReplacement.put("ZapfDingbats", zapfDingbatsMap);
    }

    private static String getFontPSRemap(Font font, Map<String, Map<Integer, String>> replaceMap)
    {
        String family = font.getFamily();
        
        Map<Integer, String> fontMap = replaceMap.get(family);
        if (fontMap != null) {
            return fontMap.get(FontMap.getStyle(font));
        }
        
//        for (String mapFamily : replaceMap.keySet()) {
//             if (family.startsWith(mapFamily)) {
//                  return replaceMap.get(mapFamily).get(FontMap.getStyle(font));
//              }
//        }
        
        return null;
    }
    
    protected static String getLogicalFontPSRemap(Font font) {
       return getFontPSRemap(font, logicalFontsReplacement);
    }

    protected static String getStandardFontPSRemap(Font font) {
       return getFontPSRemap(font, standardFontsReplacement);
    }
    
    public static boolean isStandardFont(Font font) {
        return getStandardFontPSRemap(font) != null;
    }
    
    public static String getFontRemappedPSName(Font font) {
        String logicalFontPSRemap = getLogicalFontPSRemap(font);
        if (logicalFontPSRemap != null)
          return logicalFontPSRemap;
        
        String standardFontPSRemap = getStandardFontPSRemap(font);
        if (standardFontPSRemap != null)
          return standardFontPSRemap;

        return getFontPSName(font);
    }
    
    public static String getFontPSName(Font font) {
        return font.getPSName().replace("-Derived", "");
    }
    
    public String getFontPSName() {
        return getFontPSName(this.font);
    }
 }
