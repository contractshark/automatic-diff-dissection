 package org.apache.lucene.analysis.compound;
 
 /*
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
 
 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.Arrays;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.charfilter.MappingCharFilter;
 import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
 import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 import org.apache.lucene.analysis.util.CharArraySet;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.util.Attribute;
 import org.apache.lucene.util.AttributeImpl;
 import org.xml.sax.InputSource;
 
 public class TestCompoundWordTokenFilter extends BaseTokenStreamTestCase {
 
   private static CharArraySet makeDictionary(String... dictionary) {
     return new CharArraySet(TEST_VERSION_CURRENT, Arrays.asList(dictionary), true);
   }
 
   public void testHyphenationCompoundWordsDA() throws Exception {
     CharArraySet dict = makeDictionary("lÃ¦se", "hest");
 
     InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
     HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter
         .getHyphenationTree(is);
 
     HyphenationCompoundWordTokenFilter tf = new HyphenationCompoundWordTokenFilter(TEST_VERSION_CURRENT, 
         new MockTokenizer(new StringReader("min veninde som er lidt af en lÃ¦sehest"), MockTokenizer.WHITESPACE, false), 
         hyphenator,
         dict, CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);
     assertTokenStreamContents(tf, 
         new String[] { "min", "veninde", "som", "er", "lidt", "af", "en", "lÃ¦sehest", "lÃ¦se", "hest" },
         new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 }
     );
   }
 
   public void testHyphenationCompoundWordsDELongestMatch() throws Exception {
     CharArraySet dict = makeDictionary("basketball", "basket", "ball", "kurv");
 
     InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
     HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter
         .getHyphenationTree(is);
 
     // the word basket will not be added due to the longest match option
     HyphenationCompoundWordTokenFilter tf = new HyphenationCompoundWordTokenFilter(TEST_VERSION_CURRENT, 
         new MockTokenizer(new StringReader("basketballkurv"), MockTokenizer.WHITESPACE, false), 
         hyphenator, dict,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE, 40, true);
     assertTokenStreamContents(tf, 
         new String[] { "basketballkurv", "basketball", "ball", "kurv" },
         new int[] { 1, 0, 0, 0 }
     );
 
   }
 
   /**
    * With hyphenation-only, you can get a lot of nonsense tokens.
    * This can be controlled with the min/max subword size.
    */
   public void testHyphenationOnly() throws Exception {
     InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
     HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter
         .getHyphenationTree(is);
     
     HyphenationCompoundWordTokenFilter tf = new HyphenationCompoundWordTokenFilter(
         TEST_VERSION_CURRENT,
         new MockTokenizer(new StringReader("basketballkurv"), MockTokenizer.WHITESPACE, false),
         hyphenator,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         2, 4);
     
     // min=2, max=4
     assertTokenStreamContents(tf,
         new String[] { "basketballkurv", "ba", "sket", "bal", "ball", "kurv" }
     );
     
     tf = new HyphenationCompoundWordTokenFilter(
         TEST_VERSION_CURRENT,
         new MockTokenizer(new StringReader("basketballkurv"), MockTokenizer.WHITESPACE, false),
         hyphenator,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         4, 6);
     
     // min=4, max=6
     assertTokenStreamContents(tf,
         new String[] { "basketballkurv", "basket", "sket", "ball", "lkurv", "kurv" }
     );
     
     tf = new HyphenationCompoundWordTokenFilter(
         TEST_VERSION_CURRENT,
         new MockTokenizer(new StringReader("basketballkurv"), MockTokenizer.WHITESPACE, false),
         hyphenator,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         4, 10);
     
     // min=4, max=10
     assertTokenStreamContents(tf,
         new String[] { "basketballkurv", "basket", "basketbal", "basketball", "sket", 
                        "sketbal", "sketball", "ball", "ballkurv", "lkurv", "kurv" }
     );
     
   }
 
   public void testDumbCompoundWordsSE() throws Exception {
     CharArraySet dict = makeDictionary("Bil", "DÃ¶rr", "Motor", "Tak", "Borr", "Slag", "Hammar",
         "Pelar", "Glas", "Ãgon", "Fodral", "Bas", "Fiol", "Makare", "GesÃ¤ll",
         "Sko", "Vind", "Rute", "Torkare", "Blad");
 
     DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, 
         new MockTokenizer( 
             new StringReader(
                 "BildÃ¶rr Bilmotor Biltak Slagborr Hammarborr Pelarborr GlasÃ¶gonfodral Basfiolsfodral BasfiolsfodralmakaregesÃ¤ll Skomakare Vindrutetorkare Vindrutetorkarblad abba"),
             MockTokenizer.WHITESPACE, false),
         dict);
 
     assertTokenStreamContents(tf, new String[] { "BildÃ¶rr", "Bil", "dÃ¶rr", "Bilmotor",
         "Bil", "motor", "Biltak", "Bil", "tak", "Slagborr", "Slag", "borr",
         "Hammarborr", "Hammar", "borr", "Pelarborr", "Pelar", "borr",
         "GlasÃ¶gonfodral", "Glas", "Ã¶gon", "fodral", "Basfiolsfodral", "Bas",
         "fiol", "fodral", "BasfiolsfodralmakaregesÃ¤ll", "Bas", "fiol",
         "fodral", "makare", "gesÃ¤ll", "Skomakare", "Sko", "makare",
         "Vindrutetorkare", "Vind", "rute", "torkare", "Vindrutetorkarblad",
         "Vind", "rute", "blad", "abba" }, new int[] { 0, 0, 3, 8, 8, 11, 17,
         17, 20, 24, 24, 28, 33, 33, 39, 44, 44, 49, 54, 54, 58, 62, 69, 69, 72,
         77, 84, 84, 87, 92, 98, 104, 111, 111, 114, 121, 121, 125, 129, 137,
         137, 141, 151, 156 }, new int[] { 7, 3, 7, 16, 11, 16, 23, 20, 23, 32,
         28, 32, 43, 39, 43, 53, 49, 53, 68, 58, 62, 68, 83, 72, 76, 83, 110,
         87, 91, 98, 104, 110, 120, 114, 120, 136, 125, 129, 136, 155, 141, 145,
         155, 160 }, new int[] { 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
         0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1,
         0, 0, 0, 1 });
   }
 
   public void testDumbCompoundWordsSELongestMatch() throws Exception {
     CharArraySet dict = makeDictionary("Bil", "DÃ¶rr", "Motor", "Tak", "Borr", "Slag", "Hammar",
         "Pelar", "Glas", "Ãgon", "Fodral", "Bas", "Fiols", "Makare", "GesÃ¤ll",
         "Sko", "Vind", "Rute", "Torkare", "Blad", "Fiolsfodral");
 
     DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, 
         new MockTokenizer(new StringReader("BasfiolsfodralmakaregesÃ¤ll"), MockTokenizer.WHITESPACE, false),
         dict, CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, true);
 
     assertTokenStreamContents(tf, new String[] { "BasfiolsfodralmakaregesÃ¤ll", "Bas",
         "fiolsfodral", "fodral", "makare", "gesÃ¤ll" }, new int[] { 0, 0, 3, 8,
         14, 20 }, new int[] { 26, 3, 14, 14, 20, 26 }, new int[] { 1, 0, 0, 0,
         0, 0 });
   }
 
   public void testTokenEndingWithWordComponentOfMinimumLength() throws Exception {
     CharArraySet dict = makeDictionary("ab", "cd", "ef");
 
     DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT,
       new WhitespaceTokenizer(TEST_VERSION_CURRENT,
         new StringReader(
           "abcdef")
         ),
       dict,
       CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
       CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
       CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);
 
     assertTokenStreamContents(tf,
       new String[] { "abcdef", "ab", "cd", "ef" },
       new int[] { 0, 0, 2, 4},
       new int[] { 6, 2, 4, 6},
       new int[] { 1, 0, 0, 0}
       );
   }
 
   public void testWordComponentWithLessThanMinimumLength() throws Exception {
     CharArraySet dict = makeDictionary("abc", "d", "efg");
 
     DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT,
       new WhitespaceTokenizer(TEST_VERSION_CURRENT,
         new StringReader(
           "abcdefg")
         ),
       dict,
       CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
       CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
       CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);
 
   // since "d" is shorter than the minimum subword size, it should not be added to the token stream
     assertTokenStreamContents(tf,
       new String[] { "abcdefg", "abc", "efg" },
       new int[] { 0, 0, 4},
       new int[] { 7, 3, 7},
       new int[] { 1, 0, 0}
       );
   }
 
   public void testReset() throws Exception {
     CharArraySet dict = makeDictionary("Rind", "Fleisch", "Draht", "Schere", "Gesetz",
         "Aufgabe", "Ãberwachung");
 
     Tokenizer wsTokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, new StringReader(
         "RindfleischÃ¼berwachungsgesetz"));
     DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, 
         wsTokenizer, dict,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);
     
     CharTermAttribute termAtt = tf.getAttribute(CharTermAttribute.class);
     tf.reset();
     assertTrue(tf.incrementToken());
     assertEquals("RindfleischÃ¼berwachungsgesetz", termAtt.toString());
     assertTrue(tf.incrementToken());
     assertEquals("Rind", termAtt.toString());
     wsTokenizer.setReader(new StringReader("RindfleischÃ¼berwachungsgesetz"));
     tf.reset();
     assertTrue(tf.incrementToken());
     assertEquals("RindfleischÃ¼berwachungsgesetz", termAtt.toString());
   }
 
   public void testRetainMockAttribute() throws Exception {
     CharArraySet dict = makeDictionary("abc", "d", "efg");
     Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT,
         new StringReader("abcdefg"));
     TokenStream stream = new MockRetainAttributeFilter(tokenizer);
     stream = new DictionaryCompoundWordTokenFilter(
         TEST_VERSION_CURRENT, stream, dict,
         CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
         CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);
     MockRetainAttribute retAtt = stream.addAttribute(MockRetainAttribute.class);
     stream.reset();
     while (stream.incrementToken()) {
       assertTrue("Custom attribute value was lost", retAtt.getRetain());
     }
 
   }
 
   public static interface MockRetainAttribute extends Attribute {
     void setRetain(boolean attr);
     boolean getRetain();
   }
 
   public static final class MockRetainAttributeImpl extends AttributeImpl implements MockRetainAttribute {
     private boolean retain = false;
     @Override
     public void clear() {
       retain = false;
     }
    @Override
     public boolean getRetain() {
       return retain;
     }
    @Override
     public void setRetain(boolean retain) {
       this.retain = retain;
     }
     @Override
     public void copyTo(AttributeImpl target) {
       MockRetainAttribute t = (MockRetainAttribute) target;
       t.setRetain(retain);
     }
   }
 
   private static class MockRetainAttributeFilter extends TokenFilter {
     
     MockRetainAttribute retainAtt = addAttribute(MockRetainAttribute.class);
     
     MockRetainAttributeFilter(TokenStream input) {
       super(input);
     }
     
     @Override
     public boolean incrementToken() throws IOException {
       if (input.incrementToken()){
         retainAtt.setRetain(true); 
         return true;
       } else {
       return false;
       }
     }
   }
   
   // SOLR-2891
   // *CompoundWordTokenFilter blindly adds term length to offset, but this can take things out of bounds
   // wrt original text if a previous filter increases the length of the word (in this case Ã¼ -> ue)
   // so in this case we behave like WDF, and preserve any modified offsets
   public void testInvalidOffsets() throws Exception {
     final CharArraySet dict = makeDictionary("fall");
     final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
     builder.add("Ã¼", "ue");
     final NormalizeCharMap normMap = builder.build();
     
     Analyzer analyzer = new Analyzer() {
 
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         TokenFilter filter = new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, tokenizer, dict);
         return new TokenStreamComponents(tokenizer, filter);
       }
 
       @Override
       protected Reader initReader(String fieldName, Reader reader) {
         return new MappingCharFilter(normMap, reader);
       }
     };
 
     assertAnalyzesTo(analyzer, "bankÃ¼berfall", 
         new String[] { "bankueberfall", "fall" },
         new int[] { 0,  0 },
         new int[] { 12, 12 });
   }
   
   /** blast some random strings through the analyzer */
   public void testRandomStrings() throws Exception {
     final CharArraySet dict = makeDictionary("a", "e", "i", "o", "u", "y", "bc", "def");
     Analyzer a = new Analyzer() {
 
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         return new TokenStreamComponents(tokenizer, new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, tokenizer, dict));
       }
     };
     checkRandomData(random(), a, 1000*RANDOM_MULTIPLIER);
     
     InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
     final HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
     Analyzer b = new Analyzer() {
 
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         TokenFilter filter = new HyphenationCompoundWordTokenFilter(TEST_VERSION_CURRENT, tokenizer, hyphenator);
         return new TokenStreamComponents(tokenizer, filter);
       }
     };
     checkRandomData(random(), b, 1000*RANDOM_MULTIPLIER);
   }
   
   public void testEmptyTerm() throws Exception {
     final CharArraySet dict = makeDictionary("a", "e", "i", "o", "u", "y", "bc", "def");
     Analyzer a = new Analyzer() {
 
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new KeywordTokenizer(reader);
         return new TokenStreamComponents(tokenizer, new DictionaryCompoundWordTokenFilter(TEST_VERSION_CURRENT, tokenizer, dict));
       }
     };
     checkOneTermReuse(a, "", "");
     
     InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
     final HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
     Analyzer b = new Analyzer() {
 
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new KeywordTokenizer(reader);
         TokenFilter filter = new HyphenationCompoundWordTokenFilter(TEST_VERSION_CURRENT, tokenizer, hyphenator);
         return new TokenStreamComponents(tokenizer, filter);
       }
     };
     checkOneTermReuse(b, "", "");
   }
 }
