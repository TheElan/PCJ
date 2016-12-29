/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.Message;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageBytesOutputStream implements AutoCloseable {

    private final Message message;
    private final MessageOutputStream messageOutputStream;
    private final MessageDataOutputStream messageDataOutputStream;
    private ByteBuffer[] byteBuffersArray;

    public MessageBytesOutputStream(Message message) throws IOException {
        this(message, Configuration.CHUNK_SIZE);
    }

    public MessageBytesOutputStream(Message message, int chunkSize) {
        this.message = message;

        this.messageOutputStream = new MessageOutputStream(chunkSize);
        this.messageDataOutputStream = new MessageDataOutputStream(this.messageOutputStream);
    }

    public void writeMessage() throws IOException {
        messageDataOutputStream.writeByte(message.getType().getId());

        message.write(messageDataOutputStream);
    }

    public Message getMessage() {
        return message;
    }

    public boolean isClosed() {
        return messageOutputStream.isClosed();
    }

    @Override
    public void close() throws IOException {
        messageOutputStream.close();
        byteBuffersArray = messageOutputStream.queue.stream().toArray(ByteBuffer[]::new);
    }

    public ByteBuffer[] getByteBuffersArray() {
        return byteBuffersArray;
    }
    
    private static class MessageOutputStream extends OutputStream {

        private static final int HEADER_SIZE = Integer.BYTES;
        private static final int LAST_CHUNK_BIT = (int) (1 << (Integer.SIZE - 1));
        private final int chunkSize;
        private final Queue<ByteBuffer> queue;
        volatile private boolean closed;
        volatile private ByteBuffer currentByteBuffer;

        public MessageOutputStream(int chunkSize) {
            this.chunkSize = chunkSize;
            this.queue = new ConcurrentLinkedQueue<>();

            allocateBuffer(chunkSize);
        }

        private void allocateBuffer(int size) {
            currentByteBuffer = ByteBuffer.allocate(size);
            currentByteBuffer.position(HEADER_SIZE);
        }

        private void flush(int size) {
            int length = (currentByteBuffer.position() - HEADER_SIZE);
            if (closed) {
                length = (length | LAST_CHUNK_BIT);
            }
            currentByteBuffer.putInt(0, length);
            currentByteBuffer.flip();
            queue.offer(currentByteBuffer);

            if (size > 0) {
                allocateBuffer(size);
            }
        }

        @Override
        public void flush() {
            flush(chunkSize);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            flush(0);
            currentByteBuffer = null;
        }

        public boolean isClosed() {
            return currentByteBuffer == null && queue.isEmpty();
        }

        private ByteBuffer peekByteBuffer() {
            return queue.peek();
        }

        private ByteBuffer pollByteBuffer() {
            return queue.poll();
        }

        @Override
        public void write(int b) {
            if (currentByteBuffer.hasRemaining() == false) {
                flush();
            }
            currentByteBuffer.put((byte) b);

        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            int remaining = currentByteBuffer.remaining();
            while (remaining < len) {
                currentByteBuffer.put(b, off, remaining);
                len -= remaining;
                off += remaining;
                flush(Math.max(chunkSize, len));
                
                remaining = currentByteBuffer.remaining();
            }
            currentByteBuffer.put(b, off, len);
        }
    }
}
