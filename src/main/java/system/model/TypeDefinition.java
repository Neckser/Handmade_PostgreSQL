package system.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TypeDefinition {
    private final int oid;
    private final String name;
    private final int byteLength;

    public TypeDefinition(int oid, String name, int byteLength) {
        this.oid = oid;
        this.name = name;
        this.byteLength = byteLength;
    }

    public int getOid() {return oid;}
    public String getName() {return name;}
    public int getByteLength() {return byteLength;}

    public byte[] toBytes() {
        byte[] nameBits = name.getBytes(StandardCharsets.UTF_8);
        int size = nameBits.length + 12;
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.putInt(oid);
        buf.putInt(nameBits.length);
        buf.put(nameBits);
        buf.putInt(byteLength);

        return buf.array();
    }

    public static TypeDefinition fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int oid = buf.getInt();
        int nameSize = buf.getInt();
        byte[] nameBytes = new byte[nameSize];
        buf.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int byteLength = buf.getInt();
        return new TypeDefinition(oid, name, byteLength);
    }
}
