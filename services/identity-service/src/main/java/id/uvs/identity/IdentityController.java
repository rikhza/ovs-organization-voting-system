package id.uvs.identity;

import id.uvs.common.api.ApiResponse;
import id.uvs.common.api.ServiceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
class IdentityController {
    private final List<Map<String, Object>> members = new CopyOnWriteArrayList<>();

    IdentityController() {
        members.add(Map.of("id", "mem-001", "name", "Demo Admin", "email", "admin@ovs.demo", "role", "ORG_ADMIN", "eligible", true));
        members.add(Map.of("id", "mem-002", "name", "Demo Voter", "email", "voter@ovs.demo", "role", "VOTER", "eligible", true));
    }

    @GetMapping("/api/identity/health")
    ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok("Identity service ready", ServiceInfo.ready("identity-service"));
    }

    @GetMapping("/api/identity/roles")
    ApiResponse<List<Map<String, String>>> roles() {
        return ApiResponse.ok("Supported roles", List.of(
                Map.of("code", "ORG_ADMIN", "label", "Organization admin"),
                Map.of("code", "ELECTION_OFFICER", "label", "Election officer"),
                Map.of("code", "VOTER", "label", "Eligible voter"),
                Map.of("code", "CANDIDATE", "label", "Candidate")
        ));
    }

    @PostMapping("/api/identity/login")
    ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.getOrDefault("email", "").trim().toLowerCase();
        String password = request.getOrDefault("password", "");
        if (email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }
        if (email.equals("admin@ovs.demo") && password.equals("admin123")) {
            return ApiResponse.ok("Authenticated", Map.of("token", "identity-admin-token", "role", "ORG_ADMIN", "name", "Demo Admin"));
        }
        if (email.equals("voter@ovs.demo") && password.equals("voter123")) {
            return ApiResponse.ok("Authenticated", Map.of("token", "identity-voter-token", "role", "VOTER", "name", "Demo Voter"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid demo credentials.");
    }

    @GetMapping("/api/identity/members")
    ApiResponse<List<Map<String, Object>>> members() {
        return ApiResponse.ok("Member list loaded", members);
    }

    @PostMapping("/api/identity/members")
    ApiResponse<Map<String, Object>> addMember(@RequestBody Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        String role = String.valueOf(request.getOrDefault("role", "VOTER")).trim();
        boolean eligible = Boolean.parseBoolean(String.valueOf(request.getOrDefault("eligible", true)));
        if (name.isBlank() || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required.");
        }
        Map<String, Object> member = Map.of(
                "id", "mem-" + UUID.randomUUID().toString().substring(0, 8),
                "name", name,
                "email", email,
                "role", role,
                "eligible", eligible
        );
        members.add(member);
        return ApiResponse.ok("Member created", member);
    }
}
