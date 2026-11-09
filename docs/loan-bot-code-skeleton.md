# 대출 봇 코드 뼈대

구현 전에 작성한 클래스 뼈대와 메서드 형태다. 메서드 안의 주석은 구현할 동작이다.

## 1. 권한

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    BotRole value();
}

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

@ConfigurationProperties("discord.roles")
public record DiscordRoleProperties(String admin, String member) { }

@Component
public class RoleChecker {
    public Optional<BotRole> highestRole(Member member) {
        // 역할 이름에 관리자가 있으면 ADMIN, 일반이 있으면 MEMBER
    }

    public boolean hasAccess(Member member, BotRole required) {
        // highestRole(member)에 canAccess(required) 적용
    }
}
```

## 2. 명령어 처리

```java
public interface SlashCommandHandler {
    SlashCommandData commandData();
    void handle(SlashCommandInteractionEvent event);
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandLog {
    String value();
}

@Component
public class CommandDispatcher extends ListenerAdapter {
    private final Map<String, SlashCommandHandler> handlers;
    private final RoleChecker roleChecker;
    private final LoanLogService logService;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // 1. 명령어 이름으로 Handler 찾기
        // 2. @RequireRole 확인, 실패 시 본인에게만 거부 메시지
        // 3. event.deferReply()
        // 4. handler.handle(event)
        // 5. LoanException은 안내 메시지로 변환
        // 6. @CommandLog가 있으면 성공과 거부 기록
    }
}

@Component
public class CommandRegistrar extends ListenerAdapter {
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        // 해당 서버에 명령어 등록
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        // 새로 초대된 서버에 명령어 등록
    }
}
```

## 3. 명령어 클래스

```java
@Component
@RequireRole(BotRole.MEMBER)
@CommandLog("대출등록")
public class RegisterLoanCommand implements SlashCommandHandler {
    private final LoanService loanService;
    private final LoanMessageFormatter formatter;

    @Override
    public SlashCommandData commandData() {
        // 대출등록: 고유번호, 유저명, 금액, 상환방식
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // 입력값 → RegisterLoanRequest → loanService.register → 메시지 전송
    }
}

@Component
@RequireRole(BotRole.MEMBER)
@CommandLog("대출상환")
public class RepayLoanCommand implements SlashCommandHandler { }

@Component
@RequireRole(BotRole.MEMBER)
public class LoanListCommand implements SlashCommandHandler { }

@Component
@RequireRole(BotRole.MEMBER)
public class LoanDetailCommand implements SlashCommandHandler { }

@Component
@RequireRole(BotRole.MEMBER)
public class LoanHistoryCommand implements SlashCommandHandler { }

@Component
@RequireRole(BotRole.ADMIN)
public class SettingCommand implements SlashCommandHandler { }
```

## 4. 도메인

```java
public enum RepaymentMethod { EQUAL_PAYMENT, EQUAL_PRINCIPAL, BULLET }

public enum LoanStatus { NORMAL, DUE_SOON, OVERDUE, PAID }

public enum LoanEventType {
    REGISTERED, REPAID, OVERDUE_STARTED, OVERDUE_INTEREST_ADDED, COMPLETED
}

public record Actor(String discordUserId, String displayName) { }

public record PaymentCycle(long amount, ChronoUnit unit) { }

public record Installment(
    int round,
    Instant dueAt,
    long principal,
    long interest,
    long paidAmount,
    long overdueInterest,
    long paidOverdueInterest,
    Instant lastOverdueAppliedAt
) {
    public long unpaidAmount() { }
    public long unpaidOverdueInterest() { }
}

public record Loan(
    String loanId,
    String customerId,
    String userName,
    long principal,
    RepaymentMethod method,
    BigDecimal ratePerRound,
    int rounds,
    PaymentCycle cycle,
    Instant createdAt,
    Instant completedAt,
    Instant nextDueAt,
    List<Installment> installments
) {
    public boolean isCompleted() { }
    public long remainingAmount() { }
}

public record LoanSettings(
    BigDecimal ratePerRound,
    BigDecimal overdueRatePerRound,
    int rounds,
    PaymentCycle cycle,
    Duration dueSoonThreshold,
    long maxLoanAmount,
    String logChannelId
) { }

public record LoanEvent(
    String loanId, String customerId, String userName,
    LoanEventType type, long amount, long remainingAmount, Instant occurredAt
) { }

public record LoanLog(
    Actor actor, String command, String input,
    boolean success, String result, Instant occurredAt
) { }

public class LoanException extends RuntimeException { }
```

## 5. 상환 일정 계산

```java
public record ScheduleRequest(
    long principal, BigDecimal ratePerRound, int rounds,
    PaymentCycle cycle, Instant startAt
) { }

public interface RepaymentScheduleCalculator {
    RepaymentMethod method();
    List<Installment> calculate(ScheduleRequest request);
}

@Component
public class EqualPaymentCalculator implements RepaymentScheduleCalculator { }

@Component
public class EqualPrincipalCalculator implements RepaymentScheduleCalculator { }

@Component
public class BulletRepaymentCalculator implements RepaymentScheduleCalculator { }
```

## 6. 서비스

```java
public record RegisterLoanRequest(
    String customerId, String userName, long amount,
    RepaymentMethod method, Actor actor
) { }

public record RepayLoanRequest(
    String customerId, String userName, long amount, Actor actor
) { }

public record RepaymentResult(
    Loan loan, long paidAmount, String appliedSummary, LoanStatus status
) { }

public record LoanListItem(Loan loan, LoanStatus status) { }

@Service
public class LoanService {
    public Loan register(RegisterLoanRequest request) { }
    public RepaymentResult repay(RepayLoanRequest request) { }
    public Map<LoanStatus, List<LoanListItem>> findAllGroupedByStatus() { }
    public LoanListItem findDetail(String customerId) { }
}

@Component
public class LoanStatusResolver {
    public LoanStatus resolve(Loan loan, Instant now, LoanSettings settings) { }
}

@Service
public class OverdueService {
    public void applyOverdueInterest(Instant now) { }
}

@Service
public class SettingService {
    public LoanSettings get() { }
    public LoanSettings update(String key, String value, Actor actor) { }
}

@Service
public class LoanLogService {
    public void record(LoanLog log) { }
}

@Service
public class LoanEventService {
    public void record(LoanEvent event) { }
    public List<LoanEvent> findRecent(String customerId, int limit) { }
}
```

## 7. 저장소

메모리 구현으로 시작하고, 나중에 Firestore 구현으로 교체하는 부분이다.

```java
public interface LoanRepository {
    Optional<Loan> findByCustomerId(String customerId);
    List<Loan> findAll();
    List<Loan> findActiveDueBefore(Instant now);
    void createIfNoActiveLoan(Loan loan);
    Loan update(String customerId, UnaryOperator<Loan> change);
}

public interface SettingRepository {
    LoanSettings get();
    void save(LoanSettings settings);
}

public interface LoanLogRepository {
    void save(LoanLog log);
}

public interface LoanEventRepository {
    void save(LoanEvent event);
    List<LoanEvent> findRecentByCustomerId(String customerId, int limit);
}
```

## 8. 스케줄러와 메시지

```java
@Component
public class OverdueScheduler {
    private final OverdueService overdueService;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        overdueService.applyOverdueInterest(clock.instant());
    }
}

@Component
public class LoanMessageFormatter {
    public String registered(Loan loan) { }
    public String registerRejected(String customerId, String userName) { }
    public String repaid(RepaymentResult result) { }
    public List<String> list(Map<LoanStatus, List<LoanListItem>> groups) { }
    public String detail(LoanListItem item) { }
    public String history(String customerId, String userName, List<LoanEvent> events) { }
    public String log(LoanLog log) { }
}
```

실제 구현에서는 앱 클래스나 설정 클래스에 `@EnableScheduling`과 `@ConfigurationPropertiesScan`이 추가로 필요하다.
