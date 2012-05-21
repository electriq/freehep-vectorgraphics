/*
 * Copyright 2012 Alexander Levantovsky, Magicplot Systems, LLC.
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
package org.freehep.graphicsio.pdf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexander Levantovsky, MagicPlot
 */
public class AlphaQueue
{
  private Map<Float, String> extGStates = new HashMap<Float, String>();
  private int alphaIndex = 1;

  public String getAlphaName(float alpha) {
      String alphaName = extGStates.get(alpha);
      if (alphaName == null)
      {
        alphaName = "Alpha" + alphaIndex;
        alphaIndex++;
        extGStates.put(alpha, alphaName);
      }
      return alphaName;
  }
  
  public Set<Float> keySet() {
    return extGStates.keySet();
  }
}
