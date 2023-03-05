package model.player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import model.api.YouTubeAPI;
import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.scheduling.TaskService;
import com.shurablack.core.util.LocalCache;
import model.manager.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private AudioTrack audioTrack = null;

    public static TrackScheduler INSTANCE;

    private boolean repeatQueue = false;
    private boolean autoPlay = false;
    private AudioTrack rqAudioTrack = null;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        INSTANCE = this;
    }

    public boolean isPlaying() {
        return this.player.getPlayingTrack() != null;
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.add(track);
        }
    }

    public void setAutoPlay(boolean value) {
        this.autoPlay = value;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void nextTrack() {
        if (queue.size() == 0 && audioTrack == null) {
            clearMessages();
        }

        if (audioTrack != null) {
            player.startTrack(audioTrack.makeClone(),false);
        } else {
            if (repeatQueue) {
                if (player.getPlayingTrack() != null) {
                    queue.add(player.getPlayingTrack().makeClone());
                } else {
                    queue.add(rqAudioTrack);
                }
            }
            player.startTrack(queue.poll(), false);
        }
        editQueueMessage();
    }

    public boolean removeTrack (int trackNumber) {
        if (queue.size() == 0 || queue.size()+1 < trackNumber) {
            return false;
        }
        int count = 0;
        List<AudioTrack> audioTracks = new ArrayList<>(queue);
        queue.clear();
        for (AudioTrack audioTrack : audioTracks) {
            count++;
            if (count == trackNumber) {
               continue;
            }
            queue.add(audioTrack);
        }
        editQueueMessage();
        return true;
    }

    public boolean popTrack(int trackNumber) {
        if (queue.size() == 0 || queue.size()+1 < trackNumber) {
            return false;
        }
        List<AudioTrack> audioTracks = new ArrayList<>(queue);
        queue.clear();
        queue.add(audioTracks.remove(trackNumber-1));
        queue.addAll(audioTracks);
        editQueueMessage();
        return true;
    }

    public boolean playTrack (int trackNumber) {
        if (queue.size() == 0 || queue.size()+1 < trackNumber) {
            return false;
        }
        int count = 0;
        List<AudioTrack> audioTracks = new ArrayList<>(queue);
        queue.clear();
        for (AudioTrack audioTrack : audioTracks) {
            count++;
            if (count == trackNumber) {
                player.startTrack(audioTrack, false);
                continue;
            }
            queue.add(audioTrack);
        }
        editQueueMessage();
        return true;
    }

    public ItemComponent playTrackSelect (String identifier) {
        if (queue.size() == 0) {
            return null;
        }
        List<AudioTrack> audioTracks = new ArrayList<>(queue);
        queue.clear();
        for (AudioTrack audioTrack : audioTracks) {
            if (audioTrack.getInfo().title.equals(identifier)) {
                player.startTrack(audioTrack, false);
                continue;
            }
            queue.add(audioTrack);
        }
        return createMenu();
    }

    private ItemComponent createMenu() {
        if (this.queue.isEmpty()) {
            return Button.secondary("empty","Keine Warteschlange").asDisabled();
        }

         StringSelectMenu.Builder menu = StringSelectMenu.create("player queue")
                .setPlaceholder("Lieder: " + queue.size() + " - Gesamtdauer: "
                        + createTime(queue.stream().filter(at -> !at.getInfo().isStream)
                        .mapToLong(audioTrack -> audioTrack.getInfo().length).sum(), false));

        int count = 0;
        for (AudioTrack at : this.queue) {
            if (count == 10) {
                break;
            }

            CustomEmoji icon = null;

            if (at.getInfo().uri.contains("youtube") || at.getInfo().uri.contains("youtu.be")) {
                icon = Emoji.fromCustom("youtube",1042066466746413136L,false);
            } else if (at.getInfo().uri.contains("soundcloud")) {
                icon = Emoji.fromCustom("soundcloud",1042184901308452965L,false);
            } else if (at.getInfo().uri.contains("twitch")) {
                icon = Emoji.fromCustom("twitch",1042184899597180988L,false);
            }
            AudioTrackInfo info = at.getInfo();
            menu.addOption(at.getInfo().title,at.getInfo().title, createTime(info.length, info.isStream),icon);
            count++;
        }

        return menu.build();
    }

    public void randomizeQueue() {
        List<AudioTrack> list = new ArrayList<>(queue);
        Collections.shuffle(list);
        queue.clear();
        queue.addAll(list);
        editQueueMessage();
    }

    public boolean setPosition(long time) {
        if (this.player.getPlayingTrack() == null) {
            return false;
        }
        if (this.player.getPlayingTrack().getInfo().isStream) {
            return false;
        }
        this.player.getPlayingTrack().setPosition(time);
        return true;
    }

    public long optionsToTime(int hour, int min, int sec) {
        return ((hour * 60L * 60L) + (min * 60L) + sec) * 1000L;
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (autoPlay && track.getSourceManager().getSourceName().equals("youtube")) {
            String link = YouTubeAPI.getRelatedVideos(track.getIdentifier());
            if (link == null) {
                autoPlay = false;
            } else {
                TaskService.submit(() -> DiscordBot.getAudioPlayer().loadItemOrdered(
                        DiscordBot.getMusicManager(), link,
                        new HeadlessLoadResultHandler(DiscordBot.getMusicManager())));
            }
        }
        AudioTrackInfo info = track.getInfo();

        String url = info.uri;
        StringBuilder volumen = new StringBuilder();
        for (int i = 0 ; i < 100 ; i += 10) {
            if (i < player.getVolume()) {
                volumen.append("▮ ");
            } else {
                volumen.append("▯ ");
            }
        }

        String bar = (info.isStream ? Emoji.fromCustom("live",1048222501597098034L, false).getAsMention()
                : "0:00 ●━━━━━━━━━━━━━━━ " + createTimeNoLetter(info.length, false));
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(70,130,220))
                .setDescription("[" + info.title + "](" + url + ")\n*Von " + info.author + "*\n\n" +
                        bar + "\n\n🔊 " + volumen)
                .setThumbnail(getImage(info.uri,info.identifier));

        Objects.requireNonNull(Objects.requireNonNull(JDAUtil.JDA.getGuildById(DiscordBot.GUILD))
                        .getTextChannelById(LocalCache.getChannelID("music")))
                .editMessageEmbedsById(LocalCache.getMessageID("player_main"),eb.build()).queue();
    }

    public boolean updatePlayerMessage() {
        if (player.getPlayingTrack() == null) {
            return false;
        }
        AudioTrackInfo info = player.getPlayingTrack().getInfo();

        String url = info.uri;
        StringBuilder volumen = new StringBuilder();
        for (int i = 0 ; i < 100 ; i += 10) {
            if (i < player.getVolume()) {
                volumen.append("▮ ");
            } else {
                volumen.append("▯ ");
            }
        }
        StringBuilder position = new StringBuilder(" ");
        if (!info.isStream) {
            int current = (int) (15.0 * ((double)player.getPlayingTrack().getPosition() / (double)info.length));
            for (int i = 0 ; i < current ; i++) {
                position.append("━");
            }
            position.append("●");
            for (int i = 0 ; i < (15-current) ; i++) {
                position.append("━");
            }
        }
        position.append(" ");
        String bar = (info.isStream ? Emoji.fromCustom("live",1048222501597098034L, false).getAsMention()
                : createTimeNoLetter(player.getPlayingTrack().getPosition(), false) + position + createTimeNoLetter(info.length, false));
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(70,130,220))
                .setDescription("[" + info.title + "](" + url + ")\n*Von " + info.author
                        + "*\n\n" + bar + "\n\n" + (player.getVolume() < 10
                        ? "\uD83D\uDD08 " : (player.getVolume() < 50 ? "\uD83D\uDD09 " : "\uD83D\uDD0A ")) + volumen)
                .setThumbnail(getImage(info.uri,info.identifier));

        Objects.requireNonNull(Objects.requireNonNull(JDAUtil.JDA.getGuildById(DiscordBot.GUILD))
                        .getTextChannelById(LocalCache.getChannelID("music")))
                .editMessageEmbedsById(LocalCache.getMessageID("player_main"),eb.build()).queue();
        return true;
    }

    private String getImage(String uri, String identifier) {
        if (uri.contains("youtube") || uri.contains("youtu.be")) {
            return "https://img.youtube.com/vi/" + identifier + "/hqdefault.jpg";
        } else if (uri.contains("soundcloud")) {
            return "https://wallpaperaccess.com/full/1112346.jpg";
        } else if (uri.contains("twitch")) {
            return "https://wallpapercave.com/wp/wp1957865.jpg";
        }
        return null;
    }

    private String createTime(long length, boolean isStream) {
        long sec = length / 1000L;
        long min = sec / 60L;
        long hour = min / 60L;
        sec %= 60L;
        min %= 60L;
        hour %= 24L;
        return isStream ? Emoji.fromCustom("live",1048222501597098034L, false).getAsMention() : (hour > 0L ? hour + "h " : "")
                + (min < 10 ? "0" + min : min) + "m "
                + (sec < 10 ? "0" + sec : sec) + "s";
    }

    private String createTimeNoLetter(long length, boolean isStream) {
        long sec = length / 1000L;
        long min = sec / 60L;
        long hour = min / 60L;
        sec %= 60L;
        min %= 60L;
        hour %= 24L;
        return isStream ? Emoji.fromCustom("live",1048222501597098034L, false).getAsMention() : (hour > 0L ? hour + ":" : "")
                + (min < 10 ? "0" + min + ":" : min + ":") + (sec < 10 ? "0" + sec : sec);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (repeatQueue) {
                rqAudioTrack = track.makeClone();
            }
            nextTrack();
        }
    }

    public void editQueueMessage () {
        if (this.queue.isEmpty() && this.player.getPlayingTrack() == null) {
            return;
        }
        TextChannel musicChannel = JDAUtil.JDA.getGuildById(DiscordBot.GUILD)
                .getTextChannelById(LocalCache.getChannelID("music"));
        musicChannel.editMessageComponentsById(LocalCache.getMessageID("player_queue"), ActionRow.of(createMenu())).queue();
    }

    public void clear () {
        queue.clear();
        audioTrack = null;
    }

    public void clearMessages () {
        TextChannel musicChannel = Objects.requireNonNull(JDAUtil.JDA.getGuildById(DiscordBot.GUILD))
                .getTextChannelById(LocalCache.getChannelID("music"));
        EmbedBuilder current = new EmbedBuilder()
                .setDescription("Warte auf neues Lied ...")
                .setImage("https://images.wallpaperscraft.com/image/single/headphones_camera_retro_122094_1280x720.jpg");

        musicChannel.editMessageEmbedsById(LocalCache.getMessageID("player_main"), current.build()).queue();
        musicChannel.editMessageComponentsById(LocalCache.getMessageID("player_main"),
                ActionRow.of(
                        Button.secondary("player link", Emoji.fromCustom("links",1048000262779961385L,false)),
                        Button.secondary("player search", Emoji.fromCustom("search",1048000265745350748L,false)),
                        Button.secondary("player autoplay", Emoji.fromCustom("autoplay",1048041193407258624L,false)),
                        Button.secondary("player repeatTrack", Emoji.fromCustom("track_repeat",1048000769212821535L,false)),
                        Button.secondary("player repeatQueue", Emoji.fromCustom("queue_repeat",1048000770710175805L,false))
                )
        ).queue();

        musicChannel.editMessageComponentsById(LocalCache.getMessageID("player_queue")
                , ActionRow.of(Button.secondary("empty","Keine Warteschlange").asDisabled())).queue();
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public boolean isRepeatQueue() {
        return repeatQueue;
    }

    public void setRepeatQueue(boolean repeatQueue) {
        this.repeatQueue = repeatQueue;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }
}

