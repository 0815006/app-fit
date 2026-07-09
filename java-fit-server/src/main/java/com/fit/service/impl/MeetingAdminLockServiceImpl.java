package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit.entity.MeetingAdminLock;
import com.fit.mapper.MeetingAdminLockMapper;
import com.fit.service.MeetingAdminLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class MeetingAdminLockServiceImpl extends ServiceImpl<MeetingAdminLockMapper, MeetingAdminLock> implements MeetingAdminLockService {

    private static final int MAX_SLOT = 20;

    @Override
    public MeetingAdminLock createLock(MeetingAdminLock lock) {
        // 参数校验
        validateLockSlots(lock.getStartSlot(), lock.getEndSlot());

        // 冲突检测：检查目标日期同房间是否已有重叠征用
        List<MeetingAdminLock> existing = getRoomLocks(lock.getRoomId(), lock.getLockDate());
        for (MeetingAdminLock ex : existing) {
            if (slotsOverlap(lock.getStartSlot(), lock.getEndSlot(), ex.getStartSlot(), ex.getEndSlot())) {
                throw new RuntimeException("征用时段 [" + lock.getStartSlot() + "-" + lock.getEndSlot()
                        + ") 与已有征用冲突（原因：" + ex.getReason() + "）");
            }
        }

        save(lock);
        log.info("Admin lock created: roomId={}, date={}, slots=[{},{}), reason={}",
                lock.getRoomId(), lock.getLockDate(), lock.getStartSlot(), lock.getEndSlot(), lock.getReason());
        return lock;
    }

    @Override
    public void releaseLock(String id) {
        if (!removeById(id)) {
            throw new RuntimeException("征用记录不存在");
        }
        log.info("Admin lock released: id={}", id);
    }

    @Override
    public List<MeetingAdminLock> getRoomLocks(String roomId, LocalDate date) {
        LambdaQueryWrapper<MeetingAdminLock> qw = new LambdaQueryWrapper<>();
        qw.eq(MeetingAdminLock::getRoomId, roomId)
          .eq(MeetingAdminLock::getLockDate, date)
          .orderByAsc(MeetingAdminLock::getStartSlot);
        return list(qw);
    }

    // ── helpers ──

    static void validateLockSlots(Integer startSlot, Integer endSlot) {
        if (startSlot == null || endSlot == null) {
            throw new RuntimeException("时段参数不能为空");
        }
        if (startSlot < 0 || startSlot >= MAX_SLOT) {
            throw new RuntimeException("起始时段无效，范围 [0, " + (MAX_SLOT - 1) + "]");
        }
        if (endSlot <= startSlot || endSlot > MAX_SLOT) {
            throw new RuntimeException("结束时段必须大于起始且 ≤ " + MAX_SLOT);
        }
    }

    static boolean slotsOverlap(int a1, int a2, int b1, int b2) {
        return a1 < b2 && b1 < a2;
    }
}
