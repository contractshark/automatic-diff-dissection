 package org.apache.lucene.search;
 
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
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermRef;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.ToStringUtils;
 
 import java.io.IOException;
import java.io.Serializable;
import java.util.PriorityQueue;
 
 /** Implements the fuzzy search query. The similarity measurement
  * is based on the Levenshtein (edit distance) algorithm.
  * 
 * Warning: this query is not very scalable with its default prefix
  * length of 0 - in this case, *every* term will be enumerated and
  * cause an edit score calculation.
  * 
  */
 public class FuzzyQuery extends MultiTermQuery {
   
  private static class FuzzyRewrite extends RewriteMethod implements Serializable {
    @Override
    public Query rewrite(IndexReader reader, MultiTermQuery query) throws IOException {
      int maxSize = BooleanQuery.getMaxClauseCount();
      PriorityQueue<ScoreTerm> stQueue = new PriorityQueue<ScoreTerm>(1024);
      
      TermsEnum termsEnum = query.getTermsEnum(reader);
      assert termsEnum != null;
      final String field = query.field;
      if (field == null)
        throw new NullPointerException("If you implement getTermsEnum(), you must specify a non-null field in the constructor of MultiTermQuery.");
      final MultiTermQuery.BoostAttribute boostAtt =
        termsEnum.attributes().addAttribute(MultiTermQuery.BoostAttribute.class);
      ScoreTerm bottomSt = null;
      TermRef t;
      final Term placeholderTerm = new Term(field);
      while ((t = termsEnum.next()) != null) {
        if (t == null) break;
        ScoreTerm st = new ScoreTerm(placeholderTerm.createTerm(t.toString()), boostAtt.getBoost());
        if (stQueue.size() < maxSize) {
          // record the current bottom item
          if (bottomSt == null || st.compareTo(bottomSt) > 0) {
            bottomSt = st;
          }
          // add to PQ, as it is not yet filled up
          stQueue.offer(st);
        } else {
          assert bottomSt != null;
          // only add to PQ, if the ScoreTerm is greater than the current bottom,
          // as all entries will be enqueued after the current bottom and will never be visible
          if (st.compareTo(bottomSt) < 0) {
            stQueue.offer(st);
          }
        }
        //System.out.println("current: "+st.term+"("+st.score+"), bottom: "+bottomSt.term+"("+bottomSt.score+")");
      }
      
      BooleanQuery bq = new BooleanQuery(true);
      int size = Math.min(stQueue.size(), maxSize);
      for(int i = 0; i < size; i++){
        ScoreTerm st = stQueue.poll();
        TermQuery tq = new TermQuery(st.term);      // found a match
        tq.setBoost(query.getBoost() * st.score); // set the boost
        bq.add(tq, BooleanClause.Occur.SHOULD);          // add to query
      }
      query.incTotalNumberOfTerms(bq.clauses().size());
      return bq;
    }

    // Make sure we are still a singleton even after deserializing
    protected Object readResolve() {
      return FUZZY_REWRITE;
    }
  }
  
  private final static RewriteMethod FUZZY_REWRITE = new FuzzyRewrite();

   public final static float defaultMinSimilarity = 0.5f;
   public final static int defaultPrefixLength = 0;
   
   private float minimumSimilarity;
   private int prefixLength;
   private boolean termLongEnough = false;
   
   protected Term term;
   
   /**
    * Create a new FuzzyQuery that will match terms with a similarity 
    * of at least <code>minimumSimilarity</code> to <code>term</code>.
    * If a <code>prefixLength</code> &gt; 0 is specified, a common prefix
    * of that length is also required.
    * 
    * @param term the term to search for
    * @param minimumSimilarity a value between 0 and 1 to set the required similarity
    *  between the query term and the matching terms. For example, for a
    *  <code>minimumSimilarity</code> of <code>0.5</code> a term of the same length
    *  as the query term is considered similar to the query term if the edit distance
    *  between both terms is less than <code>length(term)*0.5</code>
    * @param prefixLength length of common (non-fuzzy) prefix
    * @throws IllegalArgumentException if minimumSimilarity is &gt;= 1 or &lt; 0
    * or if prefixLength &lt; 0
    */
   public FuzzyQuery(Term term, float minimumSimilarity, int prefixLength) throws IllegalArgumentException {
     super(term.field());
     this.term = term;
     
     if (minimumSimilarity >= 1.0f)
       throw new IllegalArgumentException("minimumSimilarity >= 1");
     else if (minimumSimilarity < 0.0f)
       throw new IllegalArgumentException("minimumSimilarity < 0");
     if (prefixLength < 0)
       throw new IllegalArgumentException("prefixLength < 0");
     
     if (term.text().length() > 1.0f / (1.0f - minimumSimilarity)) {
       this.termLongEnough = true;
     }
     
     this.minimumSimilarity = minimumSimilarity;
     this.prefixLength = prefixLength;
    rewriteMethod = FUZZY_REWRITE;
   }
   
   /**
    * Calls {@link #FuzzyQuery(Term, float) FuzzyQuery(term, minimumSimilarity, 0)}.
    */
   public FuzzyQuery(Term term, float minimumSimilarity) throws IllegalArgumentException {
     this(term, minimumSimilarity, defaultPrefixLength);
   }
 
   /**
    * Calls {@link #FuzzyQuery(Term, float) FuzzyQuery(term, 0.5f, 0)}.
    */
   public FuzzyQuery(Term term) {
     this(term, defaultMinSimilarity, defaultPrefixLength);
   }
   
   /**
    * Returns the minimum similarity that is required for this query to match.
    * @return float value between 0.0 and 1.0
    */
   public float getMinSimilarity() {
     return minimumSimilarity;
   }
     
   /**
    * Returns the non-fuzzy prefix length. This is the number of characters at the start
    * of a term that must be identical (not fuzzy) to the query term if the query
    * is to match that term. 
    */
   public int getPrefixLength() {
     return prefixLength;
   }
 
   @Override @Deprecated
   protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
     if (!termLongEnough) {  // can only match if it's exact
       return new SingleTermEnum(reader, term);
     }
     return new FuzzyTermEnum(reader, getTerm(), minimumSimilarity, prefixLength);
   }
   
   @Override
   protected TermsEnum getTermsEnum(IndexReader reader) throws IOException {
     if (!termLongEnough) {  // can only match if it's exact
       return new SingleTermsEnum(reader, term);
     }
     return new FuzzyTermsEnum(reader, getTerm(), minimumSimilarity, prefixLength);
   }
   
   /**
    * Returns the pattern term.
    */
   public Term getTerm() {
     return term;
   }
 
  @Override
  public void setRewriteMethod(RewriteMethod method) {
    throw new UnsupportedOperationException("FuzzyQuery cannot change rewrite method");
  }
  
   protected static class ScoreTerm implements Comparable<ScoreTerm> {
     public Term term;
     public float score;
     
     public ScoreTerm(Term term, float score){
       this.term = term;
       this.score = score;
     }
     
     public int compareTo(ScoreTerm other) {
       if (this.score == other.score)
         return this.term.compareTo(other.term);
       else
         // inverse ordering!!!
         return Float.compare(other.score, this.score);
     }
   }
     
   @Override
   public String toString(String field) {
     final StringBuilder buffer = new StringBuilder();
     if (!term.field().equals(field)) {
         buffer.append(term.field());
         buffer.append(":");
     }
     buffer.append(term.text());
     buffer.append('~');
     buffer.append(Float.toString(minimumSimilarity));
     buffer.append(ToStringUtils.boost(getBoost()));
     return buffer.toString();
   }
   
   @Override
   public int hashCode() {
     final int prime = 31;
     int result = super.hashCode();
     result = prime * result + Float.floatToIntBits(minimumSimilarity);
     result = prime * result + prefixLength;
     result = prime * result + ((term == null) ? 0 : term.hashCode());
     return result;
   }
 
   @Override
   public boolean equals(Object obj) {
     if (this == obj)
       return true;
     if (!super.equals(obj))
       return false;
     if (getClass() != obj.getClass())
       return false;
     FuzzyQuery other = (FuzzyQuery) obj;
     if (Float.floatToIntBits(minimumSimilarity) != Float
         .floatToIntBits(other.minimumSimilarity))
       return false;
     if (prefixLength != other.prefixLength)
       return false;
     if (term == null) {
       if (other.term != null)
         return false;
     } else if (!term.equals(other.term))
       return false;
     return true;
   }
 
 
 }
