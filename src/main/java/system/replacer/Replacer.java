package system.replacer;

import system.model.BufferSlot;

public interface Replacer {
    void push(BufferSlot bufferSlot);
    void delete(int pageId);
    BufferSlot pickVictim();
}
