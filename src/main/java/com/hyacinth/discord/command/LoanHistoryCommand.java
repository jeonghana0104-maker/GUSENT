package com.hyacinth.discord.command;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.service.LoanEventService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequireRole(BotRole.MEMBER)
public class LoanHistoryCommand implements SlashCommandHandler {

	private static final int RECENT_LIMIT = 10;

	private final LoanEventService eventService;
	private final LoanMessageFormatter formatter;

	public LoanHistoryCommand(LoanEventService eventService, LoanMessageFormatter formatter) {
		this.eventService = eventService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		return Commands.slash("대출이력", "대출 이력을 최신순으로 표시합니다.")
				.addOption(OptionType.STRING, "고유번호", "대출자 고유번호", true);
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		String customerId = event.getOption("고유번호").getAsString().strip();
		List<LoanEvent> events = eventService.findRecent(customerId, RECENT_LIMIT);
		if (events.isEmpty()) {
			throw new LoanException("대출 이력이 없습니다.");
		}
		CommandReplies.send(event, formatter.split(formatter.history(customerId, events.get(0).userName(), events)));
	}
}
