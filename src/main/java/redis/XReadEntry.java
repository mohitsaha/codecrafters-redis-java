package redis;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

public class XReadEntry {
    private final String streamKey;
    private final List<SingleStreamEntry> entries;

    private XReadEntry(String streamKey, List<SingleStreamEntry> entries) {
        this.streamKey = streamKey;
        this.entries = entries;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public List<SingleStreamEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public static class SingleStreamEntry {
        private final String sequence;
        private final Map<String, String> fields;

        private SingleStreamEntry(String sequence, Map<String, String> fields) {
            this.sequence = sequence;
            this.fields = fields;
        }

        public String getSequence() {
            return sequence;
        }

        public Map<String, String> getFields() {
            return new LinkedHashMap<>(fields); // Return a copy to maintain immutability
        }

        public static class Builder {
            private String sequence;
            private Map<String, String> fields = new LinkedHashMap<>();

            public Builder sequence(String sequence) {
                this.sequence = sequence;
                return this;
            }

            public Builder addField(String key, String value) {
                fields.put(key, value);
                return this;
            }

            public SingleStreamEntry build() {
                return new SingleStreamEntry(sequence, fields);
            }
        }
    }

    public static class Builder {
        private String streamKey;
        private List<SingleStreamEntry> entries = new ArrayList<>();

        public Builder streamKey(String streamKey) {
            this.streamKey = streamKey;
            return this;
        }

        public Builder addEntry(SingleStreamEntry entry) {
            entries.add(entry);
            return this;
        }

        public XReadEntry build() {
            return new XReadEntry(streamKey, entries);
        }
    }

    // Example usage method
    public static XReadEntry fromList(List<Object> data) {
        if (data == null || data.isEmpty() || !(data.get(0) instanceof List)) {
            throw new IllegalArgumentException("Invalid data format");
        }

        List<?> entry = (List<?>) data.get(0);
        if (entry.size() != 2) {
            throw new IllegalArgumentException("Invalid entry format");
        }

        String streamKey = (String) entry.get(0);
        List<?> entries = (List<?>) entry.get(1);

        Builder builder = new Builder().streamKey(streamKey);

        for (Object streamEntry : entries) {
            List<?> entryData = (List<?>) streamEntry;
            String sequence = (String) entryData.get(0);
            List<?> fields = (List<?>) entryData.get(1);

            SingleStreamEntry.Builder entryBuilder = new SingleStreamEntry.Builder()
                    .sequence(sequence);

            for (int i = 0; i < fields.size(); i += 2) {
                entryBuilder.addField(
                        (String) fields.get(i),
                        (String) fields.get(i + 1)
                );
            }

            builder.addEntry(entryBuilder.build());
        }

        return builder.build();
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\"").append(streamKey).append("\", [");
        if (entries != null && !entries.isEmpty()) {
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(entries.get(i).sequence);
                sb.append(", ");
                sb.append(entries.get(i).fields);
            }
        }
        sb.append("]]");
        return sb.toString();
    }
}