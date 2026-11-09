package com.hyacinth.discord.command;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.discord.permission.BotRole;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.service.SettingService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
@RequireRole(BotRole.ADMIN)
public class SettingCommand implements SlashCommandHandler {

	private static final String VIEW = "보기";
	private static final String CHANGE = "변경";

	private final SettingService settingService;
	private final LoanMessageFormatter formatter;

	public SettingCommand(SettingService settingService, LoanMessageFormatter formatter) {
		this.settingService = settingService;
		this.formatter = formatter;
	}

	@Override
	public SlashCommandData commandData() {
		OptionData key = new OptionData(OptionType.STRING, "항목", "변경할 설정", true);
		SettingService.KEYS.forEach(name -> key.addChoice(name, name));
		return Commands.slash("설정", "대출 설정을 확인하거나 변경합니다.")
				.addSubcommands(
						new SubcommandData(VIEW, "현재 설정을 표시합니다."),
						new SubcommandData(CHANGE, "설정값을 변경합니다.")
								.addOptions(key)
								.addOption(OptionType.STRING, "값", "예: 1, 1분, 24시간, 없음, #채널", true));
	}

	@Override
	public void handle(SlashCommandInteractionEvent event) {
		if (VIEW.equals(event.getSubcommandName())) {
			CommandReplies.send(event, formatter.settings(settingService.get()));
			return;
		}
		String key = event.getOption("항목").getAsString();
		String value = event.getOption("값").getAsString().strip();
		if (SettingService.LOG_CHANNEL.equals(key) && !"없음".equals(value)) {
			value = logChannelId(event, value);
		}
		Actor actor = new Actor(event.getUser().getId(), event.getUser().getEffectiveName());
		CommandReplies.send(event, formatter.settings(settingService.update(key, value, actor)));
	}

	private String logChannelId(SlashCommandInteractionEvent event, String value) {
		String channelId = value.replaceAll("[<#>]", "");
		if (!channelId.matches("\\d+") || event.getGuild().getTextChannelById(channelId) == null) {
			throw new LoanException("로그 채널은 이 서버의 텍스트 채널로 입력해 주세요.");
		}
		return channelId;
	}
}
