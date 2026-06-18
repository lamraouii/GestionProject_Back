package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KanbanBoardDto {
    private List<TacheResponseDto> todo;
    private List<TacheResponseDto> inProgress;
    private List<TacheResponseDto> done;
}
