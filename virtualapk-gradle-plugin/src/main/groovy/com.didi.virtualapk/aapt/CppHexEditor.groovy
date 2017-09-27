package com.didi.virtualapk.aapt

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Class of c++ hex file (little endian) editor
 */
public class CppHexEditor {

    private File file
    private File clipFile
    private RandomAccessFile raf
    private RandomAccessFile clipRaf
    private boolean edited
    private long lengthBeforeClip

    public CppHexEditor(final File file) {
        this.file = file
        this.raf = new RandomAccessFile(file, 'rw')
    }

    protected seek(final long offset) {
        this.raf.seek(offset)
    }

    protected skip(final long count) {
        this.raf.skipBytes((int) count)
    }

    protected tellp() {
        return this.raf.getFilePointer()
    }

    protected length() {
        return this.raf.length()
    }

    protected setLength(final long length) {
        this.raf.setLength(length)
    }

    protected close() {
        this.raf.close()
    }

    /*
     * Following reader & writer convert endian from c++(aapt) to java
     *  c++: little endian
     *  java: big endian
     */
    protected byte readByte() {
        return this.raf.readByte()
    }

    protected void writeByte(val) {
        final def buffer = new byte[1]
        buffer[0] = (byte) (val & 0xFF)
        writeBytes(buffer)
    }

    protected short readShort() {
        final def buffer = readBytes(2)
        return getShort(buffer)
    }

    protected short getShort(final byte[] buffer) {
        final ByteBuffer bb = ByteBuffer.wrap(buffer)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        return bb.getShort()
    }

    protected void writeShort(i) {
        final def buffer = new byte[2];
        buffer[1] = (byte) ((i >> 8) & 0xFF);
        buffer[0] = (byte) (i & 0xFF);
        writeBytes(buffer)
    }

    protected int readInt() {
        def buffer = readBytes(4)
        ByteBuffer bb = ByteBuffer.wrap(buffer)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        return bb.getInt()
    }

    protected void writeInt(i) {
        final def buffer = new byte[4];
        buffer[3] = (byte) ((i >> 24) & 0xFF);
        buffer[2] = (byte) ((i >> 16) & 0xFF);
        buffer[1] = (byte) ((i >> 8) & 0xFF);
        buffer[0] = (byte) (i & 0xFF);
        writeBytes(buffer)
    }

    protected byte[] readBytes(n) {
        final byte[] buffer = new byte[n]
        this.raf.read(buffer)
        return buffer
    }

    protected void writeBytes(final byte[] buffer) {
        this.raf.write(buffer)
        if (!this.edited) {
            this.edited = true
        }
    }

    protected void clipLaterData(final long pos) {
        this.clipFile = new File(this.file.parentFile, "${file.name}~")
        this.clipRaf = new RandomAccessFile(this.clipFile, 'rw')

        this.lengthBeforeClip = this.raf.length()
        def sc = this.raf.channel
        def cc = this.clipRaf.channel
        sc.transferTo(pos, this.lengthBeforeClip - pos, cc)
        sc.truncate(pos)
    }

    protected void pasteLaterData(final long pos) {
        final def newPos = tellp()
        final def sc = this.raf.channel
        final def cc = this.clipRaf.channel
        cc.position(0L)
        sc.transferFrom(cc, newPos, this.lengthBeforeClip - pos)

        this.clipRaf.close()
        this.clipFile.delete()
    }

    /**
     * Print bytes in length with hex string
     *
     * @param length
     * @return
     */
    protected def dumpBytes(final long length) {
        for (int i = 0; i < length; i++) {
            def s = String.format('%02X ', readByte())
            if (i % 16 == 0) {
                s = '\t' + s
            } else if ((i + 17) % 16 == 0) {
                s += "\n"
            } else if ((i + 5) % 4 == 0) {
                s += " "
            }

            print s
        }

        println ""
    }

    /**
     * Check if has been written any bytes
     *
     * @return true edited
     */
    protected boolean isEdited() {
        return this.edited
    }
}
