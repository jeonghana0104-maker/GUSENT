package com.hyacinth.discord.command;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.service.LoanService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
@RequireRole(BotRole.MEMBER)
public class LoanDetailCommand implements SlashCommandHandler {

	private final LoanService loanService;
	private final LoanMessageFormatter formatter;

	public LoanDetailCommand(LoanService loanService, LoanMessageFormatter formatter) {
		this.loanService = loanService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		return Commands.slash("상세조회", "대출 일정과 남은 금액을 표시합니다.")
				.addOption(OptionType.STRING, "고유번호", "대출자 고유번호", true);
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		CommandReplies.send(event, formatter.split(formatter.detail(
				loanService.findDetail(event.getOption("고유번호").getAsString()))));
	}
}
