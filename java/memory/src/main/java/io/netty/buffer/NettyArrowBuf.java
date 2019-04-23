/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.netty.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

import org.apache.arrow.memory.ArrowByteBufAllocator;
import org.apache.arrow.memory.BoundsChecking;
import org.apache.arrow.util.Preconditions;

import io.netty.util.internal.PlatformDependent;

/**
 * Netty specific wrapper over ArrowBuf for use in Netty framework.
 */
public class NettyArrowBuf extends AbstractByteBuf implements AutoCloseable  {

  private final UnsafeDirectLittleEndian udle;
  private final ArrowBuf arrowBuf;
  private final ArrowByteBufAllocator arrowByteBufAllocator;
  private final int offset;
  private int length;
  private final long address;

  public NettyArrowBuf(
      final UnsafeDirectLittleEndian udle,
      final ArrowBuf arrowBuf,
      final ArrowByteBufAllocator arrowByteBufAllocator,
      final int offset,
      final int length) {
    super(udle.maxCapacity());
    this.udle = udle;
    this.arrowBuf = arrowBuf;
    this.arrowByteBufAllocator = arrowByteBufAllocator;
    this.offset = offset;
    this.length = length;
    this.address = udle.memoryAddress() + offset;
  }

  @Override
  public ByteBuf copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf copy(int index, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf retain() {
    arrowBuf.getReferenceManager().retain();
    return this;
  }

  @Override
  public ByteBuf retain(final int increment) {
    arrowBuf.getReferenceManager().retain(increment);
    return this;
  }

  @Override
  public boolean isDirect() {
    return true;
  }

  @Override
  public synchronized ByteBuf capacity(int newCapacity) {
    if (newCapacity == length) {
      return this;
    }
    Preconditions.checkArgument(newCapacity >= 0);
    if (newCapacity < length) {
      length = newCapacity;
      return this;
    }
    throw new UnsupportedOperationException("Buffers don't support resizing that increases the size.");
  }

  @Override
  public ByteBuf unwrap() {
    return udle;
  }

  @Override
  public int refCnt() {
    return arrowBuf.getReferenceManager().getRefCount();
  }

  @Override
  public ArrowByteBufAllocator alloc() {
    return arrowByteBufAllocator;
  }

  @Override
  public boolean hasArray() {
    return udle.hasArray();
  }

  @Override
  public byte[] array() {
    return udle.array();
  }

  @Override
  public int arrayOffset() {
    return udle.arrayOffset();
  }

  @Override
  public boolean hasMemoryAddress() {
    return true;
  }

  @Override
  public long memoryAddress() {
    return this.address;
  }

  @Override
  public ByteBuf touch() {
    return this;
  }

  @Override
  public ByteBuf touch(Object hint) {
    return this;
  }

  @Override
  public int nioBufferCount() {
    return 1;
  }

  @Override
  public ByteBuffer internalNioBuffer(int index, int length) {
    return udle.internalNioBuffer(offset + index, length);
  }

  @Override
  public ByteBuffer[] nioBuffers() {
    return new ByteBuffer[] {nioBuffer()};
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    return new ByteBuffer[] {nioBuffer(index, length)};
  }

  @Override
  public int capacity() {
    return arrowBuf.capacity();
  }

  @Override
  public NettyArrowBuf slice() {
    return arrowBuf.slice().asNettyBuffer();
  }

  @Override
  public NettyArrowBuf slice(int index, int length) {
    return arrowBuf.slice(index, length).asNettyBuffer();
  }

  @Override
  public void close() {
    arrowBuf.close();
  }

  @Override
  public boolean release() {
    return arrowBuf.getReferenceManager().release();
  }

  @Override
  public boolean release(int decrement) {
    return arrowBuf.getReferenceManager().release(decrement);
  }

  @Override
  public int readerIndex() {
    return arrowBuf.readerIndex();
  }

  @Override
  public int writerIndex() {
    return arrowBuf.writerIndex();
  }

  @Override
  public NettyArrowBuf readerIndex(int readerIndex) {
    super.readerIndex(readerIndex);
    //arrowBuf.readerIndex(readerIndex);
    return this;
  }

  @Override
  public NettyArrowBuf writerIndex(int writerIndex) {
    super.writerIndex(writerIndex);
    //arrowBuf.writerIndex(writerIndex);
    return this;
  }

  @Override
  public ByteBuffer nioBuffer() {
    return nioBuffer(readerIndex(), readableBytes());
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    return udle.nioBuffer(offset + index, length);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    udle.getBytes(offset + index, dst);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    udle.setBytes(offset + index, src);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    udle.getBytes(offset + index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    udle.setBytes(offset + index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    udle.getBytes(offset + index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    udle.setBytes(offset + index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
    udle.getBytes(offset + index, out, length);
    return this;
  }

  @Override
  public int setBytes(int index, InputStream in, int length) throws IOException {
    return udle.setBytes(offset + index, in, length);
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
    return udle.getBytes(offset + index, out, length);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    return udle.setBytes(offset + index, in, length);
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
    return udle.getBytes(offset + index, out, position, length);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
    return udle.setBytes(offset + index, in, position, length);
  }

  @Override
  public ByteOrder order() {
    return ByteOrder.LITTLE_ENDIAN;
  }

  @Override
  public ByteBuf order(ByteOrder endianness) {
    return this;
  }

  @Override
  protected int _getUnsignedMedium(int index) {
    return getUnsignedMedium(index);
  }

  @Override
  protected int _getUnsignedMediumLE(int index) {
    return getUnsignedMediumLE(index);
  }


  /*-------------------------------------------------*
   |                                                 |
   |            get() APIs                           |
   |                                                 |
   *-------------------------------------------------*/


  @Override
  protected byte _getByte(int index) {
    return getByte(index);
  }

  @Override
  public byte getByte(int index) {
    return arrowBuf.getByte(index);
  }

  @Override
  protected short _getShortLE(int index) {
    return getShort(index);
  }

  @Override
  protected short _getShort(int index) {
    return getShort(index);
  }

  @Override
  public short getShort(int index) {
    return arrowBuf.getShort(index);
  }

  @Override
  protected int _getIntLE(int index) {
    return getInt(index);
  }

  @Override
  protected int _getInt(int index) {
    return getInt(index);
  }

  @Override
  public int getInt(int index) {
    return arrowBuf.getInt(index);
  }

  @Override
  protected long _getLongLE(int index) {
    return getLong(index);
  }

  @Override
  protected long _getLong(int index) {
    return getLong(index);
  }

  @Override
  public long getLong(int index) {
    return arrowBuf.getLong(index);
  }


  /*-------------------------------------------------*
   |                                                 |
   |            set() APIs                           |
   |                                                 |
   *-------------------------------------------------*/


  @Override
  protected void _setByte(int index, int value) {
    setByte(index, value);
  }

  @Override
  public NettyArrowBuf setByte(int index, int value) {
    arrowBuf.setByte(index, value);
    return this;
  }

  @Override
  protected void _setShortLE(int index, int value) {
    setShort(index, value);
  }

  @Override
  protected void _setShort(int index, int value) {
    setShort(index, value);
  }

  @Override
  public NettyArrowBuf setShort(int index, int value) {
    arrowBuf.setShort(index, value);
    return this;
  }

  private long addr(int index) {
    return address + index;
  }

  /**
   * Helper function to do bounds checking at a particular
   * index for particular length of data.
   * @param index index (0 based relative to this ArrowBuf)
   * @param fieldLength provided length of data for get/set
   */
  private void chk(int index, int fieldLength) {
    if (BoundsChecking.BOUNDS_CHECKING_ENABLED) {
      // check reference count
      ensureAccessible();
      // check bounds
      if (fieldLength < 0) {
        throw new IllegalArgumentException("length: " + fieldLength + " (expected: >= 0)");
      }
      if (index < 0 || index > capacity() - fieldLength) {
        throw new IndexOutOfBoundsException(String.format(
                "index: %d, length: %d (expected: range(0, %d))", index, fieldLength, capacity()));
      }
    }
  }

  @Override
  protected void _setMedium(int index, int value) {
    setMedium(index, value);
  }

  @Override
  protected void _setMediumLE(int index, int value) {
    setMedium(index, value);
  }

  @Override
  public NettyArrowBuf setMedium(int index, int value) {
    chk(index, 3);
    final long addr = addr(index);
    // we need to store 3 bytes starting from least significant byte
    // and ignoring the most significant byte
    // since arrow memory format is little endian, we will
    // first store the first 2 bytes followed by third byte
    // example: if the 4 byte int value is ABCD where A is MSB
    // D is LSB then we effectively want to store DCB in increasing
    // address to get Little Endian byte order
    // (short)value will give us CD and PlatformDependent.putShort()
    // will store them in LE order as DC starting at address addr
    // in order to get B, we do ABCD >>> 16 = 00AB => (byte)AB which
    // gives B. We store this at address addr + 2. So finally we get
    // DCB
    PlatformDependent.putShort(addr, (short) value);
    PlatformDependent.putByte(addr + 2, (byte) (value >>> 16));
    return this;
  }

  @Override
  protected void _setInt(int index, int value) {
    setInt(index, value);
  }

  @Override
  protected void _setIntLE(int index, int value) {
    setInt(index, value);
  }

  @Override
  public NettyArrowBuf setInt(int index, int value) {
    arrowBuf.setInt(index, value);
    return this;
  }

  @Override
  protected void _setLong(int index, long value) {
    setLong(index, value);
  }

  @Override
  public void _setLongLE(int index, long value) {
    setLong(index, value);
  }

  @Override
  public NettyArrowBuf setLong(int index, long value) {
    arrowBuf.setLong(index, value);
    return this;
  }
}
