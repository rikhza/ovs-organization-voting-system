package id.uvs.gateway;

import id.uvs.common.api.ApiResponse;
import id.uvs.common.api.ServiceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
class GatewayController {
    private final BrandProperties brand;
    private final List<Map<String, Object>> voters = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> candidates = List.of(
            Map.of("id", "cand-nadia", "name", "Nadia Pratama", "vision", "Transparent budgeting and regular member forums."),
            Map.of("id", "cand-rizal", "name", "Rizal Mahendra", "vision", "Career-oriented programs and stronger alumni network.")
    );
    private final Map<String, String> voteByVoter = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> tally = new ConcurrentHashMap<>();
    private final Set<String> publishedElectionIds = ConcurrentHashMap.newKeySet();

    GatewayController(BrandProperties brand) {
        this.brand = brand;
        voters.add(Map.of(
                "id", "vtr-1001",
                "name", "Amelia Putri",
                "email", "amelia@ovs.demo",
                "role", "ORG_ADMIN",
                "eligible", true
        ));
        voters.add(Map.of(
                "id", "vtr-1002",
                "name", "Demo Voter",
                "email", "voter@ovs.demo",
                "role", "VOTER",
                "eligible", true
        ));
        voters.add(Map.of(
                "id", "vtr-1003",
                "name", "Carla Wijaya",
                "email", "carla@ovs.demo",
                "role", "VOTER",
                "eligible", false
        ));
        tally.put("cand-nadia", new AtomicInteger(0));
        tally.put("cand-rizal", new AtomicInteger(0));
    }

    @GetMapping("/api/config")
    ApiResponse<BrandProperties> config() {
        return ApiResponse.ok("Brand configuration loaded", brand);
    }

    @GetMapping("/api/services")
    ApiResponse<List<ServiceInfo>> services() {
        return ApiResponse.ok("OVS service catalog", List.of(
                ServiceInfo.ready("identity-service"),
                ServiceInfo.ready("election-service"),
                ServiceInfo.ready("voting-service"),
                ServiceInfo.ready("result-service")
        ));
    }

    @GetMapping("/api/overview")
    ApiResponse<Map<String, Object>> overview() {
        long eligibleVoters = voters.stream().filter(v -> Boolean.TRUE.equals(v.get("eligible"))).count();
        int votesCast = voteByVoter.size();
        double turnout = eligibleVoters == 0 ? 0 : ((double) votesCast / eligibleVoters) * 100;
        return ApiResponse.ok("Live election overview", Map.of(
                "activeElection", "OVS Executive Board 2026",
                "eligibleVoters", eligibleVoters,
                "votesCast", votesCast,
                "turnoutPercent", Math.round(turnout * 100.0) / 100.0,
                "status", "OPEN"
        ));
    }

    @PostMapping("/api/auth/login")
    ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.getOrDefault("email", "").trim().toLowerCase();
        String password = request.getOrDefault("password", "");
        if (email.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }

        if (email.equals("admin@ovs.demo") && password.equals("admin123")) {
            return ApiResponse.ok("Authenticated", Map.of(
                    "token", "demo-admin-token",
                    "name", "Demo Admin",
                    "role", "ORG_ADMIN",
                    "email", email
            ));
        }
        if (email.equals("voter@ovs.demo") && password.equals("voter123")) {
            return ApiResponse.ok("Authenticated", Map.of(
                    "token", "demo-voter-token",
                    "name", "Demo Voter",
                    "role", "VOTER",
                    "email", email
            ));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid demo credentials.");
    }

    @GetMapping("/api/admin/voters")
    ApiResponse<List<Map<String, Object>>> listVoters() {
        return ApiResponse.ok("Voter list loaded", voters);
    }

    @PostMapping("/api/admin/voters")
    ApiResponse<Map<String, Object>> createVoter(@RequestBody Map<String, Object> request) {
        String name = asText(request.get("name"));
        String email = asText(request.get("email")).toLowerCase();
        boolean eligible = Boolean.parseBoolean(String.valueOf(request.getOrDefault("eligible", true)));
        if (name.isBlank() || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required.");
        }
        Map<String, Object> voter = Map.of(
                "id", "vtr-" + UUID.randomUUID().toString().substring(0, 8),
                "name", name,
                "email", email,
                "role", "VOTER",
                "eligible", eligible
        );
        voters.add(voter);
        return ApiResponse.ok("Voter created", voter);
    }

    @PatchMapping("/api/admin/voters/{voterId}")
    ApiResponse<Map<String, Object>> updateVoterEligibility(@PathVariable String voterId, @RequestBody Map<String, Object> request) {
        boolean eligible = Boolean.parseBoolean(String.valueOf(request.getOrDefault("eligible", false)));
        for (int i = 0; i < voters.size(); i++) {
            Map<String, Object> voter = voters.get(i);
            if (voter.get("id").equals(voterId)) {
                Map<String, Object> updated = Map.of(
                        "id", voter.get("id"),
                        "name", voter.get("name"),
                        "email", voter.get("email"),
                        "role", voter.get("role"),
                        "eligible", eligible
                );
                voters.set(i, updated);
                return ApiResponse.ok("Voter updated", updated);
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voter not found.");
    }

    @GetMapping("/api/elections")
    ApiResponse<List<Map<String, Object>>> elections() {
        return ApiResponse.ok("Election list loaded", List.of(
                Map.of(
                        "id", "ovs-board-2026",
                        "name", "OVS Executive Board 2026",
                        "status", "OPEN",
                        "opensAt", Instant.now().minusSeconds(3600).toString(),
                        "closesAt", Instant.now().plusSeconds(28800).toString(),
                        "candidates", candidates
                )
        ));
    }

    @PostMapping("/api/votes")
    ApiResponse<Map<String, Object>> castVote(@RequestBody Map<String, String> request) {
        String voterEmail = request.getOrDefault("voterEmail", "").toLowerCase();
        String candidateId = request.getOrDefault("candidateId", "");
        if (voterEmail.isBlank() || candidateId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voter email and candidate ID are required.");
        }
        boolean voterEligible = voters.stream()
                .anyMatch(v -> v.get("email").equals(voterEmail) && Boolean.TRUE.equals(v.get("eligible")));
        if (!voterEligible) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voter is not eligible.");
        }
        if (!tally.containsKey(candidateId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate is invalid.");
        }
        if (voteByVoter.containsKey(voterEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate vote detected.");
        }

        voteByVoter.put(voterEmail, candidateId);
        tally.get(candidateId).incrementAndGet();
        return ApiResponse.ok("Vote accepted", Map.of(
                "receiptId", "rcpt-" + UUID.randomUUID().toString().substring(0, 12),
                "voterEmail", voterEmail,
                "candidateId", candidateId
        ));
    }

    @GetMapping("/api/results/{electionId}")
    ApiResponse<Map<String, Object>> results(@PathVariable String electionId) {
        List<Map<String, Object>> rows = candidates.stream()
                .map(candidate -> Map.<String, Object>of(
                        "candidateId", candidate.get("id"),
                        "candidateName", candidate.get("name"),
                        "votes", tally.get(String.valueOf(candidate.get("id"))).get()
                ))
                .toList();
        return ApiResponse.ok("Result snapshot loaded", Map.of(
                "electionId", electionId,
                "published", publishedElectionIds.contains(electionId),
                "tally", rows
        ));
    }

    @PostMapping("/api/results/{electionId}/publish")
    ApiResponse<Map<String, Object>> publish(@PathVariable String electionId) {
        publishedElectionIds.add(electionId);
        return ApiResponse.ok("Results published", Map.of(
                "electionId", electionId,
                "published", true
        ));
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
