package com.hyacinth.discord.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandRegistrar extends ListenerAdapter {

	private final List<SlashCommandHandler> handlers;

	public CommandRegistrar(List<SlashCommandHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void onGuildReady(GuildReadyEvent event) {
		register(event.getGuild());
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		register(event.getGuild());
	}

	private void register(Guild guild) {
		List<SlashCommandData> commands = handlers.stream().map(SlashCommandHandler::commandData).toList();
		guild.updateCommands().addCommands(commands).queue();
	}
}
