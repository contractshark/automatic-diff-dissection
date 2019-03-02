 /**
  * Autogenerated by Thrift
  *
  * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
  */
 package org.apache.cassandra.thrift;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */

 
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Map;
 import java.util.HashMap;
 import java.util.EnumMap;
 import java.util.Set;
 import java.util.HashSet;
 import java.util.EnumSet;
 import java.util.Collections;
 import java.util.BitSet;
 import java.util.Arrays;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import org.apache.thrift.*;
 import org.apache.thrift.meta_data.*;
 import org.apache.thrift.protocol.*;
 
 /**
  * RPC timeout was exceeded.  either a node failed mid-operation, or load was too high, or the requested op was too large.
  */
 public class TimedOutException extends Exception implements TBase<TimedOutException._Fields>, java.io.Serializable, Cloneable, Comparable<TimedOutException> {
   private static final TStruct STRUCT_DESC = new TStruct("TimedOutException");
 
 
 
   /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
   public enum _Fields implements TFieldIdEnum {
 ;
 
     private static final Map<Integer, _Fields> byId = new HashMap<Integer, _Fields>();
     private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
     static {
       for (_Fields field : EnumSet.allOf(_Fields.class)) {
         byId.put((int)field._thriftId, field);
         byName.put(field.getFieldName(), field);
       }
     }
 
     /**
      * Find the _Fields constant that matches fieldId, or null if its not found.
      */
     public static _Fields findByThriftId(int fieldId) {
       return byId.get(fieldId);
     }
 
     /**
      * Find the _Fields constant that matches fieldId, throwing an exception
      * if it is not found.
      */
     public static _Fields findByThriftIdOrThrow(int fieldId) {
       _Fields fields = findByThriftId(fieldId);
       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
       return fields;
     }
 
     /**
      * Find the _Fields constant that matches name, or null if its not found.
      */
     public static _Fields findByName(String name) {
       return byName.get(name);
     }
 
     private final short _thriftId;
     private final String _fieldName;
 
     _Fields(short thriftId, String fieldName) {
       _thriftId = thriftId;
       _fieldName = fieldName;
     }
 
     public short getThriftFieldId() {
       return _thriftId;
     }
 
     public String getFieldName() {
       return _fieldName;
     }
   }
   public static final Map<_Fields, FieldMetaData> metaDataMap = Collections.unmodifiableMap(new EnumMap<_Fields, FieldMetaData>(_Fields.class) {{
   }});
 
   static {
     FieldMetaData.addStructMetaDataMap(TimedOutException.class, metaDataMap);
   }
 
   public TimedOutException() {
   }
 
   /**
    * Performs a deep copy on <i>other</i>.
    */
   public TimedOutException(TimedOutException other) {
   }
 
   public TimedOutException deepCopy() {
     return new TimedOutException(this);
   }
 
   @Deprecated
   public TimedOutException clone() {
     return new TimedOutException(this);
   }
 
   public void setFieldValue(_Fields field, Object value) {
     switch (field) {
     }
   }
 
   public void setFieldValue(int fieldID, Object value) {
     setFieldValue(_Fields.findByThriftIdOrThrow(fieldID), value);
   }
 
   public Object getFieldValue(_Fields field) {
     switch (field) {
     }
     throw new IllegalStateException();
   }
 
   public Object getFieldValue(int fieldId) {
     return getFieldValue(_Fields.findByThriftIdOrThrow(fieldId));
   }
 
   /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
   public boolean isSet(_Fields field) {
     switch (field) {
     }
     throw new IllegalStateException();
   }
 
   public boolean isSet(int fieldID) {
     return isSet(_Fields.findByThriftIdOrThrow(fieldID));
   }
 
   @Override
   public boolean equals(Object that) {
     if (that == null)
       return false;
     if (that instanceof TimedOutException)
       return this.equals((TimedOutException)that);
     return false;
   }
 
   public boolean equals(TimedOutException that) {
     if (that == null)
       return false;
 
     return true;
   }
 
   @Override
   public int hashCode() {
     return 0;
   }
 
   public int compareTo(TimedOutException other) {
     if (!getClass().equals(other.getClass())) {
       return getClass().getName().compareTo(other.getClass().getName());
     }
 
     int lastComparison = 0;
     TimedOutException typedOther = (TimedOutException)other;
 
     return 0;
   }
 
   public void read(TProtocol iprot) throws TException {
     TField field;
     iprot.readStructBegin();
     while (true)
     {
       field = iprot.readFieldBegin();
       if (field.type == TType.STOP) { 
         break;
       }
       switch (field.id) {
         default:
           TProtocolUtil.skip(iprot, field.type);
       }
       iprot.readFieldEnd();
     }
     iprot.readStructEnd();
 
     // check for required fields of primitive type, which can't be checked in the validate method
     validate();
   }
 
   public void write(TProtocol oprot) throws TException {
     validate();
 
     oprot.writeStructBegin(STRUCT_DESC);
     oprot.writeFieldStop();
     oprot.writeStructEnd();
   }
 
   @Override
   public String toString() {
     StringBuilder sb = new StringBuilder("TimedOutException(");
     boolean first = true;
 
     sb.append(")");
     return sb.toString();
   }
 
   public void validate() throws TException {
     // check for required fields
   }
 
 }
 
