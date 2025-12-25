package system.buffer;

import system.model.BufferSlot;
import system.page.Page;

import java.io.IOException;
import java.util.List;

public interface BufferPoolManager {
    BufferSlot getPage(int pageId) throws IOException;
    void updatePage(int pageId, Page page) throws IOException;
    void pinPage(int pageId);
    void unpinPage(int pageId);
    void flushPage(int pageId) throws IOException;
    void flushAllPages() throws IOException;
    List<BufferSlot> getDirtyPages();
}
