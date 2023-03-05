package model.player;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.scheduling.TaskService;
import model.manager.DiscordBot;
import model.manager.MusicManager;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class HeadlessLoadResultHandler implements AudioLoadResultHandler {

    private final MusicManager musicManager;

    public HeadlessLoadResultHandler(MusicManager musicManager) {
        this.musicManager = musicManager;
        checkEmptyChannel();
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        musicManager.scheduler.queue(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack at : playlist.getTracks()) {
            musicManager.scheduler.queue(at);
        }
    }

    @Override
    public void noMatches() {
    }

    @Override
    public void loadFailed(FriendlyException exception) {
    }

    private void checkEmptyChannel() {
        AudioManager audioManager = JDAUtil.JDA.getGuildById(DiscordBot.GUILD).getAudioManager();
        VoiceChannel vc = (VoiceChannel) audioManager.getConnectedChannel();
        Runnable task = () -> {
            if (audioManager.isConnected() && (vc == null || vc.getMembers().size() == 1)) {
                MusicManager musicManager = DiscordBot.getMusicManager();
                musicManager.player.stopTrack();
                musicManager.scheduler.clear();
                musicManager.scheduler.clearMessages();
                musicManager.player.setVolume(20);
                musicManager.player.setPaused(false);
                musicManager.scheduler.setRepeatQueue(false);
                musicManager.scheduler.setAutoPlay(false);
                audioManager.closeAudioConnection();
                TaskService.descheduleTask("PlayerConnectionTask");
            }
        };
        if (!TaskService.getTasks().containsKey("PlayerConnectionTask")) {
            TaskService.submitCronJob("*/10 * * * *","PlayerConnectionTask" ,task);
        }
    }

}
