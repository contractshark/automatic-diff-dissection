 /*
 
    Derby - Class org.apache.derby.impl.jdbc.EmbedPreparedStatement20
 
   Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
  */
 
 package org.apache.derby.impl.jdbc;
 
 import org.apache.derby.impl.jdbc.EmbedConnection;
 import org.apache.derby.impl.jdbc.Util;
 
 import org.apache.derby.iapi.sql.ResultSet;
 import org.apache.derby.iapi.error.StandardException;
 
 import org.apache.derby.iapi.reference.SQLState;
 
 import java.io.InputStream;
 
 import java.math.BigDecimal;
 
 import java.sql.SQLException;
 import java.sql.SQLWarning;
 import java.sql.Date;
 import java.sql.Time;
 import java.sql.Timestamp;
 
 
 /* ---- New jdbc 2.0 types ----- */
 import java.sql.Array;
 import java.sql.Blob;
 import java.sql.Clob;
 import java.sql.Ref;
 import java.sql.Types;
 
 /**
  * This class extends the EmbedPreparedStatement class in order to support new
  * methods and classes that come with JDBC 2.0.
   <P><B>Supports</B>
    <UL>
    <LI> JDBC 2.0
    </UL>
  *	@see org.apache.derby.impl.jdbc.EmbedPreparedStatement
  *
  *	@author francois
  */
 public class EmbedPreparedStatement20
 	extends org.apache.derby.impl.jdbc.EmbedPreparedStatement {
 
 	//////////////////////////////////////////////////////////////
 	//
 	// CONSTRUCTORS
 	//
 	//////////////////////////////////////////////////////////////
 	/*
 		Constructor assumes caller will setup context stack
 		and restore it.
 	    @exception SQLException on error
 	 */
 	public EmbedPreparedStatement20 (EmbedConnection conn, String sql, boolean forMetaData,
 									  int resultSetType,
 									  int resultSetConcurrency,
 									  int resultSetHoldability,
 									  int autoGeneratedKeys,
 									  int[] columnIndexes,
 									  String[] columnNames)
 		throws SQLException {
 
 		super(conn, sql, forMetaData, resultSetType, resultSetConcurrency, resultSetHoldability,
 		autoGeneratedKeys, columnIndexes, columnNames);
 	}
 
     /**
      * JDBC 2.0
      *
      * Set a REF(&lt;structured-type&gt;) parameter.
      *
      * @param i the first parameter is 1, the second is 2, ...
      * @param x an object representing data of an SQL REF Type
      * @exception SQLException Feature not implemented for now.
      */
     public void setRef (int i, Ref x) throws SQLException {
 		throw Util.notImplemented();
 	}
 
 
 
 
     /**
      * JDBC 2.0
      *
      * Set an Array parameter.
      *
      * @param i the first parameter is 1, the second is 2, ...
      * @param x an object representing an SQL array
      * @exception SQLException Feature not implemented for now.
      */
     public void setArray (int i, Array x) throws SQLException {
 		throw Util.notImplemented();
 	}
 
 	/*
 	** Methods using BigDecimal, moved out of EmbedPreparedStatement
 	** to allow that class to support JSR169.
 	*/
 	/**
      * Set a parameter to a java.lang.BigDecimal value.  
      * The driver converts this to a SQL NUMERIC value when
      * it sends it to the database.
      *
      * @param parameterIndex the first parameter is 1, the second is 2, ...
      * @param x the parameter value
 	 * @exception SQLException thrown on failure.
      */
     public final void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
 		checkStatus();
 		try {
 			/* JDBC is one-based, DBMS is zero-based */
 			getParms().getParameterForSet(parameterIndex - 1).setBigDecimal(x);
 
 		} catch (Throwable t) {
 			throw EmbedResultSet.noStateChangeException(t);
 		}
 	}
 
 	/**
 		Allow explict setObject conversions by sub-classes for classes
 		not supported by this variant. In this case handle BigDecimal.
 		@return true if the object was set successfully, false if no valid
 		conversion exists.
 
 		@exception SQLException value could not be set.
 	*/
 	boolean setObjectConvert(int parameterIndex, Object x) throws SQLException
 	{
 		if (x instanceof BigDecimal) {
 			setBigDecimal(parameterIndex, (BigDecimal) x);
 			return true;
 		}
 		return false;
 	}
 }
 