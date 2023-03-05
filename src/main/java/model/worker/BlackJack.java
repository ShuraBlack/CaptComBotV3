package model.worker;

import com.shurablack.core.connection.ConnectionPool;
import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.LocalCache;
import com.shurablack.core.util.ServerUtil;
import model.database.models.GamePlayerFullModel;
import model.game.blackjack.BlackJackGame;
import model.manager.DiscordBot;
import com.shurablack.sql.SQLRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.concurrent.TimeUnit;

import static model.database.Statement.*;

public class BlackJack extends EventWorker {

    private static final BlackJackGame GAME = new BlackJackGame();

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {

        if (message.equals("!bj info") && member.hasPermission(Permission.ADMINISTRATOR)) {
            createGameMessage(channel.asTextChannel());
        } else if (message.equals("!bj reset") && member.hasPermission(Permission.ADMINISTRATOR)) {
            GAME.resetAll();
            updateGameMessage(channel.asTextChannel());
        }
    }

    @Override
    public void processPublicReactionEvent(Member member, MessageChannelUnion channel, String name, MessageReactionAddEvent event) {

        event.getReaction().removeReaction(member.getUser()).queue();

        if (!GAME.isTurn(member.getId())) {
            return;
        }

        String emote = event.getReaction().getEmoji().getName();

        if (emote.equals("☝️")) {
            GAME.playerDraw();
        } else if (emote.equals("✋")) {
            GAME.nextPlayer();
        } else if (emote.equals("\uD83E\uDD1D")) {
            GAME.doubleDown();
        }

        TextChannel textChannel = channel.asTextChannel();

        while (GAME.isFinished()) {
            GAME.dealerDraw();
            channel.editMessageEmbedsById(LocalCache.getMessageID("blackjack_board"), GAME.createResult().build()).complete();
            EmbedBuilder kickMessage = GAME.reset();
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (kickMessage != null) {
                channel.sendMessageEmbeds(kickMessage.build()).complete().delete().queueAfter(20, TimeUnit.SECONDS);
            }

            if (!GAME.isEmpty()) {
                GAME.setState(BlackJackGame.State.STARTING);
                updateGameMessage(textChannel);
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (GAME.isEmpty()) {
                    GAME.resetAll();
                    updateGameMessage(textChannel);
                    return;
                }

                if (GAME.startGame()) {
                    updateGameMessage(textChannel);
                } else {
                    return;
                }
            } else {
                GAME.resetAll();
                updateGameMessage(textChannel);
                return;
            }
        }
        updateGameMessage(textChannel);
    }

    @Override
    public void processButtonEvent(Member member, MessageChannelUnion channel, String textID, ButtonInteractionEvent event) {
        switch (textID) {
            case "join":
                if (GAME.isPlaying(member.getId()) || GAME.inQueue(member.getId())) {
                    return;
                }

                TextInput betJoin = TextInput.create("bet", "Wetteinsatz", TextInputStyle.SHORT)
                        .setPlaceholder("Zahl mit 1:100 Coin/s")
                        .setRequired(true)
                        .setRequiredRange(1,17)
                        .build();

                Modal modalJoin = Modal.create("bj join", "Blackjack - Beitreten")
                        .addActionRows(ActionRow.of(betJoin))
                        .build();

                event.replyModal(modalJoin).queue();
                break;
            case "leave":
                if (GAME.isPlaying(member.getId())) {
                    if (GAME.getState().equals(BlackJackGame.State.PLAYING) || GAME.getState().equals(BlackJackGame.State.RESULT)) {
                        sendFeedBack(String.format("%s, du kannst den Tisch nur außerhalb eines Spieles verlassen!"
                                , member.getAsMention()), ServerUtil.RED, event);
                        return;
                    }
                    GAME.removePlayer(member.getId());

                    EmbedBuilder eb = new EmbedBuilder().setDescription(String.format("%s, verlässt den Tisch!", member.getAsMention()));
                    if (GAME.isEmpty()) {
                        GAME.resetAll();
                    }
                    updateGameMessage(channel.asTextChannel());
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                }
                break;
            case "change":
                if (GAME.isPlaying(member.getId())) {
                    TextInput betChange = TextInput.create("bet", "Wetteinsatz", TextInputStyle.SHORT)
                            .setPlaceholder("Zahl mit 1:100 Coin/s")
                            .setRequired(true)
                            .setRequiredRange(1,17)
                            .build();

                    Modal modalChange = Modal.create("bj change", "Blackjack - Neuer Wetteinsatz")
                            .addActionRows(ActionRow.of(betChange))
                            .build();

                    event.replyModal(modalChange).queue();
                }
                break;
        }
    }

    @Override
    public void processModalEvent(Member member, MessageChannelUnion channel, String textID, ModalInteractionEvent event) {
        ConnectionPool pool = DiscordBot.UTIL.getConnectionPool();
        switch (event.getModalId()) {
            case "bj join":
                SQLRequest.Result<GamePlayerFullModel> player = SQLRequest.runSingle(pool,SELECT_GAME_FULL(member.getId()), GamePlayerFullModel.class);

                if (!player.isPresent()) {
                    sendFeedBack(String.format("%s, erstelle dir zuvor im Hub ein Konto!", member.getAsMention()), ServerUtil.RED, event);
                    return;
                }

                try {
                    long bet = Long.parseLong(event.getValue("bet").getAsString());

                    if (player.value.getMoney() < bet * 100) {
                        sendFeedBack(String.format("%s, du besitzt nicht genug <:gold_coin:886658702512361482>!", member.getAsMention()), ServerUtil.RED, event);
                        return;
                    }

                    int result = GAME.addPlayer(member.getEffectiveName(), member.getId(), member.getEffectiveAvatarUrl()
                            , bet,player.value.getMoney(), player.value.getBlackjack(), player.value.getDeck());

                    if (result > 0 ) {
                        sendFeedBack(String.format("%s, der Tisch ist bereits voll oder am spielen!\n" +
                                "Du wurdest in die Warteschlage verschoben. (Platz: %d)", member.getAsMention(), result), ServerUtil.BLUE, event);
                        return;
                    } else if (result < 0) {
                        sendFeedBack(String.format("%s, Es ist ein Fehler aufgetreten", member.getAsMention()), ServerUtil.RED, event);
                        return;
                    }

                    TextChannel textChannel = channel.asTextChannel();

                    EmbedBuilder eb = new EmbedBuilder().setDescription(String.format("%s, hat sich an den Tisch gesetzt", member.getAsMention()));
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                    if (GAME.playerSize() > 1) {
                        updateGameMessage(textChannel);
                        return;
                    }

                    GAME.setState(BlackJackGame.State.STARTING);
                    updateGameMessage(textChannel);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (GAME.isEmpty()) {
                        GAME.resetAll();
                        updateGameMessage(textChannel);
                        return;
                    }

                    if (!GAME.startGame()) {
                        return;
                    }

                    while (GAME.isFinished()) {
                        GAME.dealerDraw();
                        channel.editMessageEmbedsById(LocalCache.getMessageID("blackjack_board"), GAME.createResult().build()).complete();
                        EmbedBuilder kickMessage = GAME.reset();
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (kickMessage != null) {
                            channel.sendMessageEmbeds(kickMessage.build()).complete().delete().queueAfter(20, TimeUnit.SECONDS);
                        }

                        if (!GAME.isEmpty()) {
                            GAME.setState(BlackJackGame.State.STARTING);
                            updateGameMessage(textChannel);
                            try {
                                TimeUnit.SECONDS.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (GAME.isEmpty()) {
                                GAME.resetAll();
                                updateGameMessage(textChannel);
                                return;
                            }

                            if (GAME.startGame()) {
                                updateGameMessage(textChannel);
                            } else {
                                return;
                            }
                        } else {
                            GAME.resetAll();
                            updateGameMessage(textChannel);
                            return;
                        }
                    }
                    updateGameMessage(textChannel);
                } catch (NumberFormatException ignore) {}
                break;
            case "bj change":
                if (!GAME.isPlaying(member.getId())) {
                    return;
                }

                if (GAME.getState().equals(BlackJackGame.State.PLAYING)) {
                    sendFeedBack(String.format("%s, du kannst dein Wetteinsatz nicht während eines Spiels ändern!"
                            , member.getAsMention()), ServerUtil.RED, event);
                    return;
                }

                try {
                    long bet = Long.parseLong(event.getValue("bet").getAsString());
                    if (!GAME.getState().equals(BlackJackGame.State.WAITING) && !GAME.getState().equals(BlackJackGame.State.STARTING)) {
                        return;
                    }

                    if (GAME.updateMoney(member.getId(), bet)) {
                        sendFeedBack(String.format("%s, dein Wetteinsatz wurde erfolgreich auf %s <:gold_coin:886658702512361482> gewechselt!"
                                , member.getAsMention(), BlackJackGame.formatNumber(bet * 100)), ServerUtil.GREEN, event);
                        updateGameMessage(channel.asTextChannel());
                        return;
                    }

                    sendFeedBack(String.format("%s, dein Wetteinsatz konnte nicht geändert werden!"
                            , member.getAsMention()), ServerUtil.RED, event);
                } catch (NumberFormatException ignore) {}
                break;
        }
    }

    private void createGameMessage(TextChannel channel) {
        EmbedBuilder info = new EmbedBuilder()
                .setTitle("♣️ ♦️ BlackJack - Game ♠️ ♥️")
                .setDescription("Hier erfährst du die wichtigsten Befehle und Interaktionen um Blackjack spielen zu können")
                .addField("Regeln:","```\nJeder Spieler bekommt zum start 2 Karten (Dealer inkl. wobei eine Karte verdeckt liegt). Danach wird nacheinander gezogen." +
                        " Ziel ist es so nah wie möglich an 21 heran zu kommen. Hierbei zählen Nummern-Karten mit Augenzahl, " +
                        "Bube/Dame/König als 10 und ein Ass als 1. Du gewinnst wenn deine Karten näher an 21 sind als die des Dealers (2x dein Einsatz)\n```",false)
                .addField("Interaktion:","```\nNutze ☝️ für draw, ✋ für stand & \uD83E\uDD1D für double down\n```",false)
                .addField("Info:","```\nNachdem ein Spieler beigetreten ist bleiben 10 Sekunden bis die Runde startet. Nach jeder Runde wird ebenfalls 10 Sekunden das Ergebniss angezeigt." +
                        " MAX 4 Spieler sind erlaubt und verändern der Wette geht nur in der STARTING/WAITING Phase\n```",false)
                .setFooter("Der Dealer spielt nach Soft17");

        EmbedBuilder board = new EmbedBuilder()
                .setTitle("Tisch")
                .setDescription("**Spieleranzahl:** []\n**Aktiver Spieler:** [NONE]\n**Phase:** [WAITING]\n**Deck:** [0]")
                .addField("Dealer [0][x]:","Wartet...",false)
                .setImage("https://www.gamblingsites.org/app/themes/gsorg2018/images/blackjack-hard-hand-example-3.png");

        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addEmbeds(board.build())
                .setComponents(
                        ActionRow.of(
                            Button.primary("bj join", "Betreten"),
                            Button.secondary("bj change","Wetteinsatz"),
                            Button.danger("bj leave","Verlassen")
                        ));

        channel.sendMessageEmbeds(info.build()).queue();
        String mesID = channel.sendMessage(builder.build()).complete().getId();
        channel.addReactionById(mesID, Emoji.fromUnicode("☝️")).queue();
        channel.addReactionById(mesID, Emoji.fromUnicode("✋")).queue();
        channel.addReactionById(mesID, Emoji.fromUnicode("\uD83E\uDD1D")).queue();
    }

    private void updateGameMessage(TextChannel channel) {
        channel.editMessageEmbedsById(LocalCache.getMessageID("blackjack_board"), GAME.createMessage().build()).queue();
    }

    public static String getCards() {
        return GAME.getTopCards();
    }

    public static void sendFeedBack(String message, int color, StringSelectInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setDescription(message);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
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

}
