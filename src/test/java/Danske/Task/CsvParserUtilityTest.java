package Danske.Task;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;


public class CsvParserUtilityTest {

    private static final Logger LOG = LoggerFactory.getLogger(PathWatch.class);

    @Test
    public void parseCorrectly() {
        String line = "1;2";
        List<Integer> result = CsvUtility.parseLine(line);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0));
    }
}
