package ir.ac.kntu.util;

import java.util.List;

import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationStatus;

// Persistence for the {@code reservations} table. Split out of the former
// monolithic {@code DatabaseAccess} class as part of the per-domain
// repository migration.
public final class ReservationRepository {

    private ReservationRepository() {
    }

    public static void clearReservations() {
        Database.executeUpdate("DELETE FROM reservations");
    }

    public static void insertReservation(Reservation reservation) {
        Database.withPs("MERGE INTO reservations USING (VALUES (?, ?, ?, ?, ?, ?)) AS s(reservation_id, member_id, item_id, reserved_on_day, expires_on_day, status) ON reservations.reservation_id = s.reservation_id WHEN MATCHED THEN UPDATE SET member_id = s.member_id, item_id = s.item_id, reserved_on_day = s.reserved_on_day, expires_on_day = s.expires_on_day, status = s.status WHEN NOT MATCHED THEN INSERT (reservation_id, member_id, item_id, reserved_on_day, expires_on_day, status) VALUES (s.reservation_id, s.member_id, s.item_id, s.reserved_on_day, s.expires_on_day, s.status)", ps -> {
            ps.setString(1, reservation.getReservationId());
            ps.setString(2, reservation.getMemberId());
            ps.setString(3, reservation.getItemId());
            ps.setInt(4, reservation.getReservedOnDay());
            ps.setInt(5, reservation.getExpiresOnDay());
            ps.setString(6, reservation.getStatus().getLabel());
            ps.executeUpdate();
        });
    }

    public static List<Reservation> getAllReservations() {
        return Database.queryAll("SELECT * FROM reservations", rs -> new Reservation(
                rs.getString("reservation_id"),
                rs.getString("member_id"),
                rs.getString("item_id"),
                rs.getInt("reserved_on_day"),
                rs.getInt("expires_on_day"),
                ReservationStatus.fromLabel(rs.getString("status"))));
    }

    public static void deleteReservation(String reservationId) {
        Database.withPs("DELETE FROM reservations WHERE reservation_id=?", ps -> {
            ps.setString(1, reservationId);
            ps.executeUpdate();
        });
    }
}
