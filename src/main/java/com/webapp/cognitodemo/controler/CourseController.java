package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.course.CourseRequest;
import com.webapp.cognitodemo.entity.course.CourseReviewRequest;
import com.webapp.cognitodemo.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

import java.util.Map;

@Tag(name = "Courses", description = "Course catalogue")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "Get course types and all courses")
    @GetMapping
    public ResponseEntity<?> getCatalog() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courseService.getCatalog()
        ));
    }

    @Operation(summary = "Get a single course by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.getCourseById(id)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Create a new course")
    @PostMapping
    public ResponseEntity<?> createCourse(@Valid @RequestBody CourseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", courseService.createCourse(request)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Update an existing course by ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody CourseRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.updateCourse(id, request)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Delete a course by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course '" + id + "' deleted successfully"
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get reviews for a course")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getReviews(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courseService.getReviewsForCourse(id)
        ));
    }

    @Operation(summary = "Redirect to presigned S3 URL for course banner image")
    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImage(@PathVariable String id) {
        try {
            String presignedUrl = courseService.getImagePresignedUrl(id);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, presignedUrl)
                    .build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Upload a banner image for a course to S3")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.uploadCourseImage(id, file)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Image upload failed: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Redirect to a playable URL (S3 presigned or static) for a lesson's video")
    @GetMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/video")
    public ResponseEntity<?> getLessonVideo(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex) {
        try {
            String url = courseService.getLessonVideoUrl(id, moduleIndex, lessonIndex);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Redirect to a viewable URL (S3 presigned or static) for a lesson's PDF")
    @GetMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/pdf")
    public ResponseEntity<?> getLessonPdf(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex) {
        try {
            String url = courseService.getLessonPdfUrl(id, moduleIndex, lessonIndex);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * Unlike the video/pdf endpoints, this does NOT redirect — it returns the
     * resolved URL as JSON. The frontend needs the raw URL string to embed
     * into an Office Online viewer link (view.officeapps.live.com), since a
     * browser can't render a .docx by navigating straight to it the way it
     * can a video or a PDF.
     */
    @Operation(summary = "Get a viewable URL (S3 presigned or static) for a lesson's Word document")
    @GetMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/doc")
    public ResponseEntity<?> getLessonDoc(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex) {
        try {
            String url = courseService.getLessonDocUrl(id, moduleIndex, lessonIndex);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("url", url)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Upload a video for a specific curriculum lesson to S3")
    @PostMapping(value = "/{id}/lessons/{moduleIndex}/{lessonIndex}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLessonVideo(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.uploadLessonVideo(id, moduleIndex, lessonIndex, file)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Video upload failed: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Link a lesson to a video that already exists in S3, without re-uploading")
    @PostMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/video-key")
    public ResponseEntity<?> setLessonVideoKey(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex,
            @RequestBody Map<String, String> body) {
        try {
            String videoKey = body.get("videoKey");
            if (videoKey == null || videoKey.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "videoKey is required"
                ));
            }
            Map<String, Object> data = courseService.setLessonVideoKey(id, moduleIndex, lessonIndex, videoKey);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video linked to module " + (moduleIndex + 1) + ", lesson " + (lessonIndex + 1),
                    "data", data
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Link a lesson to a PDF that already exists in S3, without re-uploading")
    @PostMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/pdf-key")
    public ResponseEntity<?> setLessonPdfKey(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex,
            @RequestBody Map<String, String> body) {
        try {
            String pdfKey = body.get("pdfKey");
            if (pdfKey == null || pdfKey.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "pdfKey is required"
                ));
            }
            Map<String, Object> data = courseService.setLessonPdfKey(id, moduleIndex, lessonIndex, pdfKey);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "PDF linked to module " + (moduleIndex + 1) + ", lesson " + (lessonIndex + 1),
                    "data", data
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Link a lesson to a Word document that already exists in S3, without re-uploading")
    @PostMapping("/{id}/lessons/{moduleIndex}/{lessonIndex}/doc-key")
    public ResponseEntity<?> setLessonDocKey(
            @PathVariable String id,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex,
            @RequestBody Map<String, String> body) {
        try {
            String docKey = body.get("docKey");
            if (docKey == null || docKey.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "docKey is required"
                ));
            }
            Map<String, Object> data = courseService.setLessonDocKey(id, moduleIndex, lessonIndex, docKey);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document linked to module " + (moduleIndex + 1) + ", lesson " + (lessonIndex + 1),
                    "data", data
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Post a review for a course")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable String id,
            @Valid @RequestBody CourseReviewRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", courseService.createReview(id, request)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}