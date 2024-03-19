package com.groom.cookiehouse.service.mission;

import com.groom.cookiehouse.controller.dto.request.mission.MissionCompleteRequestDto;
import com.groom.cookiehouse.controller.dto.response.mission.CreateMissionCompleteResponseDto;
import com.groom.cookiehouse.controller.dto.response.mission.ReadAllMissionCompleteResponseDto;
import com.groom.cookiehouse.controller.dto.response.mission.ReadMissionCompleteResponseDto;
import com.groom.cookiehouse.domain.mission.Mission;
import com.groom.cookiehouse.domain.mission.MissionComplete;
import com.groom.cookiehouse.domain.user.User;
import com.groom.cookiehouse.exception.ErrorCode;
import com.groom.cookiehouse.exception.model.BadRequestException;
import com.groom.cookiehouse.exception.model.NotFoundException;
import com.groom.cookiehouse.external.client.aws.S3Service;
import com.groom.cookiehouse.repository.MissionCompleteRepository;
import com.groom.cookiehouse.repository.MissionRepository;
import com.groom.cookiehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionCompleteService {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final MissionCompleteRepository missionCompleteRepository;

    private final S3Service s3Service;

    @Transactional
    public CreateMissionCompleteResponseDto createMissionComplete(MissionCompleteRequestDto requestDto, Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));
        List<Mission> missions = missionRepository.findAllByDate(LocalDate.now());


        if (!missionCompleteRepository.existsByUser(user)) {
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
                        .furnitureId((long) furnitureIds[(int)(Math.random()*3)][i-1])
                        .build();
                missionCompleteRepository.save(missionComplete);
            }
        }

        if (!missionCompleteRepository.existsByUserAndMission(user, missions.get(0))) {
            MissionComplete missionComplete = MissionComplete.builder()
                    .image(imageUrl)
                    .content(requestDto.getMissionCompleteContent())
                    .mission(missions.get(0))
                    .user(user)
                    .furnitureId(requestDto.getFurnitureId())
                    .build();
            missionCompleteRepository.save(missionComplete);
            return CreateMissionCompleteResponseDto.of(missionComplete.getId(), missionComplete.getCreatedAt(), missionComplete.getUpdatedAt());
        } else {
            throw new BadRequestException(ErrorCode.ALREADY_MISSION_COMPLETE, ErrorCode.ALREADY_MISSION_COMPLETE.getMessage());
        }

    }

    @Transactional
    public CreateMissionCompleteResponseDto updateMissionComplete(Long userId, Long missionCompleteId, MissionCompleteRequestDto requestDto) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));

        MissionComplete missionComplete = missionCompleteRepository.findById(missionCompleteId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_MISSION_COMPLETE_EXCEPTION, ErrorCode.NOT_FOUND_MISSION_COMPLETE_EXCEPTION.getMessage()));

        s3Service.deleteFile(missionComplete.getImage());
        String imageUrl = s3Service.uploadImage(requestDto.getMissionCompleteImage(), "mission_complete");

        missionComplete.update(imageUrl, requestDto.getMissionCompleteContent(), requestDto.getFurnitureId());
        return CreateMissionCompleteResponseDto.of(missionComplete.getId(), missionComplete.getCreatedAt(), missionComplete.getUpdatedAt());
    }

    public ReadMissionCompleteResponseDto findMissionComplete(Long missionCompleteId) {
        MissionComplete missionComplete = missionCompleteRepository.findById(missionCompleteId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_MISSION_COMPLETE_EXCEPTION, ErrorCode.NOT_FOUND_MISSION_COMPLETE_EXCEPTION.getMessage()));
        return ReadMissionCompleteResponseDto.of(
                missionComplete.getMission().getMessage(),
                missionComplete.getId(),
                missionComplete.getImage(),
                missionComplete.getContent(),
                missionComplete.getMission().getDate(),
                missionComplete.getFurnitureId()
        );
    }

    public ReadAllMissionCompleteResponseDto findAllMissionComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER_EXCEPTION, ErrorCode.NOT_FOUND_USER_EXCEPTION.getMessage()));
        List<MissionComplete> missionCompletes = missionCompleteRepository.findAllByUser(user);

        if (missionCompletes.isEmpty()) {
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
                        .furnitureId((long) furnitureIds[(int)(Math.random()*3)][i-1])
                        .build();
                missionCompleteRepository.save(missionComplete);
            }
        }

        List<ReadMissionCompleteResponseDto> readMissionCompleteResponseDtos = new ArrayList<>();
        for(MissionComplete missionComplete : missionCompletes) {
            readMissionCompleteResponseDtos.add(
                    ReadMissionCompleteResponseDto.of(
                            missionComplete.getMission().getMessage(),
                            missionComplete.getId(),
                            missionComplete.getImage(),
                            missionComplete.getContent(),
                            missionComplete.getMission().getDate(),
                            missionComplete.getFurnitureId()
                    )
            );
        }
        return ReadAllMissionCompleteResponseDto.of(user.getWallpaper(), readMissionCompleteResponseDtos);
    }

}
