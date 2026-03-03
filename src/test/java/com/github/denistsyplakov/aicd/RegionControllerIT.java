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
public class RegionControllerIT {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Integer> createdRegionIds = new ArrayList<>();
    private final List<Integer> createdAccountIds = new ArrayList<>();

    @AfterEach
    void cleanup() throws Exception {
        for (Integer id : createdAccountIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/account/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdAccountIds.clear();
        for (Integer id : createdRegionIds.reversed()) {
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                    .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        }
        createdRegionIds.clear();
    }

    private HttpResponse<String> createRegion(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name));
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private int createRegionAndTrack(String name) throws Exception {
        HttpResponse<String> response = createRegion(name);
        assertThat(response.statusCode()).isEqualTo(201);
        int id = objectMapper.readTree(response.body()).get("id").asInt();
        createdRegionIds.add(id);
        return id;
    }

    @Test
    public void testGetAllRegions() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).isArray()).isTrue();
    }

    @Test
    public void testCreateRegion() throws Exception {
        String name = "Region-" + UUID.randomUUID();
        HttpResponse<String> response = createRegion(name);
        assertThat(response.statusCode()).isEqualTo(201);
        var node = objectMapper.readTree(response.body());
        assertThat(node.get("id").asInt()).isPositive();
        assertThat(node.get("name").asText()).isEqualTo(name);
        createdRegionIds.add(node.get("id").asInt());
    }

    @Test
    public void testGetRegionById() throws Exception {
        String name = "Region-" + UUID.randomUUID();
        int id = createRegionAndTrack(name);

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(name);
    }

    @Test
    public void testGetRegionByIdNotFound() throws Exception {
        String name = "Region-" + UUID.randomUUID();
        int id = createRegionAndTrack(name);
        // Delete it so we have a real non-existent id
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdRegionIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testUpdateRegion() throws Exception {
        int id = createRegionAndTrack("Region-" + UUID.randomUUID());
        String newName = "Region-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", newName));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("name").asText()).isEqualTo(newName);
    }

    @Test
    public void testUpdateRegionNotFound() throws Exception {
        int id = createRegionAndTrack("Region-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdRegionIds.remove(Integer.valueOf(id));

        String body = objectMapper.writeValueAsString(Map.of("name", "Region-" + UUID.randomUUID()));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testDeleteRegion() throws Exception {
        int id = createRegionAndTrack("Region-" + UUID.randomUUID());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
        createdRegionIds.remove(Integer.valueOf(id));
    }

    @Test
    public void testDeleteRegionNotFound() throws Exception {
        int id = createRegionAndTrack("Region-" + UUID.randomUUID());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdRegionIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testCreateDuplicateRegionName() throws Exception {
        String name = "Region-" + UUID.randomUUID();
        createRegionAndTrack(name);

        HttpResponse<String> response = createRegion(name);
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testUpdateRegionToDuplicateName() throws Exception {
        String nameA = "Region-" + UUID.randomUUID();
        String nameB = "Region-" + UUID.randomUUID();
        int idA = createRegionAndTrack(nameA);
        createRegionAndTrack(nameB);

        String body = objectMapper.writeValueAsString(Map.of("name", nameB));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + idA))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testDeleteRegionInUse() throws Exception {
        int regionId = createRegionAndTrack("Region-" + UUID.randomUUID());

        // Create an account group first (required for account)
        String agBody = objectMapper.writeValueAsString(Map.of("name", "AG-" + UUID.randomUUID()));
        HttpResponse<String> agResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .POST(HttpRequest.BodyPublishers.ofString(agBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int agId = objectMapper.readTree(agResponse.body()).get("id").asInt();

        // Create account referencing region
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
                .uri(URI.create("http://localhost:" + port + "/api/region/" + regionId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(deleteResponse.statusCode()).isEqualTo(409);

        // Cleanup: delete account first, then account-group
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + accountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdAccountIds.remove(Integer.valueOf(accountId));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + agId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testGetRegionAccounts() throws Exception {
        int regionId = createRegionAndTrack("Region-" + UUID.randomUUID());

        String agBody = objectMapper.writeValueAsString(Map.of("name", "AG-" + UUID.randomUUID()));
        HttpResponse<String> agResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .POST(HttpRequest.BodyPublishers.ofString(agBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int agId = objectMapper.readTree(agResponse.body()).get("id").asInt();

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
                .uri(URI.create("http://localhost:" + port + "/api/region/" + regionId + "/accounts"))
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
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + agId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }
}
