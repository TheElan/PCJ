/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.pcj.internal.Configuration;

/**
 *
 * @author faramir
 */
public class CloneInputStream extends InputStream {

    private static final int BYTES_CHUNK_SIZE = Configuration.CHUNK_SIZE;
    private static final byte[] EMPTY = new byte[0];

    private final List<byte[]> bytesList;
    private Iterator<byte[]> iterator;
    private byte[] currentByteArray;
    private int currentIndex;
    private long length;

    private CloneInputStream() {
        bytesList = new ArrayList<>();
        length = 0;
    }

    public static CloneInputStream clone(InputStream in) throws IOException {
        CloneInputStream cloneInputStream = new CloneInputStream();

        byte[] bytes = new byte[BYTES_CHUNK_SIZE];
        int b;
        int index = 0;
        while ((b = in.read()) != -1) {
            bytes[index++] = (byte) b;
            if (index == bytes.length) {
                cloneInputStream.addByteArray(bytes);

                bytes = new byte[BYTES_CHUNK_SIZE];
                index = 0;
            }
        }

        if (index > 0) {
            byte[] dest = new byte[index];
            System.arraycopy(bytes, 0, dest, 0, index);
            cloneInputStream.addByteArray(dest);
        }

        return cloneInputStream;
    }

    private void addByteArray(byte[] byteArray) {
        bytesList.add(byteArray);
        length += byteArray.length;
    }

    @Override
    public void reset() {
        iterator = bytesList.iterator();
        currentByteArray = EMPTY;
        currentIndex = 0;
    }

    @Override
    public int read() {
        while (currentIndex == currentByteArray.length) {
            if (iterator.hasNext()) {
                currentByteArray = iterator.next();
                currentIndex = 0;
            } else {
                return -1;
            }
        }
        return currentByteArray[currentIndex++] & 0xFF;
    }

    public long getLength() {
        return length;
    }

    public static CloneInputStream readFrom(MessageDataInputStream in) throws IOException {
        CloneInputStream cloneInputStream = new CloneInputStream();
        long length = in.readLong();

        byte[] bytes;
        for (long left = length; left > 0; left -= bytes.length) {
            int len = (int) Math.min((long) Integer.MAX_VALUE, left);
            bytes = new byte[len];
            int offset = 0;
            while (offset < len) {
                offset += in.read(bytes, offset, len - offset);
            }
            cloneInputStream.addByteArray(bytes);
        }

        return cloneInputStream;
    }

    public void writeInto(MessageDataOutputStream out) throws IOException {
        out.writeLong(length);

        long written = 0;
        for (byte[] bytesArray : bytesList) {
            int len = (int) Math.min(bytesArray.length, length - written);
            out.write(bytesArray, 0, len);
        }
    }
}
