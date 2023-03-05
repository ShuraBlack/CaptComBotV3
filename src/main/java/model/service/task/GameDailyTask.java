package model.service.task;

import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.connection.ConnectionPool;
import com.shurablack.core.util.LocalCache;
import model.database.models.GamePlayerModel;
import model.database.models.RatingModel;
import model.game.ChestRequest;
import model.game.blackjack.BlackJackGame;
import model.manager.DiscordBot;
import com.shurablack.sql.SQLRequest;
import model.worker.Game;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static model.database.Statement.*;

public class GameDailyTask implements Runnable {

    private final Logger logger;

    public GameDailyTask(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        this.logger.info("Starting GameDailyTask CronJob ...");

        ConnectionPool pool = DiscordBot.UTIL.getConnectionPool();
        SQLRequest.run(pool,GAME_DAILY());

        ChestRequest.clearHistory();

        final String[] medals = {"\uD83E\uDD47","\uD83E\uDD48","\uD83E\uDD49","\uD83D\uDCA2"};

        Guild guild = JDAUtil.JDA.getGuildById(DiscordBot.GUILD);
        List<GamePlayerModel> list = SQLRequest.runList(pool,SELECT_ORDER_GAME_MONEY(), GamePlayerModel.class);
        SQLRequest.Result<Integer> player = SQLRequest.runScalar(pool,SELECT_GAME_COUNT(),Integer.class);
        EmbedBuilder leaderboard = new EmbedBuilder()
                .setThumbnail("https://s20.directupload.net/images/220619/4vo59odf.png")
                .setTitle("Bestenliste")
                .setFooter("Letzte Aktualisierung")
                .setTimestamp(OffsetDateTime.now());
        if (player.isPresent()) {
            leaderboard.setDescription("**Registrierte Spieler:** " + player.value);
        }
        if (list.size() == 3) {
            StringBuilder s = new StringBuilder();
            for (int i = 0 ; i < 3 ; i++) {
                GamePlayerModel model = list.get(i);
                Member member = guild.retrieveMemberById(model.getUserid()).complete();
                s.append(medals[i]).append(" ").append("__").append(member.getUser().getAsTag()).append("__").append("\n")
                        .append(BlackJackGame.formatNumber(model.getMoney())).append(" <:gold_coin:886658702512361482>").append("\n\n");
            }
            leaderboard.addField("Meistes Geld",s.toString(),true);
        }
        list.clear();

        List<RatingModel> ratings = SQLRequest.runList(pool,SELECT_RATING(), RatingModel.class);
        if (ratings.size() >= 3) {
            ratings.sort(Comparator.comparingDouble(RatingModel::getBlackjack).reversed());
            StringBuilder s = new StringBuilder();
            for (int i = 0 ; i < 3 ; i++) {
                RatingModel model = ratings.get(i);
                Member member = guild.retrieveMemberById(model.getUserid()).complete();
                s.append(medals[i]).append(" ").append("__").append(member.getUser().getAsTag()).append("__").append("\n")
                        .append(Game.pointsToRank((int)model.getBlackjack())).append(" ").append((int)model.getBlackjack()).append("\n\n");
            }
            leaderboard.addField("Blackjack",s.toString(),true);

            s.delete(0, s.length()-1);
            ratings.sort(Comparator.comparingDouble(RatingModel::getChest).reversed());
            for (int i = 0 ; i < 3 ; i++) {
                RatingModel model = ratings.get(i);
                Member member = guild.retrieveMemberById(model.getUserid()).complete();
                s.append(medals[i]).append(" ").append("__").append(member.getUser().getAsTag()).append("__").append("\n")
                        .append(Game.pointsToRank((int)model.getChest())).append(" ").append((int)model.getChest()).append("\n\n");
            }
            leaderboard.addField("Chest Opening",s.toString(),true);

            ratings.clear();
        }

        guild.getTextChannelById(LocalCache.getChannelID("hub")).editMessageEmbedsById(LocalCache.getMessageID("game_leaderboard"), leaderboard.build()).queue();

        this.logger.info("Successfully finished GameDailyTask CronJob");
    }

}
