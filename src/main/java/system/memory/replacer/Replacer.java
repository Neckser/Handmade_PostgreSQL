package system.memory.replacer;

import system.memory.model.BufferSlot;

public interface Replacer {
    void push(BufferSlot bufferSlot);
    void delete(int pageId);
    BufferSlot pickVictim();
}
