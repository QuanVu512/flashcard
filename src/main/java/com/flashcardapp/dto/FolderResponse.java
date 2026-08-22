package com.flashcardapp.dto;

import com.flashcardapp.entity.Folder;

import java.time.LocalDateTime;
import java.util.UUID;

public record FolderResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt
) {

    public static FolderResponse from(Folder folder) {
        if (folder == null) {
            return null;
        }
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getDescription(),
                folder.getCreatedAt()
        );
    }
}
