package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hashSalt", "test-salt-for-unit-tests");
    }

    @Test
    void shouldCreateNewAnonymousIdWhenIdentityIsNew() {
        UUID expectedId = UUID.randomUUID();
        IdentityMapping saved = IdentityMapping.builder().anonymousId(expectedId).build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(saved);

        UUID result = service.getOrCreateAnonymousId("newuser@example.com");

        assertEquals(expectedId, result);
        verify(repository).save(any(IdentityMapping.class));
    }

    @Test
    void shouldReturnExistingAnonymousIdWithoutCreatingDuplicate() {
        UUID existingId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(existingId)
                .realIdentity("known@example.com")
                .build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(existing));

        UUID result = service.getOrCreateAnonymousId("known@example.com");

        assertEquals(existingId, result);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldProduceDeterministicHashForSameInput() {
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            IdentityMapping m = inv.getArgument(0);
            return IdentityMapping.builder()
                    .anonymousId(UUID.randomUUID())
                    .identityHash(m.getIdentityHash())
                    .build();
        });

        service.getOrCreateAnonymousId("sameuser@example.com");
        service.getOrCreateAnonymousId("sameuser@example.com");

        verify(repository, times(2)).findByIdentityHash(argThat(h -> h != null && !h.isEmpty()));
    }

    @Test
    void shouldThrowNotFoundWhenResolvingUnknownAnonymousId() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.resolveRealIdentity(unknownId));
    }

    @Test
    void shouldResolveRealIdentityForExistingMapping() {
        UUID anonymousId = UUID.randomUUID();
        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(anonymousId)
                .realIdentity("resolved@example.com")
                .build();

        when(repository.findById(anonymousId)).thenReturn(Optional.of(mapping));

        String result = service.resolveRealIdentity(anonymousId);

        assertEquals("resolved@example.com", result);
    }
}
