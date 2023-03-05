package model.game.event;

import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.util.LocalCache;
import com.shurablack.core.util.ServerUtil;
import model.manager.DiscordBot;
import model.service.task.WeeklyGameEventTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONObject;

import java.io.*;
import java.util.concurrent.ThreadLocalRandom;

public class EventManager {

    private static double DAILY_MULTIPLY = 1.0;
    private static double CHEST_DISCOUNT = 1.0;
    private static double BLACKJACK_PAYOUT = 1.0;

    public static double getDailyMultiply() {
        return DAILY_MULTIPLY;
    }

    public static double getChestDiscount() {
        return CHEST_DISCOUNT;
    }

    public static double getBlackjackPayout() {
        return BLACKJACK_PAYOUT;
    }

    public static void save() {
        try {
            FileWriter writer = new FileWriter("event.json");
            JSONObject object = new JSONObject();
            object.put("daily_multiply",DAILY_MULTIPLY);
            object.put("chest_discount",CHEST_DISCOUNT);
            object.put("blackjack_payout",BLACKJACK_PAYOUT);
            writer.write(object.toString(4));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        File file = new File("event.json");
        if (!file.exists()) {
            new WeeklyGameEventTask(ServerUtil.GLOBAL_LOGGER).run();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("event.json"))) {
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
               s.append(line);
            }

            JSONObject save = new JSONObject(s.toString());
            DAILY_MULTIPLY = save.getDouble("daily_multiply");
            CHEST_DISCOUNT = save.getDouble("chest_discount");
            BLACKJACK_PAYOUT = save.getDouble("blackjack_payout");
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateMessage(JDAUtil.JDA.getGuildById(DiscordBot.GUILD).getTextChannelById(LocalCache.getChannelID("game_info")));
    }

    public static void createEvent() {
        int chance = ThreadLocalRandom.current().nextInt(0,3);

        switch (chance) {
            case 0:
                double[] dailyPercent = {1.2,1.5,1.7,2.0,2.5,3.0};
                DAILY_MULTIPLY = dailyPercent[ThreadLocalRandom.current().nextInt(0, dailyPercent.length)];
                break;
            case 1:
                double[] chestDiscount = {0.95,0.95,0.95,0.95,0.95,0.90,0.90,0.85};
                CHEST_DISCOUNT = chestDiscount[ThreadLocalRandom.current().nextInt(0, chestDiscount.length)];
                break;
            case 2:
                double[] blackjackPercent = {1.2,1.5,1.7,2.0,2.5,3.0};
                BLACKJACK_PAYOUT = blackjackPercent[ThreadLocalRandom.current().nextInt(0, blackjackPercent.length)];
                break;
        }
    }

    public static void updateMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Dauerhaftes Event");
        eb.setDescription("Jeden _Sonntag um 15:00 Uhr_ wird ein neues Event gestartet.\nDieses Event besteht für die darauf folgende Woche.\n\n" +
                "**Folgende Effekte stehen zur Verfügung:**\nDaily Multiplikator, Mystery Box Rabatt\n& Blackjack Payout");

        if (DAILY_MULTIPLY != 1.0) {
            eb.setThumbnail("https://www.iconpacks.net/icons/2/free-dollar-coin-icon-2149-thumb.png");
            eb.addField("Aktuelles Event:","> Daily Multiplier **" + DAILY_MULTIPLY + "x**",false);
        } else if (CHEST_DISCOUNT != 1.0) {
            eb.setThumbnail("https://cdn.discordapp.com/attachments/990065719217647686/991124506338934934/box.png");
            eb.addField("Aktuelles Event:",String.format("> Chest Rabatt in höhe von **%.2f%%**", ((1.0 - CHEST_DISCOUNT)*100)),false);
        } else if (BLACKJACK_PAYOUT != 1.0) {
            eb.setThumbnail("https://cdn.discordapp.com/attachments/990065719217647686/991118602273050724/deck.png");
            eb.addField("Aktuelles Event:","> Mehr Blackjack Payout **" + BLACKJACK_PAYOUT + "x**",false);
        }

        channel.editMessageEmbedsById(LocalCache.getMessageID("game_event"),eb.build()).queue();
    }

    public static void reset() {
        DAILY_MULTIPLY = 1.0;
        CHEST_DISCOUNT = 1.0;
        BLACKJACK_PAYOUT = 1.0;
    }
}
