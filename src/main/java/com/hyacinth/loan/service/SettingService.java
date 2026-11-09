package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.domain.LoanSettings;
import com.hyacinth.loan.domain.PaymentCycle;
import com.hyacinth.loan.repository.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SettingService {

	public static final String RATE = "금리";
	public static final String OVERDUE_RATE = "연체이자율";
	public static final String ROUNDS = "상환회차";
	public static final String CYCLE = "납부주기";
	public static final String DUE_SOON = "납부임박기준";
	public static final String MAX_AMOUNT = "최대대출금액";
	public static final String LOG_CHANNEL = "로그채널";
	public static final List<String> KEYS = List.of(RATE, OVERDUE_RATE, ROUNDS, CYCLE, DUE_SOON, MAX_AMOUNT, LOG_CHANNEL);

	private static final Logger log = LoggerFactory.getLogger(SettingService.class);
	private static final Pattern PERIOD = Pattern.compile("^(\\d{1,6})\\s*(분|시간|일|개월)$");
	private static final BigDecimal MAX_RATE = BigDecimal.valueOf(30);
	private static final long MAX_LOAN_LIMIT = 1_000_000_000_000L;

	private final SettingRepository repository;
	private final Clock clock;

	public SettingService(SettingRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public LoanSettings get() {
		return repository.get();
	}

	public LoanSettings update(String key, String value, Actor actor) {
		LoanSettings current = repository.get();
		String input = value.strip();
		LoanSettings updated = switch (key) {
			case RATE -> new LoanSettings(parseRate(input), current.overdueRatePerRound(), current.rounds(),
					current.cycle(), current.dueSoonThreshold(), current.maxLoanAmount(), current.logChannelId());
			case OVERDUE_RATE -> new LoanSettings(current.ratePerRound(), parseRate(input), current.rounds(),
					current.cycle(), current.dueSoonThreshold(), current.maxLoanAmount(), current.logChannelId());
			case ROUNDS -> new LoanSettings(current.ratePerRound(), current.overdueRatePerRound(),
					(int) parseNumber(input, 1, 60, "상환회차는 1~60회로 입력해 주세요."),
					current.cycle(), current.dueSoonThreshold(), current.maxLoanAmount(), current.logChannelId());
			case CYCLE -> new LoanSettings(current.ratePerRound(), current.overdueRatePerRound(), current.rounds(),
					parseCycle(input), current.dueSoonThreshold(), current.maxLoanAmount(), current.logChannelId());
			case DUE_SOON -> new LoanSettings(current.ratePerRound(), current.overdueRatePerRound(), current.rounds(),
					current.cycle(), parseThreshold(input), current.maxLoanAmount(), current.logChannelId());
			case MAX_AMOUNT -> new LoanSettings(current.ratePerRound(), current.overdueRatePerRound(), current.rounds(),
					current.cycle(), current.dueSoonThreshold(),
					parseNumber(input, 1, MAX_LOAN_LIMIT, "최대 대출 금액은 1~1,000,000,000,000으로 입력해 주세요."),
					current.logChannelId());
			case LOG_CHANNEL -> new LoanSettings(current.ratePerRound(), current.overdueRatePerRound(), current.rounds(),
					current.cycle(), current.dueSoonThreshold(), current.maxLoanAmount(),
					"없음".equals(input) ? null : input);
			default -> throw new LoanException("알 수 없는 설정 항목입니다.");
		};
		repository.save(updated);
		log.info("Loan setting {} changed to {} by {}", key, input, actor.discordUserId());
		return updated;
	}

	private BigDecimal parseRate(String input) {
		try {
			BigDecimal rate = new BigDecimal(input.replace("%", "").strip());
			if (rate.signum() >= 0 && rate.compareTo(MAX_RATE) <= 0) {
				return rate;
			}
		}
		catch (NumberFormatException ignored) {
			// 아래의 공통 안내로 처리한다.
		}
		throw new LoanException("비율은 0~30% 사이의 숫자로 입력해 주세요.");
	}

	private long parseNumber(String input, long min, long max, String message) {
		try {
			long number = Long.parseLong(input.replace(",", ""));
			if (number >= min && number <= max) {
				return number;
			}
		}
		catch (NumberFormatException ignored) {
			// 아래의 공통 안내로 처리한다.
		}
		throw new LoanException(message);
	}

	private PaymentCycle parseCycle(String input) {
		String message = "납부 주기는 1분~12개월 범위로, 예: 1분, 2시간, 1일, 1개월 형식으로 입력해 주세요.";
		Matcher matcher = PERIOD.matcher(input);
		if (!matcher.matches()) {
			throw new LoanException(message);
		}
		PaymentCycle cycle = new PaymentCycle(Long.parseLong(matcher.group(1)), unit(matcher.group(2)));
		Instant start = clock.instant();
		Instant limit = new PaymentCycle(12, ChronoUnit.MONTHS).addTo(start, 1);
		if (cycle.amount() < 1 || cycle.addTo(start, 1).isAfter(limit)) {
			throw new LoanException(message);
		}
		return cycle;
	}

	private Duration parseThreshold(String input) {
		String message = "납부 임박 기준은 1분~30일 범위로, 예: 30분, 24시간, 3일 형식으로 입력해 주세요.";
		Matcher matcher = PERIOD.matcher(input);
		if (!matcher.matches() || "개월".equals(matcher.group(2))) {
			throw new LoanException(message);
		}
		Duration duration = Duration.of(Long.parseLong(matcher.group(1)), unit(matcher.group(2)));
		if (duration.compareTo(Duration.ofMinutes(1)) < 0 || duration.compareTo(Duration.ofDays(30)) > 0) {
			throw new LoanException(message);
		}
		return duration;
	}

	private ChronoUnit unit(String label) {
		return switch (label) {
			case "분" -> ChronoUnit.MINUTES;
			case "시간" -> ChronoUnit.HOURS;
			case "일" -> ChronoUnit.DAYS;
			default -> ChronoUnit.MONTHS;
		};
	}
}
