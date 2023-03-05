package model.listener;

import model.manager.DiscordBot;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SlashListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        if (event.isFromGuild()) {
            DiscordBot.UTIL.getHandler().onGuildSlashEvent(event.getName(), event);
        } else {
            DiscordBot.UTIL.getHandler().onGlobalSlashEvent(event.getName(), event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        DiscordBot.UTIL.getHandler().onButtonEvent(event.getButton().getId().split(" ")[0], event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        DiscordBot.UTIL.getHandler().onModalEvent(event.getModalId().split(" ")[0], event);
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        if (event.isFromGuild()) {
            DiscordBot.UTIL.getHandler().onGuildUserContextEvent(event.getId().split(" ")[0], event);
        } else {
            DiscordBot.UTIL.getHandler().onGlobalUserContextEvent(event.getId().split(" ")[0], event);
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        if (event.isFromGuild()) {
            DiscordBot.UTIL.getHandler().onGuildMessageContextEvent(event.getId().split(" ")[0], event);
        } else {
            DiscordBot.UTIL.getHandler().onGlobalMessageContextEvent(event.getId().split(" ")[0], event);
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }
        DiscordBot.UTIL.getHandler().onStringSelectionMenuEvent(event.getInteraction().getComponentId().split(" ")[0], event);
    }
}
