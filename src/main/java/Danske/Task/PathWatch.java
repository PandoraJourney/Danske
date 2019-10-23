package Danske.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableList;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.stream.Collectors.toMap;

@Component
public class PathWatch {
    private static final Logger LOG = LoggerFactory.getLogger(PathWatch.class);

    @Value("${app.new-file}")
    private Path newFileDir;

    @Autowired
    private CalculateService service;

    private Thread executor;

    private WatchService watcher;

    @PostConstruct
    public void watch() throws IOException {
        Files.createDirectories(newFileDir);
        watcher = FileSystems.getDefault().newWatchService();
        newFileDir.register(watcher, ENTRY_CREATE);
        executor = new Thread(() -> {
            watch(watcher);
        }, "File watcher");
        executor.start();
    }

    private void watch(WatchService watcher) {
        try {
            boolean keyValid = true;
            while (keyValid) {
                LOG.debug("Waiting for start file...");
                WatchKey key = watcher.take();
                try {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == ENTRY_CREATE) {
                            WatchEvent<Path> ev = (WatchEvent<Path>)event;
                            Path csvFile = ev.context();
                            if (StringUtils.endsWithIgnoreCase(csvFile.getFileName().toString(), ".csv")) {
                                LOG.info("Csv file found. Starting file processing");
                                Collection<CalculateServiceImpl.SumAndIndex> processedNodes =
                                        ImmutableList.of(new CalculateServiceImpl.SumAndIndex(0, 0, -1, new ArrayList<>()));
                                try(BufferedReader br = Files.newBufferedReader((newFileDir.resolve(csvFile)))) {
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        List<Integer> parsedLine = CsvUtility.parseLine(line);
                                        processedNodes =
                                                processedNodes.stream()
                                                               .map(node -> processNode(node, parsedLine))
                                                               .flatMap(List::stream)
                                                               .collect(toMap(CalculateServiceImpl.SumAndIndex::getIndexWithReminder,
                                                                                                         Function.identity(),
                                                                                                         this::removeCrossPath))
                                                               .values();
                                    }

                                    CalculateServiceImpl.SumAndIndex sum =
                                            processedNodes.stream()
                                                          .max(Comparator.comparingInt(CalculateServiceImpl.SumAndIndex::getSum))
                                                          .orElse(new CalculateServiceImpl.SumAndIndex(0, 0, -1, ImmutableList.of()));
                                    LOG.info("Sum is: " + sum.getSum());
                                    String path = sum.getPath()
                                                     .stream()
                                                     .map(Objects::toString)
                                                     .collect(Collectors.joining(","));
                                    LOG.info("Path: " + path);

                                }
                                Files.delete(newFileDir.resolve(csvFile));
                            }
                        } else if (event.kind() == OVERFLOW) {
                            LOG.warn("Overflow event has been received.");
                        }
                    }
                } catch (IOException | RuntimeException ex) {
                    LOG.debug("Error while processing files: " + ex.getMessage(), ex);
                }
                keyValid = key.reset();
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            LOG.debug("Closing");
        }
    }

    private List<CalculateServiceImpl.SumAndIndex> processNode(
            CalculateServiceImpl.SumAndIndex value,
            List<Integer> line) {
        return service.calculateNodeValue(value,
                                          line.get(value.getIndexWithReminder().getTargetIndex()));
    }

    private CalculateServiceImpl.SumAndIndex removeCrossPath(CalculateServiceImpl.SumAndIndex first, CalculateServiceImpl.SumAndIndex second) {
        return first.getSum() > second.getSum() ? first : second;
    }

    @PreDestroy
    public void shutdown() throws IOException, InterruptedException {
        watcher.close();
        executor.join();
    }

}
