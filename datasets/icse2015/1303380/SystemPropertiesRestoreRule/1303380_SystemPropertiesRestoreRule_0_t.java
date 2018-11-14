 package org.apache.lucene.util;
 
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

 import java.util.*;
 
 import org.junit.rules.TestRule;
 import org.junit.runner.Description;
 import org.junit.runners.model.Statement;
 
 /**
  * Restore system properties from before the nested {@link Statement}.
  */
 public class SystemPropertiesRestoreRule implements TestRule {
   public Statement apply(final Statement s, Description d) {
     return new Statement() {
       public void evaluate() throws Throwable {
         TreeMap<String,String> before = cloneAsMap(System.getProperties());
         try {
           s.evaluate();
         } finally {
           TreeMap<String,String> after = cloneAsMap(System.getProperties());
           if (!after.equals(before)) {
             // Restore original properties.
             restore(before, after);
           }
         }
       }
     };
   }
   
   static TreeMap<String,String> cloneAsMap(Properties properties) {
     TreeMap<String,String> result = new TreeMap<String,String>();
     for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
       final Object key = e.nextElement();
       // Skip non-string properties or values, they're abuse of Properties object.
       if (key instanceof String) {
         String value = properties.getProperty((String) key);
         if (value == null) {
           Object ovalue = properties.get(key);
           if (ovalue != null) {
             // ovalue has to be a non-string object. Skip the property because
             // System.clearProperty won't be able to cast back the existing value.
             continue;
           }
         }
         result.put((String) key, value);
       }
     }
     return result;
   }
 
   static void restore(
       TreeMap<String,String> before,
       TreeMap<String,String> after) {
     after.keySet().removeAll(before.keySet());
     for (String key : after.keySet()) {
       System.clearProperty(key);
     }
     for (Map.Entry<String,String> e : before.entrySet()) {
       if (e.getValue() == null) {
         System.clearProperty(e.getKey()); // Can this happen?
       } else {
         System.setProperty(e.getKey(), e.getValue());
       }
     }
   }  
 }