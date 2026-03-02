package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
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
public class AccountGroupControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCrud() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create
            AccountGroupDTO newGroup = new AccountGroupDTO(null, "Group A");
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newGroup)))
                    .build();

            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            AccountGroupDTO createdGroup = objectMapper.readValue(createResponse.body(), AccountGroupDTO.class);
            assertThat(createdGroup.id()).isNotNull();
            assertThat(createdGroup.name()).isEqualTo("Group A");

            // 2. Get All
            HttpRequest getAllRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                    .GET()
                    .build();

            HttpResponse<String> getAllResponse = client.send(getAllRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getAllResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            List<AccountGroupDTO> allGroups = objectMapper.readValue(getAllResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, AccountGroupDTO.class));
            assertThat(allGroups).extracting(AccountGroupDTO::name).contains("Group A");

            // 3. Get By ID
            HttpRequest getByIdRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                    .GET()
                    .build();

            HttpResponse<String> getByIdResponse = client.send(getByIdRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getByIdResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            AccountGroupDTO fetchedGroup = objectMapper.readValue(getByIdResponse.body(), AccountGroupDTO.class);
            assertThat(fetchedGroup).isEqualTo(createdGroup);

            // 4. Update
            AccountGroupDTO updateGroup = new AccountGroupDTO(null, "Group B");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateGroup)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            AccountGroupDTO updatedGroup = objectMapper.readValue(updateResponse.body(), AccountGroupDTO.class);
            assertThat(updatedGroup.id()).isEqualTo(createdGroup.id());
            assertThat(updatedGroup.name()).isEqualTo("Group B");

            // 5. Update to existing name (conflict)
            // Create another group first
            AccountGroupDTO anotherGroup = new AccountGroupDTO(null, "Group C");
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(anotherGroup)))
                    .build(), HttpResponse.BodyHandlers.ofString());

            HttpRequest conflictRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new AccountGroupDTO(null, "Group C"))))
                    .build();

            HttpResponse<String> conflictResponse = client.send(conflictRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(conflictResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // 6. Delete (fail because of reference constraint)
            // Insert into account table
            jdbcTemplate.execute("INSERT INTO account(name, account_group_id) VALUES ('Account 1', " + createdGroup.id() + ")");

            HttpRequest deleteFailRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteFailResponse = client.send(deleteFailRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteFailResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // Cleanup account
            jdbcTemplate.execute("DELETE FROM account WHERE account_group_id = " + createdGroup.id());

            // 7. Delete (success)
            HttpRequest deleteSuccessRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteSuccessResponse = client.send(deleteSuccessRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteSuccessResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    @Test
    public void testNotFound() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM account_group", Integer.class);

            // 1. Get Non-Existent
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + nonExistentId))
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 2. Update Non-Existent
            AccountGroupDTO updateGroup = new AccountGroupDTO(null, "Somewhere");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + nonExistentId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateGroup)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 3. Delete Non-Existent
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + nonExistentId))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
