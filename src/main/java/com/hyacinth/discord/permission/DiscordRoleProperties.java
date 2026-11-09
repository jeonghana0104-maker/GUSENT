package com.hyacinth.discord.permission;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord.roles")
public record DiscordRoleProperties(String admin, String member) {
}
