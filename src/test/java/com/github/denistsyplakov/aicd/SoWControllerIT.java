package com.github.denistsyplakov.aicd;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import com.github.denistsyplakov.aicd.repo.SoWRepository.SoWDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SoWControllerIT extends BaseIT {

    @Autowired
    private ResourceLoader resourceLoader;

    private AccountDTO account;

    @BeforeEach
    public void setup() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            AccountGroupDTO group = createAccountGroup(client, randomName("Test Group"));
            RegionDTO region = createRegion(client, randomName("Test Region"));
            account = createAccount(client, randomName("Test Account"), group.id(), region.id());
        }
    }

    @Test
    public void testCrud() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create
            String sowTitle1 = randomName("SoW 1");
            SoWDTO newSoW = new SoWDTO(null, account.id(), new Date(), sowTitle1, new BigDecimal("1000.00"), "Description 1", "Some long text content for SoW 1");
            SoWDTO createdSoW = createSoW(client, newSoW);
            assertThat(createdSoW.id()).isNotNull();
            assertThat(createdSoW.title()).isEqualTo(sowTitle1);

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
            String sowTitleUpdated = randomName("Updated SoW");
            SoWDTO updateSoW = new SoWDTO(null, account.id(), new Date(), sowTitleUpdated, new BigDecimal("2000.00"), "Updated Description", "Updated text content");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/sow/" + createdSoW.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateSoW)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            SoWDTO updatedSoW = objectMapper.readValue(updateResponse.body(), SoWDTO.class);
            assertThat(updatedSoW.title()).isEqualTo(sowTitleUpdated);

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
                SoWDTO sow = new SoWDTO(null, account.id(), new Date(), randomName("SoW Title " + i), new BigDecimal("100.00"), "Description " + i, content);
                createSoW(client, sow);
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
            String arabicTitle = randomName("عنوان عربي");
            String arabicText = "هذا نص باللغة اَلْعَرَبِيَّةُ يحتوي على كلمات مختلفة";
            SoWDTO arabicSoW = new SoWDTO(null, account.id(), new Date(), arabicTitle, new BigDecimal("150.00"), "Description Arabic", arabicText);
            createSoW(client, arabicSoW);

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
            String title = randomName("Special Symbols Title");
            String content = "This content has symbols like ?>/ and some \n line \r\n breaks and \t tabs.";
            SoWDTO specialSoW = new SoWDTO(null, account.id(), new Date(), title, new BigDecimal("250.00"), "Description", content);
            createSoW(client, specialSoW);

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

}
