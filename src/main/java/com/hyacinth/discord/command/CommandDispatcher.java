package com.hyacinth.discord.command;

import com.hyacinth.discord.log.CommandLog;
import com.hyacinth.discord.log.CommandLogContext;
import com.hyacinth.discord.permission.RequireRole;
import com.hyacinth.discord.permission.RoleChecker;
import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.domain.LoanLog;
import com.hyacinth.loan.service.LoanLogService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandDispatcher extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

	private final Map<String, SlashCommandHandler> handlers;
	private final RoleChecker roleChecker;
	private final LoanLogService logService;
	private final Clock clock;

	public CommandDispatcher(List<SlashCommandHandler> handlers, RoleChecker roleChecker,
			LoanLogService logService, Clock clock) {
		handlers.forEach(this::requireRoleAnnotation);
		this.handlers = handlers.stream()
				.collect(Collectors.toMap(handler -> handler.commandData().getName(), Function.identity()));
		this.roleChecker = roleChecker;
		this.logService = logService;
		this.clock = clock;
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		SlashCommandHandler handler = handlers.get(event.getName());
		if (handler == null) {
			return;
		}
		RequireRole required = AnnotationUtils.findAnnotation(handler.getClass(), RequireRole.class);
		if (!roleChecker.hasAccess(event.getMember(), required.value())) {
			event.reply("🚫 이 명령어를 사용할 권한이 없습니다.").setEphemeral(true).queue();
			record(handler, event, false, "권한 없음");
			return;
		}

		event.deferReply().queue();
		try {
			handler.handle(event);
			String result = CommandLogContext.takeResult();
			record(handler, event, true, result == null ? "처리 완료" : result);
		}
		catch (LoanException ex) {
			CommandLogContext.takeResult();
			CommandReplies.send(event, ex.getMessage().startsWith("🚫") ? ex.getMessage() : "🚫 " + ex.getMessage());
			record(handler, event, false, ex.getMessage().lines().findFirst().orElse("거부"));
		}
		catch (RuntimeException ex) {
			CommandLogContext.takeResult();
			log.error("Command {} failed", event.getName(), ex);
			CommandReplies.send(event, "⚠️ 명령어 처리 중 오류가 발생했습니다.");
			record(handler, event, false, "오류");
		}
	}

	private void requireRoleAnnotation(SlashCommandHandler handler) {
		if (AnnotationUtils.findAnnotation(handler.getClass(), RequireRole.class) == null) {
			throw new IllegalStateException(handler.getClass().getName() + " must declare @RequireRole");
		}
	}

	private void record(SlashCommandHandler handler, SlashCommandInteractionEvent event, boolean success, String result) {
		CommandLog commandLog = AnnotationUtils.findAnnotation(handler.getClass(), CommandLog.class);
		if (commandLog == null) {
			return;
		}
		Actor actor = new Actor(event.getUser().getId(), event.getUser().getEffectiveName());
		logService.record(new LoanLog(actor, commandLog.value(), event.getCommandString(), success, result, clock.instant()));
	}
}
