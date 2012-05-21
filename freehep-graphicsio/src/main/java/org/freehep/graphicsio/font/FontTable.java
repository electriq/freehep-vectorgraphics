// Copyright 2001-2009 FreeHEP
package org.freehep.graphicsio.font;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.freehep.graphics2d.font.CharTable;
import org.freehep.graphics2d.font.CustomCharTable;
import org.freehep.graphics2d.font.FontMap;
import org.freehep.graphics2d.font.FontUtilities;
import org.freehep.graphics2d.font.Lookup;

/**
 * A table to remember which fonts were used while writing a document.
 * 
 * @author Simon Fischer
 * @author Alexander Levantovsky, MagicPlot
 */
public abstract class FontTable {

    protected class Entry {
        private Font font;

        private String ref;

        private CharTable encoding;

        private boolean written;
        
        boolean[] codesUsed;

        private Entry(Font f, CharTable encoding, boolean embed, boolean embedStandard) {
            // get attributes of font for the stored default font
            Map<Attribute, Object> attributes = FontUtilities.getAttributes(f);

            // set default font size
            attributes.put(TextAttribute.SIZE, new Float(FontEmbedder.FONT_SIZE));

            // remove font transformations
            attributes.remove(TextAttribute.TRANSFORM);
            attributes.remove(TextAttribute.SUPERSCRIPT);

            this.font = FontMap.getFont(attributes);

            this.ref = createFontReference(this.font, encoding instanceof CustomCharTable, embed, embedStandard);
            this.encoding = encoding;
            this.written = false;
            this.codesUsed = new boolean[256];
            Arrays.fill(codesUsed, false);
        }

        public Font getFont() {
            return font;
        }

        public String getReference() {
            return ref;
        }

        protected void setReference(String ref) {
            this.ref = ref;
        }

        public CharTable getEncoding() {
            return encoding;
        }

        public void setWritten(boolean written) {
            this.written = written;
        }

        public boolean isWritten() {
            return written;
        }
        
        public void useCode(int code) {
            codesUsed[code] = true;
        }

        public void useCodes(String encoded) {
            for (int i = 0; i < encoded.length(); i++) {
                useCode((int)encoded.charAt(i));
            }
        }
        
        public boolean[] getCodesUsed() {
            return codesUsed;
        }

        public String toString() {
            return ref + "=" + font;
        }
    }

    private Hashtable<String, Entry> table;

    public FontTable() {
        this.table = new Hashtable<String, Entry>();
    }

    /**
     * Returns a default CharTable to be used for normal text (not Symbol or
     * Dingbats).
     */
    public abstract CharTable getEncodingTable();

    /**
     * Called whenever a specific font is used for the first time. Subclasses
     * may use this method to include the font instantly. This method may change
     * the value of the reference by calling <tt>e.setReference(String)</tt>
     * e.g. if it wants to substitute the font by a standard font that can be
     * addressed under a name different from the generated one.
     */
    protected abstract void firstRequest(Entry e, boolean embed, String embedAs)
            throws IOException;

    /** Creates a unique reference to address this font. */
    protected abstract String createFontReference(Font f, boolean customCharTable, boolean embed, boolean embedStandard);

    protected abstract Font substituteFont(Font font);

    /**
     * Returns a name for this font that can be used in the document. A new name
     * is generated if the font was not used yet. For different fontsizes the
     * same name is returned.
     */
    public String fontReference(Font font, boolean customEncoding, boolean embed, boolean embedStandard, String embedAs) {
        return fontEntry(font, customEncoding, embed, embedStandard, embedAs).getReference();
    }

    public void useCodes(Font font, boolean customEncoding, boolean embed, boolean embedStandard, String embedAs, String encoded) {
        fontEntry(font, customEncoding, embed, embedStandard, embedAs).useCodes(encoded);
    }

    public boolean[] getCodeUsed(Font font, boolean customEncoding, boolean embed, boolean embedStandard, String embedAs) {
        return fontEntry(font, customEncoding, embed, embedStandard, embedAs).getCodesUsed();
    }

    public Entry fontEntry(Font font, boolean customEncoding, boolean embed, boolean embedStandard, String embedAs) {
        // look for stored font
        font = substituteFont(font);
        String key = getKey(font, customEncoding, embed, embedStandard);
        Entry e = table.get(key);

        // create new one
        if (e == null) {
            e = createFontEntry(font, customEncoding, embed, embedStandard, embedAs);
            table.put(key, e);
        }

        return e;
    }
    
    private Entry createFontEntry(Font font, boolean customEncoding, boolean embed, boolean embedStandard, String embedAs) {
        Entry e = new Entry(font, customEncoding ? new CustomCharTable() : getEncodingTable(font), embed, embedStandard);
        try {
            firstRequest(e, embed, embedAs);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return e;
  }

    /**
     * To embed all derivations of a font too (with underline,
     * strikethrough etc.) the key consists all these attributes.
     *
     * @param font ist attributes are used
     * @return something like Helvetica[BOLD:1][ITALIC:0][UNDERLINE:1]
     */
    private String getKey(Font font, boolean customEncoding, boolean embed, boolean embedStandard) {
        return createFontReference(font, customEncoding, embed, embedStandard);
    }

    /**
     * Returns a Collection view of all fonts. The elements of the collection
     * are <tt>Entrie</tt>s.
     */
    public Collection<Entry> getEntries() {
        return table.values();
    }

    public CharTable getCustomCharTableFor(Font font, boolean embedStandard, String embedAs) {
        return fontEntry(font, true, true, embedStandard, embedAs).getEncoding();
    }
    
    private CharTable getEncodingTable(Font font) {
        String fontname = font.getName().toLowerCase();
        if (fontname.indexOf("symbol") >= 0)
            return Lookup.getInstance().getTable("Symbol");
        if (fontname.indexOf("zapfdingbats") >= 0)
            return Lookup.getInstance().getTable("Zapfdingbats");
        return getEncodingTable();
    }

}
