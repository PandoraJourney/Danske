package Danske.Task;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

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
//                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> ev = (WatchEvent<Path>)event;
                            Path csvFile = ev.context();
                            if (StringUtils.endsWithIgnoreCase(csvFile.getFileName().toString(), ".csv")) {
                                LOG.info("Csv file found. Starting file processing");
                                try(Stream<String> stream = Files.lines(newFileDir.resolve(csvFile))) {
                                    stream.forEach(LOG::info);
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

    @PreDestroy
    public void shutdown() throws IOException, InterruptedException {
        watcher.close();
        executor.join();
    }

}
