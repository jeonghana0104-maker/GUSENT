package com.hyacinth.discord.message;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.domain.LoanLog;
import com.hyacinth.loan.domain.LoanSettings;
import com.hyacinth.loan.domain.LoanStatus;
import com.hyacinth.loan.domain.PaymentCycle;
import com.hyacinth.loan.service.LoanListItem;
import com.hyacinth.loan.service.RepaymentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LoanMessageFormatter {

	private static final String DIVIDER = "┈┈┈┈┈┈┈┈┈┈┈┈┈┈";
	private static final int MESSAGE_LIMIT = 1900;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd").withZone(PaymentCycle.ZONE);
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(PaymentCycle.ZONE);
	private static final DateTimeFormatter FULL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(PaymentCycle.ZONE);

	public String registered(Loan loan) {
		String schedule = loan.installments().stream()
				.map(installment -> "· %s　%s".formatted(dateTime(installment.dueAt()), money(installment.amountDue())))
				.collect(Collectors.joining("\n"));
		return """
				🏦 대출 등록 완료

				#%s · %s 님

				✦ 대출금　　%s
				✦ 상환방식　%s
				✦ 금리　　　회차당 %s
				✦ 상환횟수　총 %d회

				📅 상환 일정
				%s

				%s
				✦ 총 상환 예정액　%s""".formatted(
				loan.customerId(), loan.userName(), money(loan.principal()), loan.method().label(),
				percent(loan.ratePerRound()), loan.rounds(), schedule, DIVIDER, money(loan.totalScheduledAmount()));
	}

	public String registerRejected(String customerId, String userName) {
		return """
				🚫 대출 등록 불가

				#%s · %s 님은 이미 대출 중입니다.
				한 사람당 대출은 1건만 가능합니다.""".formatted(customerId.strip(), userName.strip());
	}

	public String repaid(RepaymentResult result) {
		Loan loan = result.loan();
		StringBuilder message = new StringBuilder("""
				💰 상환 처리 완료

				#%s · %s 님

				▸ 입금액　　%s
				▸ 반영　　　%s
				▸ 남은 금액　%s
				""".formatted(loan.customerId(), loan.userName(), money(result.paidAmount()),
				result.appliedSummary(), money(loan.remainingAmount())));
		loan.nextUnpaid().ifPresent(next -> message.append("\n📅 다음 납부\n").append(nextLine(next)).append('\n'));
		message.append('\n').append(DIVIDER).append('\n')
				.append(result.status().emoji()).append(" 현재 상태　").append(result.status().label());
		return message.toString();
	}

	public List<String> list(Map<LoanStatus, List<LoanListItem>> groups) {
		List<String> sections = new ArrayList<>();
		for (LoanStatus status : LoanStatus.values()) {
			List<LoanListItem> items = groups.getOrDefault(status, List.of());
			if (!items.isEmpty()) {
				String lines = items.stream().map(item -> listLine(item.loan(), status)).collect(Collectors.joining("\n"));
				sections.add(status.emoji() + " " + status.label() + "\n" + lines);
			}
		}
		if (sections.isEmpty()) {
			return List.of("📋 대출 목록\n\n등록된 대출이 없습니다.");
		}
		return split("📋 대출 목록\n\n" + String.join("\n\n" + DIVIDER + "\n", sections));
	}

	public String detail(LoanListItem item) {
		Loan loan = item.loan();
		StringBuilder message = new StringBuilder("""
				🔎 대출 상세

				#%s · %s 님

				✦ 대출금　　%s
				✦ 상환방식　%s
				✦ 금리　　　회차당 %s
				✦ 남은 금액　%s
				✦ 연체액　　%s
				""".formatted(loan.customerId(), loan.userName(), money(loan.principal()), loan.method().label(),
				percent(loan.ratePerRound()), money(loan.remainingAmount()), money(overdueAmount(loan))));
		loan.nextUnpaid().ifPresent(next -> message.append("\n💳 이번에 갚을 금액\n").append(nextLine(next)).append('\n'));
		message.append("\n📅 상환 일정\n");
		for (Installment installment : loan.installments()) {
			message.append("· %d회차　%s　%s　%s\n".formatted(installment.round(), dateTime(installment.dueAt()),
					money(installment.amountDue() + installment.overdueInterest()), installmentState(installment)));
		}
		message.append('\n').append(DIVIDER).append('\n')
				.append(item.status().emoji()).append(" 현재 상태　").append(item.status().label());
		return message.toString();
	}

	public String history(String customerId, String userName, List<LoanEvent> events) {
		String lines = events.stream().map(this::historyLine).collect(Collectors.joining("\n"));
		return "🗂 대출 이력\n\n#%s · %s 님\n\n%s".formatted(customerId, userName, lines);
	}

	public String log(LoanLog log) {
		return """
				📝 %s 입력 로그

				처리자　<@%s>
				입력　　%s
				결과　　%s
				시각　　%s""".formatted(log.command(), log.actor().discordUserId(), log.input(),
				(log.success() ? "성공" : "거부") + " (" + log.result() + ")", FULL_TIME.format(log.occurredAt()));
	}

	public String settings(LoanSettings settings) {
		return """
				⚙️ 대출 설정

				✦ 금리　　　　　회차당 %s
				✦ 연체이자율　　회차당 %s
				✦ 상환회차　　　총 %d회
				✦ 납부주기　　　%s
				✦ 납부임박기준　%s
				✦ 최대대출금액　%s
				✦ 로그채널　　　%s""".formatted(percent(settings.ratePerRound()), percent(settings.overdueRatePerRound()),
				settings.rounds(), cycle(settings.cycle()), duration(settings.dueSoonThreshold()),
				money(settings.maxLoanAmount()),
				settings.logChannelId() == null ? "없음" : "<#" + settings.logChannelId() + ">");
	}

	public List<String> split(String text) {
		List<String> messages = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String line : text.split("\n", -1)) {
			if (!current.isEmpty() && current.length() + line.length() + 1 > MESSAGE_LIMIT) {
				messages.add(current.toString());
				current.setLength(0);
			}
			if (!current.isEmpty()) {
				current.append('\n');
			}
			current.append(line);
		}
		messages.add(current.toString());
		return messages;
	}

	private String listLine(Loan loan, LoanStatus status) {
		String prefix = "· #%s %s　".formatted(loan.customerId(), loan.userName());
		return prefix + switch (status) {
			case NORMAL -> "%s 남음　다음 %s".formatted(money(loan.remainingAmount()), date(loan.nextDueAt()));
			case DUE_SOON -> "%s　%s 납부".formatted(money(loan.nextUnpaid().orElseThrow().totalUnpaid()), date(loan.nextDueAt()));
			case OVERDUE -> "%d회차 연체　%s".formatted(overdueRounds(loan), money(overdueAmount(loan)));
			case PAID -> date(loan.completedAt()) + " 완료";
		};
	}

	private String nextLine(Installment next) {
		return "· %d회차　%s　%s".formatted(next.round(), dateTime(next.dueAt()), money(next.totalUnpaid()));
	}

	private String installmentState(Installment installment) {
		if (installment.isSettled()) {
			return "납부 완료";
		}
		if (installment.lastOverdueAppliedAt() != null) {
			return "연체";
		}
		return installment.paidAmount() > 0 ? "일부 납부" : "미납";
	}

	private String historyLine(LoanEvent event) {
		String description = switch (event.type()) {
			case REGISTERED -> "대출 등록 " + money(event.amount());
			case REPAID -> "상환 %s　남은 금액 %s".formatted(money(event.amount()), money(event.remainingAmount()));
			case OVERDUE_STARTED -> "연체 시작 %s　남은 금액 %s".formatted(money(event.amount()), money(event.remainingAmount()));
			case OVERDUE_INTEREST_ADDED -> "연체 이자 %s　남은 금액 %s".formatted(money(event.amount()), money(event.remainingAmount()));
			case COMPLETED -> "완납　　　　　남은 금액 " + money(event.remainingAmount());
		};
		return "· %s　%s".formatted(dateTime(event.occurredAt()), description);
	}

	private long overdueRounds(Loan loan) {
		return loan.installments().stream().filter(this::isOverdue).count();
	}

	private long overdueAmount(Loan loan) {
		return loan.installments().stream().filter(this::isOverdue).mapToLong(Installment::totalUnpaid).sum();
	}

	private boolean isOverdue(Installment installment) {
		return !installment.isSettled() && installment.lastOverdueAppliedAt() != null;
	}

	private String money(long amount) {
		return "%,d원".formatted(amount);
	}

	private String percent(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString() + "%";
	}

	private String date(Instant instant) {
		return DATE.format(instant);
	}

	private String dateTime(Instant instant) {
		return DATE_TIME.format(instant);
	}

	private String cycle(PaymentCycle cycle) {
		String unit = switch (cycle.unit()) {
			case MINUTES -> "분";
			case HOURS -> "시간";
			case DAYS -> "일";
			default -> "개월";
		};
		return cycle.amount() + unit;
	}

	private String duration(Duration duration) {
		long minutes = duration.toMinutes();
		if (minutes % ChronoUnit.DAYS.getDuration().toMinutes() == 0) {
			return duration.toDays() + "일";
		}
		if (minutes % 60 == 0) {
			return duration.toHours() + "시간";
		}
		return minutes + "분";
	}
}
