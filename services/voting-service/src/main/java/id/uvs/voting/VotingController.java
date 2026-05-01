package id.uvs.voting;

import id.uvs.common.api.ApiResponse;
import id.uvs.common.api.ServiceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
class VotingController {
    private final Map<String, String> voteByEmail = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> tallyByCandidate = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> receipts = new CopyOnWriteArrayList<>();

    VotingController() {
        tallyByCandidate.put("cand-nadia", new AtomicInteger(0));
        tallyByCandidate.put("cand-rizal", new AtomicInteger(0));
    }

    @GetMapping("/api/votes/health")
    ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok("Voting service ready", ServiceInfo.ready("voting-service"));
    }

    @GetMapping("/api/votes/policy")
    ApiResponse<Map<String, Object>> policy() {
        return ApiResponse.ok("Ballot safety policy", Map.of(
                "oneVotePerElection", true,
                "storeBallotSeparatelyFromIdentity", true,
                "auditEventsRequired", true,
                "maxBallotSelections", 1
        ));
    }

    @PostMapping("/api/votes/cast")
    ApiResponse<Map<String, Object>> castVote(@RequestBody Map<String, String> request) {
        String voterEmail = request.getOrDefault("voterEmail", "").trim().toLowerCase();
        String candidateId = request.getOrDefault("candidateId", "").trim();
        if (voterEmail.isBlank() || candidateId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voter email and candidate ID are required.");
        }
        if (!tallyByCandidate.containsKey(candidateId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate is invalid.");
        }
        if (voteByEmail.containsKey(voterEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate vote detected.");
        }
        voteByEmail.put(voterEmail, candidateId);
        tallyByCandidate.get(candidateId).incrementAndGet();
        Map<String, Object> receipt = Map.of(
                "receiptId", "vt-" + UUID.randomUUID().toString().substring(0, 10),
                "voterEmail", voterEmail,
                "candidateId", candidateId
        );
        receipts.add(receipt);
        return ApiResponse.ok("Vote accepted", receipt);
    }

    @GetMapping("/api/votes/receipts")
    ApiResponse<List<Map<String, Object>>> receipts() {
        return ApiResponse.ok("Vote receipts loaded", receipts);
    }

    @GetMapping("/api/votes/tally")
    ApiResponse<List<Map<String, Object>>> tally() {
        return ApiResponse.ok("Current tally loaded", List.of(
                Map.of("candidateId", "cand-nadia", "votes", tallyByCandidate.get("cand-nadia").get()),
                Map.of("candidateId", "cand-rizal", "votes", tallyByCandidate.get("cand-rizal").get())
        ));
    }
}
