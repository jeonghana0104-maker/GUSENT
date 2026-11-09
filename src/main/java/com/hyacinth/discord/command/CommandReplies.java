package com.hyacinth.discord.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.EnumSet;
import java.util.List;

final class CommandReplies {

	private CommandReplies() {
	}

	static void send(SlashCommandInteractionEvent event, String message) {
		send(event, List.of(message));
	}

	static void send(SlashCommandInteractionEvent event, List<String> messages) {
		for (String message : messages) {
			event.getHook().sendMessage(message)
					.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
					.queue();
		}
	}
}
