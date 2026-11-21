package org.linkwave.userservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.StudentProfileService;
import org.linkwave.userservice.academic.UserProfileService;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hồ sơ của chính người đang đăng nhập. Chỉ sửa được số điện thoại và địa chỉ.
 * Các trường còn lại (ngày sinh, giới tính, tình trạng, CCCD, dân tộc, tôn giáo) do quản trị viên quản lý.
 * Không trả về giá trị đầy đủ của CCCD, dân tộc, tôn giáo.
 */
@RestController
@RequestMapping("/api/v1/users/me/profile")
@RequiredArgsConstructor
public class MyProfileController {

    private final UserRepository userRepository;
    private final UserProfileService profiles;
    private final StudentProfileService students;

    public record MyProfileView(String username, String name, List<String> roles,
                                String studentCode, String classCode,
                                UserProfileService.ProfileView profile) {
    }

    public record ContactRequest(@Size(max = 20) String phone, @Size(max = 255) String address) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MyProfileView get() {
        return view(currentUserId());
    }

    @PutMapping
    @Transactional
    public MyProfileView update(@RequestBody @Valid ContactRequest body) {
        final Long userId = currentUserId();
        final UserProfileService.ProfileView current = profiles.find(userId);
        final List<String> roles = roles(userId);
        // Truyền lại mọi trường hiện có, chỉ thay số điện thoại và địa chỉ. Bảng được ghi đè theo từng cột.
        profiles.save(userId, roles.isEmpty() ? "" : roles.get(0), new UserProfileService.ProfileInput(
                current.gender(),
                current.birthDate(),
                null,
                body.phone(),
                current.birthplace(),
                current.nationality(),
                body.address(),
                null,
                null,
                null));
        return view(userId);
    }

    private MyProfileView view(Long userId) {
        final var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        final StudentProfileService.StudentInfo student = students.find(userId).orElse(null);
        return new MyProfileView(
                user.getUsername(),
                user.getName(),
                roles(userId),
                student == null ? null : student.studentCode(),
                student == null ? null : student.classCode(),
                profiles.find(userId));
    }

    private List<String> roles(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream().map(role -> role.getName()).toList())
                .orElse(List.of());
    }

    private Long currentUserId() {
        return ((DefaultUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
