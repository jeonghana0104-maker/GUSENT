package com.hyacinth.discord.command;

import com.hyacinth.discord.log.CommandLog;
import com.hyacinth.discord.log.CommandLogContext;
import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.DuplicateLoanException;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.domain.RepaymentMethod;
import com.hyacinth.loan.service.LoanService;
import com.hyacinth.loan.service.RegisterLoanRequest;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
@RequireRole(BotRole.MEMBER)
@CommandLog("대출등록")
public class RegisterLoanCommand implements SlashCommandHandler {

	private final LoanService loanService;
	private final LoanMessageFormatter formatter;

	public RegisterLoanCommand(LoanService loanService, LoanMessageFormatter formatter) {
		this.loanService = loanService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		OptionData method = new OptionData(OptionType.STRING, "상환방식", "상환 방식", true);
		for (RepaymentMethod value : RepaymentMethod.values()) {
			method.addChoice(value.label(), value.name());
		}
		return Commands.slash("대출등록", "대출을 등록하고 상환 일정을 생성합니다.")
				.addOption(OptionType.STRING, "고유번호", "대출자 고유번호", true)
				.addOption(OptionType.STRING, "유저명", "대출자 이름", true)
				.addOption(OptionType.INTEGER, "금액", "대출 금액", true)
				.addOptions(method);
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		String customerId = event.getOption("고유번호").getAsString();
		String userName = event.getOption("유저명").getAsString();
		long amount = event.getOption("금액").getAsLong();
		RepaymentMethod method = RepaymentMethod.valueOf(event.getOption("상환방식").getAsString());
		Actor actor = new Actor(event.getUser().getId(), event.getUser().getEffectiveName());

		Loan loan;
		try {
			loan = loanService.register(new RegisterLoanRequest(customerId, userName, amount, method, actor));
		}
		catch (DuplicateLoanException ex) {
			throw new LoanException(formatter.registerRejected(customerId, userName));
		}
		CommandReplies.send(event, formatter.split(formatter.registered(loan)));
		CommandLogContext.setResult("대출 등록 완료");
	}
}
