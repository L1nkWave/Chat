package org.linkwave.chatservice.academic;

import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.ServiceErrorException;
import org.linkwave.chatservice.api.academic.AcademicClient;
import org.linkwave.chatservice.api.academic.AcademicClient.ClassRoster;
import org.linkwave.chatservice.api.academic.AcademicClient.FacultyRoster;
import org.linkwave.chatservice.api.academic.AcademicClient.RostersView;
import org.linkwave.chatservice.api.academic.AcademicClient.SubjectRoster;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.chat.group.ChatCategory;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.linkwave.chatservice.invitation.InvitationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tạo nhóm lớp, môn, khoa từ dữ liệu học vụ và mời thành viên. Chạy lại nhiều lần không tạo nhóm hay lời mời trùng.
 * Không tự xóa thành viên khi họ không còn trong danh sách.
 */
@Slf4j
@Service
public class AcademicSyncService {

    /** NewGroupChatRequest giới hạn tên nhóm 32 ký tự */
    private static final int NAME_LIMIT = 32;
    private static final int DESCRIPTION_LIMIT = 256;
    /** Giá trị mặc định membersLimit của GroupChat */
    private static final int MIN_MEMBERS_LIMIT = 20;

    private final AcademicClient academicClient;
    private final ChatService chatService;
    private final InvitationService invitationService;
    private final String serviceKey;

    public AcademicSyncService(AcademicClient academicClient,
                               ChatService chatService,
                               InvitationService invitationService,
                               @Value("${internal.service-key}") String serviceKey) {
        this.academicClient = academicClient;
        this.chatService = chatService;
        this.invitationService = invitationService;
        this.serviceKey = serviceKey;
    }

    /**
     * @param adminId người chạy đồng bộ, đứng tên người gửi lời mời
     * @return thống kê của lần chạy này
     */
    public SyncReport sync(@NonNull Long adminId) {
        final RostersView rosters = academicClient.rosters(serviceKey);
        final Tally tally = new Tally();

        // lớp: sinh viên của lớp và giảng viên chủ nhiệm
        for (ClassRoster cls : orEmpty(rosters.classes())) {
            final Set<Long> members = new LinkedHashSet<>(orEmpty(cls.studentUserIds()));
            if (cls.advisorUserId() != null) {
                members.add(cls.advisorUserId());
            }
            syncGroup(adminId, ChatCategory.CLASS, "class:" + cls.classCode(),
                    cls.className(), cls.classCode(), members, tally);
        }

        // môn: sinh viên của các lớp có môn này và giảng viên dạy môn này
        for (SubjectRoster subject : orEmpty(rosters.subjects())) {
            final Set<Long> members = new LinkedHashSet<>(orEmpty(subject.studentUserIds()));
            members.addAll(orEmpty(subject.lecturerUserIds()));
            syncGroup(adminId, ChatCategory.SUBJECT, "subject:" + subject.subjectCode(),
                    subject.subjectName(), subject.subjectCode(), members, tally);
        }

        // khoa: sinh viên và giảng viên chủ nhiệm của các lớp thuộc khoa
        for (FacultyRoster faculty : orEmpty(rosters.faculties())) {
            syncGroup(adminId, ChatCategory.FACULTY, "faculty:" + faculty.facultyCode(),
                    faculty.facultyName(), faculty.facultyCode(),
                    new LinkedHashSet<>(orEmpty(faculty.userIds())), tally);
        }

        log.info("academic sync: created={}, invitations={}, alreadyMember={}, skipped={}, ambiguous={}",
                tally.created, tally.invitationsSent, tally.alreadyMember, tally.skipped,
                orEmpty(rosters.ambiguous()).size());

        return new SyncReport(
                tally.created,
                tally.invitationsSent,
                tally.alreadyMember,
                tally.skipped,
                orEmpty(rosters.ambiguous())
        );
    }

    private void syncGroup(Long adminId, ChatCategory category, String academicKey, String title,
                           String fallbackTitle, Set<Long> memberIds, Tally tally) {
        final String fullName = (title == null || title.isBlank()) ? fallbackTitle : title.strip();
        final int membersLimit = Math.max(MIN_MEMBERS_LIMIT, memberIds.size());

        GroupChat chat = chatService.findAcademicChat(academicKey).orElse(null);
        if (chat == null) {
            try {
                chat = chatService.createAcademicChat(
                        category,
                        academicKey,
                        truncate(fullName, NAME_LIMIT),
                        fullName.length() > NAME_LIMIT ? truncate(fullName, DESCRIPTION_LIMIT) : null,
                        membersLimit);
                tally.created++;
            } catch (DuplicateKeyException e) {
                // đồng bộ khác vừa tạo nhóm này: đọc lại, không tính là tạo mới
                chat = chatService.findAcademicChat(academicKey)
                        .orElseThrow(() -> new ServiceErrorException(e));
            }
        } else if (chat.getMembersLimit() < memberIds.size()) {
            // nhóm cũ có thể đã đầy theo danh sách mới: nâng giới hạn để lời mời chấp nhận được
            chat.setMembersLimit(memberIds.size());
            chatService.updateChat(chat);
        }

        for (Long userId : memberIds) {
            if (chatService.isMember(userId, chat)) {
                tally.alreadyMember++;
            } else if (invitationService.inviteFromRoster(adminId, chat, userId)) {
                tally.invitationsSent++;
            } else {
                tally.skipped++;
            }
        }
    }

    private static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit).strip();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static final class Tally {
        int created;
        int invitationsSent;
        int alreadyMember;
        int skipped;
    }
}
