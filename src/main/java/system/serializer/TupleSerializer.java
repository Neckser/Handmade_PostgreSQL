package system.serializer;

import system.model.DataType;
import system.model.HeapTuple;

public interface TupleSerializer {
    <T> HeapTuple serialize(T value, DataType dataType);

    <T> T deserialize(HeapTuple tuple);
}
