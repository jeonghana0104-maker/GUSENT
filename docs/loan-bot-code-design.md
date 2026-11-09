# 대출 봇 코드 설계

기능 규칙은 [대출 장부 봇 설계](loan-bot-design.md)를 따른다. 이 문서는 클래스 구조와 책임만 정의하며, 저장소는 메모리 구현으로 시작해 Firestore로 교체한다.

## 1. 구조

```text
디스코드 슬래시 명령어
  → CommandDispatcher   명령어 찾기, 역할 확인, 로그 기록, 오류 메시지 처리
  → 명령어 Handler      입력값 읽기, 서비스 호출, 응답 전송
  → Service             대출 규칙 처리
  → Repository          저장과 조회 (메모리 → Firestore)
```

의존 방향은 위에서 아래로만 흐른다. `loan` 패키지는 JDA와 Firebase 클래스를 사용하지 않는다.

## 2. 패키지

```text
com.hyacinth
├─ discord
│  ├─ DiscordBotConfig
│  ├─ command
│  │  ├─ SlashCommandHandler
│  │  ├─ CommandDispatcher
│  │  ├─ CommandRegistrar
│  │  ├─ RegisterLoanCommand      /대출등록
│  │  ├─ RepayLoanCommand         /대출상환
│  │  ├─ LoanListCommand          /목록
│  │  ├─ LoanDetailCommand        /상세조회
│  │  ├─ LoanHistoryCommand       /대출이력
│  │  └─ SettingCommand           /설정 보기, 변경
│  ├─ permission
│  │  ├─ RequireRole              권한 어노테이션
│  │  ├─ BotRole                  ADMIN(0), MEMBER(1)
│  │  ├─ RoleChecker              서버 역할 판단
│  │  └─ DiscordRoleProperties    역할 이름 설정
│  ├─ log
│  │  ├─ CommandLog               로그 대상 어노테이션
│  │  └─ DiscordLogPublisher      로그 채널 전송
│  └─ message
│     └─ LoanMessageFormatter     메시지 문자열 생성
├─ loan
│  ├─ domain
│  │  ├─ Loan, Installment, Repayment
│  │  ├─ RepaymentMethod, LoanStatus
│  │  ├─ LoanSettings, LoanLog
│  │  ├─ LoanEvent, LoanEventType
│  │  └─ LoanException
│  ├─ schedule
│  │  ├─ RepaymentScheduleCalculator
│  │  ├─ EqualPaymentCalculator
│  │  ├─ EqualPrincipalCalculator
│  │  └─ BulletRepaymentCalculator
│  ├─ service
│  │  ├─ LoanService
│  │  ├─ LoanStatusResolver
│  │  ├─ OverdueService
│  │  ├─ SettingService
│  │  └─ LoanLogService
│  ├─ repository
│  │  ├─ LoanRepository
│  │  ├─ SettingRepository
│  │  ├─ LoanLogRepository
│  │  └─ LoanEventRepository
│  └─ scheduler
│     └─ OverdueScheduler
└─ infra
   ├─ memory                      처음 사용하는 구현
   └─ firestore                   이후 추가하는 구현
```

## 3. 권한

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
```

| 명령어 | 어노테이션 |
|---|---|
| `/대출등록`, `/대출상환`, `/목록`, `/상세조회`, `/대출이력` | `@RequireRole(BotRole.MEMBER)` |
| `/설정` | `@RequireRole(BotRole.ADMIN)` |

사용 예:

```java
@Component
@RequireRole(BotRole.ADMIN)
public class SettingCommand implements SlashCommandHandler { }
```

- `RoleChecker`는 `event.getMember().getRoles()`의 역할 이름을 읽어 사용자의 가장 높은 `BotRole`을 구한다.
- `userRole.canAccess(requiredRole)`이 참이면 실행한다. 0순위 `ADMIN`은 1순위 `MEMBER` 명령어도 사용할 수 있다.
- `CommandDispatcher`는 Handler 실행 전에 `AnnotationUtils.findAnnotation`으로 권한을 읽는다.
- 앱 시작 시 `@RequireRole`이 없는 Handler를 발견하면 시작을 실패시켜 권한 누락을 막는다.
- 명령어는 서버 전용으로 등록한다. 디스코드 명령어 목록에는 일반 사용자에게도 `/설정`이 보일 수 있지만, 실행은 거부된다.

봇은 역할 목록을 직접 읽지만, 어떤 역할을 관리자로 볼지는 알아야 하므로 역할 이름만 설정한다. 역할 ID와 서버 ID는 입력하지 않는다.

```properties
discord.roles.admin=관리자
discord.roles.member=일반
```

서버에서 역할 이름을 바꾸면 이 설정도 같이 바꿔야 한다. 역할 판단은 `RoleChecker`에 모아 두어, 나중에 역할 ID 방식으로 바꿔도 이 클래스만 수정한다.

## 4. 명령어 처리

```java
public interface SlashCommandHandler {
    SlashCommandData commandData();
    void handle(SlashCommandInteractionEvent event);
}
```

`CommandDispatcher`가 `ListenerAdapter`를 상속하고 다음 순서로 처리한다.

1. 명령어 이름으로 Handler 찾기
2. `@RequireRole` 확인, 실패 시 본인에게만 거부 메시지 전송
3. `deferReply()` 호출, 저장소 응답이 3초를 넘어도 상호작용이 끊기지 않게 함
4. Handler 실행
5. `LoanException`은 안내 메시지로, 그 외 예외는 일반 오류 메시지로 변환
6. `@CommandLog`가 있으면 성공 또는 거부 결과 기록

`@CommandLog`는 `/대출등록`과 `/대출상환`에만 붙인다. `CommandRegistrar`는 봇이 서버에 연결되거나 새 서버에 초대될 때 해당 서버에 명령어를 등록한다. 장부는 서버별로 나누지 않으므로 하나의 서버에서 사용하는 것을 전제로 한다.

## 5. 서비스

| 클래스 | 주요 메서드 | 책임 |
|---|---|---|
| `LoanService` | `register(command)` | 중복 확인, 입력 검증, 일정 생성, 저장 |
| | `repay(command)` | 고유번호와 유저명 확인, 상환 금액 배분 |
| | `findAll()`, `findDetail(customerId)` | 목록과 상세 조회 |
| `LoanStatusResolver` | `resolve(loan, now, settings)` | 정상, 납부 임박, 연체, 완납 계산 |
| `OverdueService` | `applyOverdueInterest(now)` | 납부일이 지난 회차에 연체 이자 반영 |
| `SettingService` | `get()`, `update(key, value, actor)` | 설정 조회, 범위 검증, 변경 |
| `LoanLogService` | `record(log)` | 로그 저장, 로그 채널 전송 요청 |
| `LoanEventService` | `record(event)`, `findRecent(customerId, limit)` | 대출 이력 저장과 조회 |

`LoanService`와 `OverdueService`는 상태를 바꿀 때 `LoanEvent`도 함께 만든다. 로그는 입력 기록, 이력은 대출 변화 기록으로 구분한다.

현재 시각은 `Clock`을 주입받아 테스트에서 시간을 고정할 수 있게 한다.

## 6. 상환 일정 계산

```java
public interface RepaymentScheduleCalculator {
    RepaymentMethod method();
    List<Installment> calculate(ScheduleRequest request);
}
```

각 상환 방식을 별도 클래스로 구현하고, `Map<RepaymentMethod, RepaymentScheduleCalculator>`로 선택한다. 금액은 `long`, 비율과 중간 계산은 `BigDecimal`을 사용한다.

## 7. 저장소 인터페이스

```java
public interface LoanRepository {
    Optional<Loan> findByCustomerId(String customerId);
    List<Loan> findAll();
    List<Loan> findActiveDueBefore(Instant now);
    void createIfNoActiveLoan(Loan loan);
    Loan update(String customerId, UnaryOperator<Loan> change);
}
```

- `createIfNoActiveLoan`은 고유번호 중복 확인과 저장을 한 번에 처리한다.
- `update`는 메모리 구현에서는 잠금, Firestore 구현에서는 트랜잭션으로 처리한다.
- `SettingRepository`, `LoanLogRepository`, `LoanEventRepository`도 같은 방식으로 분리한다.
- `@Primary`나 프로필로 메모리 구현과 Firestore 구현을 바꾼다.

## 8. Firestore 전환 시 구조

| 컬렉션 | 문서 ID | 내용 |
|---|---|---|
| `loans` | 고유번호 | 현재 대출, 회차 배열, `active`, `nextDueAt` |
| `loanHistory` | 자동 ID | 완납 후 재대출 시 옮긴 이전 대출 |
| `settings` | `default` | 관리자 설정 |
| `logs` | 자동 ID | 입력 로그 |
| `loanEvents` | 자동 ID | 대출 이력, `customerId`와 `occurredAt`으로 조회 |

- 회차는 최대 60개이므로 대출 문서 안에 배열로 넣어 조회 횟수를 줄인다.
- 스케줄러는 전체 대출을 읽지 않고 `active == true`, `nextDueAt <= now`인 문서만 조회한다. Firestore 복합 색인이 필요할 수 있다.
- 무료 요금제의 일일 읽기와 쓰기 한도를 넘지 않도록, 연동 전에 공식 요금 문서를 확인한다.
- 서비스 계정 키는 `.env`처럼 Git에 올리지 않는다.

## 9. 구현 순서

1. 도메인 클래스와 3가지 일정 계산기, 단위 테스트
2. 메모리 저장소와 `LoanService`, 상환 배분 테스트
3. 권한 어노테이션, `CommandDispatcher`, 명령어 Handler
4. 연체 스케줄러와 입력 로그
5. Firestore 저장소 추가
