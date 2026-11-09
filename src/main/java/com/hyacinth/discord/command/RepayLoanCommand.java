package com.hyacinth.discord.command;

import com.hyacinth.discord.log.CommandLog;
import com.hyacinth.discord.log.CommandLogContext;
import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.service.LoanService;
import com.hyacinth.loan.service.RepayLoanRequest;
import com.hyacinth.loan.service.RepaymentResult;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
@RequireRole(BotRole.MEMBER)
@CommandLog("대출상환")
public class RepayLoanCommand implements SlashCommandHandler {

	private final LoanService loanService;
	private final LoanMessageFormatter formatter;

	public RepayLoanCommand(LoanService loanService, LoanMessageFormatter formatter) {
		this.loanService = loanService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		return Commands.slash("대출상환", "상환 금액을 대출에 반영합니다.")
				.addOption(OptionType.STRING, "고유번호", "대출자 고유번호", true)
				.addOption(OptionType.STRING, "유저명", "대출자 이름", true)
				.addOption(OptionType.INTEGER, "상환금액", "상환 금액", true);
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		Actor actor = new Actor(event.getUser().getId(), event.getUser().getEffectiveName());
		RepaymentResult result = loanService.repay(new RepayLoanRequest(
				event.getOption("고유번호").getAsString(),
				event.getOption("유저명").getAsString(),
				event.getOption("상환금액").getAsLong(),
				actor));
		CommandReplies.send(event, formatter.repaid(result));
		CommandLogContext.setResult(result.appliedSummary());
	}
}
