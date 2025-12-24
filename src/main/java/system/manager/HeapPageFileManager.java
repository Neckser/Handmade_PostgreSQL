package system.manager;

import system.page.HeapPage;
import system.page.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class HeapPageFileManager implements PageFileManager {

    @Override
    public void write(Page page, Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (page.bytes().length != HeapPage.PAGE_SIZE) {
                throw new IllegalArgumentException("Incorrect page size");
            }
            long pos;
            if (page.getPageId() >= 0) {
                pos = (long)page.getPageId() * HeapPage.PAGE_SIZE;
            } else {
                try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                    pos = channel.size();
                } catch (IOException e) {
                    pos = 0;
                }

            }
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
                ByteBuffer buf = ByteBuffer.wrap(page.bytes());
                channel.write(buf, pos);
            }
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Page read(int pageId, Path path) {
        try {
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("File not exist");
            }
            long pos = (long) pageId * HeapPage.PAGE_SIZE;
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                if (pos + HeapPage.PAGE_SIZE > channel.size()) {
                    throw new IllegalArgumentException("Overflow file");
                }
                ByteBuffer buf = ByteBuffer.allocate(HeapPage.PAGE_SIZE);
                if (channel.read(buf, pos) != HeapPage.PAGE_SIZE) {
                    throw new IllegalArgumentException("Incorrect bytes read");
                }
                byte[] pageData = buf.array();
                HeapPage nov = new HeapPage(pageId, pageData);
                if (!nov.isValid()) {
                    throw new IllegalStateException("Invalid MAGIC");
                }
                return nov;
            }

        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }
}