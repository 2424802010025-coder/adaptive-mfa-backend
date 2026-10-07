package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findByUser_UserId(Integer userId);

    // Bổ sung: Tìm thiết bị cụ thể theo userId và deviceName
    Optional<Device> findByUser_UserIdAndDeviceName(Integer userId, String deviceName);

    // Bổ sung: Lấy danh sách các thiết bị đã được gắn nhãn Tin tưởng (Trusted) của User
    List<Device> findByUser_UserIdAndIsTrustedTrue(Integer userId);
}