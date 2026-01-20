package system.memory.serializer;

import system.memory.model.DataType;
import system.memory.model.HeapTuple;

public interface TupleSerializer {
    <T> HeapTuple serialize(T value, DataType dataType);

    <T> T deserialize(HeapTuple tuple);
}
