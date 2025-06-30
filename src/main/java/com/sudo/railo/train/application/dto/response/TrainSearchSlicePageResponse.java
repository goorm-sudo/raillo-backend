package com.sudo.railo.train.application.dto.response;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "열차 검색 페이징 응답")
public record TrainSearchSlicePageResponse(
	@Schema(description = "열차 목록", example = "[{...}]")
	List<TrainSearchResponse> content,

	@Schema(description = "현재 페이지 번호", example = "0")
	int currentPage,

	@Schema(description = "페이지 크기", example = "20")
	int pageSize,

	@Schema(description = "현재 페이지 요소 수", example = "15")
	int numberOfElements,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext,

	@Schema(description = "이전 페이지 존재 여부", example = "false")
	boolean hasPrevious,

	@Schema(description = "첫 페이지 여부", example = "true")
	boolean first,

	@Schema(description = "마지막 페이지 여부", example = "false")
	boolean last
) {
	/**
	 * Slice 정보와 열차 목록으로 응답 생성
	 */
	public static TrainSearchSlicePageResponse of(List<TrainSearchResponse> trains, Slice<?> slice) {
		return new TrainSearchSlicePageResponse(
			trains,
			slice.getNumber(),
			slice.getSize(),
			slice.getNumberOfElements(),
			slice.hasNext(),
			slice.hasPrevious(),
			slice.isFirst(),
			slice.isLast()
		);
	}

	/**
	 * 빈 결과 응답 생성
	 */
	public static TrainSearchSlicePageResponse empty(Pageable pageable) {
		return new TrainSearchSlicePageResponse(
			Collections.emptyList(),
			pageable.getPageNumber(),
			pageable.getPageSize(),
			0,
			false,  // hasNext
			false,  // hasPrevious
			true,   // first
			true    // last
		);
	}

	/**
	 * 첫 페이지 응답 생성 (hasNext만 확인)
	 */
	public static TrainSearchSlicePageResponse firstPage(List<TrainSearchResponse> trains,
		int pageSize, boolean hasNext) {
		return new TrainSearchSlicePageResponse(
			trains,
			0,              // currentPage
			pageSize,       // pageSize
			trains.size(),  // numberOfElements
			hasNext,        // hasNext
			false,          // hasPrevious (첫 페이지이므로 false)
			true,           // first
			!hasNext        // last (hasNext가 false면 마지막 페이지)
		);
	}
}
