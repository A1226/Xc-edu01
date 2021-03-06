package com.xuecheng.api.course;


import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;

@Api(value="课程分类管理",description = "课程分类管理接口",tags = {"课程分类管理"})
public interface CategoryControllerApi {

    @ApiOperation("查询分类")
    CategoryNode findList();
}
