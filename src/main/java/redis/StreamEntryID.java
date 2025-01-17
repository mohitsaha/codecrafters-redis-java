package redis;

import java.util.Objects;

public class StreamEntryID implements Comparable<StreamEntryID> {
    private final long timeMillis;
    private final long sequence;

    public StreamEntryID(long timeMillis, long sequence) {
        this.timeMillis = timeMillis;
        this.sequence = sequence;
    }

    public StreamEntryID(String entryId) {
        if (entryId == null || entryId.isEmpty()) {
            throw new RedisException("Invalid stream entry ID format");
        }

        if (entryId.equals("*")) {
            this.timeMillis = System.currentTimeMillis();
            this.sequence = 0;
            return;
        }

        String[] parts = entryId.split("-");
        if (parts.length != 2) {
            throw new RedisException("Invalid stream entry ID format. Expected format: <time>-<sequence>");
        }

        try {
            if (parts[1].equals("*")) {
                this.timeMillis = Long.parseLong(parts[0]);
                this.sequence = -1; // Special marker for auto-sequence
            } else {
                this.timeMillis = Long.parseLong(parts[0]);
                this.sequence = Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            throw new RedisException("Invalid stream entry ID format. Both time and sequence must be numbers");
        }
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public long getSequence() {
        return sequence;
    }

    public boolean isAutoSequence() {
        return sequence == -1;
    }

    @Override
    public String toString() {
        return timeMillis + "-" + (sequence == -1 ? "*" : sequence);
    }

    @Override
    public int compareTo(StreamEntryID other) {
        int timeCompare = Long.compare(this.timeMillis, other.timeMillis);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Long.compare(this.sequence, other.sequence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamEntryID that = (StreamEntryID) o;
        return timeMillis == that.timeMillis && sequence == that.sequence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeMillis, sequence);
    }

    // Utility methods for range operations
    public boolean isInRange(StreamEntryID start, StreamEntryID end) {
        return (start == null || this.compareTo(start) >= 0) &&
                (end == null || this.compareTo(end) <= 0);
    }

    public StreamEntryID nextSequence() {
        return new StreamEntryID(this.timeMillis, this.sequence + 1);
    }
}