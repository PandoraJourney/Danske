package Danske.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.stream.Collectors.toList;

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
                                List<CalculateServiceImpl.SumAndIndex> start = ImmutableList.of(
                                        new CalculateServiceImpl.SumAndIndex(0, 0, -1, new ArrayList<>()));
                                try(BufferedReader br =
                                            Files.newBufferedReader((newFileDir.resolve(csvFile)))) {
                                    String line;
                                    LOG.info("Reading file lines.");
                                    while ((line = br.readLine()) != null) {
                                        List<Integer> numbers = CsvUtility.parseLine(line);
                                        start = start.stream()
                                                     .map(a -> createNewObjects(a, numbers))
                                                     .flatMap(List::stream)
                                                     .collect(toList());
                                        LOG.info("Stopped proccessing line");
                                    }

                                    CalculateServiceImpl.SumAndIndex sum = start.stream()
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

    private List<CalculateServiceImpl.SumAndIndex> createNewObjects(
            CalculateServiceImpl.SumAndIndex value,
            List<Integer> line) {
        LOG.info("Creating SumAndIndex");
        return service.calculateNodeValue(value, line.get(value.getTargetIndex()));
    }

    @PreDestroy
    public void shutdown() throws IOException, InterruptedException {
        watcher.close();
        executor.join();
    }

}
