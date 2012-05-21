// Copyright 2003-2007, FreeHEP.
package org.freehep.graphicsio.exportchooser;

import java.awt.Component;
import java.util.Properties;

import org.freehep.graphicsio.FontConstants;
import org.freehep.swing.layout.TableLayout;

/**
 * 
 * @author Mark Donszelmann
 * @author Alexander Levantovsky, MagicPlot
 * @version $Id: freehep-graphicsio/src/main/java/org/freehep/graphicsio/exportchooser/FontPanel.java 59372df5e0d9 2007/02/06 21:11:19 duns $
 */
public class FontPanel extends OptionPanel {

    /**
     * defines the optionpanel for font embedding and text as shapes.
     *
     * @param options
     * @param embeddingRootKey rootkey for {@link String abstractRootKey = AbstractVectorGraphicsIO.class.getName();#EMBED_FONTS} and {@link FontConstants#EMBED_FONTS_AS}
     * @param shapeRootKey rootkey for {@link FontConstants#TEXT_AS_SHAPES}
     */
    public FontPanel(Properties options, String embeddingRootKey, String shapeRootKey) {
      this(options, embeddingRootKey, shapeRootKey, true);
    }
    
    /**
     * defines the optionpanel for font embedding and text as shapes.
     *
     * @param options
     * @param embeddingRootKey rootkey for {@link String abstractRootKey = AbstractVectorGraphicsIO.class.getName();#EMBED_FONTS} and {@link FontConstants#EMBED_FONTS_AS}
     * @param shapeRootKey rootkey for {@link FontConstants#TEXT_AS_SHAPES}
     * @param askType 
     */    
    public FontPanel(Properties options, String embeddingRootKey, String shapeRootKey, boolean askType) {
        super("Fonts");

        // to disable / enable for TEXT_AS_SHAPES
        Component enable = null;

        // font embedding
        if (embeddingRootKey != null) {
            final OptionCheckBox embedCheckBox = new OptionCheckBox(
                options,
                embeddingRootKey + "." + FontConstants.EMBED_FONTS,
                askType ? "Embed Fonts as:" : "Embed Fonts");
            add(askType ? TableLayout.LEFT : TableLayout.FULL, embedCheckBox);

            if (askType)
            {
                final OptionComboBox comboBox = new OptionComboBox(
                    options,
                    embeddingRootKey + "." + FontConstants.EMBED_FONTS_AS,
                    FontConstants .getEmbedFontsAsList());
                add(TableLayout.RIGHT, comboBox);
                embedCheckBox.enables(comboBox);
            }

            enable = embedCheckBox;

            final OptionCheckBox skipStandardFontsCheckBox = new OptionCheckBox(
                options,
                embeddingRootKey + "." + FontConstants.EMBED_SKIP_STANDARD_FONTS,
                "Do Not Embed 14 Standard Fonts");
            skipStandardFontsCheckBox.setToolTipText("<html>14 standard fons are:<br>\n"
                    + "Times (Times New Roman): regular, bold, italic, bold-italic,<br>\n"
                    + "Helvetica (Arial): regular, bold, italic, bold-italic,<br>\n"
                    + "Courier (Courier New): regular, bold, italic, bold-italic,<br>\n"
                    + "Symbol,<br>\n"
                    + "Zapf Dingbats.");
            add(TableLayout.FULL, skipStandardFontsCheckBox);
            
            embedCheckBox.enables(skipStandardFontsCheckBox);
        }

        // text es shape
        if (shapeRootKey != null) {
            final OptionCheckBox shapeCheckBox = new OptionCheckBox(
                options,
                shapeRootKey + "." + FontConstants.TEXT_AS_SHAPES,
                "Draw Text as Shapes");
            add(TableLayout.FULL, shapeCheckBox);
            
            if (enable != null) {
                shapeCheckBox.disables(enable);
            }
        }
    }
}
