package model.service.task;

import model.worker.Ruler;
import org.apache.logging.log4j.Logger;

public class RulerReenableUserTask implements Runnable {

    private final Logger logger;

    public RulerReenableUserTask(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        this.logger.info("Starting RulerReenableUserTask CronJob ...");

        Ruler.INTERACTIONS.clear();

        this.logger.info("Successfully finished RulerReenableUserTask CronJob");
    }

}
