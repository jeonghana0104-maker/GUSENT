package com.hyacinth;

import com.hyacinth.discord.command.SlashCommandHandler;
import com.hyacinth.discord.permission.DiscordRoleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "discord.bot.enabled=false")
class HyacinthApplicationTests {

	@Autowired
	private List<SlashCommandHandler> handlers;

	@Autowired
	private DiscordRoleProperties roleProperties;

	@Test
	void contextLoads() {
	}

	@Test
	void commandsAreValidDiscordDefinitions() {
		assertThat(handlers.stream().map(handler -> handler.commandData().getName()))
				.containsExactlyInAnyOrder("대출등록", "대출상환", "목록", "상세조회", "대출이력", "설정");
	}

	@Test
	void koreanRoleNamesAreLoadedCorrectly() {
		assertThat(roleProperties.admin()).isEqualTo("관리자");
		assertThat(roleProperties.member()).isEqualTo("일반");
	}
}
