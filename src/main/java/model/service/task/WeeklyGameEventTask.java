package model.service.task;

import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.util.LocalCache;
import model.game.event.EventManager;
import model.manager.DiscordBot;
import org.apache.logging.log4j.Logger;

public class WeeklyGameEventTask implements Runnable {

    private final Logger logger;

    public WeeklyGameEventTask(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        this.logger.info("Starting WeeklyGameEventTask CronJob ...");

        EventManager.reset();
        EventManager.createEvent();
        EventManager.updateMessage(JDAUtil.JDA.getGuildById(DiscordBot.GUILD)
                .getTextChannelById(LocalCache.getChannelID("game_info")));
        EventManager.save();

        this.logger.info("Successfully finished WeeklyGameEventTask CronJob");
    }

}
