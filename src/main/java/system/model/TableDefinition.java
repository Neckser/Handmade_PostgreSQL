package system.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TableDefinition {
    private final int oid;
    private final String name;
    private final String type;
    private final String fileNode;
    private  int pagesCount;

    public TableDefinition(int oid, String name, String type, String fileNode, int pagesCount) {
        this.oid = oid;
        this.name = name;
        this.type = type;
        this.fileNode = fileNode;
        this.pagesCount = pagesCount;
    }

    public int getOid() {return this.oid;}
    public String getName() {return this.name;}
    public String getType() {return this.type;}
    public String getFileNode() {return this.fileNode;}
    public int getPagesCount() {return this.pagesCount;}
    public void setPagesCount(int count) { this.pagesCount = count;}

    public byte[] toBytes() {
        byte[] nameBits = name.getBytes(StandardCharsets.UTF_8);
        byte[] typeBits = type.getBytes(StandardCharsets.UTF_8);
        byte[] fileNodeBits = fileNode.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(nameBits.length + typeBits.length + fileNodeBits.length + 20);
        buf.putInt(oid);
        buf.putInt(nameBits.length);
        buf.put(nameBits);
        buf.putInt(typeBits.length);
        buf.put(typeBits);
        buf.putInt(fileNodeBits.length);
        buf.put(fileNodeBits);
        buf.putInt(pagesCount);

        return buf.array();
    }

    public static TableDefinition fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int oid = buf.getInt();
        int nameSize = buf.getInt();
        byte[] nameBytes = new byte[nameSize];
        buf.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int typeSize = buf.getInt();
        byte[] typeBytes = new byte[typeSize];
        buf.get(typeBytes);
        String type = new String(typeBytes, StandardCharsets.UTF_8);
        int fileNodeSize= buf.getInt();
        byte[] fileNodeBytes = new byte[fileNodeSize];
        buf.get(fileNodeBytes);
        String fileNode = new String(fileNodeBytes, StandardCharsets.UTF_8);
        int pagesCount = buf.getInt();

        return new TableDefinition(oid, name, type, fileNode, pagesCount);
    }
}
