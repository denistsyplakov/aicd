package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountRelationshipsIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        jdbcTemplate.execute("DELETE FROM sow");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM region");
        jdbcTemplate.execute("DELETE FROM account_group");
    }

    @Test
    public void testGetAccountsForRegion() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create Region
            RegionDTO region = createRegion(client, "Test Region");

            // 2. Create Account for Region
            AccountDTO account1 = createAccount(client, "Account 1", null, region.id());
            AccountDTO account2 = createAccount(client, "Account 2", null, region.id());

            // 3. Create another Region and Account
            RegionDTO otherRegion = createRegion(client, "Other Region");
            createAccount(client, "Account 3", null, otherRegion.id());

            // 4. Get Accounts for Region
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + region.id() + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<AccountDTO> accounts = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountDTO.class));
            assertThat(accounts).hasSize(2);
            assertThat(accounts).extracting(AccountDTO::name).containsExactlyInAnyOrder("Account 1", "Account 2");
        }
    }

    @Test
    public void testGetAccountsForAccountGroup() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create Account Group
            AccountGroupDTO group = createAccountGroup(client, "Test Group");

            // 2. Create Account for Group
            AccountDTO account1 = createAccount(client, "Group Account 1", group.id(), null);
            AccountDTO account2 = createAccount(client, "Group Account 2", group.id(), null);

            // 3. Create another Group and Account
            AccountGroupDTO otherGroup = createAccountGroup(client, "Other Group");
            createAccount(client, "Group Account 3", otherGroup.id(), null);

            // 4. Get Accounts for Group
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + group.id() + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<AccountDTO> accounts = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountDTO.class));
            assertThat(accounts).hasSize(2);
            assertThat(accounts).extracting(AccountDTO::name).containsExactlyInAnyOrder("Group Account 1", "Group Account 2");
        }
    }

    @Test
    public void testGetAccountsForNonExistentRegion() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM region", Integer.class);
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentId + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Test
    public void testGetAccountsForNonExistentAccountGroup() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM account_group", Integer.class);
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + nonExistentId + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    private RegionDTO createRegion(HttpClient client, String name) throws Exception {
        RegionDTO regionDTO = new RegionDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(regionDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return objectMapper.readValue(response.body(), RegionDTO.class);
    }

    private AccountGroupDTO createAccountGroup(HttpClient client, String name) throws Exception {
        AccountGroupDTO groupDTO = new AccountGroupDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(groupDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return objectMapper.readValue(response.body(), AccountGroupDTO.class);
    }

    private AccountDTO createAccount(HttpClient client, String name, Integer groupId, Integer regionId) throws Exception {
        AccountDTO accountDTO = new AccountDTO(null, name, groupId, regionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(accountDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return objectMapper.readValue(response.body(), AccountDTO.class);
    }
}
