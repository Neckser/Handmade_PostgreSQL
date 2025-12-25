package system.io;

public interface DirtyPageWriter {
    void startBackgroundWriter();
    void startCheckPointer();
}
