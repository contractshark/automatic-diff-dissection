 package org.apache.lucene.analysis.de;
 
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
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.StringReader;
 
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.KeywordMarkerTokenFilter;
import org.apache.lucene.analysis.LowerCaseTokenizer;
 import org.apache.lucene.util.Version;
 
 /**
  * Test the German stemmer. The stemming algorithm is known to work less 
  * than perfect, as it doesn't use any word lists with exceptions. We 
  * also check some of the cases where the algorithm is wrong.
  *
  */
 public class TestGermanStemFilter extends BaseTokenStreamTestCase {
 
   public void testStemming() throws Exception {
     // read test cases from external file:
     File dataDir = new File(System.getProperty("dataDir", "./bin"));
     File testFile = new File(dataDir, "org/apache/lucene/analysis/de/data.txt");
     FileInputStream fis = new FileInputStream(testFile);
     InputStreamReader isr = new InputStreamReader(fis, "iso-8859-1");
     BufferedReader breader = new BufferedReader(isr);
     while(true) {
       String line = breader.readLine();
       if (line == null)
         break;
       line = line.trim();
       if (line.startsWith("#") || line.equals(""))
         continue;    // ignore comments and empty lines
       String[] parts = line.split(";");
       //System.out.println(parts[0] + " -- " + parts[1]);
      check(parts[0], parts[1]);
     }
     breader.close();
     isr.close();
     fis.close();
   }
  
  public void testReusableTokenStream() throws Exception {
    Analyzer a = new GermanAnalyzer(Version.LUCENE_CURRENT);
    checkReuse(a, "Tisch", "tisch");
    checkReuse(a, "Tische", "tisch");
    checkReuse(a, "Tischen", "tisch");
  }
  
  public void testExclusionTableBWCompat() throws IOException {
    GermanStemFilter filter = new GermanStemFilter(new LowerCaseTokenizer(Version.LUCENE_CURRENT, 
        new StringReader("Fischen Trinken")));
    CharArraySet set = new CharArraySet(Version.LUCENE_CURRENT, 1, true);
    set.add("fischen");
    filter.setExclusionSet(set);
    assertTokenStreamContents(filter, new String[] { "fischen", "trink" });
  }

  public void testWithKeywordAttribute() throws IOException {
    CharArraySet set = new CharArraySet(Version.LUCENE_CURRENT, 1, true);
    set.add("fischen");
    GermanStemFilter filter = new GermanStemFilter(
        new KeywordMarkerTokenFilter(new LowerCaseTokenizer(Version.LUCENE_CURRENT, new StringReader( 
            "Fischen Trinken")), set));
    assertTokenStreamContents(filter, new String[] { "fischen", "trink" });
  }

  public void testWithKeywordAttributeAndExclusionTable() throws IOException {
    CharArraySet set = new CharArraySet(Version.LUCENE_CURRENT, 1, true);
    set.add("fischen");
    CharArraySet set1 = new CharArraySet(Version.LUCENE_CURRENT, 1, true);
    set1.add("trinken");
    set1.add("fischen");
    GermanStemFilter filter = new GermanStemFilter(
        new KeywordMarkerTokenFilter(new LowerCaseTokenizer(Version.LUCENE_CURRENT, new StringReader(
            "Fischen Trinken")), set));
    filter.setExclusionSet(set1);
    assertTokenStreamContents(filter, new String[] { "fischen", "trinken" });
  }
  
  /* 
   * Test that changes to the exclusion table are applied immediately
   * when using reusable token streams.
   */
  public void testExclusionTableReuse() throws Exception {
    GermanAnalyzer a = new GermanAnalyzer(Version.LUCENE_CURRENT);
    checkReuse(a, "tischen", "tisch");
    a.setStemExclusionTable(new String[] { "tischen" });
    checkReuse(a, "tischen", "tischen");
  }
  
  
  private void check(final String input, final String expected) throws Exception {
    checkOneTerm(new GermanAnalyzer(Version.LUCENE_CURRENT), input, expected);
  }
  
  private void checkReuse(Analyzer a, String input, String expected) throws Exception {
    checkOneTermReuse(a, input, expected);
  }
 }
