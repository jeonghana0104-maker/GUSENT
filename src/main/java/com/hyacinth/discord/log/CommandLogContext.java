package com.hyacinth.discord.log;

/** 명령어 실행 중 로그에 남길 처리 결과를 전달한다. */
public final class CommandLogContext {

	private static final ThreadLocal<String> RESULT = new ThreadLocal<>();

	private CommandLogContext() {
	}

	public static void setResult(String result) {
		RESULT.set(result);
	}

	public static String takeResult() {
		String result = RESULT.get();
		RESULT.remove();
		return result;
	}
}
