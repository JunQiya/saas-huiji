package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.GameDto;
import com.huiji.entity.Game;
import com.huiji.entity.GamePrize;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端游戏接口 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public Result<List<Game>> list(@RequestParam(required = false) String status) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(gameService.list(tenantId, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Game g = gameService.get(tenantId, id);
        List<GamePrize> prizes = gameService.prizes(tenantId, id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("game", g);
        data.put("prizes", prizes);
        return Result.success(data);
    }

    @PostMapping
    public Result<Game> save(@Valid @RequestBody GameDto.GameRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(gameService.save(tenantId, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        gameService.remove(tenantId, id);
        return Result.success();
    }

    @GetMapping("/{id}/prizes")
    public Result<List<GamePrize>> prizes(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(gameService.prizes(tenantId, id));
    }

    @PostMapping("/{id}/prizes")
    public Result<GamePrize> savePrize(@PathVariable Long id,
                                       @Valid @RequestBody GameDto.PrizeRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(gameService.savePrize(tenantId, id, req));
    }

    @DeleteMapping("/prizes/{prizeId}")
    public Result<Void> deletePrize(@PathVariable Long prizeId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        gameService.removePrize(tenantId, prizeId);
        return Result.success();
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(gameService.stats(tenantId, id));
    }
}
