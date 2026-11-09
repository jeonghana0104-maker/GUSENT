package com.hyacinth.discord.log;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.loan.domain.LoanLog;
import com.hyacinth.loan.service.LoanLogPublisher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class DiscordLogPublisher implements LoanLogPublisher {

	private static final Logger log = LoggerFactory.getLogger(DiscordLogPublisher.class);

	private final ObjectProvider<JDA> jda;
	private final LoanMessageFormatter formatter;

	public DiscordLogPublisher(ObjectProvider<JDA> jda, LoanMessageFormatter formatter) {
		this.jda = jda;
		this.formatter = formatter;
	}

	@Override
	public void publish(LoanLog loanLog, String channelId) {
		JDA client = jda.getIfAvailable();
		if (client == null) {
			return;
		}
		TextChannel channel = client.getTextChannelById(channelId);
		if (channel == null) {
			log.warn("Log channel {} was not found", channelId);
			return;
		}
		channel.sendMessage(formatter.log(loanLog))
				.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
				.queue(null, error -> log.warn("Failed to send a loan log", error));
	}
}
