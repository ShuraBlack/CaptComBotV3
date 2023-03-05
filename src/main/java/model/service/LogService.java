package model.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LogService {

    private static final Logger LOGGER = LogManager.getLogger(LogService.class);
    private static final Map<String,String> LOGS = new HashMap<>();

    public static void addLog(String file, String append) {
        if (!LOGS.containsKey(file)) {
            LOGS.put(file,String.format("%s - %s\n", LocalDateTime.now().plusHours(2), append));
            return;
        }
        LOGS.put(file, LOGS.get(file).concat(String.format("%s - %s\n",  LocalDateTime.now().plusHours(2), append)));

        if (LOGS.get(file).length() > 1000) {
            writeFile(file);
        }
    }

    public static void writeFile(String file) {
        try {
            FileWriter fileWriter = new FileWriter(file + ".log",true);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(LOGS.get(file));
            writer.flush();
            writer.close();
            LOGS.remove(file);
        } catch (IOException e) {
            LOGGER.error(String.format("Couldnt log the requested file <%s>\n", file + ".log"),e);
        }
    }

    public static Set<String> getFiles() {
        return LOGS.keySet();
    }
}
