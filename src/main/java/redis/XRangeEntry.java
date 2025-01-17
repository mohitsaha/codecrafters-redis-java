package redis;

import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Getter
@ToString
public class XRangeEntry {
    private final String entryId;
    private final List<String> fields;

    public XRangeEntry(String entryId, Map<String,String> fieldsMap) {
        this.entryId = entryId;
        this.fields = fieldsMap.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toList();
    }
}
