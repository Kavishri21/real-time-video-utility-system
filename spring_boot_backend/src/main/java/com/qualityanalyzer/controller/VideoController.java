package com.qualityanalyzer.controller;

import com.qualityanalyzer.dto.FinalResponse;
import com.qualityanalyzer.dto.VideoRequest;
import com.qualityanalyzer.service.AnalyzerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*") // Allow extension to hit API
public class VideoController {

    private final AnalyzerService analyzerService;

    public VideoController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @PostMapping("/analyzeVideo")
    public ResponseEntity<FinalResponse> analyzeVideo(@RequestBody VideoRequest request) {
        if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        FinalResponse response = analyzerService.analyzeVideo(request.getVideoId());
        return ResponseEntity.ok(response);
    }
}
