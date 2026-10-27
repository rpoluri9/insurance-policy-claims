package com.insurance.claims.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsuranceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long createCustomer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Priya","lastName":"Nair",
                                 "email":"priya@example.com","phone":"555-0200",
                                 "dateOfBirth":"1990-05-15","address":"9 Lake View"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("priya@example.com"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private long createPolicy(long customerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"policyType":"HEALTH","coverageAmount":100000,
                                 "effectiveDate":"2026-01-01","renewalDate":"2027-01-01"}"""
                                .formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.premiumAmount").value(5000.00))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    @WithMockUser
    void fullFlowFromCustomerToSettledClaim() throws Exception {
        long customerId = createCustomer();
        long policyId = createPolicy(customerId);

        mockMvc.perform(post("/api/policies/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"policyType":"HEALTH","coverageAmount":100000}"""
                                .formatted(customerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumAmount").value(5000.00));

        MvcResult claimResult = mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyId":%d,"description":"Hospital bill",
                                 "claimAmount":5000,"incidentDate":"2026-03-10"}"""
                                .formatted(policyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andReturn();
        long claimId = objectMapper.readTree(claimResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(put("/api/claims/" + claimId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNDER_REVIEW\",\"reason\":\"Checking documents\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(put("/api/claims/" + claimId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"reason\":\"Documents verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/claims/" + claimId + "/settle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.settledAmount").value(5000));

        mockMvc.perform(get("/api/claims/" + claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));
    }

    @Test
    @WithMockUser
    void claimAgainstCancelledPolicyIsRejected() throws Exception {
        long customerId = createCustomer();
        long policyId = createPolicy(customerId);
        mockMvc.perform(put("/api/policies/" + policyId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyId":%d,"description":"Late bill",
                                 "claimAmount":1000,"incidentDate":"2026-03-10"}"""
                                .formatted(policyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void illegalClaimTransitionIsRejected() throws Exception {
        long customerId = createCustomer();
        long policyId = createPolicy(customerId);
        MvcResult claimResult = mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyId":%d,"description":"Bill",
                                 "claimAmount":1000,"incidentDate":"2026-03-10"}"""
                                .formatted(policyId)))
                .andExpect(status().isCreated())
                .andReturn();
        long claimId = objectMapper.readTree(claimResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(put("/api/claims/" + claimId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SETTLED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }
}
