package com.groom.cookiehouse.controller;

import com.groom.cookiehouse.common.dto.BaseResponse;
import com.groom.cookiehouse.config.jwt.JwtService;
import com.groom.cookiehouse.config.resolver.UserId;
import com.groom.cookiehouse.controller.dto.response.mission.MissionResponseDto;
import com.groom.cookiehouse.exception.SuccessCode;
import com.groom.cookiehouse.service.mission.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/missions")
public class MissionController {

    private final MissionService missionService;
    private final JwtService jwtService;

    @GetMapping("/today-mission")
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<MissionResponseDto> getTodayMission(HttpServletRequest request) {
        Long userId = Long.parseLong(jwtService.getJwtContents(request.getHeader("Authorization")));
        final MissionResponseDto data = missionService.getTodayMission(userId);
        return BaseResponse.success(SuccessCode.GET_MISSION_SUCCESS, data);
    }

}
