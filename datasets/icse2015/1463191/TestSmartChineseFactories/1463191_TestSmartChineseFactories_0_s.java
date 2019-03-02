 package org.apache.lucene.analysis.cn.smart;
 
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
 
 import java.io.StringReader;
 
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 
 /** 
  * Tests for {@link SmartChineseSentenceTokenizerFactory} and 
  * {@link SmartChineseWordTokenFilterFactory}
  */
 public class TestSmartChineseFactories extends BaseTokenStreamTestCase {
   /** Test showing the behavior with whitespace */
   public void testSimple() throws Exception {
    String sentence = "æè´­ä¹°äºéå·åæè£ã";
    WhitespaceTokenizer ws = new WhitespaceTokenizer(TEST_VERSION_CURRENT, new StringReader(sentence));
    SmartChineseWordTokenFilterFactory factory = new SmartChineseWordTokenFilterFactory();
    TokenStream ts = factory.create(ws);
     // TODO: fix smart chinese to not emit punctuation tokens
     // at the moment: you have to clean up with WDF, or use the stoplist, etc
    assertTokenStreamContents(ts, 
        new String[] { "æ", "è´­ä¹°", "äº", "éå·", "å", "æè£", "," });
   }
   
   /** Test showing the behavior with whitespace */
   public void testTokenizer() throws Exception {
    String sentence = "æè´­ä¹°äºéå·åæè£ãæè´­ä¹°äºéå·åæè£ã";
    SmartChineseSentenceTokenizerFactory tokenizerFactory = new SmartChineseSentenceTokenizerFactory();
    Tokenizer tokenizer = tokenizerFactory.create(new StringReader(sentence));
    SmartChineseWordTokenFilterFactory factory = new SmartChineseWordTokenFilterFactory();
    TokenStream ts = factory.create(tokenizer);
     // TODO: fix smart chinese to not emit punctuation tokens
     // at the moment: you have to clean up with WDF, or use the stoplist, etc
    assertTokenStreamContents(ts, 
        new String[] { "æ", "è´­ä¹°", "äº", "éå·", "å", "æè£", ",", 
         "æ", "è´­ä¹°", "äº", "éå·", "å", "æè£", ","
         });
   }
 }
