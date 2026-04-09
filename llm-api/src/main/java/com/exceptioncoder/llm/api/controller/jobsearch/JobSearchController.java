package com.exceptioncoder.llm.api.controller.jobsearch;

import com.exceptioncoder.llm.api.dto.JobPostingDTO;
import com.exceptioncoder.llm.api.dto.JobSearchRequestDTO;
import com.exceptioncoder.llm.api.dto.JobSearchResponseDTO;
import com.exceptioncoder.llm.application.service.JobVectorService;
import com.exceptioncoder.llm.application.usecase.JobSearchUseCase;
import com.exceptioncoder.llm.domain.model.JobPosting;
import com.exceptioncoder.llm.domain.model.VectorSearchRequest;
import com.exceptioncoder.llm.domain.model.VectorSearchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位检索控制器
 */
@RestController
@RequestMapping("/api/job-search")
public class JobSearchController {

    private final JobSearchUseCase jobSearchUseCase;
    private final JobVectorService jobVectorService;

    public JobSearchController(JobSearchUseCase jobSearchUseCase,
                              JobVectorService jobVectorService) {
        this.jobSearchUseCase = jobSearchUseCase;
        this.jobVectorService = jobVectorService;
    }

    /**
     * 岗位相似度检索
     */
    @PostMapping("/search")
    public ResponseEntity<JobSearchResponseDTO> search(@RequestBody JobSearchRequestDTO requestDTO) {
        VectorSearchRequest request = convertToSearchRequest(requestDTO);
        VectorSearchResult result = jobSearchUseCase.execute(request);
        JobSearchResponseDTO responseDTO = convertToResponseDTO(result);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 存储岗位到向量库
     */
    @PostMapping("/store")
    public ResponseEntity<String> storeJob(@RequestBody JobPostingDTO jobPostingDTO) {
        JobPosting jobPosting = convertToJobPosting(jobPostingDTO);
        jobVectorService.storeJobPosting(jobPosting);
        return ResponseEntity.ok("岗位存储成功");
    }

    /**
     * 批量存储岗位
     */
    @PostMapping("/batch-store")
    public ResponseEntity<String> batchStoreJobs(@RequestBody List<JobPostingDTO> jobPostingDTOs) {
        List<JobPosting> jobPostings = jobPostingDTOs.stream()
            .map(this::convertToJobPosting)
            .collect(Collectors.toList());

        jobVectorService.batchStoreJobPostings(jobPostings);
        return ResponseEntity.ok("批量存储成功，共 " + jobPostings.size() + " 条");
    }

    /**
     * 删除岗位向量
     */
    @DeleteMapping("/{postingId}")
    public ResponseEntity<String> deleteJob(@PathVariable Long postingId) {
        jobVectorService.deleteJobPosting(postingId);
        return ResponseEntity.ok("岗位删除成功");
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean healthy = jobVectorService.checkHealth();
        if (healthy) {
            return ResponseEntity.ok("向量库连接正常");
        } else {
            return ResponseEntity.status(503).body("向量库连接异常");
        }
    }

    private VectorSearchRequest convertToSearchRequest(JobSearchRequestDTO dto) {
        VectorSearchRequest request = new VectorSearchRequest(dto.getQueryText(), dto.getTopK());

        if (dto.getJobFamily() != null) {
            request.addFilter("job_family", dto.getJobFamily());
        }
        if (dto.getLevel() != null) {
            request.addFilter("level", dto.getLevel());
        }
        if (dto.getCity() != null) {
            request.addFilter("city", dto.getCity());
        }
        if (dto.getFilters() != null) {
            dto.getFilters().forEach(request::addFilter);
        }

        return request;
    }

    private JobSearchResponseDTO convertToResponseDTO(VectorSearchResult result) {
        List<JobSearchResponseDTO.ScoredJobDTO> scoredJobs = result.getResults().stream()
            .map(this::convertToScoredJobDTO)
            .collect(Collectors.toList());

        return new JobSearchResponseDTO(scoredJobs, result.getTotalCount());
    }

    private JobSearchResponseDTO.ScoredJobDTO convertToScoredJobDTO(VectorSearchResult.ScoredJobPosting scored) {
        JobSearchResponseDTO.ScoredJobDTO dto = new JobSearchResponseDTO.ScoredJobDTO();
        JobPosting job = scored.getJobPosting();

        dto.setPostingId(job.getPostingId());
        dto.setJobFamily(job.getJobFamily());
        dto.setLevel(job.getLevel());
        dto.setSkills(job.getSkills());
        dto.setExperience(job.getExperience());
        dto.setEducation(job.getEducation());
        dto.setDomain(job.getDomain());
        dto.setResponsibility(job.getResponsibility());
        dto.setCity(job.getCity());
        dto.setPostTime(job.getPostTime());
        dto.setDupGroupId(job.getDupGroupId());
        dto.setScore(scored.getScore());

        return dto;
    }

    private JobPosting convertToJobPosting(JobPostingDTO dto) {
        JobPosting jobPosting = new JobPosting();
        jobPosting.setPostingId(dto.getPostingId());
        jobPosting.setJobFamily(dto.getJobFamily());
        jobPosting.setLevel(dto.getLevel());
        jobPosting.setSkills(dto.getSkills());
        jobPosting.setExperience(dto.getExperience());
        jobPosting.setEducation(dto.getEducation());
        jobPosting.setDomain(dto.getDomain());
        jobPosting.setResponsibility(dto.getResponsibility());
        jobPosting.setCity(dto.getCity());
        jobPosting.setPostTime(dto.getPostTime());
        jobPosting.setDupGroupId(dto.getDupGroupId());
        return jobPosting;
    }
}
