package io.github.habittracker.adapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habittracker.domain.model.HabitFrequency;
import io.github.habittracker.model.dto.CreateHabitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class HabitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    void listActive_returnsEmptyArray_whenNoHabits() throws Exception {
        mockMvc.perform(get("/api/habits"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createHabit_returns201_withCreatedBody() throws Exception {
        CreateHabitRequest request = new CreateHabitRequest();
        request.setName("Reading 30 min");
        request.setDescription("Read every evening");
        request.setFrequency(HabitFrequency.DAILY);

        mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Reading 30 min")))
                .andExpect(jsonPath("$.frequency", is("DAILY")))
                .andExpect(jsonPath("$.archived", is(false)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createHabit_returns400_whenNameIsBlank() throws Exception {
        CreateHabitRequest request = new CreateHabitRequest();
        request.setName("");
        request.setFrequency(HabitFrequency.DAILY);

        mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()));
    }

    @Test
    void listActive_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/habits"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_returns404_whenHabitDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/habits/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkIn_createsEntryForToday_whenRequestBodyEmpty() throws Exception {
        // najpierw utworzenie habita
        CreateHabitRequest createReq = new CreateHabitRequest();
        createReq.setName("Meditation");
        createReq.setFrequency(HabitFrequency.DAILY);

        String response = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long habitId = objectMapper.readTree(response).get("id").asLong();

        // check-in bez body z dzisiejsza data
        mockMvc.perform(post("/api/habits/{id}/check-in", habitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryDate", notNullValue()))
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void statistics_returnsZeros_forFreshHabitWithoutCheckIns() throws Exception {
        CreateHabitRequest createReq = new CreateHabitRequest();
        createReq.setName("Running");
        createReq.setFrequency(HabitFrequency.DAILY);

        String response = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long habitId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/habits/{id}/statistics", habitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak", is(0)))
                .andExpect(jsonPath("$.longestStreak", is(0)))
                .andExpect(jsonPath("$.totalCompletions", is(0)));
    }
}