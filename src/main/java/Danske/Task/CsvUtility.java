package Danske.Task;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;

public class CsvUtility {

    private static final Splitter SPLITTER = Splitter.on(";");

    public static List<Integer> parseLine(String line) {
        return StreamSupport.stream(SPLITTER.split(line).spliterator(), false)
                            .filter(Objects::nonNull)
                            .filter(a -> !a.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
    }
}
