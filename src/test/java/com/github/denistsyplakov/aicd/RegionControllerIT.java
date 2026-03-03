package com.github.denistsyplakov.aicd;

import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RegionControllerIT extends BaseIT {

    @Test
    public void testCrud() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. Create
            String regionNameNorth = randomName("North");
            RegionDTO createdRegion = createRegion(client, regionNameNorth);
            assertThat(createdRegion.id()).isNotNull();
            assertThat(createdRegion.name()).isEqualTo(regionNameNorth);

            // 2. Get All
            HttpRequest getAllRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region"))
                    .GET()
                    .build();

            HttpResponse<String> getAllResponse = client.send(getAllRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(getAllResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            List<RegionDTO> allRegions = objectMapper.readValue(getAllResponse.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, RegionDTO.class));
            assertThat(allRegions).extracting(RegionDTO::name).contains(regionNameNorth);

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
            String regionNameSouth = randomName("South");
            RegionDTO updateRegion = new RegionDTO(null, regionNameSouth);
            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateRegion)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
            RegionDTO updatedRegion = objectMapper.readValue(updateResponse.body(), RegionDTO.class);
            assertThat(updatedRegion.id()).isEqualTo(createdRegion.id());
            assertThat(updatedRegion.name()).isEqualTo(regionNameSouth);

            // 5. Update to existing name (conflict)
            // Create another region first
            String regionNameEast = randomName("East");
            createRegion(client, regionNameEast);

            HttpRequest conflictRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/region/" + createdRegion.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new RegionDTO(null, regionNameEast))))
                    .build();

            HttpResponse<String> conflictResponse = client.send(conflictRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(conflictResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

            // 6. Delete (fail because of reference constraint)
            // Insert into account table
            jdbcTemplate.update("INSERT INTO account(name, region_id) VALUES (?, ?)", randomName("Account 1"), createdRegion.id());

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
            Integer nonExistentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM region", Integer.class);

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
