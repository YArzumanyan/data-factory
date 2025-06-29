package cz.cuni.mff.df_manager.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for interacting with the artifact repository.
 */
public interface ArtifactRepositoryService {

    /**
     * Uploads an artifact file to the artifact repository.
     *
     * @param file The artifact file to upload
     * @return The artifact ID assigned by the repository
     */
    String uploadArtifact(MultipartFile file);

    /**
     * Uploads multiple artifact files to the artifact repository.
     *
     * @param files The list of artifact files to upload
     * @return The list of artifact IDs assigned by the repository
     */
    List<String> uploadArtifacts(List<MultipartFile> files);
}
