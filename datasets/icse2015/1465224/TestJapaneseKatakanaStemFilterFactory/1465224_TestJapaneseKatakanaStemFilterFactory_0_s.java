 package org.apache.lucene.analysis.ja;
 
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
 
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.TokenStream;
 
 import java.io.IOException;
 import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
 
 /**
  * Simple tests for {@link JapaneseKatakanaStemFilterFactory}
  */
 public class TestJapaneseKatakanaStemFilterFactory extends BaseTokenStreamTestCase {
   public void testKatakanaStemming() throws IOException {
    JapaneseTokenizerFactory tokenizerFactory = new JapaneseTokenizerFactory();
    Map<String, String> tokenizerArgs = Collections.emptyMap();
    tokenizerFactory.init(tokenizerArgs);
     tokenizerFactory.inform(new StringMockResourceLoader(""));
     TokenStream tokenStream = tokenizerFactory.create(
         new StringReader("æå¾æ¥ãã¼ãã£ã¼ã«è¡ãäºå®ããããå³æ¸é¤¨ã§è³æãã³ãã¼ãã¾ããã")
     );
    JapaneseKatakanaStemFilterFactory filterFactory = new JapaneseKatakanaStemFilterFactory();
    Map<String, String> filterArgs = Collections.emptyMap();
    filterFactory.init(filterArgs);
     assertTokenStreamContents(filterFactory.create(tokenStream),
         new String[]{ "æå¾æ¥", "ãã¼ãã£", "ã«", "è¡ã", "äºå®", "ã", "ãã",   // ãã¼ãã£ã¼ should be stemmed
                       "å³æ¸é¤¨", "ã§", "è³æ", "ã", "ã³ãã¼", "ã", "ã¾ã", "ã"} // ã³ãã¼ should not be stemmed
     );
   }
 }