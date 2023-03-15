package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
       /* Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        String key ="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        Page<Dish> page1 = new Page(page,pageSize);
        Page<DishDto> page2 = new Page<>();
        LambdaQueryWrapper<Dish> lambdaQuerWrapper = new LambdaQueryWrapper<>();
        lambdaQuerWrapper.like(StringUtils.isNotEmpty(name),Dish::getName,name);
        lambdaQuerWrapper.orderByAsc(Dish::getUpdateTime);
        dishService.page(page1,lambdaQuerWrapper);
        BeanUtils.copyProperties(page1,page2,"records");
        List<Dish> records = page1.getRecords();
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        page2.setRecords(list);
        return R.success(page2);
    }
    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id){
        DishDto dishDto = dishService.getByWithFlavor(id);
        return R.success(dishDto);
    }
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        /* Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        String key ="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("修改菜品成功");
    }
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        log.info(dish.toString());
        List<DishDto> dishDtos=null;
        String key = "dish_"+dish.getCategoryId()+"_"+dish.getStatus();
        dishDtos = (List<DishDto>)redisTemplate.opsForValue().get(key);
        if(dishDtos!=null){
            return R.success(dishDtos);
        }

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        lambdaQueryWrapper.eq(Dish::getStatus,1);
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByAsc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(lambdaQueryWrapper);
        dishDtos = list.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DishFlavor::getDishId,dishId);
            dishDto.setFlavors(dishFlavorService.list(queryWrapper));
            return dishDto;
        }).collect(Collectors.toList());
        redisTemplate.opsForValue().set(key,dishDtos,60, TimeUnit.MINUTES);
        return R.success(dishDtos);
    }
    @PostMapping("/status/{status}")
    public R<String> status(@RequestParam List<Long>ids , HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.info("ids:{}",ids);
        if(requestURI.length()>0) {
            requestURI = requestURI.substring(requestURI.length() - 1);
        }
        int i = Integer.parseInt(requestURI);
        Dish dish = new Dish();
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(i==1) {
            dish.setStatus(1);
            lambdaQueryWrapper.in(Dish::getId,ids);
            lambdaQueryWrapper.eq(Dish::getStatus,0);
            dishService.update(dish,lambdaQueryWrapper);
        }
        else{
            dish.setStatus(0);
            lambdaQueryWrapper.in(Dish::getId,ids);
            lambdaQueryWrapper.eq(Dish::getStatus,1);
            dishService.update(dish,lambdaQueryWrapper);
        }
        return R.success("状态修改成功");
    }
}
