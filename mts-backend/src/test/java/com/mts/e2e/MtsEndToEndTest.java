package com.mts.e2e;

import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.repository.AccountRepository;
import com.mts.repository.TransactionRepository;
import com.mts.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  E2E TEST (Top of Functional Pyramid) — REST Assured        ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  - Starts REAL HTTP server on random port                    ║
 * ║  - Uses REST Assured to hit actual REST endpoints            ║
 * ║  - Tests full stack: Controller → Service → Repo → DB       ║
 * ║  - Validates complete user journeys & business rules         ║
 * ║  - Slowest tests — keep count LOW                            ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("E2E: REST Assured API Tests")
class MtsEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    private User ravi, priya;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        ravi = userRepository.save(User.builder()
                .username("ravi.e2e").email("ravi@test.com")
                .passwordHash("hash").fullName("Ravi Kumar").isActive(true).build());

        priya = userRepository.save(User.builder()
                .username("priya.e2e").email("priya@test.com")
                .passwordHash("hash").fullName("Priya Sharma").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("ACC001").user(ravi).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("50000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("ACC002").user(ravi).accountType(AccountType.CURRENT)
                .balance(new BigDecimal("100000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("ACC003").user(priya).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("25000")).currency("INR").isActive(true).build());
    }

    // ── Smoke: App is alive ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/accounts responds with 200 and JSON array")
    void accountsEndpointIsAlive() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/accounts")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("GET /api/transactions responds with 200")
    void transactionsEndpointIsAlive() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/transactions")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/transactions/stats responds with dashboard shape")
    void statsEndpointIsAlive() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/transactions/stats")
        .then()
            .statusCode(200)
            .body("totalAccounts", notNullValue())
            .body("totalBalance", notNullValue())
            .body("totalTransactions", notNullValue());
    }

    // ── Account Features ─────────────────────────────────────────

    @Test
    @DisplayName("GET account by number returns correct details")
    void getAccountByNumber() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/accounts/ACC001")
        .then()
            .statusCode(200)
            .body("accountNumber", equalTo("ACC001"))
            .body("ownerName", equalTo("Ravi Kumar"))
            .body("accountType", equalTo("SAVINGS"))
            .body("balance", equalTo(50000.0f))
            .body("currency", equalTo("INR"))
            .body("isActive", equalTo(true));
    }

    @Test
    @DisplayName("GET accounts by username filters correctly")
    void getAccountsByUser() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/accounts/user/ravi.e2e")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("ownerName", everyItem(equalTo("Ravi Kumar")));
    }

    @Test
    @DisplayName("GET non-existent account returns 404 with error body")
    void accountNotFound() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/accounts/GHOST999")
        .then()
            .statusCode(404)
            .body("error", equalTo("Account Not Found"));
    }

    // ── Transfer: Happy Path ─────────────────────────────────────

    @Test
    @DisplayName("POST transfer succeeds and returns 201 with correct response")
    void successfulTransfer() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 10000,
                    "transferMode": "UPI",
                    "description": "Rent payment"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(201)
            .body("status", equalTo("SUCCESS"))
            .body("referenceId", notNullValue())
            .body("fromAccountNumber", equalTo("ACC001"))
            .body("toAccountNumber", equalTo("ACC003"))
            .body("amount", equalTo(10000))
            .body("transferMode", equalTo("UPI"))
            .body("description", equalTo("Rent payment"));
    }

    @Test
    @DisplayName("Balances update correctly after transfer")
    void balancesUpdateAfterTransfer() {
        // Transfer 15000 from ACC001 (50000) to ACC003 (25000)
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 15000,
                    "transferMode": "NEFT"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(201);

        // Sender: 50000 - 15000 = 35000
        given().contentType(ContentType.JSON)
        .when().get("/api/accounts/ACC001")
        .then().body("balance", equalTo(35000.0f));

        // Receiver: 25000 + 15000 = 40000
        given().contentType(ContentType.JSON)
        .when().get("/api/accounts/ACC003")
        .then().body("balance", equalTo(40000.0f));
    }

    @Test
    @DisplayName("Transaction history shows up after transfer")
    void transactionHistoryAfterTransfer() {
        // Make a transfer
        String refId =
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "fromAccountNumber": "ACC001",
                        "toAccountNumber": "ACC003",
                        "amount": 5000,
                        "transferMode": "IMPS"
                    }
                """)
            .when()
                .post("/api/transactions/transfer")
            .then()
                .statusCode(201)
                .extract().path("referenceId");

        // Look up by reference ID
        given().contentType(ContentType.JSON)
        .when().get("/api/transactions/" + refId)
        .then()
            .statusCode(200)
            .body("amount", equalTo(5000.0f))
            .body("status", equalTo("SUCCESS"));

        // Look up by account
        given().contentType(ContentType.JSON)
        .when().get("/api/transactions/account/ACC001")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1));
    }

    // ── Transfer: Business Rules ─────────────────────────────────

    @Test
    @DisplayName("Insufficient balance returns 400")
    void insufficientBalance() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 999999,
                    "transferMode": "UPI"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(400)
            .body("error", equalTo("Insufficient Balance"));
    }

    @Test
    @DisplayName("Self-transfer is blocked")
    void selfTransferBlocked() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC001",
                    "amount": 100,
                    "transferMode": "UPI"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(400)
            .body("message", containsString("same account"));
    }

    @Test
    @DisplayName("Validation errors return 400 with field details")
    void validationErrors() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(400)
            .body("error", equalTo("Validation Failed"))
            .body("details.size()", greaterThanOrEqualTo(3));
    }

    @Test
    @DisplayName("Failed transfer does not corrupt system state")
    void failedTransferKeepsState() {
        // Bad transfer
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 999999,
                    "transferMode": "UPI"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(400);

        // Balance unchanged
        given().contentType(ContentType.JSON)
        .when().get("/api/accounts/ACC001")
        .then().body("balance", equalTo(50000.0f));

        // Good transfer still works
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 100,
                    "transferMode": "UPI"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(201)
            .body("status", equalTo("SUCCESS"));
    }

    // ── Dashboard Stats ──────────────────────────────────────────

    @Test
    @DisplayName("Dashboard stats update after transfer")
    void dashboardStatsAfterTransfer() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "fromAccountNumber": "ACC001",
                    "toAccountNumber": "ACC003",
                    "amount": 1000,
                    "transferMode": "RTGS"
                }
            """)
        .when()
            .post("/api/transactions/transfer")
        .then()
            .statusCode(201);

        given().contentType(ContentType.JSON)
        .when().get("/api/transactions/stats")
        .then()
            .body("totalTransactions", equalTo(1))
            .body("successfulTransactions", equalTo(1))
            .body("totalTransferred", equalTo(1000.0f));
    }
}
