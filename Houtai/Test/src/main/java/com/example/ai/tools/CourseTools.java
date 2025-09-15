package com.example.ai.tools;

import com.example.ai.entity.po.Course;
import com.example.ai.entity.po.CourseReservation;
import com.example.ai.entity.po.School;
import com.example.ai.entity.query.CourseQuery;
import com.example.ai.service.ICourseReservationService;
import com.example.ai.service.ICourseService;
import com.example.ai.service.ISchoolService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CourseTools {

    private final ICourseService courseService;
    private final ISchoolService schoolService;
    private final ICourseReservationService reservationService;

    @Tool(description = "根据条件查询课程")
    public List<Course> queryCourses(@ToolParam(description = "查询的条件") CourseQuery query) {
        if(query==null){
            return courseService.list();
        }
        val wrapper = courseService.query();
        wrapper
//                等于条件
                .eq(query.getType()!=null,"type", query.getType())
//                小于等于条件
                .le(query.getEdu()!=null,"edu", query.getEdu());
        if(query.getSorts()!=null&&query.getSorts().size()>0){
            for (CourseQuery.Sort sort : query.getSorts()) {
                wrapper.orderBy(true, sort.getAsc(),sort.getField());
            }
        }
        return wrapper.list();
    }

    @Tool(description = "查询所有校区")
    public List<School> querySchools() {
        return schoolService.list();
    }

    @Tool(description = "生成预约单,获取预约id")
    public Integer createCourseReservation(
           @ToolParam(description = "课程id")String course,
           @ToolParam(description = "预约校区")String school,
           @ToolParam(description = "学生姓名")String studentName,
           @ToolParam(description = "联系电话")String contactInfo,
           @ToolParam(description = "备注",required = false)String remark) {
        CourseReservation reservation = new CourseReservation();
        reservation.setCourse(course);
        reservation.setSchool(school);
        reservation.setStudentName(studentName);
        reservation.setContactInfo(contactInfo);
        reservation.setRemark(remark);
        reservationService.save(reservation);
        return reservation.getId();
    }
}
