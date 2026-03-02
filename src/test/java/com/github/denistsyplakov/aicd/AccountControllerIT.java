package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
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
public class AccountControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCrud() throws Exception {
        jdbcTemplate.execute("DELETE FROM sow_text_index");
        jdbcTemplate.execute("DELETE FROM sow");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM account_group");
        jdbcTemplate.execute("DELETE FROM region");

        try (HttpClient client = HttpClient.newHttpClient()) {
            // Setup: Create Account Group and Region
            AccountGroupDTO group = createAccountGroup(client, "Test Group");
            RegionDTO region = createRegion(client, "Test Region");

            // 1. Create
            AccountDTO newAccount = new AccountDTO(null, "Account A", group.id(), region.id());
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newAccount)))
                    .build();

            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            AccountDTO createdAccount = objectMapper.readValue(createResponse.body(), AccountDTO.class);
            assertThat(createdAccount.id()).isNotNull();
            assertThat(createdAccount.name()).isEqualTo("Account A");
            assertThat(createdAccount.accountGroupId()).isEqualTo(group.id());
            assertThat(createdAccount.regionId()).isEqualTo(region.id());

            // 2. Get All
            HttpRequest getAllRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .GET()
                    .build();

            HttpResponse<String> getAllResponse = client.send(getAllRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getAllResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            List<AccountDTO> allAccounts = objectMapper.readValue(getAllResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountDTO.class));
            assertThat(allAccounts).extracting(AccountDTO::name).contains("Account A");

            // 3. Get By ID
            HttpRequest getByIdRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .GET()
                    .build();

            HttpResponse<String> getByIdResponse = client.send(getByIdRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getByIdResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            AccountDTO fetchedAccount = objectMapper.readValue(getByIdResponse.body(), AccountDTO.class);
            assertThat(fetchedAccount).isEqualTo(createdAccount);

            // 4. Update
            AccountDTO updateAccount = new AccountDTO(null, "Account B", group.id(), region.id());
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateAccount)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            AccountDTO updatedAccount = objectMapper.readValue(updateResponse.body(), AccountDTO.class);
            assertThat(updatedAccount.id()).isEqualTo(createdAccount.id());
            assertThat(updatedAccount.name()).isEqualTo("Account B");

            // 5. Update to existing name (conflict)
            // Create another account first
            AccountDTO anotherAccount = new AccountDTO(null, "Account C", group.id(), region.id());
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(anotherAccount)))
                    .build(), HttpResponse.BodyHandlers.ofString());

            HttpRequest conflictRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new AccountDTO(null, "Account C", group.id(), region.id()))))
                    .build();

            HttpResponse<String> conflictResponse = client.send(conflictRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(conflictResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // 6. Delete (fail because of reference constraint)
            // Insert into sow table
            jdbcTemplate.execute("INSERT INTO sow(account_id, date, title, amount, description) VALUES (" + createdAccount.id() + ", '2023-01-01', 'Title', 100.00, 'Desc')");

            HttpRequest deleteFailRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteFailResponse = client.send(deleteFailRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteFailResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // Cleanup sow
            jdbcTemplate.execute("DELETE FROM sow WHERE account_id = " + createdAccount.id());

            // 7. Delete (success)
            HttpRequest deleteSuccessRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteSuccessResponse = client.send(deleteSuccessRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteSuccessResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    private AccountGroupDTO createAccountGroup(HttpClient client, String name) throws Exception {
        AccountGroupDTO group = new AccountGroupDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(group)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Failed to create account group, status code: %d, body: %s", response.statusCode(), response.body())
                .isEqualTo(HttpStatus.CREATED.value());
        return objectMapper.readValue(response.body(), AccountGroupDTO.class);
    }

    private RegionDTO createRegion(HttpClient client, String name) throws Exception {
        RegionDTO region = new RegionDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(region)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Failed to create region, status code: %d, body: %s", response.statusCode(), response.body())
                .isEqualTo(HttpStatus.CREATED.value());
        return objectMapper.readValue(response.body(), RegionDTO.class);
    }

    @Test
    public void testNotFound() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM account", Integer.class);

            // 1. Get Non-Existent
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + nonExistentId))
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 2. Update Non-Existent
            AccountDTO updateAccount = new AccountDTO(null, "Somewhere", 1, 1);
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + nonExistentId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateAccount)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 3. Delete Non-Existent
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + nonExistentId))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Test
    public void testInvalidForeignKeys() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentGroupId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM account_group", Integer.class);
            Integer nonExistentRegionId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM region", Integer.class);

            // Setup: Create a valid group and region for valid FKs
            AccountGroupDTO group = createAccountGroup(client, "Valid Group");
            RegionDTO region = createRegion(client, "Valid Region");

            // 1. Create with invalid group
            AccountDTO invalidGroupAccount = new AccountDTO(null, "Invalid Group Account", nonExistentGroupId, region.id());
            HttpRequest createInvalidGroupRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidGroupAccount)))
                    .build();
            HttpResponse<String> createInvalidGroupResponse = client.send(createInvalidGroupRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createInvalidGroupResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // 2. Create with invalid region
            AccountDTO invalidRegionAccount = new AccountDTO(null, "Invalid Region Account", group.id(), nonExistentRegionId);
            HttpRequest createInvalidRegionRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidRegionAccount)))
                    .build();
            HttpResponse<String> createInvalidRegionResponse = client.send(createInvalidRegionRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createInvalidRegionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // 3. Create with both invalid
            AccountDTO invalidBothAccount = new AccountDTO(null, "Invalid Both Account", nonExistentGroupId, nonExistentRegionId);
            HttpRequest createInvalidBothRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidBothAccount)))
                    .build();
            HttpResponse<String> createInvalidBothResponse = client.send(createInvalidBothRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createInvalidBothResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // 4. Update with invalid group
            // First create a valid account
            AccountDTO validAccount = new AccountDTO(null, "Valid Account", group.id(), region.id());
            HttpResponse<String> validCreateResponse = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(validAccount)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            AccountDTO createdAccount = objectMapper.readValue(validCreateResponse.body(), AccountDTO.class);

            AccountDTO updateInvalidGroup = new AccountDTO(null, "Updated Valid Account", nonExistentGroupId, region.id());
            HttpRequest updateInvalidGroupRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateInvalidGroup)))
                    .build();
            HttpResponse<String> updateInvalidGroupResponse = client.send(updateInvalidGroupRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateInvalidGroupResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // 5. Update with invalid region
            AccountDTO updateInvalidRegion = new AccountDTO(null, "Updated Valid Account", group.id(), nonExistentRegionId);
            HttpRequest updateInvalidRegionRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateInvalidRegion)))
                    .build();
            HttpResponse<String> updateInvalidRegionResponse = client.send(updateInvalidRegionRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateInvalidRegionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            // 6. Update with both invalid
            AccountDTO updateInvalidBoth = new AccountDTO(null, "Updated Valid Account", nonExistentGroupId, nonExistentRegionId);
            HttpRequest updateInvalidBothRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + createdAccount.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateInvalidBoth)))
                    .build();
            HttpResponse<String> updateInvalidBothResponse = client.send(updateInvalidBothRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateInvalidBothResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }
}
