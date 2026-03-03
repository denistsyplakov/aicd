package com.github.denistsyplakov.aicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import com.github.denistsyplakov.aicd.repo.SoWRepository.SoWDTO;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIT {

    @LocalServerPort
    protected int port;

    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private final List<Integer> sowIds = new ArrayList<>();
    private final List<Integer> accountIds = new ArrayList<>();
    private final List<Integer> regionIds = new ArrayList<>();
    private final List<Integer> accountGroupIds = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        if (!sowIds.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM sow_text_index WHERE id IN (" + idsToCsv(sowIds) + ")");
            jdbcTemplate.execute("DELETE FROM sow WHERE id IN (" + idsToCsv(sowIds) + ")");
        }
        if (!accountIds.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM account WHERE id IN (" + idsToCsv(accountIds) + ")");
        }
        if (!regionIds.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM region WHERE id IN (" + idsToCsv(regionIds) + ")");
        }
        if (!accountGroupIds.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM account_group WHERE id IN (" + idsToCsv(accountGroupIds) + ")");
        }
        sowIds.clear();
        accountIds.clear();
        regionIds.clear();
        accountGroupIds.clear();
    }

    private String idsToCsv(List<Integer> ids) {
        return ids.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    protected String randomName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected RegionDTO createRegion(HttpClient client, String name) throws Exception {
        RegionDTO regionDTO = new RegionDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/region"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(regionDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RegionDTO created = objectMapper.readValue(response.body(), RegionDTO.class);
        regionIds.add(created.id());
        return created;
    }

    protected AccountGroupDTO createAccountGroup(HttpClient client, String name) throws Exception {
        AccountGroupDTO groupDTO = new AccountGroupDTO(null, name);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account-group"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(groupDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        AccountGroupDTO created = objectMapper.readValue(response.body(), AccountGroupDTO.class);
        accountGroupIds.add(created.id());
        return created;
    }

    protected AccountDTO createAccount(HttpClient client, String name, Integer groupId, Integer regionId) throws Exception {
        AccountDTO accountDTO = new AccountDTO(null, name, groupId, regionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(accountDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        AccountDTO created = objectMapper.readValue(response.body(), AccountDTO.class);
        accountIds.add(created.id());
        return created;
    }

    protected SoWDTO createSoW(HttpClient client, SoWDTO sowDTO) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/sow"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sowDTO)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        SoWDTO created = objectMapper.readValue(response.body(), SoWDTO.class);
        sowIds.add(created.id());
        return created;
    }

    protected SoWDTO createSoW(HttpClient client, Integer accountId, String title, String text) throws Exception {
        return createSoW(client, new SoWDTO(null, accountId, new Date(), title, new BigDecimal("100.00"), "Description", text));
    }
}
