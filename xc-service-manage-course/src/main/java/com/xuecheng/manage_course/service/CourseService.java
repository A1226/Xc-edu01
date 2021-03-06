package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.domain.course.response.TeachplanMediaPub;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CourseBaseRepository baseRepository;

    @Autowired
    CourseMarketRepository courseMarketRepository;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Autowired
    CoursePubRepository coursePubRepository;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;

    /**
     * ??????????????????
     * @param courseId
     * @return
     */
    public TeachplanNode findTeachplanList(String courseId){
        TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        return teachplanNode;
    }

    /**
     * ??????????????????
     * @param teachplan
     * @return
     */
    //??????????????????
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        if(teachplan == null ||
                StringUtils.isEmpty(teachplan.getCourseid()) ||
                StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALIDPARAM);
        }
        //??????id
        String courseid = teachplan.getCourseid();
        //???????????????parentId
        String parentid = teachplan.getParentid();
        if(StringUtils.isEmpty(parentid)){
            //???????????????????????????
            parentid = this.getTeachplanRoot(courseid);
        }
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan parentNode = optional.get();
        //??????????????????
        String grade = parentNode.getGrade();
        //?????????
        Teachplan teachplanNew = new Teachplan();
        //??????????????????teachplan???????????????teachplanNew?????????
        BeanUtils.copyProperties(teachplan,teachplanNew);
        teachplanNew.setParentid(parentid);
        teachplanNew.setCourseid(courseid);
        if(grade.equals("1")){
            teachplanNew.setGrade("2");//??????????????????????????????????????????
        }else{
            teachplanNew.setGrade("3");
        }

        teachplanRepository.save(teachplanNew);

        return new ResponseResult(CommonCode.SUCCESS);
    }

    //?????????????????????????????????????????????????????????????????????
    private String getTeachplanRoot(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(!optional.isPresent()){
            return null;
        }
        //????????????
        CourseBase courseBase = optional.get();
        //????????????????????????
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        if(teachplanList == null || teachplanList.size()<=0){
            //???????????????????????????????????????
            Teachplan teachplan = new Teachplan();
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setPname(courseBase.getName());
            teachplan.setCourseid(courseId);
            teachplan.setStatus("0");
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        //???????????????id
        return teachplanList.get(0).getId();
    }

    //????????????????????????
    public QueryResponseResult findCourseList( int page, int size, CourseListRequest courseListRequest,String companyId){
        if (courseListRequest == null){
            courseListRequest = new CourseListRequest();
        }
        if (page < 0){
            page = 0;
        }
        if (size < 0){
            size = 10;
        }
        //??????????????????
        PageHelper.startPage(page,size);
        //??????ID
        courseListRequest.setCompanyId(companyId);
        //????????????
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);
        //????????????
        List<CourseInfo> result = courseListPage.getResult();
        //????????????
        long total = courseListPage.getTotal();

        //???????????????
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        queryResult.setList(result);
        queryResult.setTotal(total);

        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
    }

    //??????????????????
    @Transactional
    public AddCourseResult addCourseBase(CourseBase courseBase){
        courseBase.setStatus("202001");
        baseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }

    //????????????????????????
    public CourseBase getCourseBaseById(String courseId){
        return courseMapper.findCourseBaseById(courseId);
    }

    //????????????????????????
    @Transactional
    public ResponseResult UpdateCourseBase(String courseId, CourseBase courseBase){
        CourseBase one = this.getCourseBaseById(courseId);
        if (one == null){
            ExceptionCast.cast(CommonCode.INVALIDPARAM);
        }
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //??????????????????
    public CourseMarket getCourseMarketById(String courseId){
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    //??????????????????
    public CourseMarket UpdateCourseMarket(String courseId,CourseMarket courseMarket){
        CourseMarket market = this.getCourseMarketById(courseId);
        if (market != null){
            market.setCharge(courseMarket.getCharge());
            market.setValid(courseMarket.getValid());
            market.setQq(courseMarket.getQq());
            market.setPrice(courseMarket.getPrice());
            market.setStartTime(courseMarket.getStartTime());
            market.setEndTime(courseMarket.getEndTime());
            courseMarketRepository.save(market);
        }else {
            market = new CourseMarket();
            //???????????????????????????market??? ????????????????????????
            BeanUtils.copyProperties(courseMarket,market);
            courseMarketRepository.save(market);
        }
        return market;
    }

    //??????????????????
    @Transactional
    public ResponseResult saveCoursePic(String courseId,String pic){
        //??????????????????
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if (optional.isPresent()){
            coursePic = optional.get();
        }
        //????????????????????????
        if (coursePic == null){
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //??????????????????
    public CoursePic findCoursePic(String courseId){
        Optional<CoursePic> optionalPic = coursePicRepository.findById(courseId);
        if (optionalPic.isPresent()){
            return optionalPic.get();
        }
        return null;
    }

    //??????????????????
    @Transactional
    public ResponseResult deleteCoursePic(String courseId){
        long deleteByCourseId = coursePicRepository.deleteByCourseid(courseId);
        if (deleteByCourseId>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //??????????????????
    public CourseView getCourseView(String id){
        CourseView courseView = new CourseView();
        //??????????????????
        Optional<CourseBase> optionalBase = courseBaseRepository.findById(id);
        if (optionalBase.isPresent()){
            CourseBase courseBase = optionalBase.get();
            courseView.setCourseBase(courseBase);
        }
        //????????????
        Optional<CourseMarket> optionalMarket = courseMarketRepository.findById(id);
        if (optionalMarket.isPresent()){
            CourseMarket courseMarket = optionalMarket.get();
            courseView.setCourseMarket(courseMarket);
        }
        //????????????
        Optional<CoursePic> coursePic = coursePicRepository.findById(id);
        if (coursePic.isPresent()){
            CoursePic getCoursePic = coursePic.get();
            courseView.setCoursePic(getCoursePic);
        }
        //??????????????????
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    //??????id????????????????????????
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }

    //????????????
    public CoursePublishResult preview(String id) {
        CourseBase one = this.findCourseBaseById(id);
        //??????cms????????????
        //??????cmsPage??????
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//??????
        cmsPage.setTemplateId(publish_templateId);//??????
        cmsPage.setDataUrl(publish_dataUrlPre+id);//??????URL
        cmsPage.setPageWebPath(publish_page_webpath);//??????????????????
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//??????????????????
        cmsPage.setPageAliase(one.getName());//????????????
        cmsPage.setPageName(id+".html");


        //????????????cms
        CmsPageResult cmsPageResult = cmsPageClient.saveCmaPage(cmsPage);
        if (!cmsPageResult.isSuccess()){
            //????????????
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        CmsPage page = cmsPageResult.getCmsPage();
        String pageId = page.getPageId();
        //?????????????????????URL
        String pageUrl = previewUrl+pageId;
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    //????????????
    @Transactional
    public CoursePublishResult publish(String id) {
        //????????????
        CourseBase one = this.findCourseBaseById(id);
        //??????cmsPage??????
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//??????
        cmsPage.setTemplateId(publish_templateId);//??????
        cmsPage.setDataUrl(publish_dataUrlPre+id);//??????URL
        cmsPage.setPageWebPath(publish_page_webpath);//??????????????????
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//??????????????????
        cmsPage.setPageAliase(one.getName());//????????????
        cmsPage.setPageName(id+".html");
        //??????cms?????????????????????????????????????????????????????????
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if (!cmsPostPageResult.isSuccess()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //??????????????????????????????????????????
        CourseBase courseBase = this.saveCoursePubState(id);
        if (courseBase == null){
            return new  CoursePublishResult(CommonCode.FAIL,null);
        }
        //????????????????????????
        //???????????????CoursePub????????????????????????
        CoursePub coursePub = createCoursePub(id);
        //???CoursePub????????????????????????
        CoursePub coursePub1 = saveCoursePub(id, coursePub);

        //?????????????????????

        //????????????URL
        String pageUrl = cmsPostPageResult.getPageUrl();

        //???teachplanMediaPub???????????????????????????
        saveTeachplanMediaPub(id);

        return new  CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    //???teachplanMediaPub???????????????????????????
    private void saveTeachplanMediaPub(String courseId){
        //?????????????????????????????????
        List<TeachplanMedia> byCourseId = teachplanMediaRepository.findByCourseId(courseId);
        //?????????TeachplanMediaPub???????????????
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMediaPub> mediaPubList = new ArrayList<>();
        //??????????????????????????????TeachplanMediaPub
        for (TeachplanMedia media : byCourseId) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(media,teachplanMediaPub);
            //???????????????
            teachplanMediaPub.setTimestamp(new Date());
            mediaPubList.add(teachplanMediaPub);
        }

        //??????????????????????????????TeachplanMediaPub
        teachplanMediaPubRepository.saveAll(mediaPubList);

    }
    //???CoursePub????????????????????????

    private CoursePub saveCoursePub(String id,CoursePub coursePub){
        CoursePub coursePubNew = null;
        //?????????ID??????
        Optional<CoursePub> optionalPub = coursePubRepository.findById(id);
        if (optionalPub.isPresent()){
            coursePubNew = optionalPub.get();
        }else {
            coursePubNew = new CoursePub();
        }
        //?????????????????????coursePub?????????????????????????????????
        BeanUtils.copyProperties(coursePub,coursePubNew);
        coursePubNew.setId(id);
        //????????????logstash??????
        coursePubNew.setTimestamp(new Date());
        //????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(format);

        //??????
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }

    //??????coursePub??????
    private CoursePub createCoursePub(String id){
        CoursePub coursePub = new CoursePub();
        //??????????????????
        Optional<CourseBase> optionalCourseBase = courseBaseRepository.findById(id);
        if (optionalCourseBase.isPresent()){
            CourseBase courseBase = optionalCourseBase.get();
            BeanUtils.copyProperties(courseBase,coursePub);
        }

        //??????????????????
        Optional<CoursePic> optionalPic = coursePicRepository.findById(id);
        if (optionalPic.isPresent()){
            CoursePic coursePic = optionalPic.get();
            BeanUtils.copyProperties(coursePic,coursePub);
        }

        //??????????????????
        Optional<CourseMarket> optionalMarket = courseMarketRepository.findById(id);
        if (optionalMarket.isPresent()){
            CourseMarket courseMarket = optionalMarket.get();
            BeanUtils.copyProperties(courseMarket,coursePub);
        }

        //??????????????????
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String teach = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(teach);

        return coursePub;
    }

    //????????????????????????
    private CourseBase saveCoursePubState(String courseId){
        CourseBase courseBase = this.findCourseBaseById(courseId);
        //??????????????????
        courseBase.setStatus("202002");
        CourseBase save = courseBaseRepository.save(courseBase);
        return save;
    }

    //???????????????????????????????????????
    @Transactional
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CommonCode.INVALIDPARAM);
        }

        //????????????ID
        String teachplanId = teachplanMedia.getTeachplanId();

        //??????????????????
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if (!optional.isPresent()){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }
        //???????????????
        Teachplan teachplan = optional.get();
        //????????????????????????
        String grade = teachplan.getGrade();

        if (StringUtils.isEmpty(grade) || !grade.equals("3")){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }

        TeachplanMedia media = null;
        Optional<TeachplanMedia> teachplanMediaOptional = teachplanMediaRepository.findById(teachplanId);
        if (!teachplanMediaOptional.isPresent()){
            media = new TeachplanMedia();
        }else {
            media = teachplanMediaOptional.get();
        }
        //?????????????????????
        media.setTeachplanId(teachplanId);
        media.setCourseId(teachplanMedia.getCourseId());
        media.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        media.setMediaId(teachplanMedia.getMediaId());
        media.setMediaUrl(teachplanMedia.getMediaUrl());

        teachplanMediaRepository.save(media);

        return new ResponseResult(CommonCode.SUCCESS);

    }
}
