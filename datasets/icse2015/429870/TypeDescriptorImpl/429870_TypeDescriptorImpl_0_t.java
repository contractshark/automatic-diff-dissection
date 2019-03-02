 /*
 
    Derby - Class org.apache.derby.catalog.types.TypeDescriptorImpl
 
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
  */
 
 package org.apache.derby.catalog.types;
 
 import org.apache.derby.iapi.services.io.StoredFormatIds;
 import org.apache.derby.iapi.services.io.Formatable;
 
 import org.apache.derby.catalog.TypeDescriptor;
 
 import java.io.ObjectOutput;
 import java.io.ObjectInput;
 import java.io.IOException;
 import java.sql.Types;
                              
 public class TypeDescriptorImpl implements TypeDescriptor, Formatable
 {
 	/********************************************************
 	**
 	**	This class implements Formatable. That means that it
 	**	can write itself to and from a formatted stream. If
 	**	you add more fields to this class, make sure that you
 	**	also write/read them with the writeExternal()/readExternal()
 	**	methods.
 	**
 	**	If, inbetween releases, you add more fields to this class,
 	**	then you should bump the version number emitted by the getTypeFormatId()
 	**	method.
 	**
 	********************************************************/
 
 	private BaseTypeIdImpl		typeId;
 	private int						precision;
 	private int						scale;
 	private boolean					isNullable;
 	private int						maximumWidth;
 
 	/**
 	 * Public niladic constructor. Needed for Formatable interface to work.
 	 *
 	 */
     public	TypeDescriptorImpl() {}
 
 	/**
 	 * Constructor for use with numeric types
 	 *
 	 * @param typeId	The typeId of the type being described
 	 * @param precision	The number of decimal digits.
 	 * @param scale		The number of digits after the decimal point.
 	 * @param isNullable	TRUE means it could contain NULL, FALSE means
 	 *			it definitely cannot contain NULL.
 	 * @param maximumWidth	The maximum number of bytes for this datatype
 	 */
 	public TypeDescriptorImpl(
 		BaseTypeIdImpl typeId,
 		int precision,
 		int scale,
 		boolean isNullable,
 		int maximumWidth)
 	{
 		this.typeId = typeId;
 		this.precision = precision;
 		this.scale = scale;
 		this.isNullable = isNullable;
 		this.maximumWidth = maximumWidth;
 	}
 
 	/**
 	 * Constructor for use with non-numeric types
 	 *
 	 * @param typeId	The typeId of the type being described
 	 * @param isNullable	TRUE means it could contain NULL, FALSE means
 	 *			it definitely cannot contain NULL.
 	 * @param maximumWidth	The maximum number of bytes for this datatype
 	 */
 	public TypeDescriptorImpl(
 		BaseTypeIdImpl typeId,
 		boolean isNullable,
 		int maximumWidth)
 	{
 		this.typeId = typeId;
 		this.isNullable = isNullable;
 		this.maximumWidth = maximumWidth;
 	}
 
 	/**
 	 * Constructor for internal uses only.  
 	 * (This is useful when the precision and scale are potentially wider than
 	 * those in the source, like when determining the dominant data type.)
 	 *
 	 * @param source	The DTSI to copy
 	 * @param precision	The number of decimal digits.
 	 * @param scale		The number of digits after the decimal point.
 	 * @param isNullable	TRUE means it could contain NULL, FALSE means
 	 *			it definitely cannot contain NULL.
 	 * @param maximumWidth	The maximum number of bytes for this datatype
 	 */
 	public TypeDescriptorImpl(
 		TypeDescriptorImpl source, 
 		int precision,
 		int scale,
 		boolean isNullable,
 		int maximumWidth)
 	{
 		this.typeId = source.typeId;
 		this.precision = precision;
 		this.scale = scale;
 		this.isNullable = isNullable;
 		this.maximumWidth = maximumWidth;
 	}
 
 	/**
 	 * Constructor for internal uses only
 	 *
 	 * @param source	The DTSI to copy
 	 * @param isNullable	TRUE means it could contain NULL, FALSE means
 	 *			it definitely cannot contain NULL.
 	 * @param maximumWidth	The maximum number of bytes for this datatype
 	 */
 	public TypeDescriptorImpl(
 		TypeDescriptorImpl source,
 		boolean isNullable,
 		int maximumWidth)
 	{
 		this.typeId = source.typeId;
 		this.precision = source.precision;
 		this.scale = source.scale;
 		this.isNullable = isNullable;
 		this.maximumWidth = maximumWidth;
 	}
 
 	/**
 	 * @see TypeDescriptor#getMaximumWidth
 	 */
 	public int	getMaximumWidth()
 	{
 		return maximumWidth;
 	}
 
 	/**
 	 * Return the length of this type in bytes.  Note that
 	 * while the JDBC API _does_ define a need for
 	 * returning length in bytes of a type, it doesn't
 	 * state clearly what that means for the various
 	 * types.  We assume therefore that the values here
 	 * are meant to match those specified by the ODBC
 	 * specification (esp. since ODBC clients are more
 	 * likely to need this value than a Java client).
 	 * The ODBC spec that defines the values we use here
 	 * can be found at the following link:
 	 * 
 	 * http://msdn.microsoft.com/library/default.asp?url=/library/
 	 * en-us/odbc/htm/odbctransfer_octet_length.asp
 	 *
 	 * @see TypeDescriptor#getMaximumWidthInBytes
 	 */
 	public int	getMaximumWidthInBytes()
 	{
 		switch (typeId.getJDBCTypeId()) {
 
 			case Types.BIT:
 			case Types.TINYINT:
 			case Types.SMALLINT:
 			case Types.INTEGER:
 			case Types.REAL:
 			case Types.DOUBLE:
 			case Types.FLOAT:
 			case Types.BINARY:
 			case Types.VARBINARY:
 			case Types.LONGVARBINARY:
 			case Types.BLOB:
 
 				// For all of these, just take the maximumWidth,
 				// since that already holds the length in bytes.
 				return maximumWidth;
 
 			// For BIGINT values, ODBC spec says to return
 			// 40 because max length of a C/C++ BIGINT in
 			// string form is 20 and we assume the client
 			// character set is Unicode (spec says to
 			// multiply by 2 for unicode).
 			case Types.BIGINT:
 				return 40;
 
 			// ODBC spec explicitly declares what the lengths
 			// should be for datetime values, based on the
 			// declared fields of SQL_DATE_STRUCT, SQL_TIME_STRUCT,
 			// and SQL_TIMESTAMP_STRUCT.  So we just use those
 			// values.
 			case Types.DATE:
 			case Types.TIME:
 				return 6;
 
 			case Types.TIMESTAMP:
 				return 16;
 
 			// ODBC spec says that for numeric/decimal values,
 			// we should use max number of digits plus 2
 			// (for sign and decimal point), since that's
 			// the length of a decimal value in string form.
 			// And since we assume client character set
 			// is unicode, we have to multiply by 2 to
 			// get the number of bytes.
 			case Types.NUMERIC:
 			case Types.DECIMAL:
 				return 2 * (precision + 2);
 
 			// ODBC spec says to use length in chars
 			// for character types, times two if we
 			// assume client character set is unicode.
 			// If 2 * character length is greater than
 			// variable type (in this case, integer),
 			// then we return the max value for an
 			// integer.
 			case Types.CHAR:
 			case Types.VARCHAR:
 			case Types.LONGVARCHAR:
 			case Types.CLOB:
 				if ((maximumWidth > 0) && (2 * maximumWidth < 0))
 				// integer overflow; return max integer possible.
 					return Integer.MAX_VALUE;
 				else
 					return 2 * maximumWidth;
 
 			case Types.ARRAY:
 			case Types.DISTINCT:
 			case Types.NULL:
 			case Types.OTHER:
 			case Types.REF:
 			case Types.STRUCT:
 			case Types.JAVA_OBJECT:
 			default:
 
 				// For these we don't know, so return the "don't-know"
 				// indicator.
 				return -1;
 
 		}
 
 	}
 
 	/**
 	 * Get the jdbc type id for this type.  JDBC type can be
 	 * found in java.sql.Types. 
 	 *
 	 * @return	a jdbc type, e.g. java.sql.Types.DECIMAL 
 	 *
 	 * @see Types
 	 */
 	public int getJDBCTypeId()
 	{
 		return typeId.getJDBCTypeId();
 	}
 
 	/**
 	 * Gets the name of this datatype.
 	 * 
 	 *
 	 *  @return	the name of this datatype
 	 */
 	public	String		getTypeName()
 	{
 		return typeId.getSQLTypeName();
 	}
 
 	/**
 	 * Returns the number of decimal digits for the datatype, if applicable.
 	 *
 	 * @return	The number of decimal digits for the datatype.  Returns
 	 *		zero for non-numeric datatypes.
 	 */
 	public int	getPrecision()
 	{
 		return precision;
 	}
 
 	/**
 	 * Returns the number of digits to the right of the decimal for
 	 * the datatype, if applicable.
 	 *
 	 * @return	The number of digits to the right of the decimal for
 	 *		the datatype.  Returns zero for non-numeric datatypes.
 	 */
 	public int	getScale()
 	{
 		return scale;
 	}
 
 	/**
 	 * Returns TRUE if the datatype can contain NULL, FALSE if not.
 	 * JDBC supports a return value meaning "nullability unknown" -
 	 * I assume we will never have columns where the nullability is unknown.
 	 *
 	 * @return	TRUE if the datatype can contain NULL, FALSE if not.
 	 */
 	public boolean	isNullable()
 	{
 		return isNullable;
 	}
 
 	/**
 	 * Set the nullability of the datatype described by this descriptor
 	 *
 	 * @param nullable	TRUE means set nullability to TRUE, FALSE
 	 *					means set it to FALSE
 	 */
 	public void setNullability(boolean nullable)
 	{
 		isNullable = nullable;
 	}
 
 	/**
 	 * Converts this data type descriptor (including length/precision)
 	 * to a string. E.g.
 
 
 	 *
 	 *			VARCHAR(30)
 	 *
 	 *	or
 	 *
 	 *			 java.util.Hashtable 
 	 *
 	 * @return	String version of datatype, suitable for running through
 	 *			the Parser.
 	 */
 	public String	getSQLstring()
 	{
 		return typeId.toParsableString( this );
 	}
 
 	public String	toString()
 	{
 		String s = getSQLstring();
 		if (!isNullable())
 			return s + " NOT NULL";
 		return s;
 	}
 
 	/**
 	 * Get the type Id stored within this type descriptor.
 	 */
 	public BaseTypeIdImpl getTypeId()
 	{
 		return typeId;
 	}
 
 	/**
 	  Compare if two TypeDescriptors are exactly the same
 	  @param object the dataTypeDescriptor to compare to.
 	  */
 	public boolean equals(Object object)
 	{
 		TypeDescriptor typeDescriptor = (TypeDescriptor)object;
 
 		if(!this.getTypeName().equals(typeDescriptor.getTypeName()) ||
 		   this.precision != typeDescriptor.getPrecision() ||
 		   this.scale != typeDescriptor.getScale() ||
 		   this.isNullable != typeDescriptor.isNullable() ||
 		   this.maximumWidth != typeDescriptor.getMaximumWidth())
 		   return false;
 	    else
 			return true;
 	}						   
 
 	// Formatable methods
 
 	/**
 	 * Read this object from a stream of stored objects.
 	 *
 	 * @param in read this.
 	 *
 	 * @exception IOException					thrown on error
 	 * @exception ClassNotFoundException		thrown on error
 	 */
 	public void readExternal( ObjectInput in )
 		 throws IOException, ClassNotFoundException
 	{
 		typeId = (BaseTypeIdImpl) in.readObject();
 		precision = in.readInt();
 		scale = in.readInt();
 		isNullable = in.readBoolean();
 		maximumWidth = in.readInt();
 	}
 
 	/**
 	 * Write this object to a stream of stored objects.
 	 *
 	 * @param out write bytes here.
 	 *
 	 * @exception IOException		thrown on error
 	 */
 	public void writeExternal( ObjectOutput out )
 		 throws IOException
 	{
 		out.writeObject( typeId );
 		out.writeInt( precision );
 		out.writeInt( scale );
 		out.writeBoolean( isNullable );
 		out.writeInt( maximumWidth );
 	}
  
 	/**
 	 * Get the formatID which corresponds to this class.
 	 *
 	 *	@return	the formatID of this class
 	 */
 	public	int	getTypeFormatId()	{ return StoredFormatIds.DATA_TYPE_IMPL_DESCRIPTOR_V01_ID; }
 }
