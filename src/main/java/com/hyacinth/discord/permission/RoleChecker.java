package com.hyacinth.discord.permission;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleChecker {

	private final DiscordRoleProperties properties;

	public RoleChecker(DiscordRoleProperties properties) {
		this.properties = properties;
	}

	public Optional<BotRole> highestRole(Member member) {
		if (member == null) {
			return Optional.empty();
		}
		Set<String> roleNames = member.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
		if (roleNames.contains(properties.admin())) {
			return Optional.of(BotRole.ADMIN);
		}
		if (roleNames.contains(properties.member())) {
			return Optional.of(BotRole.MEMBER);
		}
		return Optional.empty();
	}

	public boolean hasAccess(Member member, BotRole required) {
		return highestRole(member).filter(role -> role.canAccess(required)).isPresent();
	}
}
