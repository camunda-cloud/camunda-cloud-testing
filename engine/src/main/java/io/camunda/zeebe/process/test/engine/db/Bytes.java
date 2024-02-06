/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.db;

import com.google.common.primitives.UnsignedBytes;
import io.camunda.zeebe.db.DbValue;
import java.util.Arrays;
import org.agrona.ExpandableArrayBuffer;

/** Wrapper around a {@code byte[]} to make it {@code Comparable} */
final class Bytes implements Comparable<Bytes> {

  private final byte[] byteArray;

  private Bytes(final byte[] byteArray) {
    this.byteArray = byteArray;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(byteArray);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Bytes bytes = (Bytes) o;

    return Arrays.equals(byteArray, bytes.byteArray);
  }

  @Override
  public int compareTo(final Bytes other) {

    final int EQUAL = 0;
    final int SMALLER = -1;
    final int BIGGER = 1;

    final byte[] otherByteArray = other.byteArray;

    for (int i = 0; i < byteArray.length; i++) {
      if (i >= otherByteArray.length) {
        return BIGGER;
      }

      final byte ourByte = byteArray[i];
      final byte otherByte = otherByteArray[i];

      final int compared = UnsignedBytes.compare(ourByte, otherByte);
      if (compared < 0) {
        return SMALLER;
      } else if (compared > 0) {
        return BIGGER;
      } // else { // = equals -> continue }
    }

    if (byteArray.length == otherByteArray.length) {
      return EQUAL;
    } else {
      // the other must be a longer array then
      return SMALLER;
    }
  }

  byte[] toBytes() {
    return byteArray;
  }

  static Bytes fromByteArray(final byte[] array, final int length) {
    return new Bytes(Arrays.copyOfRange(array, 0, length));
  }

  static Bytes fromByteArray(final byte[] array) {
    return new Bytes(Arrays.copyOf(array, array.length));
  }

  static Bytes fromExpandableArrayBuffer(final ExpandableArrayBuffer buffer) {
    return fromByteArray(buffer.byteArray());
  }

  static Bytes fromDbValue(final DbValue value) {
    final ExpandableArrayBuffer valueBuffer = new ExpandableArrayBuffer(0);
    value.write(valueBuffer, 0);
    return Bytes.fromExpandableArrayBuffer(valueBuffer);
  }

  public static Bytes empty() {
    return new Bytes(new byte[0]);
  }
}
