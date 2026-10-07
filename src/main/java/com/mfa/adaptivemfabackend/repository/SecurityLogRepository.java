package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    List<SecurityLog> findTop50ByOrderByTimestampDesc();

    // Bổ sung: Kiểm tra xem User đã từng đăng nhập thành công từ IP này chưa (dùng cho TrustScoreService)
    boolean existsByUser_UserIdAndIpAddressAndStatus(Integer userId, String ipAddress, String status);

    // Bổ sung: Kiểm tra xem User đã từng đăng nhập thành công từ Thiết bị này chưa
    boolean existsByUser_UserIdAndDeviceNameAndStatus(Integer userId, String deviceName, String status);

    // Bổ sung: Đếm số lần đăng nhập thất bại gần đây để phát hiện tấn công dò quét mật khẩu
    long countByUser_UserIdAndStatusAndTimestampAfter(Integer userId, String status, LocalDateTime after);

    // Bổ sung: Lấy 10 nhật ký gần nhất của một User để phân tích hành vi
    List<SecurityLog> findTop10ByUser_UserIdOrderByTimestampDesc(Integer userId);
}