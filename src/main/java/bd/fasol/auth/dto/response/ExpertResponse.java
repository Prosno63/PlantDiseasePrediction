package bd.fasol.auth.dto.response;

import bd.fasol.model.Role;
import bd.fasol.model.User;

public record ExpertResponse(
        Long id,
        String phoneNumber,
        String name,
        String profileImageUrl,
        String designation,
        String qualification,
        String specialization,
        boolean available,
        boolean online,
        String district,
        String upazila,
        Double latitude,
        Double longitude,
        boolean isActive,
        Role role
) {
    public static ExpertResponse from(User x) {
        return new ExpertResponse(x.id, x.phoneNumber, x.name, x.profileImageUrl,
                x.designation, x.qualification, x.specialization, x.available, x.online,
                x.district, x.upazila, x.latitude, x.longitude, true, x.role);
    }
}
