package model.worker;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.LocalCache;
import com.shurablack.core.util.ServerUtil;
import model.manager.DiscordBot;
import model.manager.MusicManager;
import model.player.LoadResultHandler;
import com.shurablack.sql.SQLRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static model.database.Statement.*;

public class Player extends EventWorker {

    @Override
    public void processGuildSlashEvent(Member member, MessageChannelUnion channel, String name, SlashCommandInteractionEvent event) {
        if (!shareChannel(member)) {
            EmbedBuilder eb = new EmbedBuilder().setDescription(member.getAsMention() + ", du musst im Voice Channel des Bots sein");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (!DiscordBot.getMusicManager().scheduler.isPlaying()) {
            EmbedBuilder eb = new EmbedBuilder().setDescription(member.getAsMention() + ", der MusicPlayer ist momentan nicht aktiv");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }
        MusicManager musicManager = DiscordBot.getMusicManager();

        switch (event.getSubcommandName()) {
            case "save":
                AudioTrackInfo info = musicManager.player.getPlayingTrack().getInfo();

                SQLRequest.run(DiscordBot.UTIL.getConnectionPool(), INSERT_SONG(member.getId(), event.getOption("playlist").getAsString(), info.uri));
                EmbedBuilder save = new EmbedBuilder().setDescription(String.format("%s, Song **%s** wurde in der Playlist **%s** gespeichert"
                        , member.getAsMention(), info.title,event.getOption("playlist").getAsString()));
                event.replyEmbeds(save.build()).setEphemeral(true).queue();
                break;
            case "volume":
                int volume = event.getOption("value").getAsInt();
                if (volume > 100 || volume < 0) {
                    EmbedBuilder vol = new EmbedBuilder().setDescription(String.format("%s, Lautstärke ist außerhalb des Rahmens (0-100)", member.getAsMention()));
                    event.replyEmbeds(vol.build()).setEphemeral(true).queue();
                    return;
                }
                EmbedBuilder vol = new EmbedBuilder().setDescription(String.format("%s, Lautstärke wurde auf %d geändert", member.getAsMention(), volume));
                event.replyEmbeds(vol.build()).setEphemeral(true).queue();
                musicManager.player.setVolume(volume);
                musicManager.scheduler.updatePlayerMessage();
                break;
            case "remove":
                int removeTrackNumber = event.getOption("tracknumber").getAsInt();
                if (musicManager.scheduler.removeTrack(removeTrackNumber)) {
                    EmbedBuilder remove = new EmbedBuilder().setDescription(String.format("%s, das ausgewählte Lied wurde entfernt", member.getAsMention()));
                    event.replyEmbeds(remove.build()).setEphemeral(true).queue();
                } else {
                    EmbedBuilder remove = new EmbedBuilder().setDescription(String.format("%s, die Playlist ist leer oder die Zahl ist außerhalb der Track Nummern", member.getAsMention()));
                    event.replyEmbeds(remove.build()).setEphemeral(true).queue();
                }
                break;
            case "start":
                int startTrackNumber = event.getOption("tracknumber").getAsInt();
                if (musicManager.scheduler.playTrack(startTrackNumber)) {
                    EmbedBuilder start = new EmbedBuilder().setDescription(String.format("%s, das ausgewählte Lied wird gestartet", member.getAsMention()));
                    event.replyEmbeds(start.build()).setEphemeral(true).queue();
                } else {
                    EmbedBuilder start = new EmbedBuilder().setDescription(String.format("%s, die Playlist ist leer oder die Zahl ist außerhalb der Track Nummern", member.getAsMention()));
                    event.replyEmbeds(start.build()).setEphemeral(true).queue();
                }
                break;
            case "pop":
                int popTrackNumber = event.getOption("tracknumber").getAsInt();
                if (musicManager.scheduler.popTrack(popTrackNumber)) {
                    EmbedBuilder pop = new EmbedBuilder().setDescription(String.format("%s, das ausgewählte Lied wurde nach oben verschoben", member.getAsMention()));
                    event.replyEmbeds(pop.build()).setEphemeral(true).queue();
                } else {
                    EmbedBuilder pop = new EmbedBuilder().setDescription(String.format("%s, die Playlist ist leer oder die Zahl ist außerhalb der Track Nummern", member.getAsMention()));
                    event.replyEmbeds(pop.build()).setEphemeral(true).queue();
                }
                break;
            case "position":
                EmbedBuilder position = new EmbedBuilder().setDescription(String.format("%s, die aktuelle Position wurde verschoben", member.getAsMention()));
                event.replyEmbeds(position.build()).setEphemeral(true).queue();
                musicManager.scheduler.setPosition(
                        musicManager.scheduler.optionsToTime(
                                event.getOption("hour").getAsInt(),
                                event.getOption("minute").getAsInt(),
                                event.getOption("second").getAsInt()
                        ));
                break;
        }
    }

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        String[] args = message.split(" ");

        if (message.equals("!player")) {

            if (!member.hasPermission(channel.asTextChannel(), Permission.ADMINISTRATOR)) {
                return;
            }

            EmbedBuilder player = createTemplateMessage();
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                    .addEmbeds(player.build())
                    .setComponents(ActionRow.of(
                            Button.secondary("player link", Emoji.fromCustom("links",1048000262779961385L,false)),
                            Button.secondary("player search", Emoji.fromCustom("search",1048000265745350748L,false)),
                            Button.secondary("player autoplay", Emoji.fromCustom("autoplay",1048041193407258624L,false)),
                            Button.secondary("player repeatTrack", Emoji.fromCustom("track_repeat",1048000769212821535L,false)),
                            Button.secondary("player repeatQueue", Emoji.fromCustom("queue_repeat",1048000770710175805L,false))
                    ));
            channel.sendMessage(messageBuilder.build()).complete();

            EmbedBuilder queue = createQueueMessage();
            channel.sendMessageEmbeds(queue.build()).complete();
        } else if (args.length == 2 && args[1].equals("test")) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Capt Community Music Player")
                    .setThumbnail("https://s20.directupload.net/images/210422/zpyz5gkv.png")
                    .setDescription("**Befehle:**\nDer MusicPlayer wird über Slash-Befehle gesteuert.\nSchreibe __/player__ in den Chat und sieh dich um" +
                            "\n**Besuche unseren Web-Player:**\n[Web-Player](http://178.32.109.146:8000/player/)");
            channel.editMessageEmbedsById("1023073010955071559", eb.build()).queue();
            channel.editMessageComponentsById("1023073010955071559").queue();
        }
    }

    @Override
    public void processPublicReactionEvent(Member member, MessageChannelUnion channel, String name, MessageReactionAddEvent event) {
        if (!event.getMessageId().equals(LocalCache.getMessageID("player_main"))) {
            return;
        }

        event.getReaction().removeReaction(member.getUser()).queue();

        if (!ServerUtil.hasRole(member,"871292341053431899")  && !member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        MusicManager musicManager = DiscordBot.getMusicManager();

        if (!member.getGuild().getAudioManager().isConnected()) {
            return;
        }

        VoiceChannel vc = (VoiceChannel) Objects.requireNonNull(member.getVoiceState()).getChannel();

        if (vc == null && !member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        assert vc != null;
        if (vc.getMembers().stream().map(ISnowflake::getId)
                .noneMatch(id -> id.equals(member.getJDA().getSelfUser().getId()))) {
            EmbedBuilder eb = new EmbedBuilder().setDescription(String.format("%s, verbinde dich zuvor mit dem aktiven VoiceChannel", member.getAsMention()));
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
            return;
        }

        String emote = event.getReaction().getEmoji().getName();

        switch (emote) {
            case "⏹": // Stop
                if (musicManager.player.getPlayingTrack() == null) {
                    musicManager.player.setVolume(20);
                    musicManager.player.setPaused(false);
                    musicManager.scheduler.setRepeatQueue(false);
                    musicManager.scheduler.setAutoPlay(false);
                    member.getGuild().getAudioManager().closeAudioConnection();
                }

                musicManager.player.stopTrack();
                musicManager.scheduler.clear();
                musicManager.scheduler.clearMessages();
                break;
            case "⏯": // Resume/Pause
                musicManager.player.setPaused(!musicManager.player.isPaused());
                break;
            case "⏩": // Next
                skipTrack();
                break;
            case "\uD83D\uDD00": //Randomize
                musicManager.scheduler.randomizeQueue();
                break;
            case "\uD83D\uDD3B": // Minus
                int volumeDown = musicManager.player.getVolume() - 10;
                if (volumeDown >= 0) {
                    musicManager.player.setVolume(volumeDown);
                    musicManager.scheduler.updatePlayerMessage();
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder()
                        .setDescription(String.format("%s, Lautstärke ist bereits auf 0", member.getAsMention()));
                channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
                break;
            case "\uD83D\uDD3A": // Plus
                int volumeUp = musicManager.player.getVolume() + 10;
                if (volumeUp <= 100) {
                    musicManager.player.setVolume(volumeUp);
                    musicManager.scheduler.updatePlayerMessage();
                    return;
                }
                EmbedBuilder eb5 = new EmbedBuilder()
                        .setDescription(String.format("%s, Lautstärke ist bereits auf 100", member.getAsMention()));
                channel.sendMessageEmbeds(eb5.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
                break;
        }
    }

    @Override
    public void processButtonEvent(Member member, MessageChannelUnion channel, String textID, ButtonInteractionEvent event) {
        if (!ServerUtil.hasRole(member,"871292341053431899") && !member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        switch (textID) {
            case "player link":
                TextInput link = TextInput.create("url","Link:", TextInputStyle.SHORT)
                        .setPlaceholder("Trage deinen Link hier ein")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                Modal linkModal = Modal.create("player link", "YT Video/Playlist Link")
                        .addActionRow(link)
                        .build();

                event.replyModal(linkModal).queue();
                break;
            case "player search":
                TextInput search = TextInput.create("query","Suchfeld:", TextInputStyle.SHORT)
                        .setPlaceholder("Was suchst du?")
                        .setRequired(true)
                        .setMinLength(1)
                        .build();

                Modal searchModal = Modal.create("player search", "YouTube Suche")
                        .addActionRow(search)
                        .build();

                event.replyModal(searchModal).queue();
                break;
            case "player autoplay":
                MusicManager manager = DiscordBot.getMusicManager();
                if (manager.scheduler.isAutoPlay()) {
                    manager.scheduler.setAutoPlay(false);
                    event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SECONDARY)).queue();
                } else {
                    manager.scheduler.setAutoPlay(true);
                    event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SUCCESS)).queue();
                }
                break;
            case "player repeatTrack":
                MusicManager repeatTrack = DiscordBot.getMusicManager();
                if (repeatTrack.player.getPlayingTrack() != null) {
                    if (repeatTrack.scheduler.getAudioTrack() == null) {
                        repeatTrack.scheduler.setAudioTrack(repeatTrack.player.getPlayingTrack().makeClone());
                        event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SUCCESS)).queue();
                    } else {
                        repeatTrack.scheduler.setAudioTrack(null);
                        event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SECONDARY)).queue();
                    }
                }
                break;
            case "player repeatQueue":
                MusicManager repeatQueue = DiscordBot.getMusicManager();
                if (repeatQueue.player.getPlayingTrack() != null) {
                    if (repeatQueue.scheduler.isRepeatQueue()) {
                        repeatQueue.scheduler.setRepeatQueue(false);
                        event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SECONDARY)).queue();
                    } else {
                        repeatQueue.scheduler.setRepeatQueue(true);
                        event.getInteraction().editButton(event.getButton().withStyle(ButtonStyle.SUCCESS)).queue();
                    }
                }
                break;
        }
    }

    @Override
    public void processModalEvent(Member member, MessageChannelUnion channel, String textID, ModalInteractionEvent event) {
        if (!ServerUtil.hasRole(member,"871292341053431899") && !member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        MusicManager musicManager = DiscordBot.getMusicManager();
        switch (textID) {
            case "player link":
                String url = event.getValue("url").getAsString();
                DiscordBot.getAudioPlayer().loadItemOrdered(musicManager,
                        url, new LoadResultHandler(musicManager, member, channel.asTextChannel(), url, event, false, false));
                break;
            case "player search":
                String query = event.getValue("query").getAsString();
                DiscordBot.getAudioPlayer().loadItemOrdered(musicManager,
                        "ytsearch:" + query, new LoadResultHandler(musicManager, member, channel.asTextChannel(), query, event, false, true));
                break;
            case "player load":
                String playlist = event.getValue("playlist").getAsString();

                List<String> list = SQLRequest.runList(DiscordBot.UTIL.getConnectionPool(),SELECT_SONGS_OF_PLAYLIST(member.getId(), playlist), String.class);
                if (list.isEmpty()) {
                    EmbedBuilder eb = new EmbedBuilder().setDescription(String.format("%s, du hast keine Playlist mit dem Namen %s", member.getAsMention(), playlist));
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                }
                for (String s : list) {
                    DiscordBot.getAudioPlayer().loadItemOrdered(musicManager
                            , s, new LoadResultHandler(musicManager, member, channel.asTextChannel(), playlist, event, true,false));
                }
                EmbedBuilder eb = new EmbedBuilder().setDescription(String.format("%s, deine Playlist mit dem Namen %s wird hinzugefügt", member.getAsMention(), playlist));
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        musicManager.scheduler.editQueueMessage();
                    }
                },5000);
                break;
        }
    }

    @Override
    public void processStringSelectEvent(Member member, MessageChannelUnion channel, String textID, StringSelectInteractionEvent event) {
        if (!ServerUtil.hasRole(member,"871292341053431899") && !member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        if (!textID.equals("player queue")) {
            return;
        }

        MusicManager musicManager = DiscordBot.getMusicManager();
        event.editComponents(ActionRow.of(musicManager.scheduler.playTrackSelect(event.getValues().get(0)))).queue();
    }

    private boolean shareChannel(Member member) {
        return member.getVoiceState().getChannel() != null && member.getVoiceState().getChannel()
                .getMembers().stream().anyMatch(user -> user.getId().equals(member.getJDA().getSelfUser().getId()));
    }

    private void skipTrack() {
        MusicManager musicManager = DiscordBot.getMusicManager();
        musicManager.scheduler.nextTrack();
    }

    public void editChannelMessage (TextChannel channel) {
        String topic = "⏹ Stop/Leave — ⏯ Pause/Resume - ⏩ Skip" +
                " - \uD83D\uDD02 Repeat Current Track — \uD83D\uDD3A \uD83D\uDD3B Volume Up/Down 10";
        channel.getManager().setTopic(topic).queue();
    }

    private EmbedBuilder createTemplateMessage () {
        return new EmbedBuilder()
                .setTitle("Aktuelles Lied")
                .setImage("https://images.wallpaperscraft.com/image/single/headphones_camera_retro_122094_1280x720.jpg")
                .addField("Author","Titel",false)
                .addField("Ersteller:","Null",false)
                .addField("Dauer:","00h 00m 00s",false);
    }

    private EmbedBuilder createQueueMessage () {
        return new EmbedBuilder().setTitle("Warteschlange:").setDescription("...");
    }

    public void addReactions (TextChannel channel) {
        String mesID = LocalCache.getMessageID("player_main");

        channel.retrieveMessageById(mesID).complete().clearReactions().queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("⏹")).queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("⏯")).queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("⏩")).queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("\uD83D\uDD00")).queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("\uD83D\uDD3B")).queue();
        channel.addReactionById(mesID,Emoji.fromUnicode("\uD83D\uDD3A")).queue();
    }
}
