package com.hyacinth.discord.command;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.service.LoanService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
@RequireRole(BotRole.MEMBER)
public class LoanListCommand implements SlashCommandHandler {

	private final LoanService loanService;
	private final LoanMessageFormatter formatter;

	public LoanListCommand(LoanService loanService, LoanMessageFormatter formatter) {
		this.loanService = loanService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		return Commands.slash("목록", "대출 목록을 상태별로 표시합니다.");
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		CommandReplies.send(event, formatter.list(loanService.findAllGroupedByStatus()));
	}
}
