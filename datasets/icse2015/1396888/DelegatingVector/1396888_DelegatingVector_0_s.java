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
 
 package org.apache.mahout.math;
 
 import org.apache.mahout.math.function.DoubleDoubleFunction;
 import org.apache.mahout.math.function.DoubleFunction;
 
 import java.util.Iterator;
 
 /**
  * A delegating vector provides an easy way to decorate vectors with weights or id's and such while
  * keeping all of the Vector functionality.
  *
  * This vector implements LengthCachingVector because almost all delegates cache the length and
  * the cost of false positives is very low.
  */
 public class DelegatingVector implements Vector, LengthCachingVector {
   protected Vector delegate;
 
  public DelegatingVector(int size) {
    delegate = new DenseVector(size);
  }

   public DelegatingVector(Vector v) {
     delegate = v;
   }
 
   public Vector getVector() {
     return delegate;
   }
 
   @Override
   public double aggregate(DoubleDoubleFunction aggregator, DoubleFunction map) {
     return delegate.aggregate(aggregator, map);
   }
 
   @Override
   public double aggregate(Vector other, DoubleDoubleFunction aggregator, DoubleDoubleFunction combiner) {
     return delegate.aggregate(other, aggregator, combiner);
   }
 
   @Override
   public Vector viewPart(int offset, int length) {
     return delegate.viewPart(offset, length);
   }
 
   @Override
   public Vector clone() {
     WeightedVector r;
     try {
       r = (WeightedVector) super.clone();
     } catch (CloneNotSupportedException e) {
       throw new RuntimeException("Clone not supported for DelegatingVector, shouldn't be possible");
     }
     r.delegate = delegate.clone();
     return r;
   }
 
   @Override
   public Vector divide(double x) {
     return delegate.divide(x);
   }
 
   @Override
   public double dot(Vector x) {
     return delegate.dot(x);
   }
 
   @Override
   public double get(int index) {
     return delegate.get(index);
   }
 
   @Override
   public Element getElement(int index) {
     return delegate.getElement(index);
   }
 
   @Override
   public Vector minus(Vector that) {
     return delegate.minus(that);
   }
 
   @Override
   public Vector normalize() {
     return delegate.normalize();
   }
 
   @Override
   public Vector normalize(double power) {
     return delegate.normalize(power);
   }
 
   @Override
   public Vector logNormalize() {
     return delegate.logNormalize();
   }
 
   @Override
   public Vector logNormalize(double power) {
     return delegate.logNormalize(power);
   }
 
   @Override
   public double norm(double power) {
     return delegate.norm(power);
   }
 
   @Override
   public double getLengthSquared() {
     return delegate.getLengthSquared();
   }
 
  // not normally called because the delegate vector is who would need this and
  // they will call their own version of this method.  In fact, if the delegate is
  // also a delegating vector the same logic will apply recursively down to the first
  // non-delegating vector.  This makes this very hard to test except in trivial ways.
   @Override
  public void setLengthSquared(double d2) {
     if (delegate instanceof LengthCachingVector) {
      ((LengthCachingVector) delegate).setLengthSquared(d2);
     }
   }
 
   @Override
   public double getDistanceSquared(Vector v) {
     return delegate.getDistanceSquared(v);
   }
 
   @Override
   public double maxValue() {
     return delegate.maxValue();
   }
 
   @Override
   public int maxValueIndex() {
     return delegate.maxValueIndex();
   }
 
   @Override
   public double minValue() {
     return delegate.minValue();
   }
 
   @Override
   public int minValueIndex() {
     return delegate.minValueIndex();
   }
 
   @Override
   public Vector plus(double x) {
     return delegate.plus(x);
   }
 
   @Override
   public Vector plus(Vector x) {
     return delegate.plus(x);
   }
 
   @Override
   public void set(int index, double value) {
     delegate.set(index, value);
   }
 
   @Override
   public Vector times(double x) {
     return delegate.times(x);
   }
 
   @Override
   public Vector times(Vector x) {
     return delegate.times(x);
   }
 
   @Override
   public double zSum() {
     return delegate.zSum();
   }
 
   @Override
   public Vector assign(double value) {
     delegate.assign(value);
     return this;
   }
 
   @Override
   public Vector assign(double[] values) {
     delegate.assign(values);
     return this;
   }
 
   @Override
   public Vector assign(Vector other) {
     delegate.assign(other);
     return this;
   }
 
   @Override
   public Vector assign(DoubleDoubleFunction f, double y) {
     delegate.assign(f, y);
     return this;
   }
 
   @Override
   public Vector assign(DoubleFunction function) {
     delegate.assign(function);
     return this;
   }
 
   @Override
   public Vector assign(Vector other, DoubleDoubleFunction function) {
     delegate.assign(other, function);
     return this;
   }
 
   @Override
   public Matrix cross(Vector other) {
     return delegate.cross(other);
   }
 
   @Override
   public int size() {
     return delegate.size();
   }
 
   @Override
   public String asFormatString() {
     return delegate.asFormatString();
   }
 
   @Override
   public int hashCode() {
     return delegate.hashCode();
   }
 
   @Override
   public boolean equals(Object o) {
     return delegate.equals(o);
   }
 
   @Override
   public String toString() {
     return delegate.toString();
   }
 
   @Override
   public boolean isDense() {
     return delegate.isDense();
   }
 
   @Override
   public boolean isSequentialAccess() {
     return delegate.isSequentialAccess();
   }
 
   @Override
   public double getQuick(int index) {
     return delegate.getQuick(index);
   }
 
   @Override
   public Vector like() {
    return delegate.like();
   }
 
   @Override
   public void setQuick(int index, double value) {
     delegate.setQuick(index, value);
   }
 
   @Override
   public int getNumNondefaultElements() {
     return delegate.getNumNondefaultElements();
   }
 
   @Override
   public Iterator<Element> iterateNonZero() {
     return delegate.iterateNonZero();
   }
 
   @Override
   public Iterator<Element> iterator() {
     return delegate.iterator();
   }
 }
