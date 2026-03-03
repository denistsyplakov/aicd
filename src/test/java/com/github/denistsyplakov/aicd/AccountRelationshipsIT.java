package com.github.denistsyplakov.aicd;

import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountRelationshipsIT extends BaseIT {

    @Test
    public void testGetAccountsForRegion() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create Region
            RegionDTO region = createRegion(client, randomName("Test Region"));

            // 2. Create Account for Region
            AccountDTO account1 = createAccount(client, randomName("Account 1"), null, region.id());
            AccountDTO account2 = createAccount(client, randomName("Account 2"), null, region.id());

            // 3. Create another Region and Account
            RegionDTO otherRegion = createRegion(client, randomName("Other Region"));
            createAccount(client, randomName("Account 3"), null, otherRegion.id());

            // 4. Get Accounts for Region
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + region.id() + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<AccountDTO> accounts = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountDTO.class));
            assertThat(accounts).hasSize(2);
            assertThat(accounts).extracting(AccountDTO::name).containsExactlyInAnyOrder(account1.name(), account2.name());
        }
    }

    @Test
    public void testGetAccountsForAccountGroup() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create Account Group
            AccountGroupDTO group = createAccountGroup(client, randomName("Test Group"));

            // 2. Create Account for Group
            AccountDTO account1 = createAccount(client, randomName("Group Account 1"), group.id(), null);
            AccountDTO account2 = createAccount(client, randomName("Group Account 2"), group.id(), null);

            // 3. Create another Group and Account
            AccountGroupDTO otherGroup = createAccountGroup(client, randomName("Other Group"));
            createAccount(client, randomName("Group Account 3"), otherGroup.id(), null);

            // 4. Get Accounts for Group
            HttpRequest getAccountsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + group.id() + "/account"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(getAccountsRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<AccountDTO> accounts = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountDTO.class));
            assertThat(accounts).hasSize(2);
            assertThat(accounts).extracting(AccountDTO::name).containsExactlyInAnyOrder(account1.name(), account2.name());
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

}
