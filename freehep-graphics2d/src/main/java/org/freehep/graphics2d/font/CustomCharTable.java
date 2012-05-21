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

import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author Alexander Levantovsky, MagicPlot
 */
public class CustomCharTable implements CharTable
{
  	private Hashtable<Character, String> unicodeToName = new Hashtable<Character, String>();
	private Hashtable<String, Character> nameToUnicode = new Hashtable<String, Character>();
	private Hashtable<String, Integer> nameToEnc = new Hashtable<String, Integer>();
	private ArrayList<String> encToName = new ArrayList<String>();

        public CustomCharTable() {
            // First 32 codes are special codes, so don't use them
            for (int i = 0; i < 32; i++)
                addUnicodeChar((char) i, ".notdef");
        }

        protected int addUnicodeChar(char c) {
            String name = Lookup.getInstance().toName(c);
            if (name == null) {
                name = "uni" + unicodeToHex(c);
            }
           
            return addUnicodeChar(c, name);
        }
        
        public static String unicodeToHex(int unicode) {
            return unicodeToHex(unicode, 4);
        }

        public static String unicodeToHex(int unicode, int chars) {
            StringBuilder hex = new StringBuilder(Integer.toHexString(unicode).toUpperCase());
            while (hex.length() < chars)
              hex.insert(0, "0");
            return hex.toString();
        }
        
        protected int addUnicodeChar(char c, String name) {
            int code = encToName.size();
            if (code > 255)
              return 0;
            
            unicodeToName.put(c, name);
            nameToUnicode.put(name, c);
            nameToEnc.put(name, code);
            encToName.add(name);
            return code;
        }
        
        protected void checkCharAdded(char c) {
            if (!unicodeToName.containsKey(c))
              addUnicodeChar(c);
        }
        
  	public String toName(Character c){
            checkCharAdded(c);
            return unicodeToName.get(c);
	}

        public String toName(char c) {
            checkCharAdded(c);
            return unicodeToName.get(c);
        }

        public String toName(int enc){
            if (enc > 0 && enc < encToName.size())
                return encToName.get(enc);
            return null;
	}

        public String toName(Integer enc) {
            if (enc > 0 && enc < encToName.size())
                return encToName.get(enc);
            return null;
        }

	public int toEncoding(String name){
            return nameToEnc.get(name);
	}

        public int toEncoding(char c) {
            checkCharAdded(c);
            return toEncoding(toName(c));
        }

	public char toUnicode(String name){
            return nameToUnicode.get(name);
	}
        
	public String getName(){
            return "Custom";
	}

	public String getEncoding(){
            return "PDF";
	}
}
