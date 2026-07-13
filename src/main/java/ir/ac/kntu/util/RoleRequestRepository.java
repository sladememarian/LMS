package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.support.rolerequest.RoleRequest;

// Persistence for the {@code role_requests} table. Split out of the former
// monolithic {@code DatabaseAccess} class as part of the per-domain
// repository migration.
public final class RoleRequestRepository {

    private RoleRequestRepository() {
    }

    public static void clearRoleRequests() {
        Database.executeUpdate("DELETE FROM role_requests");
    }

    public static void insertRoleRequest(RoleRequest request) {
        Database.withPs("MERGE INTO role_requests USING (VALUES (?, ?, ?, ?, ?)) AS s(request_id, requester_email, requested_role, message, status) ON role_requests.request_id = s.request_id WHEN MATCHED THEN UPDATE SET requester_email = s.requester_email, requested_role = s.requested_role, message = s.message, status = s.status WHEN NOT MATCHED THEN INSERT (request_id, requester_email, requested_role, message, status) VALUES (s.request_id, s.requester_email, s.requested_role, s.message, s.status)", ps -> {
            ps.setString(1, request.getRequestId());
            ps.setString(2, request.getRequesterEmail());
            ps.setString(3, request.getRequestedRole());
            ps.setString(4, request.getMessage());
            ps.setString(5, request.getStatus());
            ps.executeUpdate();
        });
    }

    public static List<RoleRequest> getAllRoleRequests() {
        return Database.queryAll("SELECT * FROM role_requests", rs -> {
            RoleRequest request = new RoleRequest(rs.getString("request_id"), rs.getString("requester_email"),
                    rs.getString("requested_role"), rs.getString("message"));
            request.setStatus(rs.getString("status"));
            return request;
        });
    }

    public static void updateRoleRequestStatus(String requestId, String status) {
        Database.withPs("UPDATE role_requests SET status=? WHERE request_id=?", ps -> {
            ps.setString(1, status);
            ps.setString(2, requestId);
            ps.executeUpdate();
        });
    }
}
