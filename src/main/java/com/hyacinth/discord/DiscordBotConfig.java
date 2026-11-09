package com.hyacinth.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true", matchIfMissing = true)
public class DiscordBotConfig {

	@Bean(destroyMethod = "shutdown")
	public JDA jda(@Value("${discord.bot.token}") String token,
			ObjectProvider<ListenerAdapter> listeners) throws InterruptedException {
		return JDABuilder.createDefault(token)
				.addEventListeners(listeners.stream().toArray())
				.build()
				.awaitReady();
	}

}
