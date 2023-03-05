package model.web;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.util.LocalCache;
import model.manager.DiscordBot;
import model.manager.MusicManager;
import model.player.TrackScheduler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class PlayerPage implements HttpHandler, Page {

    private static final String QBLOCK = "<li><a href=\"LINK\" target=\"_blank\">NAME</a></li>";
    private static final String VFRAME = "<object data=\"https://www.youtube.com/embed/ID?autoplay=1&mute=1&loop=1&controls=0\"></object>";
    private static final String IFRAME = "<object data=\"https://img.youtube.com/vi/ID/hqdefault.jpg\"></object>";

    @Override
    public void handle(HttpExchange he) throws IOException {
        if (he.getRequestMethod().equals("POST")) {
            Scanner s = new Scanner(he.getRequestBody()).useDelimiter("\\A");
            StringBuilder result = new StringBuilder();
            while (s.hasNext()) {
                result.append(s.next());
            }
            if (result.toString().contains("stop")) {
                if (TrackScheduler.INSTANCE.getPlayer().getPlayingTrack() != null) {
                    TrackScheduler.INSTANCE.clear();
                    TrackScheduler.INSTANCE.clearMessages();
                    TrackScheduler.INSTANCE.getPlayer().stopTrack();
                }
            } else if (result.toString().contains("resume")) {
                if (TrackScheduler.INSTANCE.getPlayer().getPlayingTrack() != null) {
                    TrackScheduler.INSTANCE.getPlayer().setPaused(!TrackScheduler.INSTANCE.getPlayer().isPaused());
                }
            } else if (result.toString().contains("next")) {
                TrackScheduler.INSTANCE.nextTrack();
            } else if (result.toString().contains("volume")) {
                if (TrackScheduler.INSTANCE.getPlayer().getPlayingTrack() != null) {
                    TrackScheduler.INSTANCE.getPlayer().setVolume(Integer.parseInt(result.toString().split(" ")[1]));
                }
            } else if (result.toString().contains("url")) {
                MusicManager musicManager = DiscordBot.getMusicManager();
                String args = result.toString().replace("url=","");
                args = args.replace("%3A",":").replace("%2F","/").replace("%3D","=").replace("%3F","?");
                DiscordBot.getAudioPlayer().loadItemOrdered(musicManager, args, new LoadWebPlayerHandler(musicManager
                        , JDAUtil.JDA.getGuildById(DiscordBot.GUILD).getTextChannelById(LocalCache.getChannelID("music"))));
            } else if (result.toString().contains("update")) {
                TrackScheduler trackScheduler = TrackScheduler.INSTANCE;
                AudioTrack track = trackScheduler.getPlayer().getPlayingTrack();
                String url = track != null ? "https://img.youtube.com/vi/" + track.getInfo().identifier + "/hqdefault.jpg" : "none";
                String title = track != null ? track.getInfo().title : "Kein Aktiver Track";
                String author = track != null ? track.getInfo().author : "Author";
                long duration = track != null ? track.getDuration() : 0;
                long position = track != null ? track.getPosition() : 0;
                int volume = trackScheduler.getPlayer().getVolume();
                JSONObject player = new JSONObject();
                player.put("url", url);
                player.put("title", title);
                player.put("author", author);
                player.put("duration", duration);
                player.put("position", position);
                player.put("volume", volume);

                String response = player.toString();
                he.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
        }
        String response = fillContent(new PageLoader().getData("player.html"));
        he.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    @Override
    public String fillContent(String page) {
        TrackScheduler trackScheduler = TrackScheduler.INSTANCE;

        if (trackScheduler.getPlayer().getPlayingTrack() != null) {
            AudioTrackInfo info = trackScheduler.getPlayer().getPlayingTrack().getInfo();
            String embed = VFRAME.replace("ID", info.identifier);
            page = page.replace("VFRAME", embed);
            page = page.replace("TITLE", info.title);

            long sec = info.length / 1000L;
            long min = sec / 60L;
            long hour = min / 60L;
            sec %= 60L;
            min %= 60L;
            hour %= 24L;
            page = page.replace("VIDEOLINK",info.uri);
            long left = trackScheduler.getPlayer().getPlayingTrack().getDuration() - trackScheduler.getPlayer().getPlayingTrack().getPosition();
            page = page.replace("TIME", (left / 1000 + 1) + "");
            page = page.replace("VOLUME", TrackScheduler.INSTANCE.getPlayer().getVolume() + "");
            page = page.replace("VLENGTH","423px");
            page = page.replace("TLENGTH","310px");
            page = page.replace("DMAX","" + info.length / 1000);
            page = page.replace("DURATION", info.isStream ? "\uD83D\uDD34 Stream" : (hour > 0L ? hour + "h " : "")
                    + (min < 10 ? "0" + min : min) + "m "
                    + (sec < 10 ? "0" + sec : sec) + "s");
            page = page.replace("DSEC","" + trackScheduler.getPlayer().getPlayingTrack().getPosition() / 1000);
        } else {
            page = page.replace("VFRAME", "");
            page = page.replace("TITLE","Title");
            page = page.replace("DURATION", "00h 00m 00s");
            page = page.replace("VIDEOLINK","#");
            page = page.replace("http-equiv=\"refresh\" content=\"TIME\"","");
            page = page.replace("VOLUME","20");
            page = page.replace("VLENGTH","68%");
            page = page.replace("TLENGTH","50%");
            page = page.replace("DMAX", "-1");
            page = page.replace("DSEC","1");
        }

        if (!trackScheduler.getQueue().isEmpty()) {
            StringBuilder s = new StringBuilder();
            int i = 0;
            for (AudioTrack audioTrack : trackScheduler.getQueue()) {
                if (i == 7) {
                    s.append("<li>").append(trackScheduler.getQueue().size() - i).append(" weitere Lieder</li>");
                    break;
                }
                AudioTrackInfo info = audioTrack.getInfo();
                s.append(QBLOCK.replace("LINK",info.uri).replace("NAME",info.title)).append("\n").append("<br>").append("\n");
                i++;
            }
            page = page.replace("TRACKS",s.toString());
        } else {
            page = page.replace("TRACKS","Keine Lieder in der Queue");
        }

        return page;
    }

    private Boolean play(Guild guild, MusicManager musicManager, AudioTrack track) {
        if (guild.getAudioManager().isConnected()) {
            musicManager.scheduler.queue(track);
            return true;
        }
        return false;
    }

    private class LoadWebPlayerHandler implements AudioLoadResultHandler {

        private final MusicManager musicManager;
        private final TextChannel channel;

        public LoadWebPlayerHandler(MusicManager musicManager, TextChannel channel) {
            this.musicManager = musicManager;
            this.channel = channel;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if(!play(channel.getGuild(), musicManager, track)) return;
            EmbedBuilder eb = new EmbedBuilder().setDescription("Hinzugefügt zur Warteschlange **" + track.getInfo().title + "**");
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
            musicManager.scheduler.editQueueMessage();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks()) {
                if(!play(channel.getGuild(), musicManager, track)) return;
            }

            musicManager.scheduler.editQueueMessage();
            EmbedBuilder eb = new EmbedBuilder().setDescription("Hinzugefügt zur Warteschlange **" + playlist.getName() + "**");
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7, TimeUnit.SECONDS);
        }

        @Override
        public void noMatches() {
            EmbedBuilder eb = new EmbedBuilder().setDescription("Kein Ergebnis");
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7,TimeUnit.SECONDS);
        }

        @Override
        public void loadFailed(FriendlyException e) {
            EmbedBuilder eb = new EmbedBuilder().setDescription("Konnte Track nicht abspielen");
            channel.sendMessageEmbeds(eb.build()).complete().delete().queueAfter(7,TimeUnit.SECONDS);
        }
    }
}
