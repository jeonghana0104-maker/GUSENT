package com.hyacinth.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommandHandler {

	SlashCommandData commandData();

	void handle(SlashCommandInteractionEvent event);
}
