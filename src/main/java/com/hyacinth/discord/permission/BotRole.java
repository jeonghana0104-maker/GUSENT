package com.hyacinth.discord.permission;

public enum BotRole {
	ADMIN(0),
	MEMBER(1);

	private final int priority;

	BotRole(int priority) {
		this.priority = priority;
	}

	public boolean canAccess(BotRole required) {
		return priority <= required.priority;
	}
}
