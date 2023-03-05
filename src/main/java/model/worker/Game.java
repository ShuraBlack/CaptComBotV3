package model.worker;

import com.shurablack.core.connection.ConnectionPool;
import com.shurablack.core.event.EventWorker;
import model.database.models.GamePlayerFullModel;
import model.database.models.GamePlayerModel;
import model.game.ChestRequest;
import model.game.blackjack.BlackJackGame;
import model.game.event.EventManager;
import model.manager.DiscordBot;
import model.service.LogService;
import com.shurablack.sql.SQLRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.shurablack.core.util.ServerUtil.GREEN;
import static com.shurablack.core.util.ServerUtil.RED;
import static model.database.Statement.*;

public class Game extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (message.startsWith("!game add")) {
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                return;
            }

            String[] args = message.split(" ");

            SQLRequest.run(DiscordBot.UTIL.getConnectionPool(), UPDATE_GAME_MONEY(args[2], Long.parseLong(args[3])));
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(GREEN)
                    .setDescription(String.format("%s <:gold_coin:886658702512361482> wurden erfolgreich dem Konto **ID:%s** hinzugefügt!", formatNumber(Long.parseLong(args[3])), args[2]));
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(10, TimeUnit.SECONDS);
        } else if (message.equals("!game hub")) {
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                return;
            }
            //sendLeaderBoard(channel);
            sendRatingOverview(channel.asTextChannel());
            sendGameInfo(channel.asTextChannel());
        } else if (message.equals("!game shop")) {
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                return;
            }
            sendShop(channel.asTextChannel());
        } else if (message.equals("!game info")) {
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                return;
            }
            sendInfoBoard(channel.asTextChannel());
            sendEventMessage(channel.asTextChannel());
        }
    }

    @Override
    public void processButtonEvent(Member member, MessageChannelUnion channel, String textID, ButtonInteractionEvent event) {
        ConnectionPool pool = DiscordBot.UTIL.getConnectionPool();
        SQLRequest.Result<GamePlayerFullModel> player = SQLRequest.runSingle(pool, SELECT_GAME_FULL(member.getId()), GamePlayerFullModel.class);
        switch (textID) {
            case "game daily":
                if (!player.isPresent()) {
                    SQLRequest.run(pool, INSERT_GAME(member.getId()));
                    SQLRequest.run(pool, UPDATE_GAME_DAILY(member.getId(), 7000L));

                    sendFeedBack(String.format("**User:** %s\nDu hast dein tägliches Geld abgeholt\n**Kontostand:** 7000 <:gold_coin:886658702512361482>", member.getAsMention()), GREEN, event);
                    LogService.addLog("game_event",String.format("%s (%s) created a new Game account", member.getEffectiveName(), member.getId()));
                } else {
                    if (player.value.getDaily()) {
                        long daily = player.value.getDaily_bonus() + 2000L;
                        daily *= EventManager.getDailyMultiply();
                        SQLRequest.run(pool, UPDATE_GAME_DAILY(member.getId(), daily));

                        sendFeedBack(String.format("**User:** %s\nDu hast deine tägliche Belohnnung abgeholt\n**Kontostand:** %s <:gold_coin:886658702512361482>"
                                , member.getAsMention(), formatNumber(player.value.getMoney() + daily)), GREEN, event);
                        LogService.addLog("game_event",String.format("%s (%s) received his daily login", member.getEffectiveName(), member.getId()));
                    } else {

                        sendFeedBack(String.format("**User:** %s\nDu kannst aktuell deine tägliche Belohnung noch nicht abholen!" +
                                "\n**Verfügbar:** 03:00 \n**Kontostand:** %s <:gold_coin:886658702512361482>", member.getAsMention(), formatNumber(player.value.getMoney())), RED, event);
                    }
                }
                break;
            case "game stats":
                if (!player.isPresent()) {
                    SQLRequest.run(pool, INSERT_GAME(member.getId()));
                    event.replyEmbeds(sendStats(new GamePlayerFullModel(member.getId(), true, "",0, 5000L, "DEFAULT","DEFAULT,",0.0, 0.0), member)
                            .build()).setEphemeral(true).queue();
                } else {
                    event.replyEmbeds(sendStats(player.value, member).build()).setEphemeral(true).queue();
                }
                break;
            case "game send":
                TextInput receiverSend = TextInput.create("receiver", "An:", TextInputStyle.SHORT)
                        .setPlaceholder("ID des Users (z.B. 286628057551208450)")
                        .setRequiredRange(18,18)
                        .setRequired(true)
                        .build();

                TextInput amount = TextInput.create("amount", "Summe:", TextInputStyle.SHORT)
                        .setPlaceholder("Menge die du senden willst")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                Modal modalSend = Modal.create("game send", "Game - Transfer")
                        .addActionRows(ActionRow.of(receiverSend), ActionRow.of(amount))
                        .build();

                event.replyModal(modalSend).queue();
                break;
            case "game boost":
                TextInput receiverBoost = TextInput.create("receiver", "User ID:", TextInputStyle.SHORT)
                        .setPlaceholder("z.B. 286628057551208450")
                        .setRequiredRange(18,18)
                        .setRequired(true)
                        .build();

                Modal modalBoost = Modal.create("game boost", "Game - Player Boost")
                        .addActionRows(ActionRow.of(receiverBoost))
                        .build();

                event.replyModal(modalBoost).queue();
                break;
            case "game deck":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED, event);
                    return;
                }

                TextInput deckAvailable = TextInput.create("available", "Deine Verfügbaren Decks:", TextInputStyle.SHORT)
                        .setPlaceholder(player.value.getDeck())
                        .setRequiredRange(1,1)
                        .setRequired(false)
                        .build();

                TextInput deckSelect = TextInput.create("select", "Deck:", TextInputStyle.SHORT)
                        .setPlaceholder("Trage eins von den oben stehenden Decks ein")
                        .setRequired(true)
                        .build();

                Modal modalDeck = Modal.create("game deck", "Game - Deck auswahl")
                        .addActionRows(ActionRow.of(deckAvailable), ActionRow.of(deckSelect))
                        .build();

                event.replyModal(modalDeck).queue();
                break;
            case "game game search":
                TextInput search = TextInput.create("userid", "User-ID:", TextInputStyle.SHORT)
                        .setPlaceholder("ID des Users (z.B. 286628057551208450)")
                        .setRequiredRange(18,18)
                        .setRequired(true)
                        .build();

                Modal modalSearch = Modal.create("game search", "Game - Suche")
                        .addActionRows(ActionRow.of(search))
                        .build();

                event.replyModal(modalSearch).queue();
                break;
            case "game shop_money_maker":
                if (member.getRoles().stream().map(ISnowflake::getId).anyMatch(id -> id.equals("988280763831173170"))) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt bereits diesen Rang!", member.getAsMention()), RED, event);
                    return;
                }

                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED, event);
                    return;
                }

                if (player.value.getMoney() < 3000000L) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(player.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_MONEY(member.getId(),-3000000));
                member.getGuild().addRoleToMember(UserSnowflake.fromId(member.getId()), member.getGuild().getRoleById("988280763831173170")).queue();

                sendFeedBack(String.format("**User:** %s\nDu besitzt nun den Rang des Money Makers!", member.getAsMention()), GREEN, event);
                LogService.addLog("game_event",String.format("%s (%s) bought the @Money_Maker rank", member.getEffectiveName(), member.getId()));
                break;
            case "game shop_blackjack_peaker":
                if (member.getRoles().stream().map(ISnowflake::getId).anyMatch(id -> id.equals("988280464626311180"))) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt bereits diesen Rang!", member.getAsMention()), RED, event);
                    return;
                }

                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED, event);
                    return;
                }

                if (player.value.getBlackjack() < 1500) {
                    sendFeedBack(String.format("**User:** %s\nDein _Blackjack Rating_ ist zu niedrig!\n**Blackjack:** "
                            + player.value.getBlackjack() + pointsToRank((int)player.value.getBlackjack()), member.getAsMention()), RED, event);
                    return;
                }

                member.getGuild().addRoleToMember(UserSnowflake.fromId(member.getId()), member.getGuild().getRoleById("988280763831173170")).queue();

                sendFeedBack(String.format("**User:** %s\nKeiner zählt die Karten so wie du es tust!", member.getAsMention()), GREEN, event);
                LogService.addLog("game_event",String.format("%s (%s) bought the @Blackjack Peaker rank", member.getEffectiveName(), member.getId()));
                break;
            case "game shop_daily_bonus":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                TextInput dailySum = TextInput.create("amount","Menge", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("Die Menge die du haben willst")
                        .build();

                Modal dailyModal = Modal.create("game daily","Game - Daily einkaufen")
                        .addActionRows(ActionRow.of(dailySum))
                        .build();

                event.replyModal(dailyModal).queue();
                break;
            case "game shop_deck_elegant":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getDeck().contains("ELEGANT")) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt bereits das Deck!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getMoney() < 100000L) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(player.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_MONEY(member.getId(),-100000));
                SQLRequest.run(pool, UPDATE_GAME_DECKS(member.getId(), player.value.getDeck() + "ELEGANT,"));
                sendFeedBack(String.format("**User:** %s\nDu besitzt nun das Elegant Deck!", member.getAsMention()), GREEN, event);
                LogService.addLog("game_event",String.format("%s (%s) bought the Elegant deck", member.getEffectiveName(), member.getId()));
                break;
            case "game game shop_deck_jewel":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getDeck().contains("JEWEL")) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt bereits das Deck!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getMoney() < 250000L) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(player.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_MONEY(member.getId(),-250000));
                SQLRequest.run(pool, UPDATE_GAME_DECKS(member.getId(), player.value.getDeck() + "JEWEL,"));
                sendFeedBack(String.format("**User:** %s\nDu besitzt nun das Jewel Deck!", member.getAsMention()), GREEN, event);
                LogService.addLog("game_event",String.format("%s (%s) bought the Jewel deck", member.getEffectiveName(), member.getId()));
                break;
            case "game shop_deck_royal":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getDeck().contains("ROYAL")) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt bereits das Deck!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getMoney() < 1000000L) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(player.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_MONEY(member.getId(),-1000000));
                SQLRequest.run(pool, UPDATE_GAME_DECKS(member.getId(), player.value.getDeck() + "ROYAL,"));
                sendFeedBack(String.format("**User:** %s\nDu besitzt nun das Royal Deck!", member.getAsMention()), GREEN, event);
                LogService.addLog("game_event",String.format("%s (%s) bought the Royal deck", member.getEffectiveName(), member.getId()));
                break;
            case "game shop_chest":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                TextInput elegantChest = TextInput.create("elegant", "Elegant Chest/s", TextInputStyle.SHORT)
                        .setValue("0")
                        .setPlaceholder("Zahl größer >= 0")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                TextInput jewelChest = TextInput.create("jewel", "Jewel Chest/s", TextInputStyle.SHORT)
                        .setValue("0")
                        .setPlaceholder("Zahl größer >= 0")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                TextInput royalChest = TextInput.create("royal", "Royal Chest/s", TextInputStyle.SHORT)
                        .setValue("0")
                        .setPlaceholder("Zahl größer >= 0")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                Modal modal = Modal.create("game bundle", "Game - Box Bundle kaufen")
                        .addActionRows(ActionRow.of(elegantChest), ActionRow.of(jewelChest), ActionRow.of(royalChest))
                        .build();

                event.replyModal(modal).queue();
                break;
            case "game shop_chest_max":
                if (!player.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto!", member.getAsMention()), RED,event);
                    return;
                }

                ChestRequest request = new ChestRequest(player.value.simplify());
                ChestRequest.getLeftChests(request);

                long price = request.getPrice();

                if (price == 0L) {
                    sendFeedBack(String.format("**User:** %s\nDu hast bereits das Limit erreicht!", member.getAsMention()), RED,event);
                    return;
                }

                if (player.value.getMoney() < price) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(player.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                ChestRequest.getElegentWorker().accept(request);
                ChestRequest.getJewelWorker().accept(request);
                ChestRequest.getRoyalWorker().accept(request);

                SQLRequest.run(pool, UPDATE_GAME_BUNDLE(request));
                event.replyEmbeds(ChestRequest.requestToMessage(member, request).build()).setEphemeral(true).queue();
                LogService.addLog("game_event", String.format("%s (%s) bought a bundle (%d E, %d J, %d R) and received %s Unit/s"
                        , member.getEffectiveName(), member.getId(), request.getElegant(), request.getJewel(), request.getRoyal(), BlackJackGame.formatNumber(request.getMoney())));

                break;
        }
    }

    @Override
    public void processModalEvent(Member member, MessageChannelUnion channel, String textID, ModalInteractionEvent event) {
        ConnectionPool pool = DiscordBot.UTIL.getConnectionPool();
        switch (textID) {
            case "game send": {
                SQLRequest.Result<GamePlayerModel> sender = SQLRequest.runSingle(pool,SELECT_GAME(member.getId()), GamePlayerModel.class);
                if (!sender.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt noch kein Konto um Geld zu senden!", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.Result<GamePlayerModel> receiver = SQLRequest.runSingle(pool,SELECT_GAME
                        (event.getValue("receiver").getAsString()), GamePlayerModel.class);
                if (!receiver.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nKonnte angegebenen User nicht finden!", member.getAsMention()), RED, event);
                    return;
                }

                long amount;
                try {
                    amount = Long.parseLong(event.getValue("amount").getAsString());
                } catch (NumberFormatException ignored) {
                    sendFeedBack(String.format("**User:** %s\nBitte trage eine gültige Geld summe ein! (Nur Zahlen)", member.getAsMention()), RED, event);
                    return;
                }

                if (amount == 0) {
                    sendFeedBack(String.format("**User:** %s\nDu musst mehr als 0 <:gold_coin:886658702512361482> senden!", member.getAsMention()), RED, event);
                    return;
                }

                if (amount > sender.value.getMoney()) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug geld!\n**Kontostand:** %s <:gold_coin:886658702512361482>"
                            , member.getAsMention(), sender.value.getMoney()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_MONEY(member.getId(), -amount));
                SQLRequest.run(pool, UPDATE_GAME_MONEY(receiver.value.getUserid(), amount));
                sendFeedBack(String.format("**User:** %s\nDu hast erfolgreich Geld versendet!\n**Kontostand:** %s<:gold_coin:886658702512361482>\n\n**An:** %s\n**Summe:** %s<:gold_coin:886658702512361482>"
                        , member.getAsMention(), formatNumber(sender.value.getMoney() - amount), receiver.value.getUserid(), formatNumber(amount)), GREEN, event);
                LogService.addLog("game_transaction", String.format("%s (%s) sended %s Unit/s to %s", member.getEffectiveName(), member.getId(), amount, receiver.value.getUserid()));
                break;
            }
            case "game boost": {
                SQLRequest.Result<GamePlayerModel> receiver = SQLRequest.runSingle(pool,SELECT_GAME
                        (event.getValue("receiver").getAsString()), GamePlayerModel.class);

                if (!receiver.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nDer User besitzt noch kein Konto!", member.getAsMention()), RED, event);
                    return;
                }

                if (event.getValue("receiver").getAsString().equals(member.getId())) {
                    sendFeedBack(String.format("**User:** %s\nNice try, aber du kannst dich nicht selber boosten!", member.getAsMention()), RED, event);
                    return;
                }

                if (receiver.value.getBooster().contains(member.getId())) {
                    sendFeedBack(String.format("**User:** %s\nDu hast bereits diesen User heute geboostet!", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_BOOST(receiver.value.getUserid(), member.getId(), receiver.value.getBooster()));
                sendFeedBack(String.format("**User:** %s\nDu hast erfolgreich _%s_ geboostet!", member.getAsMention(), receiver.value.getUserid()), GREEN, event);
                LogService.addLog("game_event", String.format("%s (%s) boosted %s", member.getEffectiveName(), member.getId(), receiver.value.getUserid()));
                break;
            }
            case "game deck":
                SQLRequest.Result<GamePlayerModel> player = SQLRequest.runSingle(pool, SELECT_GAME(member.getId()), GamePlayerModel.class);

                String deck = event.getValue("select").getAsString();

                if (!player.value.getDeck().contains(deck)) {
                    sendFeedBack(String.format("**User:** %s\nDieses Deck ist nicht in deinem besitzt oder existiert nicht!", member.getAsMention()), RED, event);
                    return;
                }

                if (player.value.getSelect_deck().equals(deck)) {
                    sendFeedBack(String.format("**User:** %s\nDu hast dieses Deck bereits ausgewählt!", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool, UPDATE_GAME_SELECT_DECK(member.getId(), deck));
                sendFeedBack(String.format("**User:** %s\nDein Deck wurde erfolgreich auf %s gewechselt!", member.getAsMention(), deck), GREEN, event);
                break;
            case "game search":
                String searchID = event.getValue("userid").getAsString();
                SQLRequest.Result<GamePlayerFullModel> searchRequest = SQLRequest.runSingle(pool,SELECT_GAME_FULL(searchID), GamePlayerFullModel.class);

                if (!searchRequest.isPresent()) {
                    sendFeedBack(String.format("**User:** %s\nKonnte Angegebenen User nicht finden!", member.getAsMention()), RED, event);
                    return;
                }

                event.replyEmbeds(sendStats(searchRequest.value,member.getGuild().retrieveMemberById(searchID).complete()).build())
                        .setEphemeral(true).queue();
                break;
            case "game bundle":
                int elegantChest;
                int jewelChest;
                int royalChest;

                String tmp = event.getValue("elegant").getAsString();
                if (notNumeric(tmp)) {
                    sendFeedBack(String.format("**User:** %s\nGib bitte nur Zahlen ein, für das Bundle!", member.getAsMention()), RED, event);
                }
                elegantChest = Integer.parseInt(tmp);

                tmp = event.getValue("jewel").getAsString();
                if (notNumeric(tmp)) {
                    sendFeedBack(String.format("**User:** %s\nGib bitte nur Zahlen ein, für das Bundle!", member.getAsMention()), RED, event);
                }
                jewelChest = Integer.parseInt(tmp);

                tmp = event.getValue("royal").getAsString();
                if (notNumeric(tmp)) {
                    sendFeedBack(String.format("**User:** %s\nGib bitte nur Zahlen ein, für das Bundle!", member.getAsMention()), RED, event);
                }
                royalChest = Integer.parseInt(tmp);

               SQLRequest.Result<GamePlayerModel> creator = SQLRequest.runSingle(pool,SELECT_GAME(member.getId()), GamePlayerModel.class);
                if (!creator.isPresent()) {
                    return;
                }

                ChestRequest request = new ChestRequest(creator.value);
                request.setChestAmount(elegantChest, jewelChest, royalChest);
                if (!ChestRequest.validRequest(request)) {
                    sendFeedBack(String.format("**User:** %s\nDein Einkauf überschreitet das Daily Limit!", member.getAsMention()), RED, event);
                    return;
                }

                long price = request.getPrice();
                request.addMoney(-price);

                if (creator.value.getMoney() < price) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(creator.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                ChestRequest.getElegentWorker().accept(request);
                ChestRequest.getJewelWorker().accept(request);
                ChestRequest.getRoyalWorker().accept(request);

                SQLRequest.run(pool, UPDATE_GAME_BUNDLE(request));
                event.replyEmbeds(ChestRequest.requestToMessage(member, request).build()).setEphemeral(true).queue();
                LogService.addLog("game_event", String.format("%s (%s) bought a bundle (%d E, %d J, %d R) and received %s Unit/s"
                        , member.getEffectiveName(), member.getId(), elegantChest, jewelChest, royalChest, BlackJackGame.formatNumber(request.getMoney())));

                break;
            case "game daily":
                SQLRequest.Result<GamePlayerModel> buyer = SQLRequest.runSingle(pool,SELECT_GAME(member.getId()), GamePlayerModel.class);
                if (!buyer.isPresent()) {
                    return;
                }

                if (notNumeric(event.getValue("amount").getAsString())) {
                    sendFeedBack(String.format("**User:** %s\nGib bitte nur Zahlen ein!", member.getAsMention()), RED, event);
                }

                int amount = Integer.parseInt(event.getValue("amount").getAsString());

                if (amount < 1) {
                    sendFeedBack(String.format("**User:** %s\nGib bitte nur Zahlen ein welche größer als 0 sind!", member.getAsMention()), RED, event);
                }

                if (buyer.value.getMoney() < (amount * 5000L)) {
                    sendFeedBack(String.format("**User:** %s\nDu besitzt nicht genug <:gold_coin:886658702512361482>!\n**Kontostand:** "
                            + BlackJackGame.formatNumber(buyer.value.getMoney()) + " <:gold_coin:886658702512361482>", member.getAsMention()), RED, event);
                    return;
                }

                SQLRequest.run(pool,UPDATE_GAME_DAILY_BONUS(member.getId(), amount * 100));
                sendFeedBack(String.format("**User:** %s\nDein Daily Bonus wurde um %s <:gold_coin:886658702512361482> erhöht\nund ist somit auf %s <:gold_coin:886658702512361482>!"
                        , member.getAsMention(), BlackJackGame.formatNumber(amount * 100L)
                        , BlackJackGame.formatNumber((long) buyer.value.getDaily_bonus() + (amount * 100L))), GREEN, event);
                break;
        }
    }

    private void sendInfoBoard(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder()
                .setThumbnail("https://cdn.discordapp.com/attachments/990065719217647686/1007338272785170442/newspaper.png")
                .setTitle("Info Board")
                .setDescription("In dieser Nachricht werden Neuerungen sowie Änderungen Angezeigt!")
                .addBlankField(false)
                .addField("Chest Changes","```Die Belohnung der Chests wurden verändert und ein Daily Limit wurde eingeführt." +
                        " Chests stehen dadurch wieder zur Verfügung.```",false)
                .setTimestamp(OffsetDateTime.now());

        channel.editMessageEmbedsById("1007341404227387472", eb.build()).queue();
        //channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void sendEventMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Dauerhaftes Event")
                .setDescription("Temporäre Nachricht");

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void sendLeaderBoard(TextChannel channel) {
        EmbedBuilder leaderboard = new EmbedBuilder()
                .setThumbnail("https://s20.directupload.net/images/220619/4vo59odf.png")
                .setTitle("Bestenliste")
                .addField("Meistes Geld","\uD83E\uDD47.\n\uD83E\uDD48.\n\uD83E\uDD49.\n",true)
                .addField("Blackjack","\uD83E\uDD47.\n\uD83E\uDD48.\n\uD83E\uDD49.\n",true)
                .addField("Chest Opening","\uD83E\uDD47.\n\uD83E\uDD48.\n\uD83E\uDD49.\n",true)
                .setFooter("Letzte Aktualisierung")
                .setTimestamp(OffsetDateTime.now());

        String mesID = channel.sendMessageEmbeds(leaderboard.build()).complete().getId();
        System.out.println(mesID);
    }

    private void sendRatingOverview(TextChannel channel) {
        EmbedBuilder rating = new EmbedBuilder()
                .setTitle("Game Rating")
                .setImage("https://cdn.discordapp.com/attachments/990065719217647686/1007421265725902989/ranks_explain.png")
                .setDescription("Dein Rating wird erhöht durch das Gewinnen von Spielen und das eingehen von größeren Risiken!" +
                        " Ränge zwischen Bronze bis Diamant besitzen zusätzlich 5 Divisionen (100 Punkte entsprichen einer Division)" +
                        ".\n\n__Blackjack:__ Pro Runde erhälst/verlierst du 2.5 EP (bis zu 20 EP + 5 extra EP für double down)\n" +
                        "__Mystery Box:__ Je nach Auswahl deiner Chest verdienst du EP (Elegant 0.4, Jewel 1.2, Royal 2.0). " +
                        "Täglich verliert jeder Spieler eine gewisse Menge an EP\n\n");
        channel.sendMessageEmbeds(rating.build()).queue();
    }

    private void sendGameInfo(TextChannel channel) {
        EmbedBuilder hub = new EmbedBuilder()
                .setTitle("Game - Hub")
                .setDescription("Dies sind die allgemeinen Befehle, welche dir zur Verfügung stehen")
                .addField("Daily:","Hol dir einmal pro Tag eine 2.000 <:gold_coin:886658702512361482> Belohnung ab (Reset um 3:00)",false)
                .addField("Statistik:", "Sieh nach, wie viel <:gold_coin:886658702512361482> du hast und in welchem Spiel du am meisten verdienst",false)
                .addField("Transfer:","Über diesen Button kannst du jemand anderen <:gold_coin:886658702512361482> senden.\nDafür musst du in Discord unter den Einstellungen/Erweitert den Entwicklermodus aktivieren",false)
                .addField("Decks:","Ändere dein Deck Aussehen, nachdem du weitere freigeschalten hast",false)
                .addField("Boost:","Damit kannst du jeden anderen Spieler einmal pro Tag boosten (500 <:gold_coin:886658702512361482>)",false)
                .addField("Suche","Suche nach dem Profil eines anderen Spielers",false);

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                .addEmbeds(hub.build())
                .setComponents(
                        ActionRow.of(
                            Button.secondary("game daily", "Daily"),
                            Button.secondary("game stats","Statistik"),
                            Button.secondary("game send","Transfer"),
                            Button.secondary("game deck","Decks"),
                            Button.primary("game boost","Boost")
                        ));
        channel.sendMessage(messageBuilder.build()).queue();

        hub = new EmbedBuilder().setDescription("Template. Please remove!");
        messageBuilder.clear().addEmbeds(hub.build()).setComponents(ActionRow.of(
                Button.secondary("game search","Suche")
        ));
        channel.sendMessage(messageBuilder.build()).queue();
    }

    private void sendShop(TextChannel channel) {
        Role moneyRole = channel.getGuild().getRoleById("988280763831173170");
        Role blackjackRole = channel.getGuild().getRoleById("988280464626311180");

        if (Objects.isNull(moneyRole) || Objects.isNull(blackjackRole)) {
            sendFeedBack("Konnte Rollen nicht laden!", RED,channel);
            return;
        }

        EmbedBuilder shop = new EmbedBuilder()
                .setThumbnail("https://www.iconpacks.net/icons/2/free-dollar-coin-icon-2149-thumb.png")
                .setTitle("Shop")
                .setDescription(String.format("Benutze das unten vorhandene Auswahl Menu um den jeweiligen Artikel zu kaufen\n" +
                        "\n-> **Rang** %s - __3.000.000__ <:gold_coin:886658702512361482>\n-> **Rang** %s - <:gold:1007022493955010631> Gold Elo\n" +
                        "-> Daily um 100 <:gold_coin:886658702512361482> erhöhen - __5.000__ <:gold_coin:886658702512361482>\n", moneyRole.getAsMention(), blackjackRole.getAsMention()));

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                .addEmbeds(shop.build())
                .setComponents(ActionRow.of(
                        Button.secondary("game shop_money_maker", "Kaufe Money Maker"),
                        Button.secondary("game shop_blackjack_peaker","Kaufe Blackjack Peaker"),
                        Button.secondary("game shop_daily_bonus","Kaufe Daily erhöhen")
                ));
        channel.sendMessage(messageBuilder.build()).queue();

        EmbedBuilder deck = new EmbedBuilder()
                .setTitle("Deck´s")
                .setThumbnail("https://cdn.discordapp.com/attachments/990065719217647686/991118602273050724/deck.png")
                .setDescription("Kauf dir Skins für deine Spielmodis, die du im Hub wechseln kannst")
                .addField("","**Elegant** (Deck)\n[Keine Animation]\n__100.000__ <:gold_coin:886658702512361482>\n<:elegant_two:990748540550672465>" +
                        " <:elegant_ass:990748519969214474>\n<:elegant_spades:990748536482185286> <:elegant_clubs:990748522481598534>",true)
                .addField("","**Jewel** (Deck)\n[Keine Animation]\n__250.000__ <:gold_coin:886658702512361482>\n<:jewel_two:991646397524881459>" +
                        " <:jewel_ass:991646398950952971>\n<:jewel_clubs:991646402453196800> <:jewel_diamond:991646405221425183>",true)
                .addField("","**Royal** (Deck)\n[Spezial Ass]\n__1.000.000__ <:gold_coin:886658702512361482>\n<:royal_two:991668023284998245>" +
                        " <a:royal_ass:991668026225209344>\n<:royal_heart:991668013000572949> <:royal_diamonds:991668008143560774>",true);

        messageBuilder.clear().addEmbeds(deck.build())
                .setComponents(ActionRow.of(
                        Button.secondary("game shop_deck_elegant", "Kaufe Elegant"),
                        Button.secondary("game shop_deck_jewel", "Kaufe Jewel"),
                        Button.secondary("game shop_deck_royal","Kaufe Royal")
                ));
        channel.sendMessage(messageBuilder.build()).queue();

        EmbedBuilder lootbox = new EmbedBuilder()
                .setThumbnail("https://cdn.discordapp.com/attachments/990065719217647686/991124506338934934/box.png")
                .setImage("https://cdn.discordapp.com/attachments/990065719217647686/1008099380873678999/chest.png")
                .setTitle("Mystery Chest")
                .setDescription("Du magst den Nervenkitzel? Dann Versuch doch dein Glück und öffne ein paar Kisten.\n" +
                        "```diff\n- Jeder Spieler darf pro Tag 50 Elegant, 25 Jewel & 10 Royal Chests öffnen\n```");

        messageBuilder.clear().addEmbeds(lootbox.build())
                .setComponents(ActionRow.of(
                        Button.primary("game shop_chest", "Chest/s kaufen"),
                        Button.danger("game shop_chest_max", "Max kaufen")
                ));
        channel.sendMessage(messageBuilder.build()).queue();
    }

    private EmbedBuilder sendStats(GamePlayerFullModel model, Member m) {
        return new EmbedBuilder()
                .setAuthor(m.getEffectiveName(), m.getEffectiveAvatarUrl(), m.getEffectiveAvatarUrl())
                .setDescription(String.format("**User:** %s\n**ID:** %s\n**Daily Bonus:** %s\n\n", m.getAsMention(), m.getId()
                        , BlackJackGame.formatNumber((long)model.getDaily_bonus()) + " <:gold_coin:886658702512361482>"))
                .addField("__Kontostand__",String.format("> %s <:gold_coin:886658702512361482>", (model.getMoney() > 0 ? "+ " : "")
                        + formatNumber(model.getMoney()).replace("-","- ")),true)
                .addBlankField(true)
                .addField("__Rating__", String.format("> Blackjack: %s _%.1f_ EP\n> Chest: %s _%.1f EP_"
                        , pointsToRank((int)model.getBlackjack()), model.getBlackjack()
                        , pointsToRank((int) model.getChest()), model.getChest()).replace(".","/"), true);
    }

    public static String pointsToRank(int points) {
        if (points >= 3000) {
            return "<:legende:1007022496433848400>";
        }
        if (points >= 2500) {
            return "<:diamant:1007022492000452789>" + getDivision(points - 2500);
        }
        if (points >= 2000) {
            return "<:platin:1007022498296103024>" + getDivision(points - 2000);
        }
        if (points >= 1500) {
            return "<:gold:1007022493955010631>" + getDivision(points - 1500);
        }
        if (points >= 1000) {
            return "<:silber:1007022487411896320>" + getDivision(points - 1000);
        }
        if (points >= 500) {
            return "<:bronze:1007022490037518526>" + getDivision(points - 500);
        }
        if (points >= -50) {
            return "<:unranked:1007022488754081822>";
        }
        return "(Ruiniert)";
    }

    private static String getDivision(int points) {
        int div = 1 + (points / 100);
        switch (div) {
            case 1: return "Div I";
            case 2: return "Div II";
            case 3: return "Div III";
            case 4: return "Div IV";
            case 5: return "Div V";
            default: return "undefine";
        }
    }

    private String formatNumber(Long value) {
        DecimalFormat df = new DecimalFormat("#,###.##");
        return df.format(value);
    }

    private boolean notNumeric(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return true;
        }
        return false;
    }

    public static void sendFeedBack(String message, int color, ModalInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setDescription(message);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    public static void sendFeedBack(String message, int color, ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setDescription(message);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    public static void sendFeedBack(String message, int color, TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setDescription(message);
        channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
    }
}
