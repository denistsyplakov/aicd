package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import com.github.denistsyplakov.aicd.repo.SoWRepository.SoWDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SoWControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    private AccountDTO account;

    @BeforeEach
    public void setup() throws Exception {
        jdbcTemplate.execute("DELETE FROM sow_text_index");
        jdbcTemplate.execute("DELETE FROM sow");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM account_group");
        jdbcTemplate.execute("DELETE FROM region");

        try (HttpClient client = HttpClient.newHttpClient()) {
            AccountGroupDTO group = createAccountGroup(client, "Test Group");
            RegionDTO region = createRegion(client, "Test Region");
            account = createAccount(client, "Test Account", group.id(), region.id());
        }
    }

    @Test
    public void testCrud() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create
            SoWDTO newSoW = new SoWDTO(null, account.id(), new Date(), "SoW 1", new BigDecimal("1000.00"), "Description 1", "Some long text content for SoW 1");
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newSoW)))
                    .build();

            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            SoWDTO createdSoW = objectMapper.readValue(createResponse.body(), SoWDTO.class);
            assertThat(createdSoW.id()).isNotNull();
            assertThat(createdSoW.title()).isEqualTo("SoW 1");

            // 2. Get By ID
            HttpRequest getByIdRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + createdSoW.id()))
                    .GET()
                    .build();

            HttpResponse<String> getByIdResponse = client.send(getByIdRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getByIdResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            SoWDTO fetchedSoW = objectMapper.readValue(getByIdResponse.body(), SoWDTO.class);
            assertThat(fetchedSoW.id()).isEqualTo(createdSoW.id());
            assertThat(fetchedSoW.title()).isEqualTo(createdSoW.title());
            assertThat(fetchedSoW.accountId()).isEqualTo(createdSoW.accountId());
            assertThat(fetchedSoW.amount()).isEqualByComparingTo(createdSoW.amount());
            assertThat(fetchedSoW.description()).isEqualTo(createdSoW.description());
            assertThat(fetchedSoW.text()).isEqualTo(createdSoW.text());

            // 3. Update
            SoWDTO updateSoW = new SoWDTO(null, account.id(), new Date(), "Updated SoW", new BigDecimal("2000.00"), "Updated Description", "Updated text content");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + createdSoW.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateSoW)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            SoWDTO updatedSoW = objectMapper.readValue(updateResponse.body(), SoWDTO.class);
            assertThat(updatedSoW.title()).isEqualTo("Updated SoW");

            // 4. Delete
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + createdSoW.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            // Verify deleted
            HttpRequest verifyGetRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + createdSoW.id()))
                    .GET()
                    .build();
            HttpResponse<String> verifyGetResponse = client.send(verifyGetRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(verifyGetResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Test
    public void testSearch() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // Load 5+ documents and create SoWs
            for (int i = 1; i <= 5; i++) {
                Resource resource = resourceLoader.getResource("classpath:sows/sow" + i + ".txt");
                String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                SoWDTO sow = new SoWDTO(null, account.id(), new Date(), "SoW Title " + i, new BigDecimal("100.00"), "Description " + i, content);
                
                client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/sow"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sow)))
                        .build(), HttpResponse.BodyHandlers.ofString());
            }

            // Test search for AWS (should find sow1)
            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=AWS&max_text_length=50"))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(searchResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            List<SoWDTO> results = objectMapper.readValue(searchResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).text()).endsWith("... ");
            assertThat(results.get(0).text().toLowerCase()).contains("migration"); // sow1 content

            // Test search for "Banking" (should find sow2)
            HttpRequest searchBanking = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=Banking"))
                    .GET()
                    .build();
            HttpResponse<String> bankingResponse = client.send(searchBanking, HttpResponse.BodyHandlers.ofString());
            List<SoWDTO> bankingResults = objectMapper.readValue(bankingResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            assertThat(bankingResults).isNotEmpty();
            assertThat(bankingResults.get(0).text().toLowerCase()).contains("banking");

            // Test max_doc
            HttpRequest searchLimit = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=Statement&max_doc=2"))
                    .GET()
                    .build();
            HttpResponse<String> limitResponse = client.send(searchLimit, HttpResponse.BodyHandlers.ofString());
            List<SoWDTO> limitResults = objectMapper.readValue(limitResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            assertThat(limitResults).hasSize(2);

            // Test min_rank
            HttpRequest searchHighRank = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=Statement&min_rank=0.9"))
                    .GET()
                    .build();
            HttpResponse<String> highRankResponse = client.send(searchHighRank, HttpResponse.BodyHandlers.ofString());
            List<SoWDTO> highRankResults = objectMapper.readValue(highRankResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            assertThat(highRankResults).isEmpty();
        }
    }

    @Test
    public void testNonEnglishSearch() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String arabicTitle = "عنوان عربي";
            String arabicText = "هذا نص باللغة اَلْعَرَبِيَّةُ يحتوي على كلمات مختلفة";
            SoWDTO arabicSoW = new SoWDTO(null, account.id(), new Date(), arabicTitle, new BigDecimal("150.00"), "Description Arabic", arabicText);

            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(arabicSoW)))
                    .build();

            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

            // Search for "العربية"
            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=" + java.net.URLEncoder.encode("اَلْعَرَبِيَّةُ", StandardCharsets.UTF_8)))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(searchResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<SoWDTO> results = objectMapper.readValue(searchResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).title()).isEqualTo(arabicTitle);
        }
    }

    @Test
    public void testSpecialCharactersSearch() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String title = "Special Symbols Title";
            String content = "This content has symbols like ?>/ and some \n line \r\n breaks and \t tabs.";
            SoWDTO specialSoW = new SoWDTO(null, account.id(), new Date(), title, new BigDecimal("250.00"), "Description", content);

            // Create
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(specialSoW)))
                    .build();
            client.send(createRequest, HttpResponse.BodyHandlers.ofString());

            // Search for query with symbols, line breaks and extra whitespace
            String query = "symbols ?>/ \r\n  \t  breaks";
            String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/search?query=" + encodedQuery))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(searchResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

            List<SoWDTO> results = objectMapper.readValue(searchResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoWDTO.class));
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).title()).isEqualTo(title);
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
        return objectMapper.readValue(response.body(), RegionDTO.class);
    }

    private AccountDTO createAccount(HttpClient client, String name, Integer groupId, Integer regionId) throws Exception {
        AccountDTO account = new AccountDTO(null, name, groupId, regionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(account)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), AccountDTO.class);
    }
}
