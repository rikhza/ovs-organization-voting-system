package id.uvs.election;

import id.uvs.common.api.ApiResponse;
import id.uvs.common.api.ServiceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
class ElectionController {
    private final List<Map<String, Object>> elections = new CopyOnWriteArrayList<>();

    ElectionController() {
        elections.add(Map.of(
                "id", "ovs-board-2026",
                "name", "OVS Executive Board 2026",
                "status", "OPEN",
                "opensAt", OffsetDateTime.now().minusHours(1).toString(),
                "closesAt", OffsetDateTime.now().plusHours(8).toString(),
                "candidates", List.of(
                        Map.of("id", "cand-nadia", "name", "Nadia Pratama"),
                        Map.of("id", "cand-rizal", "name", "Rizal Mahendra")
                )
        ));
    }

    @GetMapping("/api/elections/health")
    ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok("Election service ready", ServiceInfo.ready("election-service"));
    }

    @GetMapping("/api/elections")
    ApiResponse<List<Map<String, Object>>> listElections() {
        return ApiResponse.ok("Election list loaded", elections);
    }

    @PostMapping("/api/elections")
    ApiResponse<Map<String, Object>> createElection(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Election name is required.");
        }
        Map<String, Object> election = Map.of(
                "id", "el-" + UUID.randomUUID().toString().substring(0, 8),
                "name", name,
                "status", "DRAFT",
                "opensAt", OffsetDateTime.now().plusDays(1).toString(),
                "closesAt", OffsetDateTime.now().plusDays(2).toString(),
                "candidates", List.of()
        );
        elections.add(election);
        return ApiResponse.ok("Election created", election);
    }
}
