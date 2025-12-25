package system.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ColumnDefinition {
    private final int oid;
    private final int tableOid;
    private final int typeOid;
    private final String name;
    private final int position;

    public ColumnDefinition(int oid, int tableOid, int type0id, String name, int position) {
        this.oid = oid;
        this.tableOid = tableOid;
        this.typeOid = type0id;
        this.name = name;
        this.position = position;
    }

    public int getOid() {return this.oid;}
    public int getTableOid() {return this.tableOid;}
    public int getType0id() {return this.typeOid;}
    public String getName() {return this.name;}
    public int getPosition() {return this.position;}


    public byte[] toBytes() {
        byte[] nameBits = name.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(nameBits.length + 20);
        buf.putInt(oid);
        buf.putInt(tableOid);
        buf.putInt(typeOid);
        buf.putInt(nameBits.length);
        buf.put(nameBits);
        buf.putInt(position);

        return buf.array();
    }

    public static ColumnDefinition fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int oid = buf.getInt();
        int tableOid = buf.getInt();
        int typeOid = buf.getInt();
        int nameSize = buf.getInt();
        byte[] nameBytes = new byte[nameSize];
        buf.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int position = buf.getInt();

        return new ColumnDefinition(oid, tableOid, typeOid, name, position);
    }
}
