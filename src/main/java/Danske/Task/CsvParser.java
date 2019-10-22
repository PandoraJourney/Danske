package Danske.Task;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;

public class CsvParser {

    private final Splitter SPLITTER = Splitter.on('\t');

    public List<Integer> parseLine(String line) {
        return StreamSupport.stream(SPLITTER.split(line).spliterator(), false)
                            .map(Integer::getInteger)
                            .collect(Collectors.toList());
    }
}
