// Copyright FreeHEP, 2003-2007
package org.freehep.graphics2d.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.*;

/**
 * 
 * @author Mark Donszelmann
 * @version $Id: freehep-graphics2d/src/main/java/org/freehep/graphics2d/font/FontUtilities.java 59372df5e0d9 2007/02/06 21:11:19 duns $
 */
public class FontUtilities {

    private FontUtilities() {
    }

    public static List<String> getAllAvailableFonts() {
        return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
    }

    private static final Properties windowsFonts = new Properties();
    static {
        // logical fonts
        windowsFonts.setProperty(Font.DIALOG, "Arial");
        windowsFonts.setProperty(Font.DIALOG_INPUT, "Courier New");
        windowsFonts.setProperty(Font.SERIF, "Times New Roman");
        windowsFonts.setProperty(Font.SANS_SERIF, "Arial");
        windowsFonts.setProperty(Font.MONOSPACED, "Courier New");

        // pdf fonts
        windowsFonts.setProperty("Courier", "Courier New");
        windowsFonts.setProperty("Helvetica", "Arial");
        windowsFonts.setProperty("Times-Roman", "Times New Roman");
        windowsFonts.setProperty("TimesRoman", "Times New Roman");
        windowsFonts.setProperty("Times", "Times New Roman");
        windowsFonts.setProperty("Symbol", "Arial Unicode MS");
        windowsFonts.setProperty("ZapfDingbats", "Arial Unicode MS");
    }

    public static String getWindowsFontFamily(String fontFamily) {
        return windowsFonts.getProperty(fontFamily, fontFamily);
    }

    /**
     * @deprecated use
     *             org.freehep.graphics2d.font.FontEncoder.getEncodedString()
     */
    public static String getEncodedString(String string, String tableName) {
        return FontEncoder.getEncodedString(string, tableName);
    }

    /**
     * Returns an unicode encoded string from an ascii encoded string, using the
     * supplied table.
     * 
     * @deprecated use
     *             org.freehep.graphics2d.font.FontEncoder.getEncodedString()
     */
    public static String getEncodedString(String string, CharTable charTable) {
        return FontEncoder.getEncodedString(string, charTable);
    }

    public interface ShowString {
        public void showString(Font font, String string, boolean customCharTable) throws IOException;
        public CharTable getCustomCharTableFor(Font font);
    }

    private static final CharTable STANDARD_CHAR_TABLES[] = {
            Lookup.getInstance().getTable("Symbol"),
            Lookup.getInstance().getTable("Zapfdingbats") };

    private static final Font STANDARD_FONT[] = {
            new Font("Symbol", Font.PLAIN, 10),
            new Font("ZapfDingbats", Font.PLAIN, 10) };

    /**
     * Shows a String and switches the encoding (and font) everytime the unicode
     * characters leave the range of the curent encoding. Outside the range of
     * the given latinTable, Symbol and ZapfDingbats are checked. If none of
     * these three encodings contain the unicode character, an undefined
     * character is used.
     */
    public static void showString(final Font font, final String string,
            final CharTable latinTable, final ShowString device, final boolean fontEmbedding) throws IOException {

        if (latinTable == null) throw new RuntimeException("FontUtilities.showString(...): latinTable cannot be 'null'");

        STANDARD_FONT[0] = new Font("Symbol", Font.PLAIN, font.getSize()).deriveFont(font.getSize2D());
        STANDARD_FONT[1] = new Font("ZapfDingbats", Font.PLAIN, font.getSize()).deriveFont(font.getSize2D());

        String out = "";
        CharTable lastTable = latinTable;
        Font lastFont = font;

        for (char ch : string.toCharArray()) {
            CharTable resultTable = latinTable;
            Font resultFont = font;
            char encodedChar = (char) resultTable.toEncoding(ch);
            if (encodedChar == 0) {
                if (fontEmbedding){
                    resultTable = device.getCustomCharTableFor(font);
                    encodedChar = (char) resultTable.toEncoding(ch);
                }
                else {
                    int tableNum = -1;
                    do {
                        tableNum++;
                        resultTable = STANDARD_CHAR_TABLES[tableNum];
                        resultFont = STANDARD_FONT[tableNum];
                        encodedChar = (char) resultTable.toEncoding(ch);
                    } while ((encodedChar == 0) && tableNum < STANDARD_CHAR_TABLES.length - 1);
                    if (encodedChar == 0) {
                        resultTable = lastTable;
                        resultFont = font;
                    }
                }
            }
            
            if (!resultTable.equals(lastTable) && out.length() > 0) {
                device.showString(lastFont, out, lastTable instanceof CustomCharTable);
                out = "";
            }
            
            // append character to out
            out += encodedChar;
            lastTable = resultTable;
            lastFont = resultFont;
        }

        device.showString(lastFont, out, lastTable instanceof CustomCharTable);
    }

    /**
     * there is a bug in the jdk 1.6 which makes
     * Font.getAttributes() not work correctly. The
     * method does not return all values. What we do here
     * is using the old JDK 1.5 method.
     *
     * @param font font
     * @return Attributes of font
     */
    public static Map<Attribute, Object> getAttributes(Font font) {
        return FontMap.getAttributes(font);
    }
}