package system.io;


import system.buffer.DefaultBufferPoolManager;

import java.io.IOException;

public class DefaultDirtyPageWriter implements DirtyPageWriter {
    private final DefaultBufferPoolManager bm;
    private final int time = 10000;
    private final int flushtime = 1000;
    private final int maxDirtyPages = 100;

    public DefaultDirtyPageWriter(DefaultBufferPoolManager bm) {
        this.bm = bm;
    }

    @Override
    public void startCheckPointer() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(time);
                    bm.flushAllPages();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public void startBackgroundWriter() {
        new Thread(() -> {
            while (true) {
                try {
                    var dirtyPages = bm.getDirtyPages();
                    for (int i = 0; i < Math.min(dirtyPages.size(), maxDirtyPages); i++) {
                        bm.flushPage(dirtyPages.get(i).getPageId());
                    }
                    Thread.sleep(flushtime);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}