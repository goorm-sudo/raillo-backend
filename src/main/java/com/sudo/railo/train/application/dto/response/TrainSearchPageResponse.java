package com.sudo.railo.train.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "열차 검색 페이징 응답")
public record TrainSearchPageResponse(
	@Schema(description = "열차 목록")
	List<TrainSearchResponse> content,

	@Schema(description = "현재 페이지 번호", example = "0")
	int currentPage,

	@Schema(description = "페이지 크기", example = "20")
	int pageSize,

	@Schema(description = "전체 요소 수", example = "45")
	long totalElements,

	@Schema(description = "전체 페이지 수", example = "3")
	int totalPages,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext,

	@Schema(description = "이전 페이지 존재 여부", example = "false")
	boolean hasPrevious
) {
	public static TrainSearchPageResponse of(List<TrainSearchResponse> content, int currentPage,
		int pageSize, long totalElements, int totalPages,
		boolean hasNext, boolean hasPrevious) {
		return new TrainSearchPageResponse(content, currentPage, pageSize,
			totalElements, totalPages, hasNext, hasPrevious);
	}
}
