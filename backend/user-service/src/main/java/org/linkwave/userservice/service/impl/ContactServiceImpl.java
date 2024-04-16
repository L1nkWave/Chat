package org.linkwave.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.NewContactRequest;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.entity.ContactEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.ContactRepository;
import org.linkwave.userservice.service.ContactService;
import org.linkwave.userservice.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final UserService userService;
    private final ContactRepository contactRepository;
    private final ModelMapper modelMapper;

    @Override
    public Pair<Integer, List<ContactDto>> getContactsByUsername(Long userId, String username, int offset, int limit) {

        log.debug("-> getContactsUsername(): username = {}, offset = {}, limit = {}", username, offset, limit);

        // convert to SQL pattern
        final var usernamePattern = username + "%";

        // find contacts by pattern
        final List<ContactEntity> contacts = contactRepository.getContactsByUsernameStartsWith(userId, usernamePattern);

        // cut up & map contacts
        final List<ContactDto> mappedContacts = contacts.stream()
                .skip(offset)
                .limit(limit)
                .map(contact -> ContactDto.builder()
                        .user(modelMapper.map(contact.getUser(), UserDto.class))
                        .addedAt(contact.getAddedAt())
                        .alias(contact.getAlias())
                        .build()
                )
                .toList();

        return Pair.of(contacts.size(), mappedContacts);
    }

    @Transactional
    @Override
    public void addContact(@NonNull Long initiatorId, @NonNull NewContactRequest newContact) {
        if (initiatorId.equals(newContact.getUserId())) {
            throw new UnacceptableRequestDataException("Invalid user id");
        }

        if (contactRepository.findContactPair(initiatorId, newContact.getUserId()).isPresent()) {
            throw new UnacceptableRequestDataException("Contact already added");
        }

        final UserEntity newUserContact = userService.findById(newContact.getUserId());
        final var contact = ContactEntity.builder()
                .ownerId(initiatorId)
                .user(newUserContact)
                .alias(newContact.getAlias())
                .build();
        contactRepository.save(contact);
    }

    @Transactional
    @Override
    public void removeContact(@NonNull Long initiatorId, Long contactId) {
        if (initiatorId.equals(contactId)) {
            throw new UnacceptableRequestDataException("Invalid user id");
        }

        final Optional<ContactEntity> contactPair = contactRepository.findContactPair(initiatorId, contactId);
        if (contactPair.isEmpty()) {
            throw new ResourceNotFoundException("Contact not found");
        }
        contactRepository.delete(contactPair.get());
    }

}
