 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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
 
 
 options {
 	STATIC = false;
 	LOOKAHEAD = 2;
 	DEBUG_PARSER = false;
 	DEBUG_TOKEN_MANAGER = false;
 	ERROR_REPORTING = true;
 	USER_TOKEN_MANAGER = false;
 	USER_CHAR_STREAM = true;
 	JAVA_UNICODE_ESCAPE = false;
 	UNICODE_INPUT = true;
 	IGNORE_CASE = true;
 	CACHE_TOKENS = true;
 }
 
 PARSER_BEGIN(ij)
 
 package org.apache.derby.impl.tools.ij;
 
 import org.apache.derby.iapi.reference.JDBC20Translation;
 import org.apache.derby.iapi.reference.JDBC30Translation;
 
 import org.apache.derby.tools.JDBCDisplayUtil;
 
 
 import org.apache.derby.iapi.tools.i18n.LocalizedInput;
 import org.apache.derby.iapi.tools.i18n.LocalizedResource;
 
 import org.apache.derby.iapi.services.info.JVMInfo;
 import org.apache.derby.tools.URLCheck;
 
 import java.lang.reflect.*;
 import java.sql.Connection;
 import java.sql.DatabaseMetaData;
 import java.sql.DriverManager;
 import java.sql.Statement;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.ResultSetMetaData;
 import java.sql.SQLException;
 import java.sql.SQLWarning;
 import java.util.Hashtable;
 import java.util.Properties;
 import java.util.StringTokenizer;
 import java.io.IOException;
 import java.util.Vector;
 import java.util.Enumeration;
 import java.util.Locale;
 import java.util.List;
 import java.util.ArrayList;
 
 
 /**
 	This parser works on a statement-at-a-time basis.
 	It maintains a connection environment that is
 	set by the caller and contains a list of
 	connections for the current thread/ij session.
 	Multi-user frameworks that use this parser
 	tend to maintain multiple connectionEnv's and
 	pass in the current one to set ij up.
 	A connectionEnv has a default connection in use,
 	and the ij connect/set connection/disconnect commands 
 	are used to change the current connection.
 
 	Each connection has associated with it a list
 	of prepared statements and cursors, created by
 	the ij prepare and get cursor statements and
 	manipulated by additional ij statements.
 
 	To enable multiple display modes, this parser will
 	not output anything, but will return
 	objects that the caller can then display.
 
 	This means the caller is responsible for displaying
 	thrown exceptions and also SQLWarnings. So, our
 	return value is the JDBC object upon which warnings
 	will be hung, i.e. the one manipulated by the statement,
 	if any.
 
 	If there is no object to display, then a null is
 	returned.
 
 	@author ames
  */
 class ij {
 	static final String PROTOCOL_PROPERTY = "ij.protocol";
     static final String USER_PROPERTY = "ij.user";
     static final String PASSWORD_PROPERTY = "ij.password";
 	static final String FRAMEWORK_PROPERTY = "framework";
 
 	boolean			elapsedTime = false;
 
 	Connection theConnection = null;
 	ConnectionEnv currentConnEnv = null;
 
 	xaAbstractHelper xahelper = null;
 	boolean exit = false;
 
 	utilMain utilInstance = null;
 	Hashtable ignoreErrors = null;
 	String protocol = null;		// the (single) unnamed protocol
 	Hashtable namedProtocols;
 
 
 
 	/**
 	 * A constructor that understands the local state that needs to be
 	 * initialized.
 	 *
 	 * @param tm			The token manager to use
 	 * @param utilInstance	The util to use
 	 */
 	ij(ijTokenManager tm, utilMain utilInstance) {
 		this(tm);
 		this.utilInstance = utilInstance;
 	}
 	
 	/**
 	   Initialize this parser from the environment
 	   (system properties). Used when ij is being run
 	   as a command line program.
 	*/
 	void initFromEnvironment() {
 
 		// load all protocols specified via properties
 		//
 		Properties p = System.getProperties();
 		protocol = p.getProperty(PROTOCOL_PROPERTY);
 		String framework_property = p.getProperty(FRAMEWORK_PROPERTY);
 		
 		if (ij.JDBC20X() && ij.JTA() && ij.JNDI())
 		{
 			try {
 				xahelper = (xaAbstractHelper) Class.forName("org.apache.derby.impl.tools.ij.xaHelper").newInstance();
 				xahelper.setFramework(framework_property);
 			} catch (Exception e) {
 			}
 
 		}
 
 
 		namedProtocols = new Hashtable();
 		String prefix = PROTOCOL_PROPERTY + ".";
 		for (Enumeration e = p.propertyNames(); e.hasMoreElements(); )
 		{
 			String key = (String)e.nextElement();
 			if (key.startsWith(prefix)) {
 				String name = key.substring(prefix.length());
 				installProtocol(name.toUpperCase(Locale.ENGLISH), p.getProperty(key));
 			}
 		}
 	}
 	/**
 	 * Return whether or not JDBC 2.0 (and greater) extension classes can be loaded
 	 *
 	 * @return true if JDBC 2.0 (and greater) extension classes can be loaded
 	 */
 	private static boolean JDBC20X()
 	{
 		try
 		{
 			Class.forName("javax.sql.DataSource");
 			Class.forName("javax.sql.ConnectionPoolDataSource");
 			Class.forName("javax.sql.PooledConnection");
 			Class.forName("javax.sql.XAConnection");
 			Class.forName("javax.sql.XADataSource");
 		}
 		catch(ClassNotFoundException cnfe)
 		{
 			return false;
 		}
 		return true;
 	}
 	/**
 	 * Return whether or not JTA classes can be loaded
 	 *
 	 * @return true if JTA classes can be loaded
 	 */
 	private static boolean JTA()
 	{
 		try
 		{
 			Class.forName("javax.transaction.xa.Xid");
 			Class.forName("javax.transaction.xa.XAResource");
 			Class.forName("javax.transaction.xa.XAException");
 		}
 		catch(ClassNotFoundException cnfe)
 		{
 			return false;
 		}
 		return true;
 	}
 
 	/**
 	 * Return whether or not JNDI extension classes can be loaded
 	 *
 	 * @return true if JNDI extension classes can be loaded
 	 */
 	private static boolean JNDI()
 	{
 		try
 		{
 			Class.forName("javax.naming.spi.Resolver");
 			Class.forName("javax.naming.Referenceable");
 			Class.forName("javax.naming.directory.Attribute");
 		}
 		catch(ClassNotFoundException cnfe)
 		{
 			return false;
 		}
 		return true;
 	}
 // FIXME: caller has to deal with ignoreErrors and handleSQLException behavior
 
 	/**
 		Add the warnings of wTail to the end of those of wHead.
 	 */
 	SQLWarning appendWarnings(SQLWarning wHead, SQLWarning wTail) {
 		if (wHead == null) return wTail;
 
 		if (wHead.getNextException() == null) {
 			wHead.setNextException(wTail);
 		} else {
 			appendWarnings(wHead.getNextWarning(), wTail);
 		}
 		return wHead;
 	}
 
 	/**
 	 * Get the "elapsedTime state".
 	 */
 	boolean getElapsedTimeState()
 	{
 		return elapsedTime;
 	}
 
 	/**
 	   this removes the outside quotes from the string.
 	   it will also swizzle the special characters
 	   into their actual characters, like '' for ', etc.
 	 */
 	String stringValue(String s) {
 		String result = s.substring(1,s.length()-1);
 		char quotes = '\'';
 		int		index;
 	
 		/* Find the first occurrence of adjacent quotes. */
 		index = result.indexOf(quotes);
 
 		/* Replace each occurrence with a single quote and begin the
 		 * search for the next occurrence from where we left off.
 		 */
 		while (index != -1)
 		{
 			result = result.substring(0, index + 1) + result.substring(index + 2);
 
 			index = result.indexOf(quotes, index + 1);
 		}
 
 		return result;
 	}
 
 	void installProtocol(String name, String value) {
 	    try {
 			// `value' is a JDBC protocol;
 			// we load the "driver" in the prototypical
 			// manner, it will register itself with
 			// the DriverManager.
 			util.loadDriverIfKnown(value);
 	    } catch (ClassNotFoundException e) {
 			throw ijException.classNotFoundForProtocol(value);
 	    } catch (IllegalArgumentException e) {
 			throw ijException.classNotFoundForProtocol(value);
 	    } catch (IllegalAccessException e) {
 			throw ijException.classNotFoundForProtocol(value);
 	    } catch (InstantiationException e) {
 			throw ijException.classNotFoundForProtocol(value);
 	    }
 		if (name == null)
 			protocol = value;
 		else
 			namedProtocols.put(name, value);
 	}
 	
 	void haveConnection() {
 		JDBCDisplayUtil.checkNotNull(theConnection, "connection");
 	}
 
 	/**
 		We do not reuse statement objects at all, because
 		some systems require you to close the object to release
 		resources (JBMS), while others will not let you reuse
 		the statement object once it is closed (WebLogic).
 
 		If you want to reuse statement objects, you need to
 		use the ij PREPARE and EXECUTE statements.
 
 		@param stmt the statement
 
 	 **/
 	ijResult executeImmediate(String stmt) throws SQLException {
 		Statement aStatement = null;
 		try {
 			long	beginTime = 0;
 			long	endTime = 0;
 			boolean cleanUpStmt = false;
 
 			haveConnection();
 			aStatement = theConnection.createStatement();
 
 			// for JCC - remove comments at the beginning of the statement
 			// and trim; do the same for Derby Clients that have versions
 			// earlier than 10.2.
 			if (currentConnEnv != null) {
 				boolean trimForDNC = currentConnEnv.getSession().getIsDNC();
 				if (trimForDNC) {
 				// we're using the Derby Client, but we only want to trim
 				// if the version is earlier than 10.2.
 					DatabaseMetaData dbmd = theConnection.getMetaData();
 					int majorVersion = dbmd.getDriverMajorVersion();
 					if ((majorVersion > 10) || ((majorVersion == 10) &&
 						(dbmd.getDriverMinorVersion() > 1)))
 					{ // 10.2 or later, so don't trim/remove comments.
 						trimForDNC = false;
 					}
 				}
 				if (currentConnEnv.getSession().getIsJCC() || trimForDNC) {
 				// remove comments and trim.
 					int nextline;
 					while(stmt.startsWith("--"))
 					{
 						nextline = stmt.indexOf('\n')+1;
 						stmt = stmt.substring(nextline);
 					}
 					stmt = stmt.trim();
 				}
 			}
 
 			aStatement.execute(stmt);
 
 			// FIXME: display results. return start time.
 			return new ijStatementResult(aStatement,true);
 
 		} catch (SQLException e) {
 			if (aStatement!=null)  // free the resource
 				aStatement.close();
 			throw e;
 		}
 	}
 
 	ijResult quit() throws SQLException {
 		exit = true;
 		if (getExpect()) { // report stats
 			// FIXME: replace with MVC...
 			// FIXME: this is a kludgy way to quiet /0 and make 0/0=1...
 			int numExpectOr1 = (numExpect==0?1:numExpect);
 			int numPassOr1 = (numPass==numExpect && numPass==0)?1:numPass;
 			int numFailOr1 = (numFail==numExpect && numFail==0)?1:numFail;
 			int numUnxOr1 = (numUnx==numExpect && numUnx==0)?1:numUnx;
 
             LocalizedResource.OutputWriter().println(LocalizedResource.getMessage("IJ_TestsRun0Pass12Fail34",
             new Object[]{
 			LocalizedResource.getNumber(numExpect), LocalizedResource.getNumber(100*(numPassOr1/numExpectOr1)),
 			LocalizedResource.getNumber(100*(numFailOr1/numExpectOr1))}));
                         if (numUnx > 0) {
 							LocalizedResource.OutputWriter().println();
 							LocalizedResource.OutputWriter().println(LocalizedResource.getMessage("IJ_UnexpResulUnx01",
 							LocalizedResource.getNumber(numUnx), LocalizedResource.getNumber(100*(numUnxOr1/numExpectOr1))));							
 			}
 		}
 		currentConnEnv.removeAllSessions();
 		theConnection = null;
 		return null;
 	}
 
 	/**
 		Async execution wants to return results off-cycle.
 		We want to control their output, and so will hold it
 		up until it is requested with a WAIT FOR asyncName
 		statement.  WAIT FOR will return the results of
 		the async statement once they are ready.  Note that using
 		a select only waits for the execute to complete; the
 		logic to step through the result set is in the caller.
 	 **/
 	ijResult executeAsync(String stmt, String name) {
 		AsyncStatement as = new AsyncStatement(theConnection, stmt);
 
 		currentConnEnv.getSession().addAsyncStatement(name,as);
 
 		as.start();
 
 		return null;
 	}
 
 
     
 	void setConnection(ConnectionEnv connEnv, boolean multipleEnvironments) {
 		Connection conn = connEnv.getConnection();
 
 		if (connEnv != currentConnEnv) // single connenv is common case
 			currentConnEnv = connEnv;
 
 		if (theConnection == conn) return; // not changed.
 
 		if ((theConnection == null) || multipleEnvironments) {
 			// must have switched env's (could check)
 			theConnection = conn;
 		} else {
 			throw ijException.needToDisconnect();
 		}
 	}
 
 	/**
 		Note the Expect Result in the output and in the stats.
 
 		FIXME
 	 */
 	int numExpect, numPass, numFail, numUnx;
 	private void noteExpect(boolean actual, boolean want) {
 		numExpect++;
 		if (actual) numPass++;
 		else numFail++;
 
                 LocalizedResource.OutputWriter().print(LocalizedResource.getMessage(actual?"IJ_Pass":"IJ_Fail"));
 		if (actual != want) {
 			numUnx++;
 					LocalizedResource.OutputWriter().println(LocalizedResource.getMessage("IJ_Unx"));
 		}
                 else LocalizedResource.OutputWriter().println();
 	}
 
 	private boolean getExpect() {
 		return Boolean.getBoolean("ij.expect");
 	}
 
 	private	ijResult	addSession
 	(
 		Connection	newConnection,
 		String		name
 	)
 		throws SQLException
 	{
 		if (currentConnEnv.haveSession(name)) {
 			throw ijException.alreadyHaveConnectionNamed(name);
 		}
 
 		currentConnEnv.addSession( newConnection, name );
 		return new ijConnectionResult( newConnection );
 	}
 
 	private String[] sortConnectionNames()
 	{
 		int size = 100;
 		int count = 0;
 		String[] array = new String[size];
 		String key;
 
                 Hashtable ss = currentConnEnv.getSessions();
 		// Calculate the number of connections in the sessions list and
 		// build an array of all the connection names.
 		for (Enumeration connectionNames = ss.keys(); connectionNames.hasMoreElements();) {
  		    if (count == size) {
 		       // need to expand the array
 		       size = size*2;
 		       String[] expandedArray = new String[size];
 		       System.arraycopy(array, 0, expandedArray, 0, count);
 		       array = expandedArray;
 		    }
 		    key = (String)connectionNames.nextElement();
 		    array[ count++ ] = key;
 		}
 
 		java.util.Arrays.sort(array, 0, count);
 
         return array;
 	}
 
 	    /**
 	  This is used at the ij startup time to see if there are already some
 	      connections made and if so, show connections made so far.
 	  Following also gets executed when user types show connections command
 	      in ij. In the former case, ignore0Rows is set whereas in the later cas
 	  it's set to false. The reason for this is, at ij startup time, if there
 	  are no connections made so far, we don't want to show anything. Only if
 	  there are connections made, we show the connections. Whereas in show
 	  connection command case, we want to show the connection status either way
 	  ie if there are no connections, we say no connections. Otherwise we list
 	  all the connections made so far.
 	    */
 	public ijResult showConnectionsMethod(boolean ignore0Rows) throws SQLException {
   		Hashtable ss = currentConnEnv.getSessions();
 	  	Vector v = new Vector();
 	  	SQLWarning w = null;
         if (ss == null || ss.size() == 0) {
         	if (!ignore0Rows)
             	v.addElement(LocalizedResource.getMessage("IJ_NoConneAvail"));
         } 
         else {
         	boolean haveCurrent=false;
 			int count = 0;
 			for (Enumeration connectionNames = ss.keys(); connectionNames.hasMoreElements();
 						connectionNames.nextElement()) 
 		    	count++;
             String[] array = sortConnectionNames();
 		    for ( int ictr = 0; ictr < count; ictr++ ) {
 				String connectionName = array[ ictr ];
             	Session s = (Session)ss.get(connectionName);
             	if (s.getConnection().isClosed()) {
             		if (currentConnEnv.getSession() != null && 
             				connectionName.equals(currentConnEnv.getSession().getName())) {
                   		currentConnEnv.removeCurrentSession();
                   		theConnection = null;
                		}
 	               	else
     	            	currentConnEnv.removeSession(connectionName);
             	}
             	else {
             		StringBuffer row = new StringBuffer();
 	              	row.append(connectionName);
               		if (currentConnEnv.getSession() != null && 
               			connectionName.equals(currentConnEnv.getSession().getName())) {
 		               	row.append('*');
                  		haveCurrent=true;
               		}
               
 		    	  	//If ij.dataSource property is set, show only connection names.
 		    	  	//In this case, URL is not used to get connection, so do not append URL
 	    		  	String dsName = util.getSystemProperty("ij.dataSource");
 				  	if(dsName == null){	
                 		row.append(" - 	");
 	              		row.append(s.getConnection().getMetaData().getURL());
 	          		}
               		// save the warnings from these connections
 	              	w = appendWarnings(w,s.getConnection().getWarnings());
     	          	s.getConnection().clearWarnings();
         	      	v.addElement(row.toString());
             	}
         	}
   	    	if (haveCurrent)
             	v.addElement(LocalizedResource.getMessage("IJ_CurreConne"));
 		    else
             	v.addElement(LocalizedResource.getMessage("IJ_NoCurreConne"));
 	  	}
 	  	return new ijVectorResult(v,w);
 	}
 
 	/**
 	   Returns a subset of the input integer array
 	   
 	   @param input The input integer array
 	   @param start Starting index, inclusive
 	   @param end   Ending index, exclusive
 	 */
 	public static int[] intArraySubset(final int[] input, int start, int end) {
 		int[] res = new int[end-start];
 		System.arraycopy(input, start, res, 0, end-start);
 		return res;
 	}
 
 	/**
 	   Verify that a table exists within a schema. Throws an exception
 	   if table does not exist.
 	   
 	   @param schema Schema for the table
 	   @param table  Name of table to check for existence of
 	 */
 	public void verifyTableExists(String schema, String table) 
 	throws SQLException {
 		if(schema == null)
 			return;
 
 		ResultSet rs = null;
 		try {
 			DatabaseMetaData dbmd = theConnection.getMetaData();
 			rs = dbmd.getTables(null,schema,table,null);
 			if(!rs.next())
 				throw ijException.noSuchTable(table);
 		} finally {
 			if(rs!=null)
 				rs.close();
 		}
 	}
 
 	/**
 	   Return a resultset of tables (or views, procs...) in the given schema. 
 
 	   @param schema  Schema to get tables for, or null for search 
 	                  in all schemas.
 	   @param tableType Types of tables to return, see
 	                  {@link java.sql.DatabaseMetaData#getTableTypes}
 	 */
 	public ijResult showTables(String schema, String[] tableType) throws SQLException {
 		ResultSet rs = null;
 		try {
 			haveConnection();
 
 			DatabaseMetaData dbmd = theConnection.getMetaData();
 			rs = dbmd.getTables(null,schema,null,tableType);
 
 			int[] displayColumns = new int[] {
 				rs.findColumn("TABLE_SCHEM"),
 				rs.findColumn("TABLE_NAME"),
 				rs.findColumn("REMARKS"),
 			};
 			int[] columnWidths = new int[] {
 				20,
 				30,
 				20,
 			};
 
 			return new ijResultSetResult(rs, displayColumns, columnWidths);
 		} catch (SQLException e) {
 			if(rs!=null)
 				rs.close();
 			throw e;
 		}
 	}
 
 	/**
 	   Return a resultset of indexes for the given table or schema
 
 	   @param schema  schema to find indexes for
 	   @param table the exact name of the table to find indexes for
 	*/
     private ResultSet getIndexInfoForTable(String schema, String table) 
       throws SQLException {
 
         ResultSet rs = null;
         try {
             haveConnection();
             verifyTableExists(schema, table);
 
             DatabaseMetaData dbmd = theConnection.getMetaData();
             rs = dbmd.getIndexInfo(null, schema, table, false, true);
 
         } catch (SQLException e) {
             if(rs!=null)
                 rs.close();
             throw e;
         }
         return rs;
     }
 
     /**
      * Used by showIndexes to get columns in correct order
      */
     private int[] getDisplayColumnsForIndex(String schema, ResultSet rs)
         throws SQLException{
         int[] displayColumns = new int[] {
             rs.findColumn("TABLE_SCHEM"),
             rs.findColumn("TABLE_NAME"),
             rs.findColumn("COLUMN_NAME"),
             rs.findColumn("NON_UNIQUE"),
             rs.findColumn("TYPE"),
             rs.findColumn("ASC_OR_DESC"),
             rs.findColumn("CARDINALITY"),
             rs.findColumn("PAGES"),
         };
         if(schema!=null) {
             displayColumns = intArraySubset(displayColumns, 1, 
                                             displayColumns.length);
         }
         return displayColumns;
     }
 
     /**
      * Used by showIndexes to get correct column widths
      */
     private int[] getColumnWidthsForIndex(String schema){
         int[] columnWidths = new int[] {
             20,
             20,
             20,
             6,
             4,
             4,
             8,
             8,
         };
         if(schema!=null) {
             columnWidths = intArraySubset(columnWidths, 1, 
                                             columnWidths.length);
         }
         return columnWidths;
     }
 
     /**
     * Used to show all indices.
     *
      * @param schema the schema indices are shown from. 
     * @param table the table name to show indices for. If <code>null</code>,
     *      all indices of the schema are returned.
      */
    public ijResult showIndexes(String schema, String table)
            throws SQLException {
           
         ijResult result = null;
 
         int[] displayColumns = null;
         int[] columnWidths = null;
 
         try {
             if (table != null) {
                 ResultSet rs = getIndexInfoForTable(schema, table);
                 displayColumns = getDisplayColumnsForIndex(schema, rs);
                 columnWidths = getColumnWidthsForIndex(schema);
                 result = new ijResultSetResult(rs, displayColumns,
                                                columnWidths); 
             }
             else {
                 /* DatabaseMetaData#getIndexInfo requires exact table names.
                  * If table is null, we must first get all table names in
                  * the appropriate schema, and then get all indices for each
                  * of these. 
                  */
                 haveConnection();
                 verifyTableExists(schema, table);
 
                 DatabaseMetaData dbmd = theConnection.getMetaData();
                 ResultSet tablers = dbmd.getTables(null,schema,null,null); 
               
                 List resultSets = new ArrayList();
                 boolean firstIteration = true;
                 ResultSet current_rs = null;
                 while (tablers.next()){
                     String tableName = tablers.getString("TABLE_NAME");
                     current_rs = getIndexInfoForTable(schema, tableName);
                     resultSets.add(current_rs);
 
                     if (firstIteration) {
                         displayColumns = getDisplayColumnsForIndex(schema,
                                                                    current_rs);
                         columnWidths = getColumnWidthsForIndex(schema);
                         firstIteration = false;
                     }              
                 }
                 result = new ijMultipleResultSetResult(resultSets,
                                                        displayColumns,
                                                        columnWidths);
             }
             return result;
         } catch (SQLException e) {
             if(result!=null)
                 result.closeStatement();
             throw e;
         }
     }
 
 	/**
 	   Return a resultset of procedures from database metadata
 	 */
 	public ijResult showProcedures(String schema) throws SQLException {
 		ResultSet rs = null;
 		try {
 			haveConnection();
 
 			DatabaseMetaData dbmd = theConnection.getMetaData();
 			rs = dbmd.getProcedures(null,schema,null);
 
 			int[] displayColumns = new int[] {
 				rs.findColumn("PROCEDURE_SCHEM"),
 				rs.findColumn("PROCEDURE_NAME"),
 				rs.findColumn("REMARKS"),
 			};
 			int[] columnWidths = new int[] {
 				20,
 				30,
 				20,
 			};
 
 			return new ijResultSetResult(rs, displayColumns, columnWidths);
 		} catch (SQLException e) {
 			if(rs!=null)
 				rs.close();
 			throw e;
 		}
 	}
 
 	/**
 	   Return a resultset of schemas from database metadata
 	 */
 	public ijResult showSchemas() throws SQLException {
 		ResultSet rs = null;
 		try {
 			haveConnection();
 
 			DatabaseMetaData dbmd = theConnection.getMetaData();
 			rs = dbmd.getSchemas();
 
 			int[] displayColumns = new int[] {
 				rs.findColumn("TABLE_SCHEM")
 			};
 			int[] columnWidths = new int[] {
 				30
 			};
 
 			return new ijResultSetResult(rs, displayColumns, columnWidths);
 		} catch (SQLException e) {
 			if(rs!=null)
 				rs.close();
 			throw e;
 		}
 	}
 
 	/**
 	   Outputs the names of all fields of given table. Outputs field
 	   names and data type.
 	 */
 	public ijResult describeTable(String schema, String table) throws SQLException {
 		ResultSet rs = null;
 		try {
 			haveConnection();
 			verifyTableExists(schema,table);
 
 			DatabaseMetaData dbmd = theConnection.getMetaData();
 			rs = dbmd.getColumns(null,schema,table,null);
 
 			int[] displayColumns = new int[] {
 				rs.findColumn("TABLE_SCHEM"),
 				rs.findColumn("TABLE_NAME"),
 				rs.findColumn("COLUMN_NAME"),
 				rs.findColumn("TYPE_NAME"),
 				rs.findColumn("DECIMAL_DIGITS"),
 				rs.findColumn("NUM_PREC_RADIX"),
 				rs.findColumn("COLUMN_SIZE"),
 				rs.findColumn("COLUMN_DEF"),
 				rs.findColumn("CHAR_OCTET_LENGTH"),
 				rs.findColumn("IS_NULLABLE"),
 			};
 			int[] columnWidths = new int[] {
 				20,
 				20,
 				20,
 				9,
 				4,
 				4,
 				6,
 				10,
 				10,
 				8
 			};
 
 			//
 			// If schema is specified (if util.getSelectedSchema in
 			// DescTableStatement() returns correct value), then we
 			// don't need to output schema and table names.
 			if(schema!=null) {
 				displayColumns = intArraySubset(displayColumns, 2, 
 												displayColumns.length);
 				columnWidths   = intArraySubset(columnWidths, 2, 
 												columnWidths.length);
 			}
 
 			return new ijResultSetResult(rs, displayColumns, columnWidths);
 		} catch (SQLException e) {
 			if(rs!=null)
 				rs.close();
 			throw e;
 		}
 	}
 
 	private Object makeXid(int xid)
 	{
 		return null;
 	}
 
 
 	
 
 }
 
 PARSER_END(ij)
 
 /* WHITE SPACE */
 
 SKIP :
 {
   " "
 | "\t"
 | "\r\n"
 | "\n"
 | "\r"
 | "\f"
 }
 
 // sqlgrammar only recognizes --, so that's all we will recognize as well.
 SPECIAL_TOKEN : /* COMMENTS */
 {
   <SINGLE_LINE_SQLCOMMENT: "--" (~["\n","\r"])* ("\n"|"\r"|"\r\n")>
 //| <SINGLE_LINE_COMMENT: "//" (~["\n","\r"])* ("\n"|"\r"|"\r\n")>
 //| <FORMAL_COMMENT: "/**" (~["*"])* "*" ("*" | (~["*","/"] (~["*"])* "*"))* "/">
 //| <MULTI_LINE_COMMENT: "/*" (~["*"])* "*" ("*" | (~["*","/"] (~["*"])* "*"))* "/">
 }
 
 TOKEN [IGNORE_CASE] :
 {	/* ij Keywords */
 	<ABSOLUTE: "absolute">
 |	<AFTER: "after">
 |	<ALIASES: "aliases">
 |	<ALL: "all">
 |	<AS: "as">
 |	<ASYNC: "async">
 |	<ATTRIBUTES: "attributes">
 |	<AUTOCOMMIT: "autocommit">
 |	<BANG: "!">
 |	<BEFORE: "before">
 |	<CLOSE: "close">
 | 	<COMMIT: "commit">
 | 	<CONNECT: "connect">
 | 	<CONNECTION: "connection">
 | 	<CONNECTIONS: "connections">
 |	<CURRENT: "current">
 |	<CURSOR: "cursor">
 |	<DESCRIBE: "describe">
 |	<DISCONNECT: "disconnect">
 |	<DRIVER: "driver">
 |	<ELAPSEDTIME: "elapsedtime">
 |	<END: "end">
 |	<EQUALS_OPERATOR: "=">
 |	<EXECUTE: "execute">
 |	<EXIT: "exit">
 |	<EXPECT: "expect">
 |	<FAIL: "fail">
 |	<FIRST: "first">
 |	<FOR: "for">
 |	<FROM: "from">
 |	<GET: "get">
 |	<GETCURRENTROWNUMBER: "getcurrentrownumber">
 |	<HOLD: "hold">
 |	<HELP: "help">
 |	<IN: "in">
 |	<INDEXES: "indexes">
 |	<INSENSITIVE: "insensitive">
 |	<INTO: "into">
 |	<LAST: "last">
 |	<LOCALIZEDDISPLAY: "localizeddisplay">
 |	<MAXIMUMDISPLAYWIDTH: "maximumdisplaywidth">
 |	<NAME: "name">
 |	<NEXT: "next">
 |	<NOHOLD: "nohold">
 |	<NOHOLDFORCONNECTION: "noholdforconnection">
 |	<OFF: "off">
 |	<ON: "on">
 |	<PASSWORD: "password">
 |	<PERIOD: ".">
 |	<PREPARE: "prepare">
 |	<PREVIOUS: "previous">
 |	<PROCEDURE: "procedure">
 |	<PROCEDURES: "procedures">
 |	<PROPERTIES: "properties">
 |	<PROTOCOL: "protocol">
 |	<QUIT: "quit">
 |	<READONLY: "readonly">
 |	<RELATIVE: "relative">
 |	<REMOVE: "remove">
 |	<RESOURCE: "resource">
 |	<ROLLBACK: "rollback">
 |	<RUN: "run">
 |	<TO: "to">
 |	<SAVEPOINT: "savepoint">
 |	<SCHEMAS: "schemas">
 |	<SCROLL: "scroll">
 |	<SENSITIVE: "sensitive">
 |	<SET: "set">
 |	<SHOW: "show">
 |	<SHUTDOWN: "shutdown">
 |	<STATEMENT: "statement">
 |	<SYNONYMS: "synonyms">
 |	<TABLES: "tables">
 |	<USER: "user">
 |	<USING: "using">
 |	<VIEWS: "views">
 |	<WAIT: "wait">
 |	<WITH: "with">
 |	<XA_1PHASE: "XA_1phase">
 |	<XA_2PHASE: "XA_2phase">
 |	<XA_DATASOURCE: "XA_datasource">
 |	<XA_CONNECT: "XA_connect">
 |	<XA_COMMIT: "XA_commit">
 |	<XA_DISCONNECT: "XA_disconnect">
 |	<XA_END: "XA_end">
 |	<XA_ENDRSCAN: "XA_endrscan">
 |	<XA_FAIL: "XA_fail">
 |	<XA_FORGET: "XA_forget">
 |	<XA_GETCONNECTION: "XA_getconnection">
 |	<XA_JOIN: "XA_join">
 |	<XA_NOFLAGS: "XA_noflags">
 |	<XA_PREPARE: "XA_prepare">
 |	<XA_RECOVER: "XA_recover">
 |	<XA_RESUME: "XA_resume">
 |	<XA_ROLLBACK: "XA_rollback">
 |	<XA_START: "XA_start">
 |	<XA_STARTRSCAN: "XA_startrscan">
 |	<XA_SUCCESS: "XA_success">
 |	<XA_SUSPEND: "XA_suspend">
 |	<DATASOURCE: "datasource">
 |	<CP_DATASOURCE: "CP_datasource">
 |	<CP_CONNECT: "CP_connect">
 |	<CP_GETCONNECTION: "CP_getconnection">
 |	<CP_DISCONNECT: "CP_disconnect">
 |	<WORK : "work">
 }
 
 TOKEN :
 {	/* Operators and punctuation -- to avoid lexical errors for SQL-J stuff, mostly */
 	<COMMA: ",">
 |   <LEFT_PAREN: "(">
 |   <RIGHT_PAREN: ")">
 |   <DOUBLE_QUOTE: "\"">
 |   <HASH: "#">
 |	<MINUS_SIGN: "-">
 |	<PLUS_SIGN: "+">
 }
 
 /**
 TOKEN :
 {
     <IDENTIFIER: ["a"-"z","A"-"Z"](["a"-"z","A"-"Z","_","0"-"9"])*>
 }
 */
 
 TOKEN :
 {   /* Identifiers */
         <IDENTIFIER: ( <LETTER> | "_" ) (<LETTER> | "_" | <DIGIT>)* >
 }
 
 TOKEN:
 {
         <#LETTER: [
                                 "a"-"z",
                                 "A"-"Z",
                                 "\u00aa",
                                 "\u00b5",
                                 "\u00ba",
                                 "\u00c0" - "\u00d6",
                                 "\u00d8" - "\u00f6",
                                 "\u00f8" - "\u01f5",
                                 "\u01fa" - "\u0217",
                                 "\u0250" - "\u02a8",
                                 "\u02b0" - "\u02b8",
                                 "\u02bb" - "\u02c1",
                                 "\u02d0" - "\u02d1",
                                 "\u02e0" - "\u02e4",
                                 "\u037a",
                                 "\u0386",
                                 "\u0388" - "\u038a",
                                 "\u038c",
                                 "\u038e" - "\u03a1",
                                 "\u03a3" - "\u03ce",
                                 "\u03d0" - "\u03d6",
                                 "\u03da",
                                 "\u03dc",
                                 "\u03de",
                                 "\u03e0",
                                 "\u03e2" - "\u03f3",
                                 "\u0401" - "\u040c",
                                 "\u040e" - "\u044f",
                                 "\u0451" - "\u045c",
                                 "\u045e" - "\u0481",
                                 "\u0490" - "\u04c4",
                                 "\u04c7" - "\u04c8",
                                 "\u04cb" - "\u04cc",
                                 "\u04d0" - "\u04eb",
                                 "\u04ee" - "\u04f5",
                                 "\u04f8" - "\u04f9",
                                 "\u0531" - "\u0556",
                                 "\u0559",
                                 "\u0561" - "\u0587",
                                 "\u05d0" - "\u05ea",
                                 "\u05f0" - "\u05f2",
                                 "\u0621" - "\u063a",
                                 "\u0640" - "\u064a",
                                 "\u0671" - "\u06b7",
                                 "\u06ba" - "\u06be",
                                 "\u06c0" - "\u06ce",
                                 "\u06d0" - "\u06d3",
                                 "\u06d5",
                                 "\u06e5" - "\u06e6",
                                 "\u0905" - "\u0939",
                                 "\u093d",
                                 "\u0958" - "\u0961",
                                 "\u0985" - "\u098c",
                                 "\u098f" - "\u0990",
                                 "\u0993" - "\u09a8",
                                 "\u09aa" - "\u09b0",
                                 "\u09b2",
                                 "\u09b6" - "\u09b9",
                                 "\u09dc" - "\u09dd",
                                 "\u09df" - "\u09e1",
                                 "\u09f0" - "\u09f1",
                                 "\u0a05" - "\u0a0a",
                                 "\u0a0f" - "\u0a10",
                                 "\u0a13" - "\u0a28",
                                 "\u0a2a" - "\u0a30",
                                 "\u0a32" - "\u0a33",
                                 "\u0a35" - "\u0a36",
                                 "\u0a38" - "\u0a39",
                                 "\u0a59" - "\u0a5c",
                                 "\u0a5e",
                                 "\u0a72" - "\u0a74",
                                 "\u0a85" - "\u0a8b",
                                 "\u0a8d",
                                 "\u0a8f" - "\u0a91",
                                 "\u0a93" - "\u0aa8",
                                 "\u0aaa" - "\u0ab0",
                                 "\u0ab2" - "\u0ab3",
                                 "\u0ab5" - "\u0ab9",
                                 "\u0abd",
                                 "\u0ae0",
                                 "\u0b05" - "\u0b0c",
                                 "\u0b0f" - "\u0b10",
                                 "\u0b13" - "\u0b28",
                                 "\u0b2a" - "\u0b30",
                                 "\u0b32" - "\u0b33",
                                 "\u0b36" - "\u0b39",
                                 "\u0b3d",
                                 "\u0b5c" - "\u0b5d",
                                 "\u0b5f" - "\u0b61",
                                 "\u0b85" - "\u0b8a",
                                 "\u0b8e" - "\u0b90",
                                 "\u0b92" - "\u0b95",
                                 "\u0b99" - "\u0b9a",
                                 "\u0b9c",
                                 "\u0b9e" - "\u0b9f",
                                 "\u0ba3" - "\u0ba4",
                                 "\u0ba8" - "\u0baa",
                                 "\u0bae" - "\u0bb5",
                                 "\u0bb7" - "\u0bb9",
                                 "\u0c05" - "\u0c0c",
                                 "\u0c0e" - "\u0c10",
                                 "\u0c12" - "\u0c28",
                                 "\u0c2a" - "\u0c33",
                                 "\u0c35" - "\u0c39",
                                 "\u0c60" - "\u0c61",
                                 "\u0c85" - "\u0c8c",
                                 "\u0c8e" - "\u0c90",
                                 "\u0c92" - "\u0ca8",
                                 "\u0caa" - "\u0cb3",
                                 "\u0cb5" - "\u0cb9",
                                 "\u0cde",
                                 "\u0ce0" - "\u0ce1",
                                 "\u0d05" - "\u0d0c",
                                 "\u0d0e" - "\u0d10",
                                 "\u0d12" - "\u0d28",
                                 "\u0d2a" - "\u0d39",
                                 "\u0d60" - "\u0d61",
                                 "\u0e01" - "\u0e2e",
                                 "\u0e30",
                                 "\u0e32" - "\u0e33",
                                 "\u0e40" - "\u0e46",
                                 "\u0e81" - "\u0e82",
                                 "\u0e84",
                                 "\u0e87" - "\u0e88",
                                 "\u0e8a",
                                 "\u0e8d",
                                 "\u0e94" - "\u0e97",
                                 "\u0e99" - "\u0e9f",
                                 "\u0ea1" - "\u0ea3",
                                 "\u0ea5",
                                 "\u0ea7",
                                 "\u0eaa" - "\u0eab",
                                 "\u0ead" - "\u0eae",
                                 "\u0eb0",
                                 "\u0eb2" - "\u0eb3",
                                 "\u0ebd",
                                 "\u0ec0" - "\u0ec4",
                                 "\u0ec6",
                                 "\u0edc" - "\u0edd",
                                 "\u0f40" - "\u0f47",
                                 "\u0f49" - "\u0f69",
                                 "\u10a0" - "\u10c5",
                                 "\u10d0" - "\u10f6",
                                 "\u1100" - "\u1159",
                                 "\u115f" - "\u11a2",
                                 "\u11a8" - "\u11f9",
                                 "\u1e00" - "\u1e9b",
                                 "\u1ea0" - "\u1ef9",
                                 "\u1f00" - "\u1f15",
                                 "\u1f18" - "\u1f1d",
                                 "\u1f20" - "\u1f45",
                                 "\u1f48" - "\u1f4d",
                                 "\u1f50" - "\u1f57",
                                 "\u1f59",
                                 "\u1f5b",
                                 "\u1f5d",
                                 "\u1f5f" - "\u1f7d",
                                 "\u1f80" - "\u1fb4",
                                 "\u1fb6" - "\u1fbc",
                                 "\u1fbe",
                                 "\u1fc2" - "\u1fc4",
                                 "\u1fc6" - "\u1fcc",
                                 "\u1fd0" - "\u1fd3",
                                 "\u1fd6" - "\u1fdb",
                                 "\u1fe0" - "\u1fec",
                                 "\u1ff2" - "\u1ff4",
                                 "\u1ff6" - "\u1ffc",
                                 "\u207f",
                                 "\u2102",
                                 "\u2107",
                                 "\u210a" - "\u2113",
                                 "\u2115",
                                 "\u2118" - "\u211d",
                                 "\u2124",
                                 "\u2126",
                                 "\u2128",
                                 "\u212a" - "\u2131",
                                 "\u2133" - "\u2138",
                                 "\u3005",
                                 "\u3031" - "\u3035",
                                 "\u3041" - "\u3094",
                                 "\u309b" - "\u309e",
                                 "\u30a1" - "\u30fa",
                                 "\u30fc" - "\u30fe",
                                 "\u3105" - "\u312c",
                                 "\u3131" - "\u318e",
                                 "\u4e00" - "\u9fa5",
                                 "\uac00" - "\ud7a3",
                                 "\uf900" - "\ufa2d",
                                 "\ufb00" - "\ufb06",
                                 "\ufb13" - "\ufb17",
                                 "\ufb1f" - "\ufb28",
                                 "\ufb2a" - "\ufb36",
                                 "\ufb38" - "\ufb3c",
                                 "\ufb3e",
                                 "\ufb40" - "\ufb41",
                                 "\ufb43" - "\ufb44",
                                 "\ufb46" - "\ufbb1",
                                 "\ufbd3" - "\ufd3d",
                                 "\ufd50" - "\ufd8f",
                                 "\ufd92" - "\ufdc7",
                                 "\ufdf0" - "\ufdfb",
                                 "\ufe70" - "\ufe72",
                                 "\ufe74",
                                 "\ufe76" - "\ufefc",
                                 "\uff21" - "\uff3a",
                                 "\uff41" - "\uff5a",
                                 "\uff66" - "\uffbe",
                                 "\uffc2" - "\uffc7",
                                 "\uffca" - "\uffcf",
                                 "\uffd2" - "\uffd7",
                                 "\uffda" - "\uffdc"
                         ]>
 }
 
 TOKEN :
 {
         <#DIGIT: [
                                 "0" - "9",
                                 "\u0660" - "\u0669",
                                 "\u06f0" - "\u06f9",
                                 "\u0966" - "\u096f",
                                 "\u09e6" - "\u09ef",
                                 "\u0a66" - "\u0a6f",
                                 "\u0ae6" - "\u0aef",
                                 "\u0b66" - "\u0b6f",
                                 "\u0be7" - "\u0bef",
                                 "\u0c66" - "\u0c6f",
                                 "\u0ce6" - "\u0cef",
                                 "\u0d66" - "\u0d6f",
                                 "\u0e50" - "\u0e59",
                                 "\u0ed0" - "\u0ed9",
                                 "\u0f20" - "\u0f29",
                                 "\uff10" - "\uff19"
                         ]>
 }
 TOKEN :
 {	/* Literals */
 	<INTEGER: (["0" - "9"])+ >
 |	<STRING: "'"
 		(
 			"''" |
 			~["'"]
 		) *
 		"'">
 }
 
 //
 // start of BNF rules
 //
 
 ijResult
 ijStatement() 
 throws SQLException
 :
 {
 	ijResult r = null;
 }
 {
 (
 	LOOKAHEAD(
 		{ 
 			getToken(1).kind == ROLLBACK && 
 			(!(getToken(3).kind == TO || getToken(3).kind == SAVEPOINT))			 			 
 		})  
 	r=RollbackStatement()	
 |	r=AbsoluteStatement()
 |	r=AfterLastStatement()
 |	r=AutocommitStatement()
 |	r=AsyncStatement()	
 |	r=Bang()	
 |	r=BeforeFirstStatement()
 | 	r=CloseStatement()
 | 	r=CommitStatement()
 | 	r=ConnectStatement()
 |	r=DescTableStatement()
 |	r=DisconnectStatement()
 |	r=DriverStatement()
 |	r=ElapsedTimeStatement()	
 |	r=ExecuteStatement()	
 |	r=FirstStatement()	
 |	r=FirstStatement()	
 |	r=JBMSPreparedStatementExec()	
 |	r=F2KExecuteProcedure()	
 |	r=ExitStatement()
 |	r=ExpectStatement()
 |	r=GetCursorStatement()	
 |	r=GetCurrentRowNumber()	
 |	r=HelpStatement()	
 |	r=IllegalStatementName()
 |	r=LastStatement()	
 |	r=LocalizedDisplay()
 |	r=MaximumDisplayWidthStatement()	
 |	r=NextStatement()	
 |	r=NoHoldForConnectionStatement()	
 |	r=PrepareStatement()
 |	r=PreviousStatement()	
 |	r=ProtocolStatement()
 |	r=ReadOnlyStatement()
 |	r=RelativeStatement()	
 |	r=RemoveStatement()	
 |	r=RunStatement()	
 |	r=SetConnectionStatement()	
 |	r=ShowStatement()	
 |	r=WaitForStatement()	
 |	r=XA_DataSourceStatement()
 |	r=XA_ConnectStatement()
 |	r=XA_CommitStatement()
 |	r=XA_DisconnectStatement()
 |	r=XA_GetConnectionStatement()
 |	r=XA_EndStatement()
 |	r=XA_ForgetStatement()
 |	r=XA_PrepareStatement()
 |	r=XA_RecoverStatement()
 |	r=XA_RollbackStatement()
 |	r=XA_StartStatement() 
 |	r=DataSourceStatement()
 |	r=CP_DataSourceStatement()
 |	r=CP_ConnectStatement()
 |	r=CP_GetConnectionStatement()
 |	r=CP_DisconnectStatement()
 )? <EOF>
 	{
 		return r;
 	}
 }
 
 /**
  * ProtocolStatement is PROTOCOL 'JDBC protocol' where
  * the protocol is used to prefix any connect request that
  * cannot find a driver.  We will take a stab at loading
  * a driver as each protocol comes in -- we only know about
  * two.
  */
 ijResult
 ProtocolStatement()
 throws SQLException
 :
 {
 	Token t;
 	String n = null;
 }
 {
 	<PROTOCOL> t=<STRING> [ <AS> n=identifier() ]
 	{
 		installProtocol(n, stringValue(t.image));
 		return null;
 	}
 }
 
 /**
  * DriverStatement is DRIVER 'class' where class is the
  * name of a class that is a JDBC driver. It is loaded
  * into the DriverManager with a Class.forName call.
  * <p>
  * You can load as many drivers as you want, the idea is
  * to load up the appropriate one(s) for the connect(s)
  * that you will be issuing.
  */
 ijResult
 DriverStatement()
 throws SQLException
 :
 {
 	Token t;
 	String sVal = null;
 }
 {
 	<DRIVER> t=<STRING>
 	{
 	    try {
 		// t.image is a class name;
 		// we load the "driver" in the prototypical
 		// manner, it will register itself with
 		// the DriverManager.
 			sVal = stringValue(t.image);
 			util.loadDriver(sVal);
 	    } catch (ClassNotFoundException e) {
 			throw ijException.classNotFound(sVal);
 	    } catch (IllegalArgumentException e) {
 			throw ijException.driverNotClassName(sVal);
 	    } catch (IllegalAccessException e) {
 			throw ijException.classNotFound(sVal);
 	    } catch (InstantiationException e) {
 			throw ijException.classNotFound(sVal);
 	    }
 		return null;
 	}
 }
 
 ijResult
 ConnectStatement()
 throws SQLException
 :
 {
 	ijResult	result;
 }
 {
 	<CONNECT> <TO>
 	( result = dynamicConnection(true) )
 	{
 		return result;
 	}
 |
 	<CONNECT>
 	( result = dynamicConnection(false) | result = staticConnection() )
 	{
 		return result;
 	}
 }
 
 
 
 /**
  * ConnectStatement is CONNECT 'url' [ PROTOCOL proto ] 
 	[ USER 	String PASSWORD String ] 
 	[ATTRIBUTES attributeName = value [, attributeName = value]* ]
 	[ AS ident ], where url is the
  * url for the database, i.e. jdbc:protocol:dbname etc.
  * Attributes are connection attributes to 
  * <p>
  * There can only be one connection at a time; if there
  * is already one, it is put on hold and this one takes its place.
  * <p>
  * if a driver can't be found, the current protocol will
  * be added at the front.
  * <p>
  * the as ident part is used for set connection.  If you don't
  * specify a name, we create one that is CONNECTION# for the #
  * of open connections that now exists. If the name duplicates,
  * an error results.
  */
 ijResult
 dynamicConnection(boolean simplifiedPath)
 throws SQLException
 :
 {
 	Token t;
 	Token userT = null;
 	Token passwordT = null;
 	String n = null, p = null, sVal;
     String userS =  util.getSystemProperty(USER_PROPERTY);
     String passwordS = util.getSystemProperty(PASSWORD_PROPERTY);
 	Properties connInfo = null;
 }
 {
 	t=<STRING>	[ <PROTOCOL> p=identifier() ] 
 				[ <USER> userT=<STRING> ]
 				[ <PASSWORD> passwordT=<STRING> ]
 		        [ <ATTRIBUTES> connInfo = attributeList() ]
 				[ <AS> n=identifier() ]
 	{
 		// t.image is a database URL
 		// we get the connection and salt it away
 		// for use with other statements.
 		//
 		// FUTURE: we could have the syntax be
 		// CONNECT <STRING> AS <IDENTIFIER>
 		// and have a SET CONNECTION string to
 		// re-activate a named connection.
 		// Or not, and wait for SQL-J to support that
 		// statement... although then we will have to
 		// figure out if we will allow that SQL-J through
 		// JDBC or not.
 		// get the value of the string
 		// n.b. at some point this will have to deal with ''s
 		if (userT != null)
     		userS = stringValue(userT.image);
 
     	if (passwordT != null)
     		passwordS = stringValue(passwordT.image);
     			
     	//If ij.dataSource property is set,use DataSource to get the connection
 		String dsName = util.getSystemProperty("ij.dataSource");
 		if (dsName != null){
     		//Check that t.image does not start with jdbc:
     		//If it starts with jdbc:, do not use DataSource to get connection
     		sVal = stringValue(t.image);
     		if(!sVal.startsWith("jdbc:") ){
     			theConnection = util.getDataSourceConnection(dsName,userS,passwordS,sVal,false);
     			return addSession( theConnection, n );
     		}
     	}    			
     	
 		if (simplifiedPath)
 			// url for the database W/O 'jdbc:protocol:', i.e. just a dbname
 			// For example,
 			//		CONNECT TO 'test'
 			// is equivalent to
 			// 		CONNECT TO 'jdbc:derby:test'
 			sVal = "jdbc:derby:" + stringValue(t.image);
 		else
 			sVal = stringValue(t.image);
 
 		// add named protocol if it was specified
 		if (p != null) {
 			String protocol = (String)namedProtocols.get(p);
 			if (protocol == null) { throw ijException.noSuchProtocol(p); }
 			sVal = protocol + sVal; 
 		}
 
 		// add protocol if no driver matches url
 		boolean noDriver = false;
 			// if we have a full URL, make sure it's loaded first
 			try {
 				if (sVal.startsWith("jdbc:"))
 					util.loadDriverIfKnown(sVal);
 			} catch (Exception e) {
 				// want to continue with the attempt
                         }
 			// By default perform extra checking on the URL attributes.
 			// This checking does not change the processing.
                         if (System.getProperty("ij.URLCheck") == null || Boolean.getBoolean("ij.URLCheck")) {
                           URLCheck aCheck = new URLCheck(sVal);
                         }
 		if (!sVal.startsWith("jdbc:") && (p == null) && (protocol != null)) {
 			sVal = protocol + sVal;
 		}
 
 
 		// If no ATTRIBUTES on the connection get them from the
 		// defaults
 		if (connInfo == null)
 			connInfo = util.updateConnInfo(userS,passwordS, 
 			utilInstance.getConnAttributeDefaults());
 		else
 			connInfo = util.updateConnInfo(userS,passwordS, connInfo);
 
 	   
 		theConnection = DriverManager.getConnection(sVal,connInfo);
 
 		return addSession( theConnection, n );
 	}
 }
 
 
 /**
  * Handles DESCRIBE table
  */
 ijResult
 DescTableStatement()
 throws SQLException
 :
 {
 	String i = null;
 	String i2 = null;
 	Token  s = null;
 }
 {
 	<DESCRIBE>
 	( ( i=identifier() <PERIOD> i2=identifier() )
 	| i2=identifier()
 	| s=<STRING>
 	)
 	{
 		if(s!=null) {
 			String image = stringValue(s.image.toUpperCase());
 
 			int dotPosition = image.indexOf('.');
 			if(dotPosition!=-1) {
 				i = image.substring(0,dotPosition);
 				i2 = image.substring(dotPosition+1);
 			}
 		}
 
 		if(i==null)
 			i = util.getSelectedSchema(theConnection);
 
 		return describeTable(i,i2);
 	}
 }
 
 
 /**
   * Handles CONNECT yadda.yadda.foo( stringArg, ... stringArg ) AS connectionName
   */
 ijResult
 staticConnection()
 throws SQLException
 :
 {
 	String			name = null;
 	Vector			idList;
 	int				idx = 0;
 	int				lastID = 0;
 	StringBuffer	buffer;
 	String			className;
 	String			methodName;
 	Class			classC;
 	Method			method;
 	int				argCount;
 	String[]		args;
 	Class			stringClass;
 	Class[]			argTypes;
 	ijResult		result = null;
 }
 {
 	idList = staticMethodName() args = staticMethodArgs() [ <AS> name = identifier() ]
 	{
 		lastID = idList.size() - 1;
 		buffer = new StringBuffer();
 
 		for ( ; idx < lastID; idx++ )
 		{
 			if ( idx > 0 ) { buffer.append( "." ); }
 			buffer.append( (String) idList.elementAt( idx ) );
 		}
 		methodName = (String) idList.elementAt( idx );
 		className = buffer.toString();
 
 		try {
 			argCount = args.length;
 			argTypes = new Class[ argCount ];
 			stringClass = Class.forName( "java.lang.String" );
 			for ( idx = 0; idx < argCount; idx++ ) { argTypes[ idx ] = stringClass; }
 
 			classC = Class.forName( className );
 			method = classC.getMethod( methodName, argTypes );
 			theConnection = (Connection) method.invoke( null, args );
 			result = addSession( theConnection, name );
 
 		} 
 		catch (java.lang.reflect.InvocationTargetException ite) {
 			Throwable t = ite.getTargetException();
 			if (t instanceof SQLException)
 				throw (SQLException) t;
 
 			throw new SQLException( t.toString() );
 		}
 		catch (Exception e) { throw new SQLException( e.toString() ); }
 
 		return result;
 	}
 }
 
 
 /**
  * SetConnectionStatement is SET CONNECTION ident
  * <p>
  * Moves to the named session, if it exists. If it doesn't
  * exist, remains on the current session and returns an error.
  */
 ijResult
 SetConnectionStatement()
 throws SQLException
 :
 {
 	String t;
 }
 {
 	<SET> <CONNECTION> t=identifier()
 	{
 		if (!currentConnEnv.haveSession(t)) {
 			throw ijException.noSuchConnection(t);
 		}
 		currentConnEnv.setCurrentSession(t);
 		theConnection = currentConnEnv.getConnection();
 		return new ijConnectionResult(theConnection);
 	}
 }
 
 /**
  * Handles showing current connections for the current environment, and
  * SHOW TABLES/VIEWS/... commands.
  */
 ijResult
 ShowStatement()
 throws SQLException
 :
 {
 	String schema  = null;
 	String tblname = null;
 	String str     = null;
 	String[] types = null;
 	Token t = null;
 	Token v = null;
 }
 {
 	<SHOW> <CONNECTIONS>
 	{
 		return showConnectionsMethod(false);
 	}
 |   <SHOW> (t=<TABLES> | v=<VIEWS> | <SYNONYMS> | <ALIASES>)
 		[ <IN> schema=identifier() ]
 	{
 		if(t!=null) {
 		    types = new String[] { "TABLE", "SYSTEM TABLE" };
 		}
 		else if(v!=null)
 			types = new String[] { "VIEW" };
 		else
 			types = new String[] { "ALIAS" };
 		return showTables(schema, types);
 	}
 |	<SHOW> <INDEXES> 
 		[ (<IN> schema=identifier()) |
 		  (<FROM> tblname=identifier() [ <PERIOD> str=identifier() ] ) ]
 	{
 		if(str != null) {
 			// if absolute table reference given
 			schema = tblname;
 			tblname = str;
 		}
 
 		// If user specifies a table name, then assume schema is
 		// current schema. Note that getSelectedSchema may return
 		// null for some DBMSes.
 		if(schema == null && tblname != null)
 			schema = util.getSelectedSchema(theConnection);
 		return showIndexes(schema,tblname);
 	}
 |	<SHOW> <PROCEDURES>
 		[ <IN> schema=identifier() ]
 	{
 		return showProcedures(schema);
 	}
 |	<SHOW> <SCHEMAS>
 	{
 		return showSchemas();
 	}
 }
 
 /**
  * CommitStatement is simply COMMIT.
  * It commits the current transation.
  */
 ijResult
 CommitStatement()
 throws SQLException
 :
 {
 }
 {
 	<COMMIT> [ <WORK> ]
 	{
 		haveConnection();
 		theConnection.commit();
 		return null;
 	}
 }
 
 /**
  * RollbackStatement is simply ROLLBACK.
  * It undoes the current transation.
  */
 ijResult
 RollbackStatement()
 throws SQLException
 :
 {
 }
 {
 	<ROLLBACK> [ <WORK> ]
 	{
 		haveConnection();
 		theConnection.rollback();
 		return null;
 	}
 }
 
 /**
  * DisconnectStatement is simply DISCONNECT [ ALL | CURRENT | connectionName ]
  * it ends the specified connection(s) and
  * releases its statement resource.
  * <p>
  * If ALL is specified, it disconnects all available sessions
  * in the current environment.
  */
 ijResult
 DisconnectStatement()
 throws SQLException
 :
 {
 	Token a = null;
 	String n = null;
 }
 {
 	<DISCONNECT> [ ( <CURRENT> | a = <ALL> | n = identifier() ) ]
 	{
 		if ( a == null ) { 
 			if (n == null) {
 		        // only remove the current session
 			    haveConnection();
 			    // Also need to release the session object
 			    currentConnEnv.removeCurrentSession();
 			    theConnection = null;
 			}
 			else {
 			    if (! currentConnEnv.haveSession(n))
 				    throw ijException.noSuchConnection(n);
 				currentConnEnv.removeSession(n);
 			    if (currentConnEnv.getSession() == null)
 				    theConnection = null;
 			}
 		} else {
 			currentConnEnv.removeAllSessions();
 			theConnection = null;
 		}
 		return null;
 	}
 }
 
 ijResult
 ExitStatement()
 throws SQLException
 :
 {
 }
 {
 	<EXIT>
 	{
 		return quit();
 	}
 |	<QUIT>
 	{
 		return quit();
 	}
 }
 
 ijResult
 IllegalStatementName()
 throws SQLException
 :
 {
 	Token s = null;
 }
 {
 	<PREPARE> <PROCEDURE> <AS> s=<STRING>
 	{
 		// "procedure" is not allowed as a statement name. this is
 		// because "execute procedure" is a valid Foundation2000
 		// command
 		throw ijException.illegalStatementName( "procedure" );
 	}
 }
 
 ijResult
 PrepareStatement()
 throws SQLException
 :
 {
 	Token t;
 	String i;
 	PreparedStatement ps;
 	String sVal;
 }
 {
 	<PREPARE> i=identifier() <AS> t=<STRING>
 	{
 		haveConnection();
 		sVal = stringValue(t.image);
 		ps = theConnection.prepareStatement(sVal);
 		JDBCDisplayUtil.checkNotNull(ps,"prepared statement");
 		currentConnEnv.getSession().addPreparedStatement(i,ps);
 
 		// all we want callers to see are the warnings.
 		SQLWarning w = ps.getWarnings();
 		ps.clearWarnings();
 		return new ijWarningResult(w);
 	}
 }
 
 ijResult
 GetCursorStatement()
 throws SQLException
 :
 {
 	haveConnection();
 	int scrollType = JDBC20Translation.TYPE_FORWARD_ONLY;
 	Token s;
 	Token scrolling = null;
 	Token withtoken = null;
 	int holdType = utilInstance.getHoldability(theConnection);
 	String c;
 	Statement st = null;
 	String sVal;
 	ResultSet rs = null;
 	SQLWarning warns;	
 }
 {
 	<GET> [ scrolling = <SCROLL>  scrollType = scrollType()]
 	[ withtoken = <WITH> holdType = holdType()] <CURSOR> c=identifier() <AS> s=<STRING>
 	{
 		sVal = stringValue(s.image);
 		try {
 			st = utilInstance.createStatement(theConnection, scrollType, holdType);
 			JDBCDisplayUtil.checkNotNull(st,"cursor");
 			st.setCursorName(c);
 			rs = st.executeQuery(sVal);
 			JDBCDisplayUtil.checkNotNull(rs,"cursor");
 			Session sn = currentConnEnv.getSession();
 			sn.addCursorStatement(c,st);
 			sn.addCursor(c,rs);
 		} catch (SQLException e) {
 			if (rs!=null) rs.close();
 			if (st!=null) st.close();
 			throw e;
 		}
 
 		// all we want callers to see are the warnings.
 		SQLWarning w1 = theConnection.getWarnings();
 		SQLWarning w2 = st.getWarnings();
 		SQLWarning w3 = rs.getWarnings();
 		theConnection.clearWarnings();
 		st.clearWarnings();
 		rs.clearWarnings();
 		warns = appendWarnings(w1,w2);
 		return new ijWarningResult(appendWarnings(warns,w3));
 	}
 }
 
 int
 scrollType()
 throws SQLException
 :
 {
 }
 {
 	<INSENSITIVE>
 	{
 		return JDBC20Translation.TYPE_SCROLL_INSENSITIVE;
 	}
 |
 	<SENSITIVE>
 	{
 		return JDBC20Translation.TYPE_SCROLL_SENSITIVE;
 	}
 }	
 
 int
 holdType()
 throws SQLException
 :
 {
 }
 {
 	<HOLD>
 	{
 		return JDBC30Translation.HOLD_CURSORS_OVER_COMMIT;
 	}
 |
 	<NOHOLD>
 	{
 		return JDBC30Translation.CLOSE_CURSORS_AT_COMMIT;
 	}
 }	
 
 ijResult
 AbsoluteStatement()
 throws SQLException
 :
 {
 	int row;
 	String c;
 	ResultSet rs;
 }
 {
 	<ABSOLUTE> row = intLiteral() c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.absolute(rs, row);
 	}
 }
 
 ijResult
 RelativeStatement()
 throws SQLException
 :
 {
 	int row;
 	String c;
 	ResultSet rs;
 }
 {
 	<RELATIVE> row = intLiteral() c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.relative(rs, row);
 	}
 }
 
 ijResult
 BeforeFirstStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<BEFORE> <FIRST> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.beforeFirst(rs);
 	}
 }
 
 ijResult
 FirstStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<FIRST> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.first(rs);
 	}
 }
 
 ijResult
 NextStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<NEXT> c=identifier()
 	{
 		haveConnection();
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return new ijRowResult(rs, rs.next());
 	}
 }
 
 ijResult
 AfterLastStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<AFTER> <LAST> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.afterLast(rs);
 	}
 }
 
 ijResult
 LastStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<LAST> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.last(rs);
 	}
 }
 
 ijResult
 PreviousStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 }
 {
 	<PREVIOUS> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 		return utilInstance.previous(rs);
 	}
 }
 
 ijResult
 GetCurrentRowNumber()
 throws SQLException
 :
 {
 	ResultSet rs;
 	String c;
 }
 {
 	<GETCURRENTROWNUMBER> c=identifier()
 	{
 		haveConnection();
 		// Verify that we have JDBC 2.0
 		Session s = currentConnEnv.getSession();
 		rs = (ResultSet) s.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 
 	LocalizedResource.OutputWriter().println(utilInstance.getCurrentRowNumber(rs));
 		return null;
 	}
 }
 
 ijResult
 CloseStatement()
 throws SQLException
 :
 {
 	String c;
 	ResultSet rs;
 	Statement s;
 }
 {
 	<CLOSE> c=identifier()
 	{
 		haveConnection();
 		Session sn = currentConnEnv.getSession();
 		rs = (ResultSet) sn.getCursor(c);
 		JDBCDisplayUtil.checkNotNull(rs,"cursor");
 		s = (Statement) sn.getCursorStatement(c);
 		JDBCDisplayUtil.checkNotNull(s,"cursor");
 		rs.close();
 		s.close();
 		sn.removeCursor(c);
 		sn.removeCursorStatement(c);
 
 		return null;
 	}
 }
 
 /**
  * Hack to get the grammar to leave a
  * EXECUTE STATEMENT &lt;stmt&gt; alone.  Short
  * circuit the ij EXECUTE built in.
  */
 ijResult JBMSPreparedStatementExec()	
 throws SQLException :
 {
 	Token s = null;
 }
 {
 	<EXECUTE> <STATEMENT> s = <STRING>
 	{
 		return executeImmediate(stringValue(s.image));
 	}
 }
 		
 
 /**
  * Hack to get the grammar to leave a
  * EXECUTE PROCEDURE &lt;procSpec&gt; alone.  Short
  * circuit the ij EXECUTE built in so that
  * we can deploy ij against Foundation2000.
  */
 ijResult F2KExecuteProcedure()	
 throws SQLException :
 {
 	Token s = null;
 }
 {
 	<EXECUTE> <PROCEDURE> s = <STRING>
 	{
 		haveConnection();
 
 		Statement	aStatement = theConnection.createStatement();
 		String		text = "execute procedure " + s;
 
 		aStatement.execute( text );
 
 		return new ijStatementResult( aStatement,true );
 	}
 }
 		
 /**
  * Two forms of execute: immediate, with a string
  * and prepared, with the id of a prepared statement.
  * We expect the latter form will
  * eventually support a USING clause to supply
  * parameter values (that will be constants).
  * No parameters yet, however.
  * <p>
  * Syntax:
  *   EXECUTE statementSource [ USING statementSource] ;
  *
  *	 statementSource is an identifier of a previously prepared statement
  *	 or a string containing SQL-J text.
  */
 ijResult
 ExecuteStatement()
 throws SQLException
 :
 {
 	String i = null;
 	Token s = null;
 	PreparedStatement ps;
 	String sVal = null;
 
 	String iUsing = null;
 	Token sUsing = null;
 	Token	usingObject = null;
 }
 {
 	<EXECUTE> 
 	( i=identifier()
 	| s=<STRING>
 	)
 	( <USING> ( iUsing=identifier()
 			| sUsing=<STRING>
 			)
 	)?
 	{
 	    if (iUsing!=null || sUsing!=null) { // parameters in use
 			String sUsingVal = null;
 			PreparedStatement psUsing;
 			SQLWarning warns = null;
 
 			haveConnection();
 
 			/*
 				Steps:
 				1. find or prepare the statement
 				2. execute the using statement
 				3. push the row of the using statement into the parameters
 				4. execute the statement against those parameters
 				5. clear the parameters
 			 */
 			/*
 				get the prepared statement
 			 */
 			boolean closeWhenDone = false; // will we close the ps when done?
     		if (i!=null) {
 				ps = (PreparedStatement) currentConnEnv.getSession().getPreparedStatement(i);
 				JDBCDisplayUtil.checkNotNull(ps,"prepared statement "+i);
     		}
     		else { // (s!=null)
 				sVal = stringValue(s.image);
 				ps = theConnection.prepareStatement(sVal);
 				closeWhenDone = true;
 				JDBCDisplayUtil.checkNotNull(ps,"prepared statement");
 				warns = appendWarnings(warns, ps.getWarnings());
 				ps.clearWarnings();
     		}
 
 			/*
 				execute the using statement
 			 */
     		if (iUsing!=null) {
 				psUsing = (PreparedStatement) currentConnEnv.getSession().getPreparedStatement(iUsing);
 				JDBCDisplayUtil.checkNotNull(psUsing,"prepared statement "+iUsing);
     		}
     		else { // (sUsing!=null)
 				sUsingVal = stringValue(sUsing.image);
 				psUsing = theConnection.prepareStatement(sUsingVal);
 				JDBCDisplayUtil.checkNotNull(psUsing,"prepared statement");
 				warns = appendWarnings(warns, psUsing.getWarnings());
 				psUsing.clearWarnings();
     		}
 
 			ResultSet rsUsing;
 			/*
 				If the USING statement is not a query, we
 				will not execute the statement; the number of
 				rows controls the execution.
 			 */
 			if (psUsing.execute()) {
 				rsUsing = psUsing.getResultSet();
 
 				/*
 					push the row of the using statement into the parameters
 				 */
 
 				ResultSetMetaData rsmdUsing = rsUsing.getMetaData();
 				int numCols = rsmdUsing.getColumnCount();
 
 				/*
 					Insufficient or too many parameters will
 					be caught at the JDBC level, and halt execution.
 				 */
 				boolean exec = false;
 
 				/* Only do 1 next on rsUsing if autocommit is on,
 				 * since rsUsing will be closed when ps is closed.
 				 */
 			    boolean autoCommited = false;
 				ijMultiResult result = new ijMultiResult(ps,rsUsing,closeWhenDone);
 
 //				while (! autoCommited && rsUsing.next()) {
 //					// note the first time through
 //					if (!exec) {
 //						exec = true;
 //
 //						// send a warning if additional results may be lost
 //						if (theConnection.getAutoCommit()) {
 //							// FIXME: currOut.println("IJ WARNING: Autocommit may close using result set");
 //							autoCommited = true;
 //						}
 //					}
 //					for (int c=1; c<=numCols; c++) {
 //						if (usingObject == null)
 //						{
 //							ps.setObject(c,rsUsing.getObject(c),
 //								rsmdUsing.getColumnType(c));
 //						} 
 //						else
 //						{
 //							ps.setObject(c,rsUsing.getObject(c));
 //						}
 //					}
 //
 //					/*
 //						4. execute the statement against those parameters
 //					 */
 //
 //					ps.execute();
 //					result.addStatementResult(ps);
 //
 //					/*
 //						5. clear the parameters
 //					 */
 //					ps.clearParameters();
 //
 //				}
 //				if (!exec) {
 //					throw ijException.noUsingResults();
 //				}
 //
 //				if (! theConnection.getAutoCommit())
 //				{
 //					rsUsing.close();
 //				}
 //				// REMIND: any way to look for more rsUsing rows if autoCommit?
 //				// perhaps just document the behavior... 
 
 				return result;
 			}
 			else
 				throw ijException.noUsingResults();
 		}
 		else { // no parameters in use
 	    	if (i!=null) {
 				haveConnection();
 				ps = (PreparedStatement) currentConnEnv.getSession().getPreparedStatement(i);
 				JDBCDisplayUtil.checkNotNull(ps,"prepared statement "+i);
 				ps.execute();
 
 				return new ijStatementResult(ps,false);
 	    	}
 	    	else { // (s!=null)
 			    return executeImmediate(stringValue(s.image));
 	    	}
 	    }
 	}
 }
 
 /**
  * Async: like execute immediate, without using,
  * but runs the statement in a separate thread, against
  * the current connection.
  * <p>
  * Syntax:
  *   ASYNC asyncName statementSource 
  *
  *	 statementSource is a string containing SQL-J text.
  */
 ijResult
 AsyncStatement()
 throws SQLException
 :
 {
 	Token s = null;
 	String n = null;
 }
 {
 	<ASYNC> n=identifier() s=<STRING>
 	{
 	    return executeAsync(stringValue(s.image), n);
 	}
 }
 
 /**
  * Wait for: the second half of Async, waits for completion
  * if needed and then supplies the result.  Only execute is done,
  * not row fetching.
  * <p>
  * Syntax:
  *   WAIT FOR asyncName 
  *
  *	 asyncName is a name used in an ASYNC statement previously
  */
 ijResult
 WaitForStatement()
 throws SQLException
 :
 {
 	Token s = null;
 	String n = null;
 }
 {
 	<WAIT> <FOR> n=identifier()
 	{
 		AsyncStatement as = currentConnEnv.getSession().getAsyncStatement(n);
 		if (as == null) throw ijException.noSuchAsyncStatement(n);
 		try {
 		    as.join(); // we wait for it to finish.
 		} catch (InterruptedException ie) {
 			throw ijException.waitInterrupted(ie);
 		}
 		return as.getResult();
 	}
 }
 
 /**
  * RemoveStatement is REMOVE identifier. It identifies
  * a previously prepared statement.  We would prefer a DROP
  * syntax, but SQL-J is using that word and I want to point out
  * that special processing will be needed to give that parser
  * this parser's input for unrecognized text.
  */
 ijResult
 RemoveStatement()
 throws SQLException
 :
 {
 	String i;
 	PreparedStatement ps;
 }
 {
 	<REMOVE> i=identifier()
 	{
 		haveConnection();
 		Session s = currentConnEnv.getSession();
 		ps = (PreparedStatement) s.getPreparedStatement(i);
 		JDBCDisplayUtil.checkNotNull(ps,"prepared statement "+i);
 		ps.close();
 		s.removePreparedStatement(i);
 
 		return null;
 	}
 }
 
 ijResult
 RunStatement()
 throws SQLException
 :
 {
 	Token i;
     Token r = null;
 	PreparedStatement ps;
 }
 {
 	<RUN> 	[r = <RESOURCE>] i=<STRING>
 	{
 		if (utilInstance==null) return null;
 	    if (r == null)
 			utilInstance.newInput(stringValue(i.image));
 		else
             utilInstance.newResourceInput(stringValue(i.image));
 		return null;
     }
 }
 
 /**
  * Autocommit lets you control this aspect of the connection.
  * REMIND: should have a general way to set all connection attributes,
  * this is a shortcut for immediate needs.
  * <p>
  * Syntax:
  *   AUTOCOMMIT [ ON | OFF ] ;
  */
 ijResult
 AutocommitStatement()
 throws SQLException
 :
 {
 	Token on=null;
 }
 {
 	<AUTOCOMMIT> 
 	( on=<ON>
 	| <OFF>
 	)
 	{
 		haveConnection();
 		// REMIND: want to warn if unchanged?
 		theConnection.setAutoCommit((on==null?false:true));
 
 		return null;
 	}
 }
 
 /**
  * By default, holdability is set to true for Connection objects. This syntax NOHOLDFORCONNECTION lets you set it to close cursors at commit.
  * Syntax:
  *   NOHOLDFORCONNECTION ;
  */
 ijResult
 NoHoldForConnectionStatement()
 throws SQLException
 :
 {
 	Token on=null;
 }
 {
 	<NOHOLDFORCONNECTION> 
 	{
 		haveConnection();
 		theConnection = utilInstance.setHoldability(theConnection, JDBC30Translation.CLOSE_CURSORS_AT_COMMIT);
 
 		return null;
 	}
 }
 
 /**
  * Localizeddisplay controls locale sensitive data representayion
  * <p>
  * Syntax:
  *   LOCALIZEDDISPLAY [ ON | OFF ] ;
  */
 ijResult LocalizedDisplay()
 :
 {
  	Token on=null;
 }
 {
 	<LOCALIZEDDISPLAY> 
 	( on=<ON>
 	| <OFF>
 	)
 	{
 		LocalizedResource.enableLocalization((on==null?false:true));
 		return null;
 	}
 }
 
 /**
  * ReadOnly lets you control this aspect of the connection.
  * REMIND: should have a general way to set all connection attributes,
  * this is a shortcut for immediate needs.
  * <p>
  * Syntax:
  *   READONLY [ ON | OFF ] ;
  */
 ijResult
 ReadOnlyStatement()
 throws SQLException
 :
 {
 	Token on=null;
 }
 {
 	<READONLY> 
 	( on=<ON>
 	| <OFF>
 	)
 	{
 		haveConnection();
 		theConnection.setReadOnly((on==null?false:true));
 		return null;
 	}
 }
 
 /**
  * Elapsedtime on causes ij to dump out the elapsed time it takes
  * to run a user statement at the end of that statement.
  * <p>
  * Syntax:
  *   ELAPSEDTIME [ ON | OFF ] ;
  */
 ijResult
 ElapsedTimeStatement() :
 {
 	Token on=null;
 }
 {
 	<ELAPSEDTIME> 
 	( on=<ON>
 	| <OFF>
 	)
 	{
 		elapsedTime = (on != null);
 		return null;
 	}
 }
 
 /**
  * MaximumDisplayWidth EXACT_NUMERIC changes the maximum display width for
  * java.lang.String to the specified EXACT_NUMERIC.
  * This is only used by the console view.
  * <p>
  * Syntax:
  *   MAXIMUMDISPLAYWIDTH INTEGER ;
  */
 ijResult
 MaximumDisplayWidthStatement() :
 {
 	int	  maxWidth;
 }
 {
 	<MAXIMUMDISPLAYWIDTH> maxWidth=intValue()
 	{
 		JDBCDisplayUtil.setMaxDisplayWidth(maxWidth);
 		return null;
 	}
 }
 
 int
 intValue() :
 {
 	Token t;
 }
 {
 	t = <INTEGER>
 	{
 		return Integer.parseInt(t.image);
 	}
 }
 
 /**
  * Bang lets you issue a system command using System.exec.
  * <p>
  * Syntax:
  *   ! 'command to issue' ;
  */
 ijResult
 Bang() :
 {
 	Token cmd=null;
 }
 {
 	<BANG> cmd=<STRING>
 	{
 	  ijResult result = null;
 	  try {
 		Process p = Runtime.getRuntime().exec(stringValue(cmd.image));
 		LocalizedInput in = new LocalizedInput(p.getInputStream());
 		int c;
 		Vector v = new Vector();
 		StringBuffer output = new StringBuffer();
 		// echo output
 		while ((c = in.read()) != -1) {
 			output.append((char)c);
 		}
 		in.close();
 		// echo errors
 		in = new LocalizedInput(p.getErrorStream());
 		// echo output
 		while ((c = in.read()) != -1) {
 			output.append((char)c);
 		}
 		in.close();
 		v.addElement(output);
 		result = new ijVectorResult(v,null);
 		// wait for completion
 		try {
 			p.waitFor();
 		} catch (InterruptedException e) {
 			throw ijException.bangException(e);
 		}
 	  } catch (IOException ioe) {
 		throw ijException.bangException(ioe);
 	  }
 	  return result;
 	}
 }
 
 /**
 	ExpectStatement is EXPECT [ FAIL ] {'String'}* END EXPECT
 	<p>
 	Will eventually detect the lines that the strings are without
 	special literals, but for now this is expedient (except for the
 	doubling of quotes...)
 	<p>
 	Used to test the previous statement's output. Note that ij must be
 	in "expect" mode to use this statement, otherwise it is just
 	ignored.  This is due to the overhead of tracking the prior statement's
 	output.
  */
 ijResult
 ExpectStatement() :
 {
 	Token f = null;
 	Vector stringVector = new Vector();
 }
 {
 	<EXPECT> [ f = <FAIL> ] StringList(stringVector) <END> <EXPECT>
 	{
 		if (!getExpect()) return null; // don't bother processing.
 
 		// FIXME
 
 		// Do the comparison of the string list to the prior rows of
 		// output, using a row-by-row perl-regex comparison.
 		boolean result = true;
 
 		// register the result and whether it should be true or false
 		// FIXME: how to find the expecter??
 		noteExpect(result, f==null);
 
 		return null;
 	}
 }
 
 void
 StringList(Vector v) :
 {
 }
 {
 	StringItem(v) ( StringItem(v) ) *
 }
 
 void
 StringItem(Vector v) :
 {
 	Token s;
 }
 {
 	s = <STRING>
 	{
 		v.addElement(s);
 	}
 }
 
 /**
 	Haven't included: ASYNC, !, EXPECT
 	Don't include: XA_*
  **/
 ijResult
 HelpStatement() :
 {
 }
 {
 	<HELP>
 	{
 		Vector v = new Vector();
 
                 StringTokenizer st = new StringTokenizer(LocalizedResource.getMessage("IJ_HelpText"), "\n");
 		while (st.hasMoreTokens()) {
 		    v.addElement(st.nextToken());
 		}
 							 
 		return new ijVectorResult(v,null);
 	}
 }
 
 String
 identifier() :
 {
 	Token t;
 }
 {
 	t=<IDENTIFIER>
 	{
 		// identifiers are case insensitive, so we map them up.
 		// ij doesn't recognize any use of delimited identifiers in its syntax.
 		return (t.image.toUpperCase(Locale.ENGLISH));
 	}
 }
 
 int
 intLiteral() throws SQLException :
 {
 	String	sign = "";
 	Token	tok;
 }
 {
 	[ sign = sign() ] tok = <INTEGER>
 	{
 		/*
 		** The various java parse utilities can't handle leading +,
 		** so only concatenate leading -.
 		*/
 
 		String num = tok.image;
 
 		if (sign.equals("-"))
 		{
 			num = sign.concat(num);
 		}
 
 		return Integer.parseInt(num);
 	}
 }
 
 Vector
 staticMethodName() throws SQLException :
 {
 	Vector	list = new Vector();
 }
 {
 	methodLeg( list ) ( <PERIOD> methodLeg( list ) )+
 	{
 		return list;		
 	}
 }
 
 void
 methodLeg( Vector list ) throws SQLException :
 {
 	Token	id;
 }
 {
 	id = <IDENTIFIER>
 	{
 		list.addElement( id.image );
 	}
 }
 
 String[]
 staticMethodArgs() throws SQLException :
 {
 	Vector		list = new Vector();
 	String[]	args;
 }
 {
 	<LEFT_PAREN>
 		[ oneStaticArg( list ) ( <COMMA> oneStaticArg( list ) )* ]
 	<RIGHT_PAREN>
 	{
 		args = new String[ list.size() ];
 		list.copyInto( args );
 
 		return args;
 	}
 }
 
 void
 oneStaticArg( Vector list ) throws SQLException :
 {
 	Token	tok;
 }
 {
 	tok = <STRING>
 	{
 		list.addElement( stringValue( tok.image ) );
 	}
 }
 
 
 
 /*
  * <A NAME="sign">sign</A>
  */
 String
 sign() throws SQLException :
 {
 	Token	s;
 }
 {
 	s = <PLUS_SIGN>
 	{
 		return s.image;
 	}
 |
 	s = <MINUS_SIGN>
 	{
 		return s.image;
 	}
 }
 
 /**
 	Undocumented commands to help XA testing.
 
 	This is the grammer for the XA commands
 
 	&lt;XA_DATASOURCE&gt; 'dbname' ( &lt;CREATE&gt; | shutdown ) 
 		 - get a XADataSource whose database name is dbname and make that
 		XADataSource the current XADataSource
 
 	&lt;XA_CONNECT&gt; 	[ &lt;USER&gt; 'user' ]
 			[ &lt;PASSWORD&gt; 'password' ]
 			[ &lt;AS&gt; xaconnid ] 
 		- make an XAConnection using the current XADataSource and make
 		that XAConnection the current XAConnection.  If xaconnid is 
 		given, then associate xaconnid with the XAConnection.  
 		(xaconnid not implemeneted)
 
 
 	&lt;XA_COMMIT&gt;  ( &lt;XA_1PHASE&gt; | &lt;XA_2PHASE&gt; ) xid
 		- commit a global transaction xid
 
 
 	&lt;XA_DISCONNECT&gt; [ xaconnid = identifier() ] 
 		- disconnect an XAConnection.  If xaconnid is given, then
 		disconnect the XAConnection with the given xaconnid. 
 		(xaconnid not implemeneted)
 
 
 	&lt;XA_END&gt; ( &lt;XA_SUSPEND&gt; | &lt;XA_SUCCESS&gt; | &lt;XA_FAIL&gt; ) xid
 		- dissociate a transaction from the current XAConnection or end
 		an already suspened one 
 
 	&lt;XA_FORGET&gt; xid		- forget about a global transaction
 
 	&lt;XA_GETCONNECTION&gt;  [ &lt;AS&gt; connid ] 
 		- get a Connection object from the current XAConnection.
 		If connid is given, then associate connid with the connection.
 		(connid not implemented)
 
 	&lt;XA_PREPARE&gt; xid	- prepare a global transaction
 
 	&lt;XA_RECOVER&gt; ( &lt;XA_NOFLAGS&gt; | &lt;XA_STARTRSCAN&gt; | &lt;XA_ENDRSCAN&gt; )
 	 	- return the list of in-doubt transactions
 
 	&lt;XA_ROLLBACK&gt; xid	- rollback a global transaction
 
 	&lt;XA_START&gt; ( &lt;XA_NOFLAGS&gt; | &lt;XA_JOIN&gt; | &lt;XA_RESUME&gt; ) xid
 		- associate a transaction or start a new global
 		transaction with the current XAConnection.
 
 	The following is for testing other JDBC2.0 ext interface, DataSource
 	and ConnectionPoolDataSource.  Strictly speaking, these are not xa, but
 	their functionality will be lumped into xaHelper because these are here
 	only for testing purposes.
 
 	&lt;DATASOURCE&gt; 'dbname'	[ &lt;PROTOCOL&gt; 'protocol' ]
 				[ &lt;USER&gt; 'user' ]
 				[ &lt;PASSWORD&gt; 'password' ]
 				[ &lt;AS&gt; n=identifier() ]
 		- get a data source whose database name is dbname and make that
 		DataSource the current DataSource.  If &lt;PROTOCOL&gt; is specified,
 		the DataSource may be remote.   Get a connection from that
 		dataSource and use the user/password if specified.
 
 	&lt;CP_DATASOURCE&gt; 'dbname' [ &lt;PROTOCOL&gt; 'protocol' ]
 		- get a connection pool data source whose database name is
 		dbname and make that DataSource the current CPDataSource.  
 		If &lt;PROTOCOL&gt; is specified, the DataSource may be
 		remote.
 
 	&lt;CP_CONNECT&gt;	[ &lt;USER&gt; 'user' ]
 			[ &lt;PASSWORD&gt; 'password' ]
 			[ &lt;AS&gt; cpconnid ]
 		- make a PooledConnection using the current CPDataSource and
 		make that PooledConnection the current PooledConnection.
 		If cpconnid is given, then associate cpconnid with the
 		PooledConnection. (cpconnid not implemented).
 
 	&lt;CP_GETCONNECTION&gt; [ &lt;AS&gt; connid ]
 		- get a Connection object from the current PooledConnection.
 		If connid is given, the associate connid with the connection.
 		(connid not implemented)
 
 	&lt;CP_DISCONNECT&gt; [  cpconnid = identifier() ] 
 		- disconnect a PooledConnection.  If cpconnid is given, then
 		disconnect the PooledConnection with the given cpconnid. 
 		(cpconnid not implemented)
 
 */
 
 
 /**
  * XA_DataSourceStatement is XA_DataSource 'dbname' ( create | shutdown )
  * We new'ed an instance of XADataSource as the current DataSource and set its
  * database name to dbname.
  */
 ijResult
 XA_DataSourceStatement()
 throws SQLException
 :
 {
 	Token dbname = null;
 	Token shut = null;
 	String create = null;
 }
 {
 	<XA_DATASOURCE> dbname=<STRING> 
 		[ ( shut = <SHUTDOWN> | create = identifier() ) ]
 	{
 		xahelper.XADataSourceStatement(this, dbname, shut, create);
 
 		return null;
 	}
 
 }
 
 /**
  * XA_ConnectStatement is XA_CONNECT (&lt;AS&gt; connid)
  * make a XAConnection using the currentXADataSource and make that XAConnection
  * the current XAConnection.  If connid is given, then associate connid with
  * the XAConnection.  This connid is not th xid.
  */
 ijResult
 XA_ConnectStatement()
 throws SQLException
 :
 {
 	Token userT = null;
 	Token passwordT = null;
 	String n = null;
 }
 {
 	<XA_CONNECT>	[ <USER> userT=<STRING> ]
 			[ <PASSWORD> passwordT=<STRING> ]
 			[ <AS> n=identifier() ]
 	{
 		xahelper.XAConnectStatement(this, userT, passwordT, n);
 		return null;
 	}
 }
 
 /**
  * XA_DisconnectStatement is XA_DISCONNECT [xaconnid = identifier()]
  * disconnect the current XAConnection 
  * If xaconnid is given, then disconnect XAConnection with xaconnid (xaconnid
  *	not implemented).
  * 
  */
 ijResult
 XA_DisconnectStatement()
 throws SQLException
 :
 {
 	String n = null;
 }
 {
 	<XA_DISCONNECT>	[ n = identifier() ]
 	{
 		xahelper.XADisconnectStatement(this, n);
 		return null;
 	}
 }
 
 /**
  * XA_CommitStatement is XA_COMMIT [ XA_1PHASE | XA_2PHASE ] xid
  * commits a global transaction xid
  */
 ijResult
 XA_CommitStatement()
 throws SQLException
 :
 {
 	Token onePhase=null;
 	Token twoPhase=null;
 	int xid = 0;
 }
 {
 	<XA_COMMIT> ( onePhase=<XA_1PHASE> | twoPhase=<XA_2PHASE> ) xid=intValue()
 	{
 		xahelper.CommitStatement(this, onePhase, twoPhase, xid);
 		return null;
 	}
 }
 
 /**
  * XA_EndStatement is XA_END [ XA_SUSPEND | XA_SUCCESS | XA_FAIL] xid
  * dissociates a transaction from the current XAConnection or end an already
  * suspended one
  */
 ijResult
 XA_EndStatement()
 throws SQLException
 :
 {
 	int flag = 0;
 	int xid = 0;
 }
 {
 	<XA_END> flag=xatmflag() xid=intValue()
 	{
 		xahelper.EndStatement(this, flag, xid);
 		return null;
 	}
 }
 
 /**
  * XA_ForgetStatement is XA_FORGET xid
  * forgets about a heuristically completed transaction
  */
 ijResult
 XA_ForgetStatement()
 throws SQLException
 :
 {
 	int xid = 0;
 }
 {
 	<XA_FORGET> xid=intValue()
 	{
 		xahelper.ForgetStatement(this, xid);
 		return null;
 	}
 }
 
 
 /**
  * XA_GetConnectionStatement is XA_GETCONNECTION
  * it gets a Connection from the currentXAConnection and uses that as the
  * current connection 
  */
 ijResult
 XA_GetConnectionStatement()
 throws SQLException
 :
 {
 	String n = "XA";
 }
 {
 	<XA_GETCONNECTION>  [ <AS> n=identifier() ]
 	{
 		theConnection = xahelper.XAGetConnectionStatement(this, n);
 		currentConnEnv.addSession(theConnection, n);
 
 		return new ijConnectionResult(theConnection);
 	}
 }
 
 /**
  * XA_PrepareStatement is XA_PREPARE xid
  * prepares a global transaction
  */
 ijResult
 XA_PrepareStatement()
 throws SQLException
 :
 {
 	int xid = 0;
 }
 {
 	<XA_PREPARE> xid = intValue()
 	{
 		xahelper.PrepareStatement(this, xid);
 		return null;
 	}
 }
 
 /**
  * XA_RecoverStatement is XA_RECOVER flag
  * displays the list of prepared transactions
  */
 ijResult
 XA_RecoverStatement()
 throws SQLException
 :
 {
 	int flag = 0;
 }
 {
 	<XA_RECOVER> flag=xatmflag()
 	{
 		return xahelper.RecoverStatement(this, flag);
 	}
 }
 
 /**
  * XA_RollbackStatement is XA_Rollback xid
  * rolls back a global transaction
  */
 ijResult
 XA_RollbackStatement()
 throws SQLException
 :
 {
 	int xid = 0;
 }
 {
 	<XA_ROLLBACK> xid = intValue()
 	{
 		xahelper.RollbackStatement(this, xid);
 		return null;
 	}
 }
 
 
 /**
  * XA_StartStatement is XA_START [ XA_NOFLAGS | XA_JOIN | XA_RESUME ] xid
  * start or associates a transaction with the current XAConnection
  */
 ijResult XA_StartStatement() throws SQLException
 :
 {
 	int flag = 0;
 	int xid = 0;
 }
 {
 	<XA_START> flag=xatmflag() xid=intValue()
 	{
 		xahelper.StartStatement(this, flag, xid);
 		return null;
 	}
 }
 
 int
 xatmflag()
 throws SQLException
 :
 {
 }
 {
 	<XA_ENDRSCAN>
 	{
 		return JDBC20Translation.XA_ENDRSCAN;
 	}
 |
 	<XA_FAIL>
 	{
 		return JDBC20Translation.XA_FAIL;
 	}
 |
 	<XA_JOIN>
 	{
 		return JDBC20Translation.XA_JOIN;
 	}
 |
 	<XA_NOFLAGS>
 	{
 		return JDBC20Translation.XA_NOFLAGS;
 	}
 |
 	<XA_RESUME>
 	{
 		return JDBC20Translation.XA_RESUME;
 	}
 |	
 	<XA_STARTRSCAN>
 	{
 		return JDBC20Translation.XA_STARTRSCAN;
 	}
 |
 	<XA_SUCCESS>
 	{
 		return JDBC20Translation.XA_SUCCESS;
 	}
 |
 	<XA_SUSPEND>
 	{
 		return JDBC20Translation.XA_SUSPEND;
 	}
 
 }
 
 
 /**
  * DataSourceStatement is 
  *	DataSource 'dbname' 
  *		[ &lt;PROTCOL&gt; 'protocol']
  *		[ &lt;USER&gt; 'user' ]
  *		[ &lt;PASSWORD&gt; 'password' ]
  *		[ &lt;AS&gt; n=identifier() ]
  *
  * We new'ed an instance of DataSource as the current DataSource and set its
  * database name to dbname.  Also get a connection
  */
 ijResult DataSourceStatement() throws SQLException
 :
 {
 	Token dbname = null;
 	Token protocol = null;
 	Token userT = null;
 	Token passwordT = null;
 	String n = null;
 }
 {
 	<DATASOURCE> dbname=<STRING>	[ <PROTOCOL> protocol=<STRING> ]
 					[ <USER> userT=<STRING> ]
 					[ <PASSWORD> passwordT=<STRING> ]
 					[ <AS> n = identifier() ]
 	{
 
 		theConnection = xahelper.DataSourceStatement(this, dbname, protocol,
 			userT, passwordT, n);
 
 		return addSession( theConnection, n );
 	}
 
 }
 
 /**
  * CP_DataSourceStatement is
  *	CP_DataSource 'dbname' [ &lt;PROTOCOL&gt; 'protocol' ]
  *		- get a connection pool data source whose database name is
  *		dbname and make that DataSource the current CPDataSource.  
  *		If &lt;PROTOCOL&gt; is specified, the DataSource may be
  *		remote.
  */
 ijResult CP_DataSourceStatement() throws SQLException
 :
 {
 	Token dbname = null;
 	Token protocol = null;
 }
 {
 	<CP_DATASOURCE> dbname=<STRING> [ <PROTOCOL> protocol=<STRING> ]
 	{
 		xahelper.CPDataSourceStatement(this, dbname, protocol);
 		return null;
 	}
 }
 
 /**
  * CP_ConnectStatement is
  *	&lt;CP_CONNECT&gt;	[ &lt;USER&gt; 'user' ]
  *			[ &lt;PASSWORD&gt; 'password' ]
  *			[ &lt;AS&gt; cpconnid ]
  * make a PooledConnection using the current CPDataSource and
  * make that PooledConnection the current PooledConnection.
  * If cpconnid is given, then associate cpconnid with the
  * PooledConnection. (cpconnid not implemented).
  */
 ijResult CP_ConnectStatement() throws SQLException
 :
 {
 	Token userT = null;
 	Token passwordT = null;
 	String n = null;
 }
 {
 	<CP_CONNECT>	[ <USER> userT=<STRING> ]
 			[ <PASSWORD> passwordT=<STRING> ]
 			[ <AS> n = identifier() ]
 	{
 		xahelper.CPConnectStatement(this, userT, passwordT, n);
 		return null;
 	}
 }
 
 /**
  * CP_GetConnectionStatement is
  *	&lt;CP_GETCONNECTION&gt; [ &lt;AS&gt; connid ]
  * get a Connection object from the current PooledConnection.
  * If connid is given, the associate connid with the connection.
  * (connid not implemented)
  */
 ijResult CP_GetConnectionStatement() throws SQLException
 :
 {
 	String n = "Pooled";
 }
 {
 	<CP_GETCONNECTION> [ <AS> n=identifier() ]
 	{
 		theConnection = xahelper.CPGetConnectionStatement(this, n);
 		currentConnEnv.addSession(theConnection, n);
 		return new ijConnectionResult(theConnection);
 	}
 }
 
 /**
  * CP_DisconnectStatement is
  *	&lt;CP_DISCONNECT&gt; [ cpconnid = identifier() ]
  * disconnect a PooledConnection.  If cpconnid is given, then
  * disconnect the PooledConnection with the given cpconnid. 
  * (cpconnid not implemented)
  */
 ijResult CP_DisconnectStatement() throws SQLException
 :
 {
 	String n = null;
 }
 {
 	<CP_DISCONNECT> [ n = identifier() ]
 	{
 		xahelper.CPDisconnectStatement(this, n);
 		return null;
 	}
 
 }
 
 
 
 Properties
 	attributeList() :
 {
 	Properties properties = new Properties();
 	Token tok;
 	String value;
 }
 {
 
 	[ property(properties) (<COMMA> property(properties) )* ]
 
 	{
 		//properties.put(tok.image,value);
 		return properties;	
 	}
 
 }
 
 void
 property(Properties properties)  :
 {
 	String key;
 	String value;
 }
 {
 	key = caseSensitiveIdentifierOrKeyword() 
 		<EQUALS_OPERATOR> value = caseSensitiveIdentifierOrKeyword() 
 	{
 		properties.put(key, value);
 	}
 }
 
 
 String
 caseSensitiveIdentifierOrKeyword () :
 {
 	String value=null;
 	Token tok;
 }
 {
 	value = keyword()
 	{
 		return value;
 	}
 |
 	tok = <IDENTIFIER>
 	{
 		return tok.image;
 	}
 
 
 }
 
 String
 caseSensitiveIdentifier() :
 {
 	Token tok;
 }
 {
 	tok = <IDENTIFIER>
 	{
 		return tok.image;
 	}
 
 }
 
 String 
 keyword() :
 {
 	Token tok;
 	String value= null;
 }
 {
 	(
 	tok = <ABSOLUTE>
 |	tok = <AFTER>
 |	tok = <ALIASES>
 |	tok = <ALL>
 |	tok = <AS>
 |	tok = <ASYNC>
 |	tok = <ATTRIBUTES>
 |	tok = <AUTOCOMMIT>
 |	tok = <BANG>
 |	tok = <BEFORE>
 |	tok = <CLOSE>
 | 	tok = <COMMIT>
 | 	tok = <CONNECT>
 | 	tok = <CONNECTION>
 | 	tok = <CONNECTIONS>
 |	tok = <CURRENT>
 |	tok = <CURSOR>
 |	tok = <DESCRIBE>
 |	tok = <DISCONNECT>
 |	tok = <DRIVER>
 |	tok = <ELAPSEDTIME>
 |	tok = <END>
 |	tok = <EXECUTE>
 |	tok = <EXIT>
 |	tok = <EXPECT>
 |	tok = <FAIL>
 |	tok = <FIRST>
 |	tok = <FOR>
 |	tok = <FROM>
 |	tok = <GET>
 |	tok = <GETCURRENTROWNUMBER>
 |	tok = <HOLD>
 |	tok = <HELP>
 |	tok = <IN>
 |	tok = <INDEXES>
 |	tok = <INSENSITIVE>
 |	tok = <INTO>
 |	tok = <LAST>
 |	tok = <LOCALIZEDDISPLAY>
 |	tok = <MAXIMUMDISPLAYWIDTH>
 |	tok = <NAME>
 |	tok = <NEXT>
 |	tok = <NOHOLD>
 |	tok = <NOHOLDFORCONNECTION>
 |	tok = <OFF>
 |	tok = <ON>
 |	tok = <PASSWORD>
 |	tok = <PERIOD>
 |	tok = <PREPARE>
 |	tok = <PREVIOUS>
 |	tok = <PROCEDURE>
 |	tok = <PROCEDURES>
 |	tok = <PROPERTIES>
 |	tok = <PROTOCOL>
 |	tok = <QUIT>
 |	tok = <READONLY>
 |	tok = <RELATIVE>
 |	tok = <REMOVE>
 |	tok = <RESOURCE>
 |	tok = <ROLLBACK>
 |	tok = <RUN>
 |	tok = <TO>
 |	tok = <SCHEMAS>
 |	tok = <SCROLL>
 |	tok = <SENSITIVE>
 |	tok = <SET>
 |	tok = <SHOW>
 |	tok = <SHUTDOWN>
 |	tok = <STATEMENT>
 |	tok = <SYNONYMS>
 |	tok = <TABLES>
 |	tok = <USER>
 |	tok = <USING>
 |	tok = <VIEWS>
 |	tok = <WAIT>
 |	tok = <WITH>
 |	tok = <XA_1PHASE>
 |	tok = <XA_2PHASE>
 |	tok = <XA_DATASOURCE>
 |	tok = <XA_CONNECT>
 |	tok = <XA_COMMIT>
 |	tok = <XA_DISCONNECT>
 |	tok = <XA_END>
 |	tok = <XA_ENDRSCAN>
 |	tok = <XA_FAIL>
 |	tok = <XA_FORGET>
 |	tok = <XA_GETCONNECTION>
 |	tok = <XA_JOIN>
 |	tok = <XA_NOFLAGS>
 |	tok = <XA_PREPARE>
 |	tok = <XA_RECOVER>
 |	tok = <XA_RESUME>
 |	tok = <XA_ROLLBACK>
 |	tok = <XA_START>
 |	tok = <XA_STARTRSCAN>
 |	tok = <XA_SUCCESS>
 |	tok = <XA_SUSPEND>
 |	tok = <DATASOURCE>
 |	tok = <CP_DATASOURCE>
 |	tok = <CP_CONNECT>
 |	tok = <CP_GETCONNECTION>
 |	tok = <CP_DISCONNECT>
 |	tok = <WORK>
 	)
 	{
 
 		return tok.image;
 
 	}
 }