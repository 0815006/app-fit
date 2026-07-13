package com.fit.controller;

import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.UserFavoriteDish;
import com.fit.service.FavoriteDishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/favorite-dish")
@RequiredArgsConstructor
public class FavoriteDishController {

    private final FavoriteDishService service;

    /**
     * 收藏/取消收藏（Toggle）
     */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@RequestBody Map<String, String> body) {
        String empNo = EmpContext.getEmpNo();
        String dishName = body.get("dishName");
        if (dishName == null || dishName.isBlank()) {
            return Result.error("dishName 不能为空");
        }
        Map<String, Object> data = service.toggle(empNo, dishName.trim());
        return Result.success(data);
    }

    /**
     * 批量查询收藏状态
     */
    @GetMapping("/check")
    public Result<List<String>> check(@RequestParam String dishNames) {
        String empNo = EmpContext.getEmpNo();
        if (dishNames == null || dishNames.isBlank()) {
            return Result.success(List.of());
        }
        List<String> nameList = Arrays.stream(dishNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> data = service.check(empNo, nameList);
        return Result.success(data);
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/list")
    public Result<List<UserFavoriteDish>> list() {
        String empNo = EmpContext.getEmpNo();
        List<UserFavoriteDish> data = service.list(empNo);
        return Result.success(data);
    }

    /**
     * 删除单个收藏
     */
    @DeleteMapping("/{dishName}")
    public Result<Void> delete(@PathVariable String dishName) {
        String empNo = EmpContext.getEmpNo();
        service.delete(empNo, dishName);
        return Result.success();
    }
}
