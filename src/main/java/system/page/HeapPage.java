package system.page;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HeapPage implements Page{

    public static final int PAGE_SIZE = 8192;
    private static final int HEADER_SIZE = 10;
    private static final int MAGIC = 0x00DDDDDD;
    private static final short LEN_DELETED = (short)0xFFFF;
    private static final int SLOT_SIZE = 4;
    private final ByteBuffer buf;
    private final int pageId;

    public HeapPage(int pageId) {
        this.buf = ByteBuffer.allocate(PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        this.pageId = pageId;
        buf.putInt(0, MAGIC);
        buf.putShort(4, (short) 0);
        buf.putShort(6, (short) HEADER_SIZE);
        buf.putShort(8, (short) PAGE_SIZE);
    }

    public HeapPage(int pageId, byte[] bytes) {
        if (bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException();
        }
        this.pageId = pageId;
        this.buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (!isValid()) {
            throw new IllegalStateException();
        }
    }

    private int lower() { return buf.getShort(6) & 0xFFFF; }
    private int upper() { return buf.getShort(8) & 0xFFFF; }
    private void setLower(int v) { buf.putShort(6, (short) v); }
    private void setUpper(int v) { buf.putShort(8, (short) v); }
    private int slotCount() { return buf.getShort(4) & 0xFFFF; }
    private void setSlotCount(int v) { buf.putShort(4, (short) v); }
    private int getFreeSpace() { return upper() - lower(); }

    @Override
    public byte[] bytes() {
        byte[] out = new byte[PAGE_SIZE];
        int pos = buf.position();
        buf.rewind();
        buf.get(out);
        buf.position(pos);
        return out;
    }

    @Override
    public int getPageId() {
        return this.pageId;
    }

    @Override
    public int size() {
        return slotCount();
    }

    @Override
    public boolean isValid() {
        return buf.getInt(0) == MAGIC;
    }

    @Override
    public byte[] read(int idx) {
        if (idx < 0 || idx >= slotCount()) throw new IndexOutOfBoundsException();
        int slotPos = HEADER_SIZE + idx * SLOT_SIZE;
        int off = buf.getShort(slotPos) & 0xFFFF;
        int len = buf.getShort(slotPos + 2) & 0xFFFF;
        if (len == (LEN_DELETED & 0xFFFF)) return null;
        byte[] out = new byte[len];
        int p = buf.position();
        buf.position(off);
        buf.get(out);
        buf.position(p);
        return out;
    }

    @Override
    public void write(byte[] data) {
        int need = SLOT_SIZE + data.length;
        if (getFreeSpace() < need) throw new IllegalArgumentException("not enough space");
        int newUpper = upper() - data.length;
        buf.position(newUpper);
        buf.put(data);
        int slotPos = lower();
        buf.putShort(slotPos, (short) newUpper);
        buf.putShort(slotPos + 2, (short) data.length);
        setLower(slotPos + SLOT_SIZE);
        setUpper(newUpper);
        setSlotCount(slotCount() + 1);
    }
}