package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EntityCRUDIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Set<Integer> regionIds = new LinkedHashSet<>();
    private final Set<Integer> accountGroupIds = new LinkedHashSet<>();
    private final Set<Integer> accountIds = new LinkedHashSet<>();
    private final Set<Integer> sowIds = new LinkedHashSet<>();

    @AfterEach
    void cleanup() {
        for (Integer sowId : sowIds) {
            jdbcTemplate.update("delete from sow_text_index where id = ?", sowId);
            jdbcTemplate.update("delete from sow where id = ?", sowId);
        }
        for (Integer accountId : accountIds) {
            jdbcTemplate.update("delete from account where id = ?", accountId);
        }
        for (Integer accountGroupId : accountGroupIds) {
            jdbcTemplate.update("delete from account_group where id = ?", accountGroupId);
        }
        for (Integer regionId : regionIds) {
            jdbcTemplate.update("delete from region where id = ?", regionId);
        }

        sowIds.clear();
        accountIds.clear();
        accountGroupIds.clear();
        regionIds.clear();
    }

    @Test
    void regionCrudUniqueAndUsageScenarios() throws Exception {
        JsonNode region = createRegion("North");
        int regionId = region.path("id").asInt();

        HttpResponse<String> duplicate = send("POST", "/api/regions", "{\"name\":\"North\"}");
        assertThat(duplicate.statusCode()).isEqualTo(409);

        HttpResponse<String> all = send("GET", "/api/regions", null);
        assertThat(all.statusCode()).isEqualTo(200);
        assertThat(all.body()).contains("\"id\":" + regionId);

        HttpResponse<String> getById = send("GET", "/api/regions/" + regionId, null);
        assertThat(getById.statusCode()).isEqualTo(200);

        HttpResponse<String> updated = send("PUT", "/api/regions/" + regionId, "{\"name\":\"North Updated\"}");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("North Updated");

        int missingId = createAndDeleteRegionForMissingId();
        assertThat(send("GET", "/api/regions/" + missingId, null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", "/api/regions/" + missingId, "{\"name\":\"x\"}").statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/api/regions/" + missingId, null).statusCode()).isEqualTo(404);

        JsonNode accountGroup = createAccountGroup("Team A");
        JsonNode account = createAccount("Acme", accountGroup.path("id").asInt(), regionId);

        HttpResponse<String> deleteInUse = send("DELETE", "/api/regions/" + regionId, null);
        assertThat(deleteInUse.statusCode()).isEqualTo(409);

        HttpResponse<String> accountsByRegion = send("GET", "/api/regions/" + regionId + "/accounts", null);
        assertThat(accountsByRegion.statusCode()).isEqualTo(200);
        assertThat(accountsByRegion.body()).contains("\"id\":" + account.path("id").asInt());
    }

    @Test
    void accountGroupCrudUsageAndMissingScenarios() throws Exception {
        JsonNode group = createAccountGroup("Group One");
        int groupId = group.path("id").asInt();

        HttpResponse<String> getById = send("GET", "/api/account-groups/" + groupId, null);
        assertThat(getById.statusCode()).isEqualTo(200);

        HttpResponse<String> updated = send("PUT", "/api/account-groups/" + groupId, "{\"name\":\"Group One Updated\"}");
        assertThat(updated.statusCode()).isEqualTo(200);

        int missingId = createAndDeleteGroupForMissingId();
        assertThat(send("GET", "/api/account-groups/" + missingId, null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", "/api/account-groups/" + missingId, "{\"name\":\"x\"}").statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/api/account-groups/" + missingId, null).statusCode()).isEqualTo(404);

        JsonNode region = createRegion("South");
        JsonNode account = createAccount("Acc", groupId, region.path("id").asInt());

        HttpResponse<String> deleteInUse = send("DELETE", "/api/account-groups/" + groupId, null);
        assertThat(deleteInUse.statusCode()).isEqualTo(409);

        HttpResponse<String> accountsByGroup = send("GET", "/api/account-groups/" + groupId + "/accounts", null);
        assertThat(accountsByGroup.statusCode()).isEqualTo(200);
        assertThat(accountsByGroup.body()).contains("\"id\":" + account.path("id").asInt());
    }

    @Test
    void accountCrudAndInvalidReferenceScenarios() throws Exception {
        JsonNode region = createRegion("West");
        JsonNode group = createAccountGroup("Group West");

        JsonNode account = createAccount("Account W", group.path("id").asInt(), region.path("id").asInt());
        int accountId = account.path("id").asInt();

        HttpResponse<String> getById = send("GET", "/api/accounts/" + accountId, null);
        assertThat(getById.statusCode()).isEqualTo(200);

        HttpResponse<String> update = send("PUT", "/api/accounts/" + accountId,
                json(Map.of("name", "Account W2", "accountGroupId", group.path("id").asInt(), "regionId", region.path("id").asInt())));
        assertThat(update.statusCode()).isEqualTo(200);

        int missingRegionId = createAndDeleteRegionForMissingId();
        int missingGroupId = createAndDeleteGroupForMissingId();

        HttpResponse<String> invalidRegionCreate = send("POST", "/api/accounts",
                json(Map.of("name", "BadRegion", "accountGroupId", group.path("id").asInt(), "regionId", missingRegionId)));
        assertThat(invalidRegionCreate.statusCode()).isEqualTo(409);

        HttpResponse<String> invalidGroupCreate = send("POST", "/api/accounts",
                json(Map.of("name", "BadGroup", "accountGroupId", missingGroupId, "regionId", region.path("id").asInt())));
        assertThat(invalidGroupCreate.statusCode()).isEqualTo(409);

        int missingAccountId = createAndDeleteAccountForMissingId(group.path("id").asInt(), region.path("id").asInt());
        assertThat(send("GET", "/api/accounts/" + missingAccountId, null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", "/api/accounts/" + missingAccountId,
                json(Map.of("name", "X", "accountGroupId", group.path("id").asInt(), "regionId", region.path("id").asInt()))).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/api/accounts/" + missingAccountId, null).statusCode()).isEqualTo(404);

        assertThat(send("DELETE", "/api/accounts/" + accountId, null).statusCode()).isEqualTo(204);
    }

    @Test
    void sowCrudSearchAndNegativeScenarios() throws Exception {
        JsonNode region = createRegion("East");
        JsonNode group = createAccountGroup("Group East");
        JsonNode account = createAccount("Account East", group.path("id").asInt(), region.path("id").asInt());

        JsonNode sow = createSoW(account.path("id").asInt(), "Title One", "hello ???? ????? text");
        int sowId = sow.path("id").asInt();

        HttpResponse<String> searchFound = send("GET",
                "/api/sows/search?q=" + encode("hello") + "&maxDoc=10&minRank=0&maxTextLength=200", null);
        assertThat(searchFound.statusCode()).isEqualTo(200);
        assertThat(searchFound.body()).contains("\"id\":" + sowId);

        HttpResponse<String> update = send("PUT", "/api/sows/" + sowId,
                json(Map.of(
                        "accountId", account.path("id").asInt(),
                        "date", LocalDate.now().toString(),
                        "title", "Title Updated",
                        "amount", 220.55,
                        "description", "Updated",
                        "text", "only updated terms"
                )));
        assertThat(update.statusCode()).isEqualTo(200);

        HttpResponse<String> oldTermSearch = send("GET",
                "/api/sows/search?q=" + encode("hello") + "&maxDoc=10&minRank=0&maxTextLength=200", null);
        assertThat(oldTermSearch.statusCode()).isEqualTo(200);
        assertThat(oldTermSearch.body()).doesNotContain("\"id\":" + sowId);

        int missingAccountId = createAndDeleteAccountForMissingId(group.path("id").asInt(), region.path("id").asInt());
        HttpResponse<String> invalidAccountCreate = send("POST", "/api/sows",
                json(Map.of(
                        "accountId", missingAccountId,
                        "date", LocalDate.now().toString(),
                        "title", "Bad",
                        "amount", 1.0,
                        "description", "Bad",
                        "text", "Bad"
                )));
        assertThat(invalidAccountCreate.statusCode()).isEqualTo(409);

        int missingSowId = createAndDeleteSoWForMissingId(account.path("id").asInt());
        assertThat(send("GET", "/api/sows/" + missingSowId, null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", "/api/sows/" + missingSowId,
                json(Map.of(
                        "accountId", account.path("id").asInt(),
                        "date", LocalDate.now().toString(),
                        "title", "X",
                        "amount", 3.0,
                        "description", "X",
                        "text", "X"
                ))).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/api/sows/" + missingSowId, null).statusCode()).isEqualTo(404);

        assertThat(send("GET", "/api/sows/search?q=" + encode("x") + "&maxDoc=0&minRank=0&maxTextLength=10", null).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/api/sows/search?q=" + encode("x") + "&maxDoc=1&minRank=-1&maxTextLength=10", null).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/api/sows/search?q=" + encode("x") + "&maxDoc=1&minRank=0&maxTextLength=-1", null).statusCode()).isEqualTo(400);

        assertThat(send("DELETE", "/api/sows/" + sowId, null).statusCode()).isEqualTo(204);

        HttpResponse<String> afterDeleteSearch = send("GET",
                "/api/sows/search?q=" + encode("updated") + "&maxDoc=10&minRank=0&maxTextLength=200", null);
        assertThat(afterDeleteSearch.statusCode()).isEqualTo(200);
        assertThat(afterDeleteSearch.body()).doesNotContain("\"id\":" + sowId);
    }

    @Test
    void sowSearchSupportsSpecialCharactersLanguagesCroppingAndLimit() throws Exception {
        JsonNode region = createRegion("Search Region");
        JsonNode group = createAccountGroup("Search Group");
        JsonNode account = createAccount("Search Account", group.path("id").asInt(), region.path("id").asInt());

        for (String line : loadSoWDocs()) {
            createSoW(account.path("id").asInt(), "Doc", line);
        }

        HttpResponse<String> specialSearch = send("GET",
                "/api/sows/search?q=" + encode("C++ O'Reilly") + "&maxDoc=10&minRank=0&maxTextLength=200", null);
        assertThat(specialSearch.statusCode()).isEqualTo(200);

        HttpResponse<String> hebrewArabicSearch = send("GET",
                "/api/sows/search?q=" + encode("???? ?????") + "&maxDoc=10&minRank=0&maxTextLength=200", null);
        assertThat(hebrewArabicSearch.statusCode()).isEqualTo(200);

        HttpResponse<String> limitedSearch = send("GET",
                "/api/sows/search?q=" + encode("globaltoken") + "&maxDoc=3&minRank=0&maxTextLength=30", null);
        assertThat(limitedSearch.statusCode()).isEqualTo(200);
        JsonNode array = MAPPER.readTree(limitedSearch.body());
        assertThat(array.size()).isLessThanOrEqualTo(3);
        assertThat(array.get(0).path("text").asText()).endsWith("... ");

        HttpResponse<String> strictRankSearch = send("GET",
                "/api/sows/search?q=" + encode("globaltoken") + "&maxDoc=10&minRank=100&maxTextLength=200", null);
        assertThat(strictRankSearch.statusCode()).isEqualTo(200);
        assertThat(MAPPER.readTree(strictRankSearch.body()).size()).isEqualTo(0);
    }

    private JsonNode createRegion(String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/regions", json(Map.of("name", name)));
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode node = MAPPER.readTree(response.body());
        regionIds.add(node.path("id").asInt());
        return node;
    }

    private JsonNode createAccountGroup(String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/account-groups", json(Map.of("name", name)));
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode node = MAPPER.readTree(response.body());
        accountGroupIds.add(node.path("id").asInt());
        return node;
    }

    private JsonNode createAccount(String name, int accountGroupId, int regionId) throws Exception {
        HttpResponse<String> response = send("POST", "/api/accounts",
                json(Map.of("name", name, "accountGroupId", accountGroupId, "regionId", regionId)));
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode node = MAPPER.readTree(response.body());
        accountIds.add(node.path("id").asInt());
        return node;
    }

    private JsonNode createSoW(int accountId, String title, String text) throws Exception {
        HttpResponse<String> response = send("POST", "/api/sows",
                json(Map.of(
                        "accountId", accountId,
                        "date", LocalDate.now().toString(),
                        "title", title,
                        "amount", 100.12,
                        "description", "Description",
                        "text", text
                )));
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode node = MAPPER.readTree(response.body());
        sowIds.add(node.path("id").asInt());
        return node;
    }

    private int createAndDeleteRegionForMissingId() throws Exception {
        JsonNode region = createRegion("tmp-region-" + System.nanoTime());
        int id = region.path("id").asInt();
        assertThat(send("DELETE", "/api/regions/" + id, null).statusCode()).isEqualTo(204);
        return id;
    }

    private int createAndDeleteGroupForMissingId() throws Exception {
        JsonNode group = createAccountGroup("tmp-group-" + System.nanoTime());
        int id = group.path("id").asInt();
        assertThat(send("DELETE", "/api/account-groups/" + id, null).statusCode()).isEqualTo(204);
        return id;
    }

    private int createAndDeleteAccountForMissingId(int accountGroupId, int regionId) throws Exception {
        JsonNode account = createAccount("tmp-account-" + System.nanoTime(), accountGroupId, regionId);
        int id = account.path("id").asInt();
        assertThat(send("DELETE", "/api/accounts/" + id, null).statusCode()).isEqualTo(204);
        return id;
    }

    private int createAndDeleteSoWForMissingId(int accountId) throws Exception {
        JsonNode sow = createSoW(accountId, "tmp-sow", "temporary text");
        int id = sow.path("id").asInt();
        assertThat(send("DELETE", "/api/sows/" + id, null).statusCode()).isEqualTo(204);
        return id;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (body == null) {
            if ("GET".equals(method)) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String json(Map<String, Object> body) throws Exception {
        return MAPPER.writeValueAsString(body);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<String> loadSoWDocs() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:sow-docs.txt");
        List<String> lines = new ArrayList<>();
        for (String line : new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
