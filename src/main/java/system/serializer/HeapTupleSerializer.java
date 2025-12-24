package system.serializer;

import system.model.DataType;
import system.model.HeapTuple;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeapTupleSerializer implements TupleSerializer {

    @Override
    public <T> HeapTuple serialize(T value, DataType dataType) {
        byte[] data;
        switch (dataType) {
            case INT64:
                ByteBuffer buf = ByteBuffer.allocate(8);
                buf.putLong((Long) value);
                data = buf.array();
                break;
            case VARCHAR:
                byte[] sb = ((String) value).getBytes(StandardCharsets.UTF_8);
                if (sb.length > 255) {
                    throw new IllegalArgumentException("Max length 255");
                }
                ByteBuffer buff = ByteBuffer.allocate(1 + sb.length);
                buff.put((byte) sb.length);
                buff.put(sb);
                data = buff.array();
                break;
            default:
                throw new IllegalArgumentException();
        }
        return new HeapTuple(data, dataType);
    }

    @Override
    public <T> T deserialize(HeapTuple tuple) {
        DataType type = tuple.type();
        byte[] data = tuple.data();
        switch (type) {
            case INT64:
                if (data.length != 8) {
                    throw new IllegalArgumentException("Incorrect size");
                }
                ByteBuffer buf = ByteBuffer.wrap(data);
                Long lv = buf.getLong();
                return (T) lv;

            case VARCHAR:
                if (data.length < 1 || (data.length != 1 + (data[0] & 0xFF))) {
                    throw new IllegalArgumentException();
                }
                String sv = new String(data, 1, data[0] & 0xFF, StandardCharsets.UTF_8);
                return (T) sv;
            default:
                throw new IllegalArgumentException();
        }
    }
}