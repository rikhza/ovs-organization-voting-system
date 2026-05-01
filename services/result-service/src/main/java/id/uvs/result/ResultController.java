package id.uvs.result;

import id.uvs.common.api.ApiResponse;
import id.uvs.common.api.ServiceInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
class ResultController {
    private final Set<String> publishedIds = ConcurrentHashMap.newKeySet();

    @GetMapping("/api/results/health")
    ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok("Result service ready", ServiceInfo.ready("result-service"));
    }

    @GetMapping("/api/results/demo")
    ApiResponse<Map<String, Object>> demoResult() {
        return ApiResponse.ok("Demo tally", Map.of(
                "electionId", "ukm-chair-2026",
                "published", false,
                "tally", List.of(
                        Map.of("candidateId", "cand-nadia", "candidate", "Nadia Pratama", "votes", 0),
                        Map.of("candidateId", "cand-rizal", "candidate", "Rizal Mahendra", "votes", 0)
                )
        ));
    }

    @PostMapping("/api/results/{electionId}/publish")
    ApiResponse<Map<String, Object>> publish(@PathVariable String electionId) {
        publishedIds.add(electionId);
        return ApiResponse.ok("Results published", Map.of(
                "electionId", electionId,
                "published", true
        ));
    }

    @GetMapping("/api/results/{electionId}/publication")
    ApiResponse<Map<String, Object>> publication(@PathVariable String electionId) {
        return ApiResponse.ok("Publication state loaded", Map.of(
                "electionId", electionId,
                "published", publishedIds.contains(electionId)
        ));
    }
}
