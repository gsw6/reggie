package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @CacheEvict(value="setmealCache",allEntries = true)
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmeal){
        setmealService.saveWithDish(setmeal);
        return R.success("套餐添加成功");
    }
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        Page<Setmeal> setmealPage = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(name!=null,Setmeal::getName,name);
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(setmealPage,lambdaQueryWrapper);
        BeanUtils.copyProperties(setmealPage,setmealDtoPage,"records");
        List<Setmeal> list = setmealPage.getRecords();
        List<SetmealDto> list1 = list.stream().map((item)->{
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(list1);
        return R.success(setmealDtoPage);
    }
    @PostMapping("/status/{status}")
    public R<String> status(@RequestParam List<Long> ids, HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.info("ids:{}",ids);
        if(requestURI.length()>0) {
            requestURI = requestURI.substring(requestURI.length() - 1);
        }
        int i = Integer.parseInt(requestURI);
        Setmeal setmeal = new Setmeal();
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(i==1) {
            setmeal.setStatus(1);
            lambdaQueryWrapper.in(Setmeal::getId,ids);
            lambdaQueryWrapper.eq(Setmeal::getStatus,0);
            setmealService.update(setmeal,lambdaQueryWrapper);
        }
        else{
            setmeal.setStatus(0);
            lambdaQueryWrapper.in(Setmeal::getId,ids);
            lambdaQueryWrapper.eq(Setmeal::getStatus,1);
            setmealService.update(setmeal,lambdaQueryWrapper);
        }
        return R.success("状态修改成功");
    }

    @CacheEvict(value="setmealCache",allEntries = true)
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids:{}",ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }
    @Cacheable(value = "setmealCache",key="#setmeal.categoryId+'_'+#setmeal.status")
    @GetMapping("list")
    public R<List<SetmealDto>> list(Setmeal setmeal){
        log.info(setmeal.toString());
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        lambdaQueryWrapper.eq(Setmeal::getStatus,1);
        lambdaQueryWrapper.orderByAsc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(lambdaQueryWrapper);
        List<SetmealDto> setmealDtos = list.stream().map((item)->{
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            Long dishId = item.getId();
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId,dishId);
            setmealDto.setSetmealDishes(setmealDishService.list(queryWrapper));
            return setmealDto;
        }).collect(Collectors.toList());
        return R.success(setmealDtos);
    }
}
