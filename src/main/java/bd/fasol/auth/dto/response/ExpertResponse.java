package bd.fasol.auth.dto.response;

import bd.fasol.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ExpertResponse(
        Long id,
        String name,
        String profileImageUrl,
        String designation,
        String qualification,
        List<Long> cropIds,
        boolean active,
        boolean acceptingConsultations,
        String availabilityStatus,
        List<Double> location,
        Integer displayOrder,
        Instant updatedAt
) {
    public static ExpertResponse from(User x) {
        List<Double> location = x.latitude == null || x.longitude == null
                ? null : List.of(x.latitude, x.longitude);
        return new ExpertResponse(x.id, x.name, x.profileImageUrl, x.designation, x.qualification,
                new ArrayList<>(x.cropIds), x.isActive, x.acceptingConsultations,
                x.availabilityStatus, location, x.displayOrder, x.updatedAt);
    }
}