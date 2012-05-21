/*
 * Copyright 2012 Alexander.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
  * @author Alexander Levantovsky, MagicPlot
 */
public abstract class GlyphUsageCharTable implements CharTable
{
      private Map<String, boolean[]> glyphUsageMap = new HashMap<String, boolean[]>();

      public void setUseGlyph(Font font, int encoding) {
          boolean[] usage = glyphUsageMap.get(font.getPSName());
          if (usage == null) {
              usage = new boolean[256];
              Arrays.fill(usage, false);
              glyphUsageMap.put(font.getPSName(), usage);
             
//              System.out.println("Added: " + getName() + getEncoding() + " Font: " + font.getPSName());
          }
          usage[encoding] = true;
      }
    
      public boolean isUsedGlyph(Font font, int encoding) {
          boolean[] usage = glyphUsageMap.get(font.getPSName());
          // If font was not used at all - it may be an error, so return true
          return usage == null || usage[encoding];
      }
}
