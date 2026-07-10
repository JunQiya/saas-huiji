package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.EmployeeDto;
import com.huiji.security.PreAllowed;
import com.huiji.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 员工接口 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String role) {
        return Result.success(employeeService.list(storeId, role));
    }

    @PostMapping
    @PreAllowed({"TENANT_ADMIN"})
    public Result<Map<String, Object>> create(@Valid @RequestBody EmployeeDto.EmployeeRequest req) {
        return Result.success(employeeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAllowed({"TENANT_ADMIN"})
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody EmployeeDto.EmployeeUpdate req) {
        return Result.success(employeeService.update(id, req));
    }

    @PutMapping("/{id}/password")
    @PreAllowed({"TENANT_ADMIN"})
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody EmployeeDto.PasswordReset req) {
        employeeService.resetPassword(id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAllowed({"TENANT_ADMIN"})
    public Result<Void> disable(@PathVariable Long id) {
        employeeService.disable(id);
        return Result.success();
    }

    @GetMapping("/{id}/performance")
    public Result<List<Map<String, Object>>> performance(@PathVariable Long id) {
        return Result.success(employeeService.performance(id));
    }
}
