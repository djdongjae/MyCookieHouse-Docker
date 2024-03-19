package com.groom.cookiehouse.service.auth;

import com.groom.cookiehouse.controller.dto.response.auth.TokenResponseDto;
import com.groom.cookiehouse.domain.mission.MissionComplete;
import com.groom.cookiehouse.external.client.aws.S3Service;
import com.groom.cookiehouse.oauth2.ClientRegistration;
import com.groom.cookiehouse.oauth2.ClientRegistrationRepository;
import com.groom.cookiehouse.oauth2.OAuth2Token;
import com.groom.cookiehouse.oauth2.service.OAuth2Service;
import com.groom.cookiehouse.oauth2.userInfo.OAuth2UserInfo;
import com.groom.cookiehouse.config.jwt.JwtService;
import com.groom.cookiehouse.controller.dto.response.auth.SignInResponseDto;
import com.groom.cookiehouse.domain.user.SocialType;
import com.groom.cookiehouse.domain.user.User;
import com.groom.cookiehouse.exception.ErrorCode;
import com.groom.cookiehouse.exception.model.NotFoundException;
import com.groom.cookiehouse.repository.MissionCompleteRepository;
import com.groom.cookiehouse.repository.MissionRepository;
import com.groom.cookiehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;

    private final S3Service s3Service;

    private final Long TOKEN_EXPIRATION_TIME_ACCESS = 100 * 24 * 60 * 60 * 1000L; // 100일
    private final Long TOKEN_EXPIRATION_TIME_REFRESH = 200 * 24 * 60 * 60 * 1000L; // 200일

    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    public final RestTemplate restTemplate;
    private final MissionRepository missionRepository;
    private final MissionCompleteRepository missionCompleteRepository;

    public SignInResponseDto login(String code, String provider, String state) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        OAuth2Service oAuth2Service = new OAuth2Service(restTemplate);
        OAuth2Token oAuth2Token = oAuth2Service.getAccessToken(clientRegistration, code, state);
        OAuth2UserInfo oAuth2UserInfo = oAuth2Service.getUserInfo(clientRegistration, oAuth2Token.getToken());
        return signIn(oAuth2UserInfo, provider);
    }

    @Transactional
    public SignInResponseDto signIn(OAuth2UserInfo oAuth2UserInfo, String provider) {
        SocialType socialType = SocialType.valueOf(provider.toUpperCase());
        String socialId = oAuth2UserInfo.getId();
        String userName = oAuth2UserInfo.getName();
        Boolean isRegistered = userRepository.existsBySocialIdAndSocialType(socialId, socialType);

        if (!isRegistered) {
            User newUser = User.builder()
                    .userName(userName)
                    .socialId(socialId)
                    .socialType(socialType)
                    .build();
            User user = userRepository.save(newUser);

        }

        User user = userRepository.findBySocialIdAndSocialType(socialId, socialType)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));

        String accessToken = jwtService.issuedToken(String.valueOf(user.getId()), TOKEN_EXPIRATION_TIME_ACCESS);
        String refreshToken = jwtService.issuedToken(String.valueOf(user.getId()), TOKEN_EXPIRATION_TIME_REFRESH);
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        boolean isHouseBuilt = true;
        if (user.getHouseName() == null) {
            isHouseBuilt = false;
            int[][] furnitureIds = {
                    {11, 21, 31, 41, 51, 61, 71, 81, 91, 101, 111, 121, 131, 141, 151, 161, 171, 181},
                    {12, 22, 32, 42, 52, 62, 72, 82, 92, 102, 112, 122, 132, 142, 152, 162, 172, 182},
                    {13, 23, 33, 43, 53, 63, 73, 83, 93, 103, 113, 123, 133, 143, 153, 163, 173, 183}
            };

            for (int i = 1; i < 19; i++) {
                MissionComplete missionComplete = MissionComplete.builder()
                        .image("https://s3.ap-northeast-2.amazonaws.com/cookiehouse-image/mission_complete/image/4da8ac24-4f2b-4a0a-b754-1c281f063b83.png2024-03-19T10%3A01%3A56.009893548")
                        .content("쿠하와 함께라면 여러분도 최우수상")
                        .mission(
                                missionRepository.findById((long) i).orElseThrow(
                                        () ->new NotFoundException(ErrorCode.NOT_FOUND_MISSION_EXCEPTION, ErrorCode.NOT_FOUND_MISSION_EXCEPTION.getMessage())
                                ))
                        .user(user)
                        .furnitureId((long) furnitureIds[(int)(Math.random()*3)][i])
                        .build();
                missionCompleteRepository.save(missionComplete);
            }
        }
        return SignInResponseDto.of(user.getId(), user.getUserName(), accessToken, refreshToken, isRegistered, isHouseBuilt);
    }

    @Transactional
    public TokenResponseDto issueToken(String refreshToken) {
        jwtService.verifyToken(refreshToken);

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));

        String newAccessToken = jwtService.issuedToken(String.valueOf(user.getId()), TOKEN_EXPIRATION_TIME_ACCESS);
        String newRefreshToken = jwtService.issuedToken(String.valueOf(user.getId()), TOKEN_EXPIRATION_TIME_REFRESH);

        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);
        return TokenResponseDto.of(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void signOut(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));
        user.updateRefreshToken(null);
    }
    @Transactional
    public void unlink(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));
        List<MissionComplete> missionCompleteList = user.getMissionCompleteList();
        if (missionCompleteList != null) {
            for (MissionComplete missionComplete : missionCompleteList) {
                s3Service.deleteFile(missionComplete.getImage());
            }
        }
        userRepository.deleteById(userId);
    }
}
