package com.hyacinth.discord.permission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotRoleTest {

	@Test
	void adminCanUseMemberCommandsButMemberCannotUseAdminCommands() {
		assertThat(BotRole.ADMIN.canAccess(BotRole.MEMBER)).isTrue();
		assertThat(BotRole.ADMIN.canAccess(BotRole.ADMIN)).isTrue();
		assertThat(BotRole.MEMBER.canAccess(BotRole.MEMBER)).isTrue();
		assertThat(BotRole.MEMBER.canAccess(BotRole.ADMIN)).isFalse();
	}
}
