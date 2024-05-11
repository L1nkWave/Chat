package org.linkwave.userservice.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.NewContactRequest;
import org.linkwave.userservice.entity.ContactEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.ContactRepository;
import org.linkwave.userservice.service.ContactService;
import org.linkwave.userservice.service.UserService;
import org.linkwave.userservice.service.impl.ContactServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.linkwave.userservice.entity.RoleEntity.Roles.USER;
import static org.linkwave.userservice.utils.UsersUtils.generateUsers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserService userService;

    private ModelMapper modelMapper;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        contactService = new ContactServiceImpl(userService, contactRepository, modelMapper);
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

        final var search = "toxic";
        final var searchPattern = "%toxic%";

        final List<UserEntity> users = generateUsers(usersCount, search, role);
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

        when(contactRepository.getContactsByUsernameNameAliasContains(userId, searchPattern)).thenReturn(contacts);

        final Pair<Integer, List<ContactDto>> result = contactService.getContactsBySearch(userId, search, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.getFirst()).isEqualTo(users.size());
        assertThat(result.getSecond()).isNotEmpty();
        assertThat(result.getSecond()).isEqualTo(expectedContacts);
    }

    @Test
    @DisplayName("Should add contact when not added already")
    void Should_AddContact_When_NotAddedAlready() {
        final Long userId = 1L;
        final Long newContactUserId = 2L;
        final var newContact = new NewContactRequest(newContactUserId, "Alias");

        when(contactRepository.findContactPair(userId, newContactUserId)).thenReturn(Optional.empty());

        contactService.addContact(userId, newContact);
        verify(contactRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw exception when add contact with invalid user id")
    void Should_ThrowException_When_AddContactWithInvalidUserId() {
        final Long userId = 1L;
        final Long newContactUserId = 1L;
        final var newContact = new NewContactRequest(newContactUserId, "Alias");

        assertThrows(UnacceptableRequestDataException.class, () -> contactService.addContact(userId, newContact));
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when add already added contact")
    void Should_ThrowException_When_AddAlreadyAddedContact() {
        final Long userId = 1L;
        final Long newContactUserId = 2L;
        final var newContact = new NewContactRequest(newContactUserId, "Alias");

        when(contactRepository.findContactPair(userId, newContactUserId)).thenReturn(Optional.of(ContactEntity.builder().build()));

        assertThrows(UnacceptableRequestDataException.class, () -> contactService.addContact(userId, newContact));
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should remove contact when exists")
    void Should_RemoveContact_When_Exists() {
        final Long userId = 1L;
        final Long contactUserId = 2L;

        final var existingContact = ContactEntity.builder()
                .id(1L)
                .alias("Alias")
                .ownerId(userId)
                .user(UserEntity.builder().build())
                .build();

        when(contactRepository.findContactPair(userId, contactUserId)).thenReturn(Optional.of(existingContact));

        contactService.removeContact(userId, contactUserId);
        verify(contactRepository, times(1)).delete(existingContact);
    }

    @Test
    @DisplayName("Should throw exception when remove non existing contact")
    void Should_ThrowException_When_RemoveNonExistingContact() {
        final Long userId = 1L;
        final Long contactUserId = 2L;

        when(contactRepository.findContactPair(userId, contactUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contactService.removeContact(userId, contactUserId));
        verify(contactRepository, times(0)).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when remove contact with invalid contact id")
    void Should_ThrowException_When_RemoveContactWithInvalidContactId() {
        final Long userId = 1L;
        final Long contactUserId = 1L;

        assertThrows(UnacceptableRequestDataException.class, () -> contactService.removeContact(userId, contactUserId));
        verify(contactRepository, times(0)).delete(any());
    }

}
