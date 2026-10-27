package com.insurance.claims.controller;

import com.insurance.claims.model.Claim;
import com.insurance.claims.model.Requests.CreateClaimRequest;
import com.insurance.claims.model.Requests.UpdateClaimStatusRequest;
import com.insurance.claims.service.ClaimService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claims;

    public ClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @PostMapping
    public ResponseEntity<Claim> registerClaim(@Valid @RequestBody CreateClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claims.registerClaim(request));
    }

    @GetMapping
    public List<Claim> listClaims() {
        return claims.listClaims();
    }

    @GetMapping("/{id}")
    public Claim getClaim(@PathVariable Long id) {
        return claims.getClaim(id);
    }

    @GetMapping("/policy/{policyId}")
    public List<Claim> listClaimsForPolicy(@PathVariable Long policyId) {
        return claims.listClaimsForPolicy(policyId);
    }

    @PutMapping("/{id}/status")
    public Claim updateStatus(@PathVariable Long id,
                              @Valid @RequestBody UpdateClaimStatusRequest request) {
        return claims.updateStatus(id, request.status(), request.reason());
    }

    @PostMapping("/{id}/settle")
    public Claim settleClaim(@PathVariable Long id) {
        return claims.settleClaim(id);
    }
}
