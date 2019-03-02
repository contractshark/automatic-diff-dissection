 package org.apache.lucene.analysis.br;
 
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
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.analysis.core.LowerCaseTokenizer;
 import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
 import org.apache.lucene.analysis.util.CharArraySet;
 
 /**
  * Test the Brazilian Stem Filter, which only modifies the term text.
  * 
  * It is very similar to the snowball portuguese algorithm but not exactly the same.
  *
  */
 public class TestBrazilianStemmer extends BaseTokenStreamTestCase {
   
   public void testWithSnowballExamples() throws Exception {
 	 check("boa", "boa");
 	 check("boainain", "boainain");
 	 check("boas", "boas");
 	 check("bÃ´as", "boas"); // removes diacritic: different from snowball portugese
 	 check("boassu", "boassu");
 	 check("boataria", "boat");
 	 check("boate", "boat");
 	 check("boates", "boat");
 	 check("boatos", "boat");
 	 check("bob", "bob");
 	 check("boba", "bob");
 	 check("bobagem", "bobag");
 	 check("bobagens", "bobagens");
 	 check("bobalhÃµes", "bobalho"); // removes diacritic: different from snowball portugese
 	 check("bobear", "bob");
 	 check("bobeira", "bobeir");
 	 check("bobinho", "bobinh");
 	 check("bobinhos", "bobinh");
 	 check("bobo", "bob");
 	 check("bobs", "bobs");
 	 check("boca", "boc");
 	 check("bocadas", "boc");
 	 check("bocadinho", "bocadinh");
 	 check("bocado", "boc");
 	 check("bocaiÃºva", "bocaiuv"); // removes diacritic: different from snowball portuguese
 	 check("boÃ§al", "bocal"); // removes diacritic: different from snowball portuguese
 	 check("bocarra", "bocarr");
 	 check("bocas", "boc");
 	 check("bode", "bod");
 	 check("bodoque", "bodoqu");
 	 check("body", "body");
 	 check("boeing", "boeing");
 	 check("boem", "boem");
 	 check("boemia", "boem");
 	 check("boÃªmio", "boemi"); // removes diacritic: different from snowball portuguese
 	 check("bogotÃ¡", "bogot");
 	 check("boi", "boi");
 	 check("bÃ³ia", "boi"); // removes diacritic: different from snowball portuguese
 	 check("boiando", "boi");
 	 check("quiabo", "quiab");
 	 check("quicaram", "quic");
 	 check("quickly", "quickly");
 	 check("quieto", "quiet");
 	 check("quietos", "quiet");
 	 check("quilate", "quilat");
 	 check("quilates", "quilat");
 	 check("quilinhos", "quilinh");
 	 check("quilo", "quil");
 	 check("quilombo", "quilomb");
 	 check("quilomÃ©tricas", "quilometr"); // removes diacritic: different from snowball portuguese
 	 check("quilomÃ©tricos", "quilometr"); // removes diacritic: different from snowball portuguese
 	 check("quilÃ´metro", "quilometr"); // removes diacritic: different from snowball portoguese
 	 check("quilÃ´metros", "quilometr"); // removes diacritic: different from snowball portoguese
 	 check("quilos", "quil");
 	 check("quimica", "quimic");
 	 check("quilos", "quil");
 	 check("quimica", "quimic");
 	 check("quimicas", "quimic");
 	 check("quimico", "quimic");
 	 check("quimicos", "quimic");
 	 check("quimioterapia", "quimioterap");
 	 check("quimioterÃ¡picos", "quimioterap"); // removes diacritic: different from snowball portoguese
 	 check("quimono", "quimon");
 	 check("quincas", "quinc");
 	 check("quinhÃ£o", "quinha"); // removes diacritic: different from snowball portoguese
 	 check("quinhentos", "quinhent");
 	 check("quinn", "quinn");
 	 check("quino", "quin");
 	 check("quinta", "quint");
 	 check("quintal", "quintal");
 	 check("quintana", "quintan");
 	 check("quintanilha", "quintanilh");
 	 check("quintÃ£o", "quinta"); // removes diacritic: different from snowball portoguese
 	 check("quintessÃªncia", "quintessente"); // versus snowball portuguese 'quintessent'
 	 check("quintino", "quintin");
 	 check("quinto", "quint");
 	 check("quintos", "quint");
 	 check("quintuplicou", "quintuplic");
 	 check("quinze", "quinz");
 	 check("quinzena", "quinzen");
 	 check("quiosque", "quiosqu");
   }
   
   public void testNormalization() throws Exception {
     check("Brasil", "brasil"); // lowercase by default
     check("BrasÃ­lia", "brasil"); // remove diacritics
     check("quimio5terÃ¡picos", "quimio5terapicos"); // contains non-letter, diacritic will still be removed
     check("Ã¡Ã¡", "Ã¡Ã¡"); // token is too short: diacritics are not removed
     check("Ã¡Ã¡Ã¡", "aaa"); // normally, diacritics are removed
   }
   
   public void testReusableTokenStream() throws Exception {
     Analyzer a = new BrazilianAnalyzer(TEST_VERSION_CURRENT);
     checkReuse(a, "boa", "boa");
     checkReuse(a, "boainain", "boainain");
     checkReuse(a, "boas", "boas");
     checkReuse(a, "bÃ´as", "boas"); // removes diacritic: different from snowball portugese
   }
  
   public void testStemExclusionTable() throws Exception {
     BrazilianAnalyzer a = new BrazilianAnalyzer(TEST_VERSION_CURRENT, 
         CharArraySet.EMPTY_SET, new CharArraySet(TEST_VERSION_CURRENT, asSet("quintessÃªncia"), false));
     checkReuse(a, "quintessÃªncia", "quintessÃªncia"); // excluded words will be completely unchanged.
   }
   
   public void testWithKeywordAttribute() throws IOException {
     CharArraySet set = new CharArraySet(TEST_VERSION_CURRENT, 1, true);
     set.add("BrasÃ­lia");
     BrazilianStemFilter filter = new BrazilianStemFilter(
         new KeywordMarkerFilter(new LowerCaseTokenizer(TEST_VERSION_CURRENT, new StringReader(
             "BrasÃ­lia Brasilia")), set));
     assertTokenStreamContents(filter, new String[] { "brasÃ­lia", "brasil" });
   }
 
   private void check(final String input, final String expected) throws Exception {
     checkOneTerm(new BrazilianAnalyzer(TEST_VERSION_CURRENT), input, expected);
   }
   
   private void checkReuse(Analyzer a, String input, String expected) throws Exception {
     checkOneTermReuse(a, input, expected);
   }
 
   /** blast some random strings through the analyzer */
   public void testRandomStrings() throws Exception {
    checkRandomData(random(), new BrazilianAnalyzer(TEST_VERSION_CURRENT), 10000*RANDOM_MULTIPLIER);
   }
   
   public void testEmptyTerm() throws IOException {
     Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new KeywordTokenizer(reader);
         return new TokenStreamComponents(tokenizer, new BrazilianStemFilter(tokenizer));
       }
     };
     checkOneTermReuse(a, "", "");
   }
 }
