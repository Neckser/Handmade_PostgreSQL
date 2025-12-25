package system.buffer;

import system.manager.PageFileManager;
import system.model.BufferSlot;
import system.page.Page;
import system.replacer.Replacer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultBufferPoolManager implements BufferPoolManager {
    private final Map<Integer, BufferSlot> store = new HashMap<>();
    private final PageFileManager io;
    private final Replacer replacer;
    private final int maxsize;
    private final Path path;

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer) {
        this.maxsize = poolSize;
        this.io = pageFileManager;
        this.replacer = replacer;
        this.path = Path.of("data.heap");
    }

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer, Path dataPath) {
        this.maxsize = poolSize;
        this.io = pageFileManager;
        this.replacer = replacer;
        this.path = dataPath;
    }

    @Override
    public BufferSlot getPage(int pageId) throws IOException{
        BufferSlot desc = store.get(pageId);
        if (desc == null) {
            ensureFrame();
            Page page = io.read(pageId, path);
            desc = new BufferSlot(pageId, page);
            replacer.push(desc);
            store.put(pageId, desc);
        }

        desc.incrementUsage();
        return desc;
    }

    @Override
    public void updatePage(int pageId, Page page) throws IOException {
        BufferSlot desc = store.get(pageId);
        if (desc == null) {
            ensureFrame();
            desc = new BufferSlot(pageId, page);
            replacer.push(desc);
            store.put(pageId, desc);
        } else {
            desc.setPage(page);
        }
        desc.setDirty(true);
    }

    @Override
    public void pinPage(int pageId) {
        BufferSlot desc = store.get(pageId);
        if (desc == null) {
            throw new IllegalArgumentException("Page not found");
        }
        replacer.delete(pageId);
        desc.setPinned(true);
    }

    @Override
    public void unpinPage(int pageId) {
        BufferSlot desc = store.get(pageId);
        if (desc == null) {
            throw new IllegalArgumentException("Page not found");
        }
        desc.setPinned(false);
        replacer.push(desc);
    }

    @Override
    public void flushAllPages() throws IOException {
        for (BufferSlot desc : getDirtyPages()) {
            if (desc == null) {
                throw new IllegalArgumentException("Page not found");
            }
            io.write(desc.getPage(), path);
            desc.setDirty(false);
        }
    }

    @Override
    public void flushPage(int pageId) throws IOException{
        BufferSlot desc = store.get(pageId);
        if (desc == null) {
            throw new IllegalArgumentException("Page not found");
        }
        io.write(desc.getPage(), path);
        desc.setDirty(false);
    }

    private void ensureFrame() throws IOException {
        if (store.size() < maxsize) {
            return;
        }
        BufferSlot vic = replacer.pickVictim();
        if (vic == null) {
            throw new IOException("No free space");
        }
        store.remove(vic.getPageId());
        if (vic.isDirty()) {
            io.write(vic.getPage(), path);
        }
    }

    @Override
    public List<BufferSlot> getDirtyPages() {
        return store.values().stream().filter(slot -> slot.isDirty()).toList();
    }
}