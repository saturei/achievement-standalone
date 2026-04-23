package com.example.achievement.util;

import com.example.achievement.entity.Achievement;
import com.example.achievement.repository.AchievementRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VersionGenerator {

    private final AchievementRepository achievementRepository;

    public VersionGenerator(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    public String generateManagementVersion(String achievementName, String productExternalVersion) {
        if (productExternalVersion == null || productExternalVersion.isEmpty()) {
            return "V1.0.0";
        }
        
        Optional<Achievement> latestAchievement = achievementRepository
                .findTopByNameAndProductExternalVersionOrderByCreatedAtDesc(achievementName, productExternalVersion);

        if (!latestAchievement.isPresent()) {
            return "V1.0.0";
        }

        String currentVersion = latestAchievement.get().getVersion();
        return incrementVersion(currentVersion);
    }

    public String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "V1.0.0";
        }
        
        String versionNum = version.replace("V", "");
        String[] parts = versionNum.split("\\.");

        int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        patch++;
        if (patch > 99) {
            patch = 0;
            minor++;
            if (minor > 99) {
                minor = 0;
                major++;
            }
        }

        return String.format("V%d.%d.%d", major, minor, patch);
    }
}
