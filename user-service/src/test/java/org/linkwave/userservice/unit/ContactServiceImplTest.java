package org.linkwave.userservice.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.entity.ContactEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.ContactRepository;
import org.linkwave.userservice.service.ContactService;
import org.linkwave.userservice.service.impl.ContactServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.linkwave.userservice.entity.RoleEntity.Roles.USER;
import static org.linkwave.userservice.utils.UsersUtils.generateUsers;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    private ModelMapper modelMapper;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        contactService = new ContactServiceImpl(contactRepository, modelMapper);
    }

    @Test
    @DisplayName("Should find contacts by username with offset and limit")
    void shouldFindContactsByUsernameWithOffsetAndLimit() {

        final var userId = 1L;
        final var role = RoleEntity.builder()
                .id(1)
                .name(USER.getValue())
                .build();

        final var usersCount = 10;
        final int offset = 8;
        final int limit = 10;

        final var searchUsername = "toxic";
        final var searchUsernamePattern = "toxic%";

        final List<UserEntity> users = generateUsers(usersCount, searchUsername, role);
        final List<ContactEntity> contacts = users.stream()
                .map(user -> ContactEntity.builder()
                        .ownerId(userId)
                        .user(user)
                        .alias("Contact")
                        .build())
                .toList();

        final List<ContactDto> expectedContacts = List.of(
                modelMapper.map(contacts.get(8), ContactDto.class),
                modelMapper.map(contacts.get(9), ContactDto.class)
        );

        when(contactRepository.getContactsByUsernameStartsWith(userId, searchUsernamePattern)).thenReturn(contacts);

        final Pair<Integer, List<ContactDto>> result = contactService.getContactsByUsername(userId, searchUsername, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.getFirst()).isEqualTo(users.size());
        assertThat(result.getSecond()).isNotEmpty();
        assertThat(result.getSecond()).isEqualTo(expectedContacts);
    }

}
