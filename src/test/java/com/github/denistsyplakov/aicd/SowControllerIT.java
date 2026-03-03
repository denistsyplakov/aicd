package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.liquibase.enabled=false")
public class SowControllerIT {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<Integer> createdSowIds = new ArrayList<>();
    private final List<Integer> createdAccountIds = new ArrayList<>();
    private int sharedRegionId;
    private int sharedAgId;
    private int sharedAccountId;

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

        String accountBody = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", sharedAgId,
                "regionId", sharedRegionId));
        HttpResponse<String> accountResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(accountBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        sharedAccountId = objectMapper.readTree(accountResponse.body()).get("id").asInt();
        createdAccountIds.add(sharedAccountId);
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

    private Map<String, Object> baseSow(String title, String text) {
        Map<String, Object> sow = new HashMap<>();
        sow.put("accountId", sharedAccountId);
        sow.put("date", "2024-01-01");
        sow.put("title", title);
        sow.put("amount", 5000);
        sow.put("description", "Test description");
        sow.put("text", text);
        return sow;
    }

    private int createSowAndTrack(String title, String text) throws Exception {
        String body = objectMapper.writeValueAsString(baseSow(title, text));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        int id = objectMapper.readTree(response.body()).get("id").asInt();
        createdSowIds.add(id);
        return id;
    }

    @Test
    public void testCreateSow() throws Exception {
        String title = "SoW-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(baseSow(title, "some text content"));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        var node = objectMapper.readTree(response.body());
        assertThat(node.get("id").asInt()).isPositive();
        assertThat(node.get("title").asText()).isEqualTo(title);
        createdSowIds.add(node.get("id").asInt());
    }

    @Test
    public void testGetSowById() throws Exception {
        String title = "SoW-" + UUID.randomUUID();
        int id = createSowAndTrack(title, "some text content");

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("title").asText()).isEqualTo(title);
    }

    @Test
    public void testGetSowByIdNotFound() throws Exception {
        int id = createSowAndTrack("SoW-" + UUID.randomUUID(), "text");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdSowIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testUpdateSow() throws Exception {
        int id = createSowAndTrack("SoW-" + UUID.randomUUID(), "original text");
        String newTitle = "SoW-" + UUID.randomUUID();
        Map<String, Object> updatedSow = baseSow(newTitle, "updated text content");
        String body = objectMapper.writeValueAsString(updatedSow);

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).get("title").asText()).isEqualTo(newTitle);
    }

    @Test
    public void testDeleteSow() throws Exception {
        int id = createSowAndTrack("SoW-" + UUID.randomUUID(), "text");

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
        createdSowIds.remove(Integer.valueOf(id));
    }

    @Test
    public void testDeleteSowNotFound() throws Exception {
        int id = createSowAndTrack("SoW-" + UUID.randomUUID(), "text");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        createdSowIds.remove(Integer.valueOf(id));

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void testCreateSowInvalidAccountId() throws Exception {
        // Create and delete an account to get a non-existent id
        String accountBody = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", sharedAgId,
                "regionId", sharedRegionId));
        HttpResponse<String> accountResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(accountBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int nonExistentAccountId = objectMapper.readTree(accountResponse.body()).get("id").asInt();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + nonExistentAccountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());

        Map<String, Object> sow = new HashMap<>(baseSow("SoW-" + UUID.randomUUID(), "text"));
        sow.put("accountId", nonExistentAccountId);
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sow)))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void testUpdateSowInvalidAccountId() throws Exception {
        int id = createSowAndTrack("SoW-" + UUID.randomUUID(), "text");

        String accountBody = objectMapper.writeValueAsString(Map.of(
                "name", "Account-" + UUID.randomUUID(),
                "accountGroupId", sharedAgId,
                "regionId", sharedRegionId));
        HttpResponse<String> accountResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .POST(HttpRequest.BodyPublishers.ofString(accountBody))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        int nonExistentAccountId = objectMapper.readTree(accountResponse.body()).get("id").asInt();
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account/" + nonExistentAccountId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());

        Map<String, Object> sow = new HashMap<>(baseSow("SoW-" + UUID.randomUUID(), "text"));
        sow.put("accountId", nonExistentAccountId);
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sow)))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    private List<Integer> loadTestDocumentsAndCreate() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("sow-test-documents.json");
        List<Map<String, Object>> docs = objectMapper.readValue(is, new TypeReference<>() {});
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            Map<String, Object> sow = new HashMap<>();
            sow.put("accountId", sharedAccountId);
            sow.put("date", "2024-01-01");
            sow.put("title", doc.get("title"));
            sow.put("amount", 1000);
            sow.put("description", doc.get("description"));
            sow.put("text", doc.get("text"));
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow"))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sow)))
                    .header("Content-Type", "application/json")
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(201);
            int id = objectMapper.readTree(response.body()).get("id").asInt();
            ids.add(id);
            createdSowIds.add(id);
        }
        return ids;
    }

    private HttpResponse<String> search(String query, int maxDoc, double minRank, int maxTextLength) throws Exception {
        Map<String, Object> req = Map.of(
                "query", query,
                "maxDoc", maxDoc,
                "minRank", minRank,
                "maxTextLength", maxTextLength);
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/search"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testSearchEnglish() throws Exception {
        loadTestDocumentsAndCreate();

        HttpResponse<String> response = search("cloud infrastructure", 10, 0.0, 1000);
        assertThat(response.statusCode()).isEqualTo(200);
        var results = objectMapper.readTree(response.body());
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThan(0);
        // The Cloud Infrastructure doc should be first (highest rank)
        assertThat(results.get(0).get("title").asText()).contains("Cloud");
    }

    @Test
    public void testSearchHebrew() throws Exception {
        loadTestDocumentsAndCreate();

        // Search for a Hebrew word that appears exactly (standalone) in the Hebrew document
        HttpResponse<String> response = search("חיפוש", 10, 0.0, 1000);
        assertThat(response.statusCode()).isEqualTo(200);
        var results = objectMapper.readTree(response.body());
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThan(0);
        assertThat(results.get(0).get("title").asText()).contains("Hebrew");
    }

    @Test
    public void testSearchArabic() throws Exception {
        loadTestDocumentsAndCreate();

        // Search for an Arabic word that appears in the Arabic document
        HttpResponse<String> response = search("العربية", 10, 0.0, 1000);
        assertThat(response.statusCode()).isEqualTo(200);
        var results = objectMapper.readTree(response.body());
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThan(0);
        assertThat(results.get(0).get("title").asText()).contains("Arabic");
    }

    @Test
    public void testSearchMaxDoc() throws Exception {
        loadTestDocumentsAndCreate();

        // Search for a broad term that matches many documents, limit to 2
        HttpResponse<String> response = search("the", 2, 0.0, 1000);
        assertThat(response.statusCode()).isEqualTo(200);
        var results = objectMapper.readTree(response.body());
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isLessThanOrEqualTo(2);
    }

    @Test
    public void testSearchMaxTextLength() throws Exception {
        String longText = "cloud infrastructure migration platform " + "a".repeat(500);
        createSowAndTrack("SoW-" + UUID.randomUUID(), longText);

        int maxLength = 50;
        HttpResponse<String> response = search("cloud infrastructure", 10, 0.0, maxLength);
        assertThat(response.statusCode()).isEqualTo(200);
        var results = objectMapper.readTree(response.body());
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isGreaterThan(0);
        String text = results.get(0).get("text").asText();
        assertThat(text.length()).isLessThanOrEqualTo(maxLength + "... ".length());
        assertThat(text).endsWith("... ");
    }

    @Test
    public void testSearchMinRankFiltering() throws Exception {
        loadTestDocumentsAndCreate();

        // With a very high minRank, we should get fewer or no results
        HttpResponse<String> responseHighRank = search("cloud", 10, 0.9, 1000);
        assertThat(responseHighRank.statusCode()).isEqualTo(200);
        var highRankResults = objectMapper.readTree(responseHighRank.body());

        // With minRank=0, we should get more results
        HttpResponse<String> responseNoFilter = search("cloud", 10, 0.0, 1000);
        assertThat(responseNoFilter.statusCode()).isEqualTo(200);
        var noFilterResults = objectMapper.readTree(responseNoFilter.body());

        // High rank filter should return <= results compared to no filter
        assertThat(highRankResults.size()).isLessThanOrEqualTo(noFilterResults.size());
    }
}
