package system.replacer;

import system.model.BufferSlot;

import java.util.List;
import java.util.ArrayList;

public class ClockReplacer implements Replacer {
    private final List<BufferSlot> spisok = new ArrayList<>();
    private int pos = 0;

    @Override
    public void push(BufferSlot slot) {
        if (!slot.isPinned() && !spisok.contains(slot)) {
            spisok.add(slot);
        }
    }

    @Override
    public void delete(int pageId) {
        spisok.removeIf(slot -> slot.getPageId() == pageId);
        if (pos >= spisok.size()) {
            pos = 0;
        }
    }

    @Override
    public BufferSlot pickVictim() {
        if (spisok.isEmpty()) {
            return null;
        }

        int state = 0;

        while (state < spisok.size() * 2) {
            BufferSlot currentSlot = spisok.get(pos);

            if (!currentSlot.isPinned()) {
                if (currentSlot.getUsageCount() == 0) {
                    BufferSlot victim = currentSlot;
                    spisok.remove(pos);
                    if (pos >= spisok.size()) {
                        pos = 0;
                    }
                    return victim;
                } else {
                    currentSlot.decrementUsage();
                }
            }
            state += 1;
            pos += 1;
        }
        return null;
    }
}