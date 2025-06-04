package cz.cuni.mff.df_manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request model for uploading artifacts (datasets or plugins).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactUploadRequest {
    private MultipartFile file;
    private String title;
    private String description;
}