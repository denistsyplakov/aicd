package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RegionControllerIT {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCrud() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create
            RegionDTO newRegion = new RegionDTO(null, "North");
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newRegion)))
                    .build();

            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            RegionDTO createdRegion = objectMapper.readValue(createResponse.body(), RegionDTO.class);
            assertThat(createdRegion.id()).isNotNull();
            assertThat(createdRegion.name()).isEqualTo("North");

            // 2. Get All
            HttpRequest getAllRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region"))
                    .GET()
                    .build();

            HttpResponse<String> getAllResponse = client.send(getAllRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getAllResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            List<RegionDTO> allRegions = objectMapper.readValue(getAllResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, RegionDTO.class));
            assertThat(allRegions).extracting(RegionDTO::name).contains("North");

            // 3. Get By ID
            HttpRequest getByIdRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .GET()
                    .build();

            HttpResponse<String> getByIdResponse = client.send(getByIdRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getByIdResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            RegionDTO fetchedRegion = objectMapper.readValue(getByIdResponse.body(), RegionDTO.class);
            assertThat(fetchedRegion).isEqualTo(createdRegion);

            // 4. Update
            RegionDTO updateRegion = new RegionDTO(null, "South");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateRegion)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            RegionDTO updatedRegion = objectMapper.readValue(updateResponse.body(), RegionDTO.class);
            assertThat(updatedRegion.id()).isEqualTo(createdRegion.id());
            assertThat(updatedRegion.name()).isEqualTo("South");

            // 5. Update to existing name (conflict)
            // Create another region first
            RegionDTO anotherRegion = new RegionDTO(null, "East");
            client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(anotherRegion)))
                    .build(), HttpResponse.BodyHandlers.ofString());

            HttpRequest conflictRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new RegionDTO(null, "East"))))
                    .build();

            HttpResponse<String> conflictResponse = client.send(conflictRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(conflictResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // 6. Delete (fail because of reference constraint)
            // Insert into account table
            jdbcTemplate.execute("INSERT INTO account(name, region_id) VALUES ('Account 1', " + createdRegion.id() + ")");

            HttpRequest deleteFailRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteFailResponse = client.send(deleteFailRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteFailResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // Cleanup account
            jdbcTemplate.execute("DELETE FROM account WHERE region_id = " + createdRegion.id());

            // 7. Delete (success)
            HttpRequest deleteSuccessRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteSuccessResponse = client.send(deleteSuccessRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteSuccessResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    @Test
    public void testNotFound() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            Integer nonExistentId = 9999;

            // 1. Get Non-Existent
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentId))
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 2. Update Non-Existent
            RegionDTO updateRegion = new RegionDTO(null, "Somewhere");
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateRegion)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

            // 3. Delete Non-Existent
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + nonExistentId))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
