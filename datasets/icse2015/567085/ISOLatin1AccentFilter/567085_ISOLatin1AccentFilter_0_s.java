 package org.apache.lucene.analysis;
 
 /**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 /**
  * A filter that replaces accented characters in the ISO Latin 1 character set 
  * (ISO-8859-1) by their unaccented equivalent. The case will not be altered.
  * <p>
  * For instance, '&agrave;' will be replaced by 'a'.
  * <p>
  */
 public class ISOLatin1AccentFilter extends TokenFilter {
   public ISOLatin1AccentFilter(TokenStream input) {
     super(input);
   }
 
   private char[] output = new char[256];
   private int outputPos;
 
   public final Token next(Token result) throws java.io.IOException {
     result = input.next(result);
     if (result != null) {
      outputPos = 0;
      removeAccents(result.termBuffer(), result.termLength());
       result.setTermBuffer(output, 0, outputPos);
       return result;
     } else
       return null;
   }
 
  private final void addChar(char c) {
    if (outputPos == output.length) {
      char[] newArray = new char[2*output.length];
      System.arraycopy(output, 0, newArray, 0, output.length);
      output = newArray;
    }
    output[outputPos++] = c;
  }

   /**
    * To replace accented characters in a String by unaccented equivalents.
    */
   public final void removeAccents(char[] input, int length) {
     int pos = 0;
     for (int i=0; i<length; i++, pos++) {
      switch (input[pos]) {
       case '\u00C0' : // Ã
       case '\u00C1' : // Ã
       case '\u00C2' : // Ã
       case '\u00C3' : // Ã
       case '\u00C4' : // Ã
       case '\u00C5' : // Ã
        addChar('A');
         break;
       case '\u00C6' : // Ã
        addChar('A');
        addChar('E');
         break;
       case '\u00C7' : // Ã
        addChar('C');
         break;
       case '\u00C8' : // Ã
       case '\u00C9' : // Ã
       case '\u00CA' : // Ã
       case '\u00CB' : // Ã
        addChar('E');
         break;
       case '\u00CC' : // Ã
       case '\u00CD' : // Ã
       case '\u00CE' : // Ã
       case '\u00CF' : // Ã
        addChar('I');
         break;
       case '\u00D0' : // Ã
        addChar('D');
         break;
       case '\u00D1' : // Ã
        addChar('N');
         break;
       case '\u00D2' : // Ã
       case '\u00D3' : // Ã
       case '\u00D4' : // Ã
       case '\u00D5' : // Ã
       case '\u00D6' : // Ã
       case '\u00D8' : // Ã
        addChar('O');
         break;
       case '\u0152' : // Å
        addChar('O');
        addChar('E');
         break;
       case '\u00DE' : // Ã
        addChar('T');
        addChar('H');
         break;
       case '\u00D9' : // Ã
       case '\u00DA' : // Ã
       case '\u00DB' : // Ã
       case '\u00DC' : // Ã
        addChar('U');
         break;
       case '\u00DD' : // Ã
       case '\u0178' : // Å¸
        addChar('Y');
         break;
       case '\u00E0' : // Ã 
       case '\u00E1' : // Ã¡
       case '\u00E2' : // Ã¢
       case '\u00E3' : // Ã£
       case '\u00E4' : // Ã¤
       case '\u00E5' : // Ã¥
        addChar('a');
         break;
       case '\u00E6' : // Ã¦
        addChar('a');
        addChar('e');
         break;
       case '\u00E7' : // Ã§
        addChar('c');
         break;
       case '\u00E8' : // Ã¨
       case '\u00E9' : // Ã©
       case '\u00EA' : // Ãª
       case '\u00EB' : // Ã«
        addChar('e');
         break;
       case '\u00EC' : // Ã¬
       case '\u00ED' : // Ã­
       case '\u00EE' : // Ã®
       case '\u00EF' : // Ã¯
        addChar('i');
         break;
       case '\u00F0' : // Ã°
        addChar('d');
         break;
       case '\u00F1' : // Ã±
        addChar('n');
         break;
       case '\u00F2' : // Ã²
       case '\u00F3' : // Ã³
       case '\u00F4' : // Ã´
       case '\u00F5' : // Ãµ
       case '\u00F6' : // Ã¶
       case '\u00F8' : // Ã¸
        addChar('o');
         break;
       case '\u0153' : // Å
        addChar('o');
        addChar('e');
         break;
       case '\u00DF' : // Ã
        addChar('s');
        addChar('s');
         break;
       case '\u00FE' : // Ã¾
        addChar('t');
        addChar('h');
         break;
       case '\u00F9' : // Ã¹
       case '\u00FA' : // Ãº
       case '\u00FB' : // Ã»
       case '\u00FC' : // Ã¼
        addChar('u');
         break;
       case '\u00FD' : // Ã½
       case '\u00FF' : // Ã¿
        addChar('y');
         break;
       default :
        addChar(input[pos]);
         break;
       }
     }
   }
 }
