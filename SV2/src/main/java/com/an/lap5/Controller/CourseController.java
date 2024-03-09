package com.an.lap5.Controller;

import com.an.lap5.Models.Course;
import com.an.lap5.Service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping("/courses")
public class CourseController {

    RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private CourseService courseService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        if (course != null) {
            return ResponseEntity.ok(course);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course, HttpSession session) {
        if (session.getAttribute("token") == null) {
            // Nếu không có token, yêu cầu người dùng đăng nhập
            System.out.println("Token not found. Please login first.");
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter username: ");
            String username = scanner.nextLine();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                // Gửi yêu cầu đăng nhập và nhận token
                String response = restTemplate.postForObject("http://localhost:8080/login", requestEntity, String.class);
                // Lưu token vào session
                session.setAttribute("token", response);
                // Tiếp tục thực hiện tạo khóa học
                return createCourseAfterLogin(course);
            } catch (HttpClientErrorException.BadRequest ex) {
                System.out.println("Login failed: Bad Request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Trả về lỗi xác thực
            }
        }

        // Lấy token từ session
        String token = (String) session.getAttribute("token");

        // Tạo một HttpHeaders object và thêm token vào header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        return createCourseAfterLogin(course);
    }

    // Phương thức để tạo khóa học sau khi đã xác thực thành công
    private ResponseEntity<Course> createCourseAfterLogin(Course course) {
        Course createdCourse = courseService.saveCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        Course existingCourse = courseService.getCourseById(id);
        if (existingCourse != null) {
            course.setId(id);
            Course updatedCourse = courseService.saveCourse(course);
            return ResponseEntity.ok(updatedCourse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        Course existingCourse = courseService.getCourseById(id);
        if (existingCourse != null) {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
