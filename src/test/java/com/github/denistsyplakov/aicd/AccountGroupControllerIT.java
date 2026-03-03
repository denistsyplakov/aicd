package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.liquibase.enabled=false")
public class AccountGroupControllerIT {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Integer> createdAccountGroupIds = new ArrayList<>();
    private final List<Integer> createdAccountIds = new ArrayList<>();

    @AfterEach
    void cleanup() throws Exception {
        for (Integer id : createdAccountIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdAccountIds.clear();
        for (Integer id : createdAccountGroupIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdAccountGroupIds.clear();
    }

    private HttpResponse<String> createAccountGroup(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name));
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private int createAccountGroupAndTrack(String name) throws Exception {
        HttpResponse<String> response = createAccountGroup(name);
        assertThat(response.statusCode()).isEqualTo(201);
        int id = objectMapper.readTree(response.body()).get("id").asInt();
        createdAccountGroupIds.add(id);
        return id;
    }

    @Test
    public void testGetAllAccountGroups() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).isArray()).isTrue();
    }

    @Test
    public void testCreateAccountGroup() throws Exception {
        String name = "AG-" + UUID.randomUUID();
        HttpResponse<String> response = createAccountGroup(name);
        assertThat(response.statusCode()).isEqualTo(201);
        var node = objectMapper.readTree(response.body());
        assertThat(node.get("id").asInt()).isPositive();
        assertThat(node.get("name").asText()).isEqualTo(name);
        createdAccountGroupIds.add(node.get("id").asInt());
    }

    @Test
    public void testGetAccountGroupById() throws Exception {
        String name = "AG-" + UUID.randomUUID();
        int id = createAccountGroupAndTrack(name);

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(name);
    }

    @Test
    public void testGetAccountGroupByIdNotFound() throws Exception {
        int id = createAccountGroupAndTrack("AG-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountGroupIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testUpdateAccountGroup() throws Exception {
        int id = createAccountGroupAndTrack("AG-" + UUID.randomUUID());
        String newName = "AG-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", newName));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(newName);
    }

    @Test
    public void testUpdateAccountGroupNotFound() throws Exception {
        int id = createAccountGroupAndTrack("AG-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountGroupIds.remove(Integer.valueOf(id));

        String body = objectMapper.writeValueAsString(Map.of("name", "AG-" + UUID.randomUUID()));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testDeleteAccountGroup() throws Exception {
        int id = createAccountGroupAndTrack("AG-" + UUID.randomUUID());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
        createdAccountGroupIds.remove(Integer.valueOf(id));
    }

    @Test
    public void testDeleteAccountGroupNotFound() throws Exception {
        int id = createAccountGroupAndTrack("AG-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountGroupIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testCreateDuplicateAccountGroupName() throws Exception {
        String name = "AG-" + UUID.randomUUID();
        createAccountGroupAndTrack(name);

        HttpResponse<String> response = createAccountGroup(name);
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testUpdateAccountGroupToDuplicateName() throws Exception {
        String nameA = "AG-" + UUID.randomUUID();
        String nameB = "AG-" + UUID.randomUUID();
        int idA = createAccountGroupAndTrack(nameA);
        createAccountGroupAndTrack(nameB);

        String body = objectMapper.writeValueAsString(Map.of("name", nameB));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + idA))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testDeleteAccountGroupInUse() throws Exception {
        int agId = createAccountGroupAndTrack("AG-" + UUID.randomUUID());

        // Create a region for the account
        String regionBody = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> regionResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(regionBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int regionId = objectMapper.readTree(regionResponse.body()).get("id").asInt();

        // Create account referencing account group
        String accountBody = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", agId,
                "regionId", regionId));
        HttpResponse<String> accountResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(accountBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int accountId = objectMapper.readTree(accountResponse.body()).get("id").asInt();
        createdAccountIds.add(accountId);

        HttpResponse<String> deleteResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + agId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(deleteResponse.statusCode()).isEqualTo(409);

        // Cleanup
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + accountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(accountId));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + regionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testGetAccountGroupAccounts() throws Exception {
        int agId = createAccountGroupAndTrack("AG-" + UUID.randomUUID());

        String regionBody = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> regionResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(regionBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int regionId = objectMapper.readTree(regionResponse.body()).get("id").asInt();

        String accountName = "Account-" + UUID.randomUUID();
        String accountBody = objectMapper.writeValueAsString(Map.of(
                "name", accountName,
                "accountGroupId", agId,
                "regionId", regionId));
        HttpResponse<String> accountResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(accountBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int accountId = objectMapper.readTree(accountResponse.body()).get("id").asInt();
        createdAccountIds.add(accountId);

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + agId + "/accounts"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var accounts = objectMapper.readTree(response.body());
        assertThat(accounts.isArray()).isTrue();
        boolean found = false;
        for (var account : accounts) {
            if (account.get("id").asInt() == accountId) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();

        // Cleanup
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + accountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(accountId));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + regionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }
}
