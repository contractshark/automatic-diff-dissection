 package org.apache.lucene.analysis.hunspell;
 
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
 import java.io.InputStream;
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.util.ResourceLoader;
 import org.apache.lucene.analysis.util.ResourceLoaderAware;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 import org.apache.lucene.util.IOUtils;
 
 /**
 * TokenFilterFactory that creates instances of {@link HunspellStemFilter}.
 * Example config for British English:
  * <pre class="prettyprint">
  * &lt;filter class=&quot;solr.HunspellStemFilterFactory&quot;
  *    dictionary=&quot;en_GB.dic,my_custom.dic&quot;
  *    affix=&quot;en_GB.aff&quot;
 *         ignoreCase=&quot;false&quot;
 *         longestOnly=&quot;false&quot; /&gt;</pre>
  * Both parameters dictionary and affix are mandatory.
  * Dictionaries for many languages are available through the OpenOffice project.
  * 
  * See <a href="http://wiki.apache.org/solr/Hunspell">http://wiki.apache.org/solr/Hunspell</a>
 * @lucene.experimental
  */
 public class HunspellStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
   private static final String PARAM_DICTIONARY = "dictionary";
   private static final String PARAM_AFFIX = "affix";
   private static final String PARAM_RECURSION_CAP = "recursionCap";
  private static final String PARAM_IGNORE_CASE   = "ignoreCase";
  private static final String PARAM_LONGEST_ONLY  = "longestOnly";
 
  private final String dictionaryFiles;
   private final String affixFile;
   private final boolean ignoreCase;
  private final boolean longestOnly;
  private Dictionary dictionary;
   private int recursionCap;
   
   /** Creates a new HunspellStemFilterFactory */
   public HunspellStemFilterFactory(Map<String,String> args) {
     super(args);
    dictionaryFiles = require(args, PARAM_DICTIONARY);
     affixFile = get(args, PARAM_AFFIX);
     ignoreCase = getBoolean(args, PARAM_IGNORE_CASE, false);
     recursionCap = getInt(args, PARAM_RECURSION_CAP, 2);
    longestOnly = getBoolean(args, PARAM_LONGEST_ONLY, false);
    // this isnt necessary: we properly load all dictionaries.
    // but recognize and ignore for back compat
    getBoolean(args, "strictAffixParsing", true);
     if (!args.isEmpty()) {
       throw new IllegalArgumentException("Unknown parameters: " + args);
     }
   }
 
   @Override
   public void inform(ResourceLoader loader) throws IOException {
    String dicts[] = dictionaryFiles.split(",");
 
     InputStream affix = null;
     List<InputStream> dictionaries = new ArrayList<InputStream>();
 
     try {
       dictionaries = new ArrayList<InputStream>();
      for (String file : dicts) {
         dictionaries.add(loader.openResource(file));
       }
       affix = loader.openResource(affixFile);
 
      this.dictionary = new Dictionary(affix, dictionaries, ignoreCase);
     } catch (ParseException e) {
      throw new IOException("Unable to load hunspell data! [dictionary=" + dictionaries + ",affix=" + affixFile + "]", e);
     } finally {
       IOUtils.closeWhileHandlingException(affix);
       IOUtils.closeWhileHandlingException(dictionaries);
     }
   }
 
   @Override
   public TokenStream create(TokenStream tokenStream) {
    return new HunspellStemFilter(tokenStream, dictionary, true, recursionCap, longestOnly);
   }
 }
