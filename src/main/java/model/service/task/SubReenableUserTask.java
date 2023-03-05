package model.service.task;

import model.worker.Ruler;
import org.apache.logging.log4j.Logger;

public class SubReenableUserTask implements Runnable{

    private final Logger logger;

    public SubReenableUserTask(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        this.logger.info("Starting SubReenableUserTask CronJob ...");

        Ruler.INTERACTIONS.clear();

        this.logger.info("Successfully finished SubReenableUserTask CronJob");
    }
}
