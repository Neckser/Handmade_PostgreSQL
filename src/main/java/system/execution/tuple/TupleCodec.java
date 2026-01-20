package system.execution.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TupleCodec {
    private TupleCodec() {}

    public static List<Object> decodeTagged(byte[] rowData) {
        ByteBuffer b = ByteBuffer.wrap(rowData).order(ByteOrder.LITTLE_ENDIAN);
        List<Object> row = new ArrayList<>();

        while (b.hasRemaining()) {
            byte tag = b.get();
            switch (tag) {
                case 0 -> row.add(null);
                case 1 -> row.add(b.getInt());
                case 2 -> row.add(b.getLong());
                case 3 -> row.add(b.get() != 0);
                case 4 -> {
                    int len = b.getShort() & 0xFFFF;
                    byte[] s = new byte[len];
                    b.get(s);
                    row.add(new String(s, StandardCharsets.UTF_8));
                }
                default -> throw new IllegalStateException("Bad tag in tuple: " + tag);
            }
        }
        return row;
    }
}
