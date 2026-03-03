package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.repo.SoWRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EntityCRUDIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpClient client = HttpClient.newHttpClient();

    private final List<Integer> regionIds = new ArrayList<>();
    private final List<Integer> accountGroupIds = new ArrayList<>();
    private final List<Integer> accountIds = new ArrayList<>();
    private final List<Integer> sowIds = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        for (int id : sowIds) {
            jdbcTemplate.update("DELETE FROM sow_text_index WHERE id = ?", id);
            jdbcTemplate.update("DELETE FROM sow WHERE id = ?", id);
        }
        for (int id : accountIds) {
            jdbcTemplate.update("DELETE FROM account WHERE id = ?", id);
        }
        for (int id : accountGroupIds) {
            jdbcTemplate.update("DELETE FROM account_group WHERE id = ?", id);
        }
        for (int id : regionIds) {
            jdbcTemplate.update("DELETE FROM region WHERE id = ?", id);
        }
    }

    @Test
    public void testRegionCRUD() throws Exception {
        // Create
        RegionRepository.RegionDTO region = new RegionRepository.RegionDTO(null, "Test Region");
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(region)))
                .build();
        HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("[DEBUG_LOG] Region create response: " + createResp.body());
        assertThat(createResp.statusCode()).isEqualTo(200);
        RegionRepository.RegionDTO created = objectMapper.readValue(createResp.body(), RegionRepository.RegionDTO.class);
        assertThat(created.name()).isEqualTo("Test Region");
        regionIds.add(created.id());

        // Get All
        HttpRequest getAllReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .GET().build();
        HttpResponse<String> getAllResp = client.send(getAllReq, HttpResponse.BodyHandlers.ofString());
        assertThat(getAllResp.statusCode()).isEqualTo(200);
        assertThat(getAllResp.body()).contains("Test Region");

        // Get By Id
        HttpRequest getByIdReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + created.id()))
                .GET().build();
        HttpResponse<String> getByIdResp = client.send(getByIdReq, HttpResponse.BodyHandlers.ofString());
        assertThat(getByIdResp.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(getByIdResp.body(), RegionRepository.RegionDTO.class).name()).isEqualTo("Test Region");

        // Update
        RegionRepository.RegionDTO updateRegion = new RegionRepository.RegionDTO(created.id(), "Updated Region");
        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateRegion)))
                .build();
        HttpResponse<String> updateResp = client.send(updateReq, HttpResponse.BodyHandlers.ofString());
        assertThat(updateResp.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(updateResp.body(), RegionRepository.RegionDTO.class).name()).isEqualTo("Updated Region");

        // Negative: Unique Constraint
        RegionRepository.RegionDTO anotherRegion = new RegionRepository.RegionDTO(null, "Another Region");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(anotherRegion)))
                .build(), HttpResponse.BodyHandlers.ofString());
        // Wait, I should add it to cleanup list
        int anotherId = jdbcTemplate.queryForObject("SELECT id FROM region WHERE name = 'Another Region'", Integer.class);
        regionIds.add(anotherId);

        RegionRepository.RegionDTO duplicateRegion = new RegionRepository.RegionDTO(null, "Another Region");
        HttpRequest dupReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(duplicateRegion)))
                .build();
        HttpResponse<String> dupResp = client.send(dupReq, HttpResponse.BodyHandlers.ofString());
        assertThat(dupResp.statusCode()).isEqualTo(409);

        // Delete (will be tested in cleanup or later if needed)
    }

    @Test
    public void testDeleteConstraint() throws Exception {
        // Create Region
        RegionRepository.RegionDTO region = new RegionRepository.RegionDTO(null, "Protected Region");
        HttpResponse<String> res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(region)))
                .build(), HttpResponse.BodyHandlers.ofString());
        RegionRepository.RegionDTO createdRegion = objectMapper.readValue(res.body(), RegionRepository.RegionDTO.class);
        regionIds.add(createdRegion.id());

        // Create Account Group
        AccountGroupRepository.AccountGroupDTO group = new AccountGroupRepository.AccountGroupDTO(null, "Test Group");
        res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(group)))
                .build(), HttpResponse.BodyHandlers.ofString());
        AccountGroupRepository.AccountGroupDTO createdGroup = objectMapper.readValue(res.body(), AccountGroupRepository.AccountGroupDTO.class);
        accountGroupIds.add(createdGroup.id());

        // Create Account
        AccountRepository.AccountDTO account = new AccountRepository.AccountDTO(null, "Dependent Account", createdGroup.id(), createdRegion.id());
        res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(account)))
                .build(), HttpResponse.BodyHandlers.ofString());
        AccountRepository.AccountDTO createdAccount = objectMapper.readValue(res.body(), AccountRepository.AccountDTO.class);
        accountIds.add(createdAccount.id());

        // Try Delete Region
        HttpRequest delRegionReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                .DELETE().build();
        HttpResponse<String> delRegionResp = client.send(delRegionReq, HttpResponse.BodyHandlers.ofString());
        assertThat(delRegionResp.statusCode()).isEqualTo(409);

        // Try Delete Group
        HttpRequest delGroupReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id()))
                .DELETE().build();
        HttpResponse<String> delGroupResp = client.send(delGroupReq, HttpResponse.BodyHandlers.ofString());
        assertThat(delGroupResp.statusCode()).isEqualTo(409);
    }

    @Test
    public void testNegativeScenarios() throws Exception {
        // Account with non-existent Region
        AccountRepository.AccountDTO invalidAccountRegion = new AccountRepository.AccountDTO(null, "Invalid Account", 1, 999999);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidAccountRegion)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(404);

        // Account with non-existent Group
        AccountRepository.AccountDTO invalidAccountGroup = new AccountRepository.AccountDTO(null, "Invalid Account", 999999, 1);
        req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidAccountGroup)))
                .build();
        resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(404);

        // SoW with non-existent Account
        SoWRepository.SoWDTO invalidSoW = new SoWRepository.SoWDTO(null, 999999, LocalDate.now(), "Title", new BigDecimal("100"), "Desc", "Text");
        req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidSoW)))
                .build();
        resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(404);
    }

    @Test
    public void testSoWSearch() throws Exception {
        // Setup Account
        RegionRepository.RegionDTO region = new RegionRepository.RegionDTO(null, "Search Region");
        HttpResponse<String> res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(region)))
                .build(), HttpResponse.BodyHandlers.ofString());
        RegionRepository.RegionDTO createdRegion = objectMapper.readValue(res.body(), RegionRepository.RegionDTO.class);
        regionIds.add(createdRegion.id());

        AccountGroupRepository.AccountGroupDTO group = new AccountGroupRepository.AccountGroupDTO(null, "Search Group");
        res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(group)))
                .build(), HttpResponse.BodyHandlers.ofString());
        AccountGroupRepository.AccountGroupDTO createdGroup = objectMapper.readValue(res.body(), AccountGroupRepository.AccountGroupDTO.class);
        accountGroupIds.add(createdGroup.id());

        AccountRepository.AccountDTO account = new AccountRepository.AccountDTO(null, "Search Account", createdGroup.id(), createdRegion.id());
        res = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(account)))
                .build(), HttpResponse.BodyHandlers.ofString());
        AccountRepository.AccountDTO createdAccount = objectMapper.readValue(res.body(), AccountRepository.AccountDTO.class);
        accountIds.add(createdAccount.id());

        // Create SoWs
        for (int i = 1; i <= 5; i++) {
            String text = Files.readString(Path.of("src/test/resources/sow" + i + ".txt"));
            SoWRepository.SoWDTO sow = new SoWRepository.SoWDTO(null, createdAccount.id(), LocalDate.now(), "SoW " + i, new BigDecimal("1000.00"), "Description " + i, text);
            res = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sow)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            sowIds.add(objectMapper.readValue(res.body(), SoWRepository.SoWDTO.class).id());
        }

        // Test Search
        String query = "microservices";
        HttpRequest searchReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=" + query + "&maxDoc=10&minRank=0.0&maxTextLength=50"))
                .GET().build();
        HttpResponse<String> searchResp = client.send(searchReq, HttpResponse.BodyHandlers.ofString());
        assertThat(searchResp.statusCode()).isEqualTo(200);
        List<SoWRepository.SoWDTO> results = objectMapper.readValue(searchResp.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWRepository.SoWDTO.class));
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).text()).contains("microservices");
        assertThat(results.get(0).text().length()).isLessThanOrEqualTo(54); // 50 + "... "

        // Test Search Arabic/Hebrew (even if limited by 'english' tsvector, they might match if indexed differently, but here we test if it doesn't crash)
        query = "Arabic";
        searchReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=" + query + "&maxDoc=10&minRank=0.0&maxTextLength=50"))
                .GET().build();
        searchResp = client.send(searchReq, HttpResponse.BodyHandlers.ofString());
        assertThat(searchResp.statusCode()).isEqualTo(200);
        
        // Test accounts for region/group
        HttpRequest regAccReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id() + "/accounts"))
                .GET().build();
        assertThat(client.send(regAccReq, HttpResponse.BodyHandlers.ofString()).body()).contains("Search Account");

        HttpRequest grpAccReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group/" + createdGroup.id() + "/accounts"))
                .GET().build();
        assertThat(client.send(grpAccReq, HttpResponse.BodyHandlers.ofString()).body()).contains("Search Account");
    }
}
