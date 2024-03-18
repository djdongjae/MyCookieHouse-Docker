package com.groom.cookiehouse.controller;

import com.groom.cookiehouse.common.dto.BaseResponse;
import com.groom.cookiehouse.config.jwt.JwtService;
import com.groom.cookiehouse.controller.dto.response.user.UserResponseDto;
import com.groom.cookiehouse.exception.SuccessCode;
import com.groom.cookiehouse.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<UserResponseDto> getUser(HttpServletRequest request) {
        Long userId = Long.parseLong(jwtService.getJwtContents(request.getHeader("Authorization")));
        final UserResponseDto data = userService.getUser(userId);
        return BaseResponse.success(SuccessCode.GET_USER_INFO_SUCCESS, data);
    }

}
