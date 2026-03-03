package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class AccountControllerIT {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Integer> createdSowIds = new ArrayList<>();
    private final List<Integer> createdAccountIds = new ArrayList<>();
    private int sharedRegionId;
    private int sharedAgId;

    @BeforeEach
    void setUp() throws Exception {
        String regionBody = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> regionResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(regionBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        sharedRegionId = objectMapper.readTree(regionResponse.body()).get("id").asInt();

        String agBody = objectMapper.writeValueAsString(Map.of("name", "AG-" + UUID.randomUUID()));
        HttpResponse<String> agResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .POST(HttpRequest.BodyPublishers.ofString(agBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        sharedAgId = objectMapper.readTree(agResponse.body()).get("id").asInt();
    }

    @AfterEach
    void cleanup() throws Exception {
        for (Integer id : createdSowIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdSowIds.clear();
        for (Integer id : createdAccountIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdAccountIds.clear();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + sharedAgId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + sharedRegionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> createAccount(String name, int accountGroupId, int regionId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "accountGroupId", accountGroupId,
                "regionId", regionId));
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private int createAccountAndTrack(String name) throws Exception {
        HttpResponse<String> response = createAccount(name, sharedAgId, sharedRegionId);
        assertThat(response.statusCode()).isEqualTo(201);
        int id = objectMapper.readTree(response.body()).get("id").asInt();
        createdAccountIds.add(id);
        return id;
    }

    @Test
    public void testGetAllAccounts() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).isArray()).isTrue();
    }

    @Test
    public void testCreateAccount() throws Exception {
        String name = "Account-" + UUID.randomUUID();
        HttpResponse<String> response = createAccount(name, sharedAgId, sharedRegionId);
        assertThat(response.statusCode()).isEqualTo(201);
        var node = objectMapper.readTree(response.body());
        assertThat(node.get("id").asInt()).isPositive();
        assertThat(node.get("name").asText()).isEqualTo(name);
        createdAccountIds.add(node.get("id").asInt());
    }

    @Test
    public void testGetAccountById() throws Exception {
        String name = "Account-" + UUID.randomUUID();
        int id = createAccountAndTrack(name);

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(name);
    }

    @Test
    public void testGetAccountByIdNotFound() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testUpdateAccount() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());
        String newName = "Account-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", newName,
                "accountGroupId", sharedAgId,
                "regionId", sharedRegionId));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(newName);
    }

    @Test
    public void testUpdateAccountNotFound() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(id));

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", sharedAgId,
                "regionId", sharedRegionId));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testDeleteAccount() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
        createdAccountIds.remove(Integer.valueOf(id));
    }

    @Test
    public void testDeleteAccountNotFound() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testCreateAccountInvalidRegionId() throws Exception {
        // Create and delete a region to get a non-existent id
        String regionBody = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> regionResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(regionBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int nonExistentRegionId = objectMapper.readTree(regionResponse.body()).get("id").asInt();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentRegionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = createAccount("Account-" + UUID.randomUUID(), sharedAgId, nonExistentRegionId);
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testCreateAccountInvalidAccountGroupId() throws Exception {
        String agBody = objectMapper.writeValueAsString(Map.of("name", "AG-" + UUID.randomUUID()));
        HttpResponse<String> agResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .POST(HttpRequest.BodyPublishers.ofString(agBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int nonExistentAgId = objectMapper.readTree(agResponse.body()).get("id").asInt();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + nonExistentAgId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = createAccount("Account-" + UUID.randomUUID(), nonExistentAgId, sharedRegionId);
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testUpdateAccountInvalidFk() throws Exception {
        int id = createAccountAndTrack("Account-" + UUID.randomUUID());

        String regionBody = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> regionResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(regionBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int nonExistentRegionId = objectMapper.readTree(regionResponse.body()).get("id").asInt();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentRegionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", sharedAgId,
                "regionId", nonExistentRegionId));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testDeleteAccountInUse() throws Exception {
        int accountId = createAccountAndTrack("Account-" + UUID.randomUUID());

        // Create a SoW referencing the account
        String sowBody = objectMapper.writeValueAsString(Map.of(
                "accountId", accountId,
                "date", "2024-01-01",
                "title", "SoW-" + UUID.randomUUID(),
                "amount", 1000,
                "description", "test",
                "text", "test text"));
        HttpResponse<String> sowResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .POST(HttpRequest.BodyPublishers.ofString(sowBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int sowId = objectMapper.readTree(sowResponse.body()).get("id").asInt();
        createdSowIds.add(sowId);

        HttpResponse<String> deleteResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + accountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(deleteResponse.statusCode()).isEqualTo(409);
    }
}
