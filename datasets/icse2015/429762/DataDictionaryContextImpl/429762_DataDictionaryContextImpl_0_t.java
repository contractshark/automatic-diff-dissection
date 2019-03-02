 /*
 
    Derby - Class org.apache.derby.impl.sql.catalog.DataDictionaryContextImpl
 
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
  */
 
 package org.apache.derby.impl.sql.catalog;
 
 import org.apache.derby.iapi.services.context.ContextImpl;
 import org.apache.derby.iapi.services.context.ContextManager;
 import org.apache.derby.iapi.error.StandardException;
 import org.apache.derby.iapi.sql.dictionary.DataDictionary;
 import org.apache.derby.iapi.sql.dictionary.DataDictionaryContext;
 import org.apache.derby.iapi.error.ExceptionSeverity;
 /*
  * DataDictionaryContextImpl
  *
  * This class supports both nested and outer data dictionaries. The
  * outer data dictionary defines the name space for a database.
  * A nested data dictionary defines a name space for a publication
  * (or schema in the future).
  *
  * During error processing, a nested data dictionary context is popped
  * for statement errors or higher. The outer data dictionary context
  * is not popped by cleanupOnError.
  */
 class DataDictionaryContextImpl 
 	extends ContextImpl
 	implements DataDictionaryContext
 {
 	//
 	// DataDictionaryContext interface
 	//
 	// we might want these to refuse to return
 	// anything if they are in-use -- would require
 	// the interface provide a 'done' call, and
 	// we would mark them in-use whenever a get happened.
 	public DataDictionary getDataDictionary()
 	{
 		return dataDictionary;
 	}
 
 	public void cleanupOnError(Throwable error)
 	{
 		if (error instanceof StandardException)
 		{
 			StandardException se = (StandardException)error;
 		 	if (se.getSeverity() < ExceptionSeverity.SESSION_SEVERITY)
 				return;
 		}
 		popMe();
 	}
 
 	//
 	// class interface
 	//
 	// this constructor is called with the data dictionary
 	// to be saved when the context
 	// is created
 	
 	DataDictionaryContextImpl(ContextManager cm, DataDictionary dataDictionary)
 	{
 		super(cm, DataDictionaryContext.CONTEXT_ID);
 
 		this.dataDictionary = dataDictionary;
 	}
 
 	final DataDictionary			dataDictionary;
 }
