package bd.fasol.auth.dto.response;

import bd.fasol.model.Role;
import bd.fasol.model.User;

public record UserResponse(
    Long id, String phoneNumber, String name, Role role,
    String profileImageUrl, String designation, String qualification,
    String specialization, boolean available, boolean online,
    String district, String upazila) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.id, user.phoneNumber, user.name, user.role, user.profileImageUrl,
        user.designation, user.qualification, user.specialization,
        user.available, user.online, user.district, user.upazila);
  }
}
