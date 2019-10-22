package Danske.Task;

import java.util.List;

import org.junit.jupiter.api.Test;


public class CsvParserTest {

    @Test
    public void parseCorrectly() {
        CsvParser parser = new CsvParser();

        String line = "1\t2";
        List<Integer> result = parser.parseLine(line);
        asset

    }
}
