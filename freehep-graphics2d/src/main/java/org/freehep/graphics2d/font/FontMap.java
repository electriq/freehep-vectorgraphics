/*
 * Copyright 2012 Alexander Levantovsky, MagicPlot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freehep.graphics2d.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Levantovsky
 */
public class FontMap
{
  private static final String[] PS_ITALIC_SUFFIXES = new String[]
  {
    "Italic", "Ital", "It", "Inclined", "Ic", "Oblique", "Obl"
  };
  private static final String[] PS_BOLD_SUFFIXES = new String[]
  {
    "Bold", "Bd", "Medium", "Medi", "Md", "Demi", "Dm"
  };
  //
  private static FontMap instance = null;
  // Family+style->Font
  private Map<String, Map<Integer, Font>> allFontsMap;
  private String[] allFontFamilies;

  /**
   * Font class wrapper to fix italic bugs. Italic text handling takes into
   * account italic slant angle when counting text advance (baseline width). So
   * plain text following italic text is moved to the right and it is a bug. This
   * Font class override getItalicAngle() method which always return 0 and the
   * problem of plain text after italic goes away.
   */
  static class AdequateFont extends Font
  {
    public AdequateFont(Font font)
    {
      super(font);
      this.style = PLAIN;
    }

    @Override
    public float getItalicAngle()
    {
      super.getItalicAngle();
      return 0;
    }

    @Override
    public int getStyle()
    {
      return FontMap.getStyle(this);
    }

    @Override
    public boolean isBold()
    {
      return FontMap.isBold(this);
    }

    @Override
    public boolean isItalic()
    {
      return FontMap.isItalic(this);
    }

    @Override
    public boolean isPlain()
    {
      return !isItalic() && !isBold();
    }

    @Override
    public Font deriveFont(int style, float size)
    {
      return new AdequateFont(super.deriveFont(style, size));
    }

    @Override
    public Font deriveFont(int style, AffineTransform trans)
    {
      return new AdequateFont(super.deriveFont(style, trans));
    }

    @Override
    public Font deriveFont(float size)
    {
      return new AdequateFont(super.deriveFont(size));
    }

    @Override
    public Font deriveFont(AffineTransform trans)
    {
      return new AdequateFont(super.deriveFont(trans));
    }

    @Override
    public Font deriveFont(int style)
    {
      return new AdequateFont(super.deriveFont(style));
    }

    @Override
    public Font deriveFont(Map<? extends Attribute, ?> attributes)
    {
      return new AdequateFont(super.deriveFont(attributes));
    }
  }

  private FontMap()
  {
    updateAllFontsMap();
  }

  public static FontMap getInstance()
  {
    if (instance == null)
      instance = new FontMap();
    return instance;
  }

  public Map<String, Map<Integer, Font>> getAllFontsMap()
  {
    return allFontsMap;
  }

  private static void sortFontsByPSName(Font[] fonts)
  {
    Arrays.sort(fonts, new Comparator<Font>()
    {
      public int compare(Font o1, Font o2)
      {
        return o1.getPSName().compareTo(o2.getPSName());
      }
    });
  }

  public final void updateAllFontsMap()
  {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Font[] allFonts = ge.getAllFonts();
    sortFontsByPSName(allFonts);
    allFontsMap = new HashMap<String, Map<Integer, Font>>(allFonts.length * 4);

    for (Font font : allFonts)
    {
      String family = font.getFamily();
      Map<Integer, Font> fontMap = allFontsMap.get(family);
      if (fontMap == null)
      {
        fontMap = new HashMap<Integer, Font>();
        allFontsMap.put(family, fontMap);
      }

      int style = getStyle(font);

      Font existingFont = fontMap.get(style);
      if (existingFont == null)
        fontMap.put(style, font);
      else
      {
        int rating = (getItalicIndex(font) + 1) + (getBoldIndex(font) + 1);
        int existingRating = (getItalicIndex(existingFont) + 1) + (getBoldIndex(existingFont) + 1);
        
        if (rating < existingRating)
          fontMap.put(style, font);
      }
    }

    allFontFamilies = new String[allFontsMap.size()];
    int i = 0;
    for (String family : allFontsMap.keySet())
      allFontFamilies[i++] = family;
    Arrays.sort(allFontFamilies);
  }

  public static Font getFont(Font font)
  {
    return getInstance().getFont(font.getFamily(), getStyle(font), font.getSize2D());
  }

  public Font getFont(String family, int style, float size)
  {
    return new AdequateFont(getRawFont(family, style, size));
  }

  private Font getRawFont(String family, int style, float size)
  {
    // Leave only used bits
    int corrStyle = style & (Font.PLAIN | Font.BOLD | Font.ITALIC);
    
    Map<Integer, Font> fontMap = allFontsMap.get(family);
    if (fontMap != null)
    {
      Font font = fontMap.get(corrStyle);
      if (font != null)
        return font.deriveFont(size);

      font = fontMap.get(Font.PLAIN);
      if (font != null)
        return font.deriveFont(corrStyle, size);
    }

    return new Font(family, corrStyle, (int)size).deriveFont(size);
  }

  public Font getFont(String family, boolean italic, boolean bold, float size)
  {
    return getFont(family, (italic ? Font.ITALIC : 0) | (bold ? Font.BOLD : 0), size);
  }

  public String[] getAllFontFamilies()
  {
    return allFontFamilies;
  }

  public static String getPSStyleString(Font font)
  {
    String psName = font.getPSName();
    int pos = psName.indexOf('-');
    if (pos < 0)
      pos = psName.indexOf('.');

    if (pos < 0)
      return "";

    return psName.substring(pos + 1);
  }

  private static int getBoldIndex(Font font)
  {
    String styleStr = getPSStyleString(font).toLowerCase();
    for (int i = 0; i < PS_BOLD_SUFFIXES.length; i++)
      if (styleStr.contains(PS_BOLD_SUFFIXES[i].toLowerCase()))
        return i;
    return -1;
  }

  private static int getItalicIndex(Font font)
  {
    String styleStr = getPSStyleString(font).toLowerCase();
    for (int i = 0; i < PS_ITALIC_SUFFIXES.length; i++)
      if (styleStr.contains(PS_ITALIC_SUFFIXES[i].toLowerCase()))
        return i;
    return -1;
  }

  public static boolean isBold(Font font)
  {
    return getBoldIndex(font) >= 0;
  }

  public static boolean isItalic(Font font)
  {
    return getItalicIndex(font) >= 0;
  }

  public static int getStyle(Font font)
  {
    int style = Font.PLAIN;
    if (isItalic(font))
      style |= Font.ITALIC;
    if (isBold(font))
      style |= Font.BOLD;
    return style;
  }

  public static Font getFont(Map<Attribute, Object> attributes)
  {
    return getInstance().getFontFrom(attributes);
  }

  /**
   * This method should be used to create font from attributes
   *
   * @param font
   * @param attributes
   * @return
   */
  public Font getFontFrom(Map<Attribute, Object> attributes)
  {
    Font baseFont = (Font) attributes.get(TextAttribute.FONT);

    String family = (String) attributes.get(TextAttribute.FAMILY);
    if (family == null)
      family = baseFont.getFamily();

    Number size = (Number) attributes.get(TextAttribute.SIZE);
    if (size == null)
      size = baseFont.getSize2D();

    Number posture = (Number) attributes.get(TextAttribute.POSTURE);
    if (posture == null)
      posture = isItalic(baseFont) ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR;

    Number weight = (Number) attributes.get(TextAttribute.WEIGHT);
    if (weight == null)
      weight = isBold(baseFont) ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR;

    Font font = getFont(family,
            !TextAttribute.POSTURE_REGULAR.equals(posture),
            !TextAttribute.WEIGHT_REGULAR.equals(weight),
            size.floatValue());

    Map<Attribute, Object> attrs = new HashMap<Attribute, Object>();
    attrs.put(TextAttribute.TRANSFORM, attributes.get(TextAttribute.TRANSFORM));
    // This is really used!
    attrs.put(TextAttribute.SUPERSCRIPT, attributes.get(TextAttribute.SUPERSCRIPT));

    return font.deriveFont(attrs);
  }

  public static Map<Attribute, Object> getAttributes(Font font)
  {
    Map<Attribute, Object> result = new HashMap<Attribute, Object>(7, (float) 0.9);
    result.put(TextAttribute.FONT, font);

    result.put(TextAttribute.TRANSFORM, font.getTransform());
    result.put(TextAttribute.FAMILY, font.getFamily());
    result.put(TextAttribute.SIZE, new Float(font.getSize2D()));
    result.put(TextAttribute.WEIGHT, isBold(font)
            ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
    result.put(TextAttribute.POSTURE, isItalic(font)
            ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);

    result.put(TextAttribute.SUPERSCRIPT, new Integer(0));
    result.put(TextAttribute.WIDTH, new Float(1));
    return result;
  }
}
